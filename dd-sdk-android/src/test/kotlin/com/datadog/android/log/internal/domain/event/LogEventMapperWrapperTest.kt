/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.log.internal.domain.event

import android.util.Log
import com.datadog.android.event.EventMapper
import com.datadog.android.log.model.LogEvent
import com.datadog.android.utils.config.LoggerTestConfiguration
import com.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.annotations.TestConfigurationsProvider
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.util.Locale
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@ForgeConfiguration(Configurator::class)
internal class LogEventMapperWrapperTest {

    lateinit var testedEventMapper: LogEventMapperWrapper

    @Mock
    lateinit var mockWrappedEventMapper: EventMapper<LogEvent>

    @Mock
    lateinit var mockLogEvent: LogEvent

    @BeforeEach
    fun `set up`() {
        testedEventMapper = LogEventMapperWrapper(mockWrappedEventMapper)
    }

    @Test
    fun `M map and return the LogEvent W map`() {
        // GIVEN
        whenever(mockWrappedEventMapper.map(mockLogEvent)).thenReturn(mockLogEvent)

        // WHEN
        val mappedEvent = testedEventMapper.map(mockLogEvent)

        // THEN
        verify(mockWrappedEventMapper).map(mockLogEvent)
        Assertions.assertThat(mappedEvent).isEqualTo(mockLogEvent)
    }

    @Test
    fun `M return null if the mapped returned event is not the same instance W map`() {
        // GIVEN
        whenever(mockWrappedEventMapper.map(mockLogEvent)).thenReturn(mock())

        // WHEN
        val mappedEvent = testedEventMapper.map(mockLogEvent)

        // THEN
        verify(mockWrappedEventMapper).map(mockLogEvent)
        Assertions.assertThat(mappedEvent).isNull()
    }

    @Test
    fun `M log a warning if the mapped returned event is not the same instance W map`() {
        // GIVEN
        whenever(mockWrappedEventMapper.map(mockLogEvent)).thenReturn(mock())

        // WHEN
        testedEventMapper.map(mockLogEvent)

        // THEN
        verify(logger.mockDevLogHandler).handleLog(
            Log.WARN,
            LogEventMapperWrapper.NOT_SAME_EVENT_INSTANCE_WARNING_MESSAGE.format(
                Locale.US,
                mockLogEvent.toString()
            )
        )
    }

    @Test
    fun `M return null if the mapped returned event is null W map`() {
        // GIVEN
        whenever(mockWrappedEventMapper.map(mockLogEvent)).thenReturn(null)

        // WHEN
        val mappedEvent = testedEventMapper.map(mockLogEvent)

        // THEN
        verify(mockWrappedEventMapper).map(mockLogEvent)
        Assertions.assertThat(mappedEvent).isNull()
    }

    @Test
    fun `M log a warning if the mapped returned event is null W map`() {
        // GIVEN
        whenever(mockWrappedEventMapper.map(mockLogEvent)).thenReturn(null)

        // WHEN
        testedEventMapper.map(mockLogEvent)

        // THEN
        verify(logger.mockDevLogHandler).handleLog(
            Log.WARN,
            LogEventMapperWrapper.EVENT_NULL_WARNING_MESSAGE.format(
                Locale.US,
                mockLogEvent.toString()
            )
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
