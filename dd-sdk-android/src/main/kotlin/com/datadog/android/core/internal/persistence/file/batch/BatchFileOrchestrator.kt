/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.persistence.file.batch

import com.datadog.android.core.internal.persistence.file.FileOrchestrator
import com.datadog.android.core.internal.persistence.file.FilePersistenceConfig
import com.datadog.android.core.internal.persistence.file.canWriteSafe
import com.datadog.android.core.internal.persistence.file.deleteSafe
import com.datadog.android.core.internal.persistence.file.existsSafe
import com.datadog.android.core.internal.persistence.file.isFileSafe
import com.datadog.android.core.internal.persistence.file.lengthSafe
import com.datadog.android.core.internal.persistence.file.listFilesSafe
import com.datadog.android.core.internal.persistence.file.mkdirsSafe
import com.datadog.android.log.Logger
import com.datadog.android.log.internal.utils.errorWithTelemetry
import java.io.File
import java.io.FileFilter
import java.util.Locale

internal class BatchFileOrchestrator(
    private val rootDir: File,
    private val config: FilePersistenceConfig,
    private val internalLogger: Logger
) : FileOrchestrator {

    private val fileFilter = BatchFileFilter()

    // Offset the recent threshold for read and write to avoid conflicts
    // Arbitrary offset as ±5% of the threshold
    private val recentReadDelayMs = (config.recentDelayMs * 1.05).toLong()
    private val recentWriteDelayMs = (config.recentDelayMs * 0.95).toLong()

    // keep track of how many items were written in the last known file
    private var previousFile: File? = null
    private var previousFileItemCount: Int = 0

    // region FileOrchestrator

    override fun getWritableFile(dataSize: Int): File? {
        if (!isRootDirValid()) {
            return null
        }

        if (dataSize > config.maxItemSize) {
            internalLogger.errorWithTelemetry(
                ERROR_LARGE_DATA.format(
                    Locale.US,
                    dataSize,
                    config.maxItemSize
                )
            )
            return null
        }

        deleteObsoleteFiles()
        freeSpaceIfNeeded()

        val reusableFile = getReusableWritableFile(dataSize)

        return reusableFile ?: createNewFile()
    }

    override fun getReadableFile(excludeFiles: Set<File>): File? {
        if (!isRootDirValid()) {
            return null
        }

        deleteObsoleteFiles()

        val files = listSortedBatchFiles()

        return files.firstOrNull {
            (it !in excludeFiles) && !isFileRecent(it, recentReadDelayMs)
        }
    }

    override fun getAllFiles(): List<File> {
        if (!isRootDirValid()) {
            return emptyList()
        }

        return listSortedBatchFiles()
    }

    override fun getFlushableFiles(): List<File> {
        return getAllFiles()
    }

    override fun getRootDir(): File? {
        if (!isRootDirValid()) {
            return null
        }

        return rootDir
    }

    // endregion

    // region Internal

    @Suppress("LiftReturnOrAssignment")
    private fun isRootDirValid(): Boolean {
        if (rootDir.existsSafe()) {
            if (rootDir.isDirectory) {
                if (rootDir.canWriteSafe()) {
                    return true
                } else {
                    internalLogger.errorWithTelemetry(
                        ERROR_ROOT_NOT_WRITABLE.format(Locale.US, rootDir.path)
                    )
                    return false
                }
            } else {
                internalLogger.errorWithTelemetry(
                    ERROR_ROOT_NOT_DIR.format(Locale.US, rootDir.path)
                )
                return false
            }
        } else {
            synchronized(rootDir) {
                // double check if directory was already created by some other thread
                // entered this branch
                if (rootDir.existsSafe()) {
                    return true
                }

                if (rootDir.mkdirsSafe()) {
                    return true
                } else {
                    internalLogger.errorWithTelemetry(
                        ERROR_CANT_CREATE_ROOT.format(Locale.US, rootDir.path)
                    )
                    return false
                }
            }
        }
    }

    private fun createNewFile(): File {
        val newFileName = System.currentTimeMillis().toString()
        val newFile = File(rootDir, newFileName)
        previousFile = newFile
        previousFileItemCount = 1
        return newFile
    }

    private fun getReusableWritableFile(dataSize: Int): File? {
        val files = listSortedBatchFiles()
        val lastFile = files.lastOrNull() ?: return null

        val lastKnownFile = previousFile
        val lastKnownFileItemCount = previousFileItemCount
        if (lastKnownFile != lastFile) {
            // this situation can happen because:
            // 1. `lastFile` is a file written during a previous session
            // 2. `lastFile` was created by another system/process
            // 3. `lastKnownFile` was deleted
            // In any case, we don't know the item count, so to be safe, we create a new file
            return null
        }

        val isRecentEnough = isFileRecent(lastFile, recentWriteDelayMs)
        val hasRoomForMore = (lastFile.lengthSafe() + dataSize) < config.maxBatchSize
        val hasSlotForMore = (lastKnownFileItemCount < config.maxItemsPerBatch)

        return if (isRecentEnough && hasRoomForMore && hasSlotForMore) {
            previousFileItemCount = lastKnownFileItemCount + 1
            lastFile
        } else {
            null
        }
    }

    private fun isFileRecent(file: File, delayMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val fileTimestamp = file.name.toLongOrNull() ?: 0L
        return fileTimestamp >= (now - delayMs)
    }

    private fun deleteObsoleteFiles() {
        val files = listSortedBatchFiles()
        val threshold = System.currentTimeMillis() - config.oldFileThreshold
        files
            .asSequence()
            .filter { (it.name.toLongOrNull() ?: 0) < threshold }
            .forEach { it.deleteSafe() }
    }

    private fun freeSpaceIfNeeded() {
        val files = listSortedBatchFiles()
        val sizeOnDisk = files.sumOf { it.lengthSafe() }
        val maxDiskSpace = config.maxDiskSpace
        val sizeToFree = sizeOnDisk - maxDiskSpace
        if (sizeToFree > 0) {
            internalLogger.errorWithTelemetry(
                ERROR_DISK_FULL.format(Locale.US, sizeOnDisk, maxDiskSpace, sizeToFree)
            )
            files.fold(sizeToFree) { remainingSizeToFree, file ->
                if (remainingSizeToFree > 0) {
                    val fileSize = file.lengthSafe()
                    if (file.deleteSafe()) {
                        remainingSizeToFree - fileSize
                    } else {
                        remainingSizeToFree
                    }
                } else {
                    remainingSizeToFree
                }
            }
        }
    }

    private fun listSortedBatchFiles(): List<File> {
        return rootDir.listFilesSafe(fileFilter).orEmpty().sorted()
    }

    // endregion

    // region FileFilter

    internal class BatchFileFilter : FileFilter {
        override fun accept(file: File?): Boolean {
            return file != null &&
                file.isFileSafe() &&
                file.name.matches(batchFileNameRegex)
        }
    }

    // endregion

    companion object {
        private val batchFileNameRegex = Regex("\\d+")
        internal const val ERROR_ROOT_NOT_WRITABLE = "The provided root dir is not writable: %s"
        internal const val ERROR_ROOT_NOT_DIR = "The provided root file is not a directory: %s"
        internal const val ERROR_CANT_CREATE_ROOT = "The provided root file can't be created: %s"
        internal const val ERROR_LARGE_DATA = "Can't write data with size %d (max item size is %d)"
        internal const val ERROR_DISK_FULL = "Too much disk space used (%d/%d): " +
            "cleaning up to free %d bytes…"
    }
}
