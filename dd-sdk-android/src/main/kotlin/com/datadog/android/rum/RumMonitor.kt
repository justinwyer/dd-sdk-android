/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.annotation.FloatRange
import androidx.fragment.app.Fragment
import com.datadog.android.Datadog
import com.datadog.android.core.internal.CoreFeature
import com.datadog.android.core.internal.sampling.RateBasedSampler
import com.datadog.android.core.internal.utils.devLogger
import com.datadog.android.rum.internal.RumFeature
import com.datadog.android.rum.internal.domain.event.RumEventSourceProvider
import com.datadog.android.rum.internal.monitor.DatadogRumMonitor
import com.datadog.android.telemetry.internal.TelemetryEventHandler
import com.datadog.tools.annotation.NoOpImplementation

/**
 *  A class enabling Datadog RUM features.
 *
 *  It allows you to record User events that can be explored and analyzed in Datadog Dashboards.
 *
 *  You can only have one active RumMonitor, and should register/retrieve it from the [GlobalRum] object.
 */
@Suppress("ComplexInterface", "TooManyFunctions")
@NoOpImplementation
interface RumMonitor {

    /**
     * Notifies that a View is being shown to the user, linked with the [key] instance.
     * @param key the instance that represents the active view (usually your
     * [Activity] or [Fragment] instance).
     * @param name the name of the view (usually your [Activity] or [Fragment] full class name)
     * @param attributes additional custom attributes to attach to the view. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [stopView]
     */
    fun startView(
        key: Any,
        name: String,
        attributes: Map<String, Any?> = emptyMap()
    )

    /**
     * Stops a previously started View, linked with the [key] instance.
     * @param key the instance that represents the active view (usually your
     * [Activity] or [Fragment] instance).
     * @param attributes additional custom attributes to attach to the view. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [startView]
     */
    fun stopView(
        key: Any,
        attributes: Map<String, Any?> = emptyMap()
    )

    /**
     * Notifies that a User Action happened.
     * This is used to track discrete user actions (e.g.: tap).
     * @param type the action type
     * @param name the action identifier
     * @param attributes additional custom attributes to attach to the action. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [startUserAction]
     * @see [stopUserAction]
     */
    fun addUserAction(
        type: RumActionType,
        name: String,
        attributes: Map<String, Any?>
    )

    /**
     * Notifies that a User Action started.
     * This is used to track long running user actions (e.g.: scroll). Such a user action must
     * be stopped with [stopUserAction], and will be stopped automatically if it lasts more than
     * 10 seconds.
     * @param type the action type
     * @param name the action identifier
     * @param attributes additional custom attributes to attach to the action. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [stopUserAction]
     * @see [addUserAction]
     */
    fun startUserAction(
        type: RumActionType,
        name: String,
        attributes: Map<String, Any?>
    )

    /**
     * Notifies that a User Action stopped.
     * This is used to stop tracking long running user actions (e.g.: scroll), started
     * with [startUserAction].
     * @param attributes additional custom attributes to attach to the action
     * @see [addUserAction]
     * @see [startUserAction]
     * @deprecated Use [stopUserAction] with name parameter instead.
     */
    @Deprecated(
        "This method is deprecated. Please" +
            " use RumMonitor#stopUserAction(type, name, attributes) instead"
    )
    fun stopUserAction(
        attributes: Map<String, Any?> = emptyMap()
    )

    /**
     * Notifies that a User Action stopped, and update the action's type and name.
     * This is used to stop tracking long running user actions (e.g.: scroll), started
     * with [startUserAction].
     * @param type the action type (overriding the last started action)
     * @param name the action identifier (overriding the last started action)
     * @param attributes additional custom attributes to attach to the action. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [addUserAction]
     * @see [startUserAction]
     */
    fun stopUserAction(
        type: RumActionType,
        name: String,
        attributes: Map<String, Any?> = emptyMap()
    )

    /**
     * Notify that a new Resource is being loaded, linked with the [key] instance.
     * @param key the instance that represents the resource being loaded (usually your
     * request or network call instance).
     * @param method the method used to load the resource (E.g., for network: "GET" or "POST")
     * @param url the url or local path of the resource being loaded
     * @param attributes additional custom attributes to attach to the resource. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [stopResource]
     * @see [stopResourceWithError]
     */
    fun startResource(
        key: String,
        method: String,
        url: String,
        attributes: Map<String, Any?> = emptyMap()
    )

    /**
     * Stops a previously started Resource, linked with the [key] instance.
     * @param key the instance that represents the active view (usually your
     * request or network call instance).
     * @param statusCode the status code of the resource (if any)
     * @param size the size of the resource, in bytes
     * @param kind the type of resource loaded
     * @param attributes additional custom attributes to attach to the resource. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [startResource]
     * @see [stopResourceWithError]
     */
    fun stopResource(
        key: String,
        statusCode: Int?,
        size: Long?,
        kind: RumResourceKind,
        attributes: Map<String, Any?>
    )

    /**
     * Stops a previously started Resource that failed loading, linked with the [key] instance.
     * @param key the instance that represents the active view (usually your
     * request or network call instance).
     * @param statusCode the status code of the resource (if any)
     * @param message a message explaining the error
     * @param source the source of the error
     * @param throwable the throwable
     * @param attributes additional custom attributes to attach to the error. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [startResource]
     * @see [stopResource]
     */
    fun stopResourceWithError(
        key: String,
        statusCode: Int?,
        message: String,
        source: RumErrorSource,
        throwable: Throwable,
        attributes: Map<String, Any?> = emptyMap()
    )

    /**
     * Stops a previously started Resource that failed loading, linked with the [key] instance by
     * providing the intercepted stacktrace.
     * Note: This method should only be used from hybrid application.
     * @param key the instance that represents the active view (usually your
     * request or network call instance).
     * @param statusCode the status code of the resource (if any)
     * @param message a message explaining the error
     * @param source the source of the error
     * @param stackTrace the error stacktrace
     * @param errorType the type of the error. Usually it should be the canonical name of the
     * of the Exception class.
     * @param attributes additional custom attributes to attach to the error. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     * @see [startResource]
     * @see [stopResource]
     */
    @SuppressWarnings("LongParameterList")
    fun stopResourceWithError(
        key: String,
        statusCode: Int?,
        message: String,
        source: RumErrorSource,
        stackTrace: String,
        errorType: String?,
        attributes: Map<String, Any?> = emptyMap()
    )

    /**
     * Notifies that an error occurred in the active View.
     * @param message a message explaining the error
     * @param source the source of the error
     * @param throwable the throwable
     * @param attributes additional custom attributes to attach to the error. Attributes can be
     * nested up to 9 levels deep. Keys using more than 9 levels will be sanitized by SDK.
     */
    fun addError(
        message: String,
        source: RumErrorSource,
        throwable: Throwable?,
        attributes: Map<String, Any?>
    )

    /**
     * Notifies that an error occurred in the active View.
     * @param message a message explaining the error
     * @param source the source of the error
     * @param stacktrace the error stacktrace information
     * @param attributes additional custom attributes to attach to the error
     */
    fun addErrorWithStacktrace(
        message: String,
        source: RumErrorSource,
        stacktrace: String?,
        attributes: Map<String, Any?>
    )

    /**
     * Adds a specific timing in the active View. The timing duration will be computed as the
     * difference between the time the View was started and the time this function was called.
     * @param name the name of the new custom timing attribute. Timings can be
     * nested up to 8 levels deep. Names using more than 8 levels will be sanitized by SDK.
     */
    fun addTiming(
        name: String
    )

    // region Builder

    /**
     * A Builder class for a [RumMonitor].
     */
    class Builder {

        private var samplingRate: Float = RumFeature.samplingRate
        private var sessionListener: RumSessionListener? = null

        /**
         * Sets the sampling rate for RUM Sessions.
         *
         * @param samplingRate the sampling rate must be a value between 0 and 100. A value of 0
         * means no RUM event will be sent, 100 means all sessions will be kept.
         */
        fun sampleRumSessions(@FloatRange(from = 0.0, to = 100.0) samplingRate: Float): Builder {
            this.samplingRate = samplingRate
            return this
        }

        /**
         * Sets the Session listener.
         * @param sessionListener the listener to notify whenever a new Session starts.
         */
        fun setSessionListener(sessionListener: RumSessionListener): Builder {
            this.sessionListener = sessionListener
            return this
        }

        /**
         * Builds a [RumMonitor] based on the current state of this Builder.
         */
        fun build(): RumMonitor {
            val rumApplicationId = CoreFeature.rumApplicationId
            return if (!RumFeature.isInitialized()) {
                devLogger.e(
                    RUM_NOT_ENABLED_ERROR_MESSAGE + "\n" +
                        Datadog.MESSAGE_SDK_INITIALIZATION_GUIDE
                )
                NoOpRumMonitor()
            } else if (rumApplicationId.isNullOrBlank()) {
                devLogger.e(INVALID_APPLICATION_ID_ERROR_MESSAGE)
                NoOpRumMonitor()
            } else {
                DatadogRumMonitor(
                    applicationId = rumApplicationId,
                    samplingRate = samplingRate,
                    writer = RumFeature.persistenceStrategy.getWriter(),
                    handler = Handler(Looper.getMainLooper()),
                    telemetryEventHandler = TelemetryEventHandler(
                        CoreFeature.serviceName,
                        CoreFeature.sdkVersion,
                        RumEventSourceProvider(CoreFeature.sourceName),
                        CoreFeature.timeProvider,
                        RateBasedSampler(RumFeature.telemetrySamplingRate / 100)
                    ),
                    firstPartyHostDetector = CoreFeature.firstPartyHostDetector,
                    cpuVitalMonitor = RumFeature.cpuVitalMonitor,
                    memoryVitalMonitor = RumFeature.memoryVitalMonitor,
                    frameRateVitalMonitor = RumFeature.frameRateVitalMonitor,
                    backgroundTrackingEnabled = RumFeature.backgroundEventTracking,
                    timeProvider = CoreFeature.timeProvider,
                    sessionListener = sessionListener
                )
            }
        }

        companion object {
            internal const val RUM_NOT_ENABLED_ERROR_MESSAGE =
                "You're trying to create a RumMonitor instance, " +
                    "but the SDK was not initialized or RUM feature was disabled " +
                    "in your Configuration. No RUM data will be sent."
            internal const val INVALID_APPLICATION_ID_ERROR_MESSAGE =
                "You're trying to create a RumMonitor instance, " +
                    "but the RUM application id was null or empty. No RUM data will be sent."
        }
    }
}
