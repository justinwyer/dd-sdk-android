/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.log.internal.net

import com.datadog.android.core.internal.net.DataOkHttpUploaderV2
import com.datadog.android.core.internal.net.DataOkHttpUploaderV2Test
import com.datadog.android.log.Logger
import com.datadog.android.utils.forge.Configurator
import com.nhaarman.mockitokotlin2.mock
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import okhttp3.Call
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class LogsOkHttpUploaderV2Test : DataOkHttpUploaderV2Test<LogsOkHttpUploaderV2>() {

    override fun buildTestedInstance(callFactory: Call.Factory): LogsOkHttpUploaderV2 {
        return LogsOkHttpUploaderV2(
            fakeEndpoint,
            fakeClientToken,
            fakeSource,
            fakeSdkVersion,
            callFactory,
            mockAndroidInfoProvider,
            Logger(mock())
        )
    }

    override fun expectedPath(): String {
        return "/api/v2/logs"
    }

    override fun expectedQueryParams(): Map<String, String> {
        return mapOf(
            DataOkHttpUploaderV2.QUERY_PARAM_SOURCE to fakeSource
        )
    }
}
