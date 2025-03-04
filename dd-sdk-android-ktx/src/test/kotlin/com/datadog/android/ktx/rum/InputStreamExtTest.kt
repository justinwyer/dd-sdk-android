/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.ktx.rum

import com.datadog.android.rum.resource.RumResourceInputStream
import com.datadog.tools.unit.forge.BaseConfigurator
import com.nhaarman.mockitokotlin2.mock
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.InputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(
        MockitoExtension::class,
        ForgeExtension::class
    )
)
@ForgeConfiguration(value = BaseConfigurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InputStreamExtTest {

    @Test
    fun `M wrap inputStream W asRumResource()`(
        @StringForgery url: String
    ) {
        // Given
        val mockIS: InputStream = mock()

        // When
        val result = mockIS.asRumResource(url)

        // Then
        assertThat(result).isInstanceOf(RumResourceInputStream::class.java)
        val rumRIS = result as RumResourceInputStream
        assertThat(rumRIS.delegate).isSameAs(mockIS)
        assertThat(rumRIS.url).isEqualTo(url)
    }
}
