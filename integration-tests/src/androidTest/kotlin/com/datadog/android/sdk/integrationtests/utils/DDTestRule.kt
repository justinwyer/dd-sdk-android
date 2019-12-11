package com.datadog.android.sdk.integrationtests.utils

import android.app.Activity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.datadog.android.Datadog
import com.datadog.android.log.EndpointUpdateStrategy
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.nio.charset.StandardCharsets
import java.util.*

class DDTestRule<T : Activity>(activityClass: Class<T>) : ActivityTestRule<T>(activityClass) {
    private val mockWebServer: MockWebServer = MockWebServer()
    val requestObjects = LinkedList<JsonObject>()

    override fun beforeActivityLaunched() {
        mockWebServer.apply {
            start()
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val jsonArray =
                        JsonParser.parseString(request.body.readString(StandardCharsets.UTF_8))
                            .asJsonArray
                    jsonArray.forEach {
                        requestObjects.add(it.asJsonObject)
                    }
                    return mockResponse(200)
                }
            }
        }
        val fakeEndpoint = mockWebServer.url("/").toString().removeSuffix("/")
        Datadog.setEndpointUrl(fakeEndpoint, EndpointUpdateStrategy.DISCARD_OLD_LOGS)
        super.beforeActivityLaunched()
    }

    override fun afterActivityFinished() {
        mockWebServer.shutdown()
        // clean all logs
        requestObjects.clear()
        InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
        super.afterActivityFinished()
    }

    private fun mockResponse(code: Int): MockResponse {
        return MockResponse()
            .setResponseCode(code)
            .setBody("{}")
    }

}