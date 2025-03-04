/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.resource

import com.datadog.android.rum.RumErrorSource
import com.datadog.android.rum.RumResourceKind
import com.datadog.android.rum.internal.domain.event.ResourceTiming
import com.datadog.android.utils.config.GlobalRumMonitorTestConfiguration
import com.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.annotations.TestConfigurationsProvider
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.LongForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.system.measureNanoTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class RumResourceInputStreamTest {

    lateinit var testedInputStream: RumResourceInputStream

    @Mock
    lateinit var mockInputStream: InputStream

    @StringForgery
    lateinit var fakeUrl: String

    @StringForgery
    lateinit var fakeMessage: String

    @BeforeEach
    fun `set up`() {
        testedInputStream = RumResourceInputStream(mockInputStream, fakeUrl)

        // 𝕄 start resource 𝕎 init
        verify(rumMonitor.mockInstance).startResource(
            testedInputStream.key,
            RumResourceInputStream.METHOD,
            fakeUrl,
            emptyMap()
        )
        verify(rumMonitor.mockInstance).waitForResourceTiming(testedInputStream.key)
    }

    // region Atomic Methods

    @Test
    fun `𝕄 return byte 𝕎 read()`(
        @IntForgery(-128, 127) byte: Int
    ) {
        // Given
        whenever(mockInputStream.read()) doReturn byte

        // When
        val result = testedInputStream.read()

        // Then
        assertThat(result).isEqualTo(byte)
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 fill byte array 𝕎 read(array)`(
        @StringForgery text: String
    ) {
        // Given
        val input = text.toByteArray()
        val length = min(8, input.size)
        val byteArray = ByteArray(length)
        whenever(mockInputStream.read(byteArray)) doAnswer {
            val dest = it.arguments.first() as ByteArray
            System.arraycopy(input, 0, dest, 0, dest.size)
            dest.size
        }

        // When
        val count = testedInputStream.read(byteArray)

        // Then
        assertThat(count).isEqualTo(length)
        val expectedResult = input.take(length).toByteArray()
        assertThat(byteArray).isEqualTo(expectedResult)
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 fill byte array 𝕎 read(array, offset, length)`(
        @StringForgery text: String,
        @IntForgery(min = 4, max = 16) offset: Int
    ) {
        // Given
        val input = text.toByteArray()
        val length = min(8, input.size)
        val byteArray = ByteArray(length + offset) { 0x20 }
        whenever(mockInputStream.read(byteArray, offset, length)) doAnswer {
            val dest = it.arguments[0] as ByteArray
            val off = it.arguments[1] as Int
            val len = it.arguments[2] as Int
            System.arraycopy(input, 0, dest, off, len)
            len
        }

        // When
        val count = testedInputStream.read(byteArray, offset, length)

        // Then
        assertThat(count).isEqualTo(length)
        val expectedResult = ByteArray(length + offset) { 0x20 }
        System.arraycopy(input, 0, expectedResult, offset, length)
        assertThat(byteArray).isEqualTo(expectedResult)
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 return available bytes 𝕎 available()`(
        @IntForgery(min = 4, max = 16) available: Int
    ) {
        // Given
        whenever(mockInputStream.available()) doReturn available

        // When
        val result = testedInputStream.available()

        // Then
        assertThat(result).isEqualTo(available)
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 skip bytes on delegate 𝕎 skip()`(
        @LongForgery(0, 1024) n: Long,
        @LongForgery(0, 1024) skipped: Long
    ) {
        // Given
        whenever(mockInputStream.skip(n)) doReturn skipped

        // When
        val result = testedInputStream.skip(n)

        // Then
        assertThat(result).isEqualTo(skipped)
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 return mark supported 𝕎 markSupported()`(
        @BoolForgery markSupported: Boolean
    ) {
        // Given
        whenever(mockInputStream.markSupported()) doReturn markSupported

        // When
        val result = testedInputStream.markSupported()

        // Then
        assertThat(result).isEqualTo(markSupported)
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 mark position on delegate 𝕎 mark()`(
        @IntForgery(0, 1024) readlimit: Int
    ) {
        // When
        testedInputStream.mark(readlimit)

        // Then
        verify(mockInputStream).mark(readlimit)
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 reset delegate 𝕎 reset()`() {
        // When
        testedInputStream.reset()

        // Then
        verify(mockInputStream).reset()
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 close delegate and stop Resource 𝕎 close()`() {
        // When
        testedInputStream.close()

        // Then
        verify(rumMonitor.mockInstance).addResourceTiming(
            eq(testedInputStream.key),
            any()
        )
        verify(rumMonitor.mockInstance).stopResource(
            testedInputStream.key,
            null,
            0L,
            RumResourceKind.OTHER,
            emptyMap()
        )
        verify(mockInputStream).close()
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    // endregion

    // region Failing Atomic Methods

    @Test
    fun `𝕄 send error 𝕎 read() with throwable`() {
        // Given
        whenever(mockInputStream.read()) doThrow IOException(fakeMessage)

        // When
        val throwable = assertThrows<IOException>(fakeMessage) {
            testedInputStream.read()
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_READ,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error 𝕎 read(array) with throwable`(
        @IntForgery(32, 128) length: Int
    ) {
        // Given
        val byteArray = ByteArray(length)
        whenever(mockInputStream.read(byteArray)) doThrow IOException(fakeMessage)

        // When
        val throwable = assertThrows<IOException>(fakeMessage) {
            testedInputStream.read(byteArray)
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_READ,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error 𝕎 read(array, offset, length) with throwable`(
        @IntForgery(min = 4, max = 16) offset: Int,
        @IntForgery(32, 128) length: Int
    ) {
        // Given
        val byteArray = ByteArray(length + offset)
        whenever(mockInputStream.read(byteArray, offset, length)) doThrow IOException(fakeMessage)

        // When
        val throwable = assertThrows<IOException>(fakeMessage) {
            testedInputStream.read(byteArray, offset, length)
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_READ,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error bytes 𝕎 available() with throwable`() {
        // Given
        whenever(mockInputStream.available()) doThrow IOException(fakeMessage)

        // When
        val throwable = assertThrows<IOException>(fakeMessage) {
            testedInputStream.available()
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_READ,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error 𝕎 skip() with throwable`(
        @LongForgery(0, 1024) n: Long
    ) {
        // Given
        whenever(mockInputStream.skip(n)) doThrow IOException(fakeMessage)

        // When
        val throwable = assertThrows<IOException>(fakeMessage) {
            testedInputStream.skip(n)
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_SKIP,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error 𝕎 markSupported() with throwable`() {
        // Given
        whenever(mockInputStream.markSupported()) doThrow RuntimeException(fakeMessage)

        // When
        val throwable = assertThrows<RuntimeException>(fakeMessage) {
            testedInputStream.markSupported()
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_READ,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error 𝕎 mark() with throwable`(
        @IntForgery(0, 1024) readlimit: Int
    ) {
        // Given
        whenever(mockInputStream.mark(readlimit)) doThrow RuntimeException(fakeMessage)

        // When
        val throwable = assertThrows<RuntimeException>(fakeMessage) {
            testedInputStream.mark(readlimit)
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_MARK,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error 𝕎 reset() with throwable`() {
        // Given
        whenever(mockInputStream.reset()) doThrow IOException(fakeMessage)

        // When
        val throwable = assertThrows<IOException>(fakeMessage) {
            testedInputStream.reset()
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_RESET,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error 𝕎 close() with throwable`() {
        // Given
        whenever(mockInputStream.close()) doThrow IOException(fakeMessage)

        // When
        val throwable = assertThrows<Throwable>(fakeMessage) {
            testedInputStream.close()
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_CLOSE,
            RumErrorSource.SOURCE,
            throwable
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 send error only once 𝕎 read() + close() with throwable`() {
        // Given
        val message2 = "again but not $fakeMessage"
        whenever(mockInputStream.read()) doThrow IOException(fakeMessage)
        whenever(mockInputStream.close()) doThrow IOException(message2)

        // When
        val throwable1 = assertThrows<Throwable>(fakeMessage) {
            testedInputStream.read()
        }

        // Then
        verify(rumMonitor.mockInstance).stopResourceWithError(
            testedInputStream.key,
            null,
            RumResourceInputStream.ERROR_READ,
            RumErrorSource.SOURCE,
            throwable1
        )
        verifyNoMoreInteractions(rumMonitor.mockInstance)
    }

    // endregion

    // region Scenarios

    @Test
    fun `𝕄 register resource 𝕎 read() + close() {bufferedReader}`(
        @StringForgery content: String
    ) {
        // Given
        val contentBytes = content.toByteArray()
        val inputStream = contentBytes.inputStream()
        testedInputStream = RumResourceInputStream(inputStream, fakeUrl)

        // When
        val result = testedInputStream.bufferedReader().use(BufferedReader::readText)

        // Then
        assertThat(result).isEqualTo(content)
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                testedInputStream.key,
                RumResourceInputStream.METHOD,
                fakeUrl,
                emptyMap()
            )
            verify(rumMonitor.mockInstance).waitForResourceTiming(testedInputStream.key)
            verify(rumMonitor.mockInstance).addResourceTiming(eq(testedInputStream.key), any())
            verify(rumMonitor.mockInstance).stopResource(
                testedInputStream.key,
                null,
                contentBytes.size.toLong(),
                RumResourceKind.OTHER,
                emptyMap()
            )
            verifyNoMoreInteractions(rumMonitor.mockInstance)
        }
    }

    @Test
    fun `𝕄 register resource 𝕎 read() + close() {manual}`(
        @StringForgery content: String
    ) {
        // Given
        val contentBytes = content.toByteArray()
        val inputStream = contentBytes.inputStream()
        testedInputStream = RumResourceInputStream(inputStream, fakeUrl)

        // When
        val result = testedInputStream.use {
            val byteArray = ByteArray(1024)
            var i = 0
            var b: Int
            do {
                b = it.read()
                if (b >= 0) {
                    byteArray[i] = b.toByte()
                    i++
                }
            } while (b >= 0)
            String(byteArray.take(i).toByteArray())
        }

        // Then
        assertThat(result).isEqualTo(content)
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                testedInputStream.key,
                RumResourceInputStream.METHOD,
                fakeUrl,
                emptyMap()
            )
            verify(rumMonitor.mockInstance).waitForResourceTiming(testedInputStream.key)
            verify(rumMonitor.mockInstance).addResourceTiming(eq(testedInputStream.key), any())
            verify(rumMonitor.mockInstance).stopResource(
                testedInputStream.key,
                null,
                contentBytes.size.toLong(),
                RumResourceKind.OTHER,
                emptyMap()
            )
            verifyNoMoreInteractions(rumMonitor.mockInstance)
        }
    }

    @Test
    fun `𝕄 register resource 𝕎 read() + mark() + reset() + close() {manual}`(
        forge: Forge
    ) {
        // Given
        val content = forge.anAlphabeticalString(size = forge.anInt(16, 1024))
        val contentBytes = content.toByteArray()
        val inputStream = contentBytes.inputStream()
        testedInputStream = RumResourceInputStream(inputStream, fakeUrl)
        val mark = contentBytes.size / 3
        val len1 = mark
        val len2 = contentBytes.size - (len1 + mark)

        // When
        val result = testedInputStream.use {
            val byteArray = ByteArray(len1 + len1 + len1 + len2)
            it.read(byteArray, 0, len1)
            it.mark(mark)
            it.read(byteArray, mark, len1)
            it.reset()
            it.read(byteArray, len1 + mark, len1)
            it.read(byteArray, len1 + mark + mark, len2)
            String(byteArray.take(len1 + len1 + len1 + len2).toByteArray())
        }

        // Then
        val duplicated = content.drop(len1).take(mark)
        val expectedResult = content.take(len1) + duplicated + duplicated + content.takeLast(len2)
        assertThat(result).isEqualTo(expectedResult)
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                testedInputStream.key,
                RumResourceInputStream.METHOD,
                fakeUrl,
                emptyMap()
            )
            verify(rumMonitor.mockInstance).waitForResourceTiming(testedInputStream.key)
            verify(rumMonitor.mockInstance).addResourceTiming(eq(testedInputStream.key), any())
            verify(rumMonitor.mockInstance).stopResource(
                testedInputStream.key,
                null,
                (len1 + mark + mark + len2).toLong(),
                RumResourceKind.OTHER,
                emptyMap()
            )
            verifyNoMoreInteractions(rumMonitor.mockInstance)
        }
    }

    @Test
    fun `𝕄 register resource 𝕎 read() + skip() + close() {manual}`(
        forge: Forge
    ) {
        // Given
        val content = forge.anAlphabeticalString(size = forge.anInt(16, 1024))
        val contentBytes = content.toByteArray()
        val inputStream = contentBytes.inputStream()
        testedInputStream = RumResourceInputStream(inputStream, fakeUrl)
        val skipped = contentBytes.size / 3
        val len1 = skipped
        val len2 = contentBytes.size - (len1 + skipped)

        // When
        val result = testedInputStream.use {
            val byteArray = ByteArray(len1 + len2)
            it.read(byteArray, 0, len1)
            it.skip(skipped.toLong())
            it.read(byteArray, len1, len2)
            String(byteArray.take(len1 + len2).toByteArray())
        }

        // Then
        val expectedResult = content.take(len1) + content.takeLast(len2)
        assertThat(result).isEqualTo(expectedResult)
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                testedInputStream.key,
                RumResourceInputStream.METHOD,
                fakeUrl,
                emptyMap()
            )
            verify(rumMonitor.mockInstance).waitForResourceTiming(testedInputStream.key)
            verify(rumMonitor.mockInstance).addResourceTiming(eq(testedInputStream.key), any())
            verify(rumMonitor.mockInstance).stopResource(
                testedInputStream.key,
                null,
                (len1 + len2).toLong(),
                RumResourceKind.OTHER,
                emptyMap()
            )
            verifyNoMoreInteractions(rumMonitor.mockInstance)
        }
    }

    @Test
    fun `𝕄 register resource with timing 𝕎 read() + close() {bufferedReader}`(
        @StringForgery content: String
    ) {
        // Given
        val contentBytes = content.toByteArray()
        val inputStream = contentBytes.inputStream()
        testedInputStream = RumResourceInputStream(inputStream, fakeUrl)
        Thread.sleep(500)
        var download: Long

        // When
        val result = testedInputStream.bufferedReader().use {
            var text: String
            download = measureNanoTime {
                text = it.readText()
            }
            Thread.sleep(500)
            text
        }

        // Then
        assertThat(result).isEqualTo(content)
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                testedInputStream.key,
                RumResourceInputStream.METHOD,
                fakeUrl,
                emptyMap()
            )
            verify(rumMonitor.mockInstance).waitForResourceTiming(testedInputStream.key)
            argumentCaptor<ResourceTiming> {
                verify(rumMonitor.mockInstance).addResourceTiming(
                    eq(testedInputStream.key),
                    capture()
                )
                assertThat(firstValue.connectDuration).isEqualTo(0L)
                assertThat(firstValue.dnsDuration).isEqualTo(0L)
                assertThat(firstValue.sslDuration).isEqualTo(0L)
                assertThat(firstValue.firstByteDuration).isEqualTo(0L)
                assertThat(firstValue.downloadStart).isGreaterThan(
                    TimeUnit.MILLISECONDS.toNanos(500)
                )
                assertThat(firstValue.downloadDuration).isLessThanOrEqualTo(download)
            }
            verify(rumMonitor.mockInstance).stopResource(
                testedInputStream.key,
                null,
                contentBytes.size.toLong(),
                RumResourceKind.OTHER,
                emptyMap()
            )
            verifyNoMoreInteractions(rumMonitor.mockInstance)
        }
    }

    // endregion

    companion object {
        val rumMonitor = GlobalRumMonitorTestConfiguration()

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(rumMonitor)
        }
    }
}
