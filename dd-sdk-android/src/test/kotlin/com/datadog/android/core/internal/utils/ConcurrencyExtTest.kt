/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.utils

import com.datadog.android.log.internal.utils.ERROR_WITH_TELEMETRY_LEVEL
import com.datadog.android.utils.config.LoggerTestConfiguration
import com.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.annotations.TestConfigurationsProvider
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.LongForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
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
internal class ConcurrencyExtTest {

    @Test
    fun `M execute task W executeSafe()`(
        @StringForgery name: String
    ) {
        // Given
        val service: ExecutorService = mock()
        val runnable: Runnable = mock()
        doNothing().whenever(service).execute(runnable)

        // When
        service.executeSafe(name, runnable)

        // Then
        verify(service).execute(runnable)
    }

    @Test
    fun `M not throw W executeSafe() {rejected exception}`(
        @StringForgery name: String,
        @StringForgery message: String
    ) {
        // Given
        val service: ExecutorService = mock()
        val runnable: Runnable = mock()
        val exception = RejectedExecutionException(message)
        doThrow(exception).whenever(service).execute(runnable)

        // When
        service.executeSafe(name, runnable)

        // Then
        verify(service).execute(runnable)
        verify(logger.mockSdkLogHandler).handleLog(
            ERROR_WITH_TELEMETRY_LEVEL,
            "Unable to schedule $name task on the executor",
            exception
        )
    }

    @Test
    fun `M schedule task W scheduleSafe()`(
        @StringForgery name: String,
        @LongForgery delay: Long,
        @Forgery unit: TimeUnit
    ) {
        // Given
        val service: ScheduledExecutorService = mock()
        val runnable: Runnable = mock()
        val future: ScheduledFuture<*> = mock()
        whenever(service.schedule(runnable, delay, unit)) doReturn future

        // When
        val result: Any? = service.scheduleSafe(name, delay, unit, runnable)

        // Then
        assertThat(result).isSameAs(future)
        verify(service).schedule(runnable, delay, unit)
    }

    @Test
    fun `M not throw W scheduleSafe() {rejected exception}`(
        @StringForgery name: String,
        @LongForgery delay: Long,
        @Forgery unit: TimeUnit,
        @StringForgery message: String
    ) {
        // Given
        val service: ScheduledExecutorService = mock()
        val runnable: Runnable = mock()
        val exception = RejectedExecutionException(message)
        doThrow(exception).whenever(service).schedule(runnable, delay, unit)

        // When
        val result: Any? = service.scheduleSafe(name, delay, unit, runnable)

        // Then
        assertThat(result).isNull()
        verify(service).schedule(runnable, delay, unit)
        verify(logger.mockSdkLogHandler).handleLog(
            ERROR_WITH_TELEMETRY_LEVEL,
            "Unable to schedule $name task on the executor",
            exception
        )
    }

    companion object {
        val logger = LoggerTestConfiguration()

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(logger)
        }
    }
}
