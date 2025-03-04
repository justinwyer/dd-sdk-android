# Getting Started with Android RUM Collection

## Overview

Datadog Real User Monitoring (RUM) enables you to visualize and analyze the real-time performance and user journeys of your application's individual users.

The Datadog Android SDK supports Android 4.4 (API level 19)+ and Android TV.

## Setup

1. Declare SDK as a dependency.
2. Specify application details in UI.
3. Initialize the library with application context.
4. Initialize RUM Monitor, Interceptor and start sending data.

### Declare SDK as dependency

Declare [dd-sdk-android][1] and the [Gradle plugin][12] as a dependency in your **application module's** `build.gradle` file.

```
buildscript {
    dependencies {
        classpath("com.datadoghq:dd-sdk-android-gradle-plugin:x.x.x")
    }
}
plugins {
    id("com.datadoghq.dd-sdk-android-gradle-plugin")
    //(...)
}
android {
    //(...)
}
dependencies {
    implementation "com.datadoghq:dd-sdk-android:x.x.x" 
    //(...)
}

```

### Specify application details in the UI

1. Navigate to [**UX Monitoring** > **RUM Applications** > **New Application**][2].
2. Select `android` as the application type and enter an application name to generate a unique Datadog application ID and client token.

{{< img src="real_user_monitoring/android/create_rum_application.png" alt="Create a RUM application in Datadog workflow" style="width:90%;">}}

To ensure the safety of your data, you must use a client token. If you used only [Datadog API keys][3] to configure the `dd-sdk-android` library, they would be exposed client-side in the Android application's APK byte code. 

For more information about setting up a client token, see the [Client Token documentation][4].

### Initialize the library with application context

See [`ViewTrackingStrategy`][5] to enable automatic tracking of all your views (activities, fragments, and more), [`trackingConsent`][6] to add GDPR compliance for your EU users, and [other configuration options][7] to initialize the library.

{{< site-region region="us" >}}
{{< tabs >}}
{{% tab "Kotlin" %}}
```kotlin
    class SampleApplication : Application() {
        override fun onCreate() {
            super.onCreate()
            val configuration = Configuration.Builder(
                    logsEnabled = true,
                    tracesEnabled = true,
                    crashReportsEnabled = true,
                    rumEnabled = true
                )
                .useSite(DatadogSite.US1)
                .trackInteractions()
                .trackLongTasks(durationThreshold)
                .useViewTrackingStrategy(strategy)
                .build()
            val credentials = Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>)
            Datadog.initialize(this, credentials, configuration, trackingConsent)
        }
    }
```
{{% /tab %}}
{{% tab "Java" %}}
```java
    public class SampleApplication extends Application { 
        @Override 
        public void onCreate() { 
            super.onCreate();
            final Configuration configuration = 
                    new Configuration.Builder(true, true, true, true)
                            .trackInteractions()
                            .trackLongTasks(durationThreshold)
                            .useViewTrackingStrategy(strategy)
                            .useSite(DatadogSite.US1)
                            .build();
               final Credentials credentials = new Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>);
               Datadog.initialize(this, credentials, configuration, trackingConsent); 
        }
    }
```
{{% /tab %}}
{{< /tabs >}}
{{< /site-region >}}

{{< site-region region="eu" >}}
{{< tabs >}}
{{% tab "Kotlin" %}}
```kotlin
    class SampleApplication : Application() {
        override fun onCreate() {
            super.onCreate()
            val configuration = Configuration.Builder(
                    logsEnabled = true,
                    tracesEnabled = true,
                    crashReportsEnabled = true,
                    rumEnabled = true
                )
                .useSite(DatadogSite.EU1)
                .trackInteractions()
                .trackLongTasks(durationThreshold)
                .useViewTrackingStrategy(strategy)
                .build()
            val credentials = Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>)
            Datadog.initialize(this, credentials, configuration, trackingConsent)
        }
    }
```
{{% /tab %}}
{{% tab "Java" %}}
```java
    public class SampleApplication extends Application { 
        @Override 
        public void onCreate() { 
            super.onCreate();
            final Configuration configuration = 
                    new Configuration.Builder(true, true, true, true)
                            .trackInteractions()
                            .trackLongTasks(durationThreshold)
                            .useViewTrackingStrategy(strategy)
                            .useSite(DatadogSite.EU1)
                            .build();
            Credentials credentials = new Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>);
            Datadog.initialize(this, credentials, configuration, trackingConsent); 
        }
    }
```
{{% /tab %}}
{{< /tabs >}}
{{< /site-region >}}

{{< site-region region="us3" >}}
{{< tabs >}}
{{% tab "Kotlin" %}}
```kotlin
    class SampleApplication : Application() {
        override fun onCreate() {
            super.onCreate()
            val configuration = Configuration.Builder(
                    logsEnabled = true,
                    tracesEnabled = true,
                    crashReportsEnabled = true,
                    rumEnabled = true
                )
                .useSite(DatadogSite.US3)
                .trackInteractions()
                .trackLongTasks(durationThreshold)
                .useViewTrackingStrategy(strategy)
                .build()
            val credentials = Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>)
            Datadog.initialize(this, credentials, configuration, trackingConsent)
        }
    }
```
{{% /tab %}}
{{% tab "Java" %}}
```java
    public class SampleApplication extends Application { 
        @Override 
        public void onCreate() { 
            super.onCreate();
            final Configuration configuration = 
                    new Configuration.Builder(true, true, true, true)
                            .trackInteractions()
                            .trackLongTasks(durationThreshold)
                            .useViewTrackingStrategy(strategy)
                            .useSite(DatadogSite.US3)
                            .build();
            Credentials credentials = new Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>);
            Datadog.initialize(this, credentials, configuration, trackingConsent); 
        }
    }
```
{{% /tab %}}
{{< /tabs >}}
{{< /site-region >}}

{{< site-region region="us5" >}}
{{< tabs >}}
{{% tab "Kotlin" %}}
```kotlin
    class SampleApplication : Application() {
        override fun onCreate() {
            super.onCreate()
            val configuration = Configuration.Builder(
                    logsEnabled = true,
                    tracesEnabled = true,
                    crashReportsEnabled = true,
                    rumEnabled = true
                )
                .useSite(DatadogSite.US5)
                .trackInteractions()
                .trackLongTasks(durationThreshold)
                .useViewTrackingStrategy(strategy)
                .build()
            val credentials = Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>)
            Datadog.initialize(this, credentials, configuration, trackingConsent)
        }
    }
```
{{% /tab %}}
{{% tab "Java" %}}
```java
    public class SampleApplication extends Application { 
        @Override 
        public void onCreate() { 
            super.onCreate();
            final Configuration configuration = 
                    new Configuration.Builder(true, true, true, true)
                            .trackInteractions()
                            .trackLongTasks(durationThreshold)
                            .useViewTrackingStrategy(strategy)
                            .useSite(DatadogSite.US5)
                            .build();
            Credentials credentials = new Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>);
            Datadog.initialize(this, credentials, configuration, trackingConsent); 
        }
    }
```
{{% /tab %}}
{{< /tabs >}}
{{< /site-region >}}

{{< site-region region="gov" >}}
{{< tabs >}}
{{% tab "Kotlin" %}}
```kotlin
    class SampleApplication : Application() {
        override fun onCreate() {
            super.onCreate()
            val configuration = Configuration.Builder(
                    logsEnabled = true,
                    tracesEnabled = true,
                    crashReportsEnabled = true,
                    rumEnabled = true
                )
                .useSite(DatadogSite.US1_FED)
                .trackInteractions()
                .trackLongTasks(durationThreshold)
                .useViewTrackingStrategy(strategy)
                .build()
            val credentials = Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>)
            Datadog.initialize(this, credentials, configuration, trackingConsent)
        }
    }
```
{{% /tab %}}
{{% tab "Java" %}}
```java
    public class SampleApplication extends Application { 
        @Override 
        public void onCreate() { 
            super.onCreate();
            final Configuration configuration = 
                    new Configuration.Builder(true, true, true, true)
                            .trackInteractions()
                            .trackLongTasks(durationThreshold)
                            .useViewTrackingStrategy(strategy)
                            .useSite(DatadogSite.US1_FED)
                            .build();
            Credentials credentials = new Credentials(<CLIENT_TOKEN>, <ENV_NAME>, <APP_VARIANT_NAME>, <APPLICATION_ID>);
            Datadog.initialize(this, credentials, configuration, trackingConsent); 
        }
    }
```
{{% /tab %}}
{{< /tabs >}}
{{< /site-region >}}

The credentials for initialization require your application's variant name and uses the value of `BuildConfig.FLAVOR` or an empty string if you don't have variants. This enables the correct ProGuard `mapping.txt` file to automatically upload at build time so you can view de-obfuscated RUM error stack traces. For more information, see the [guide to uploading Android source mapping files][8].

### Initialize RUM Monitor and Interceptor

Configure and register the RUM Monitor. You only need to do it once in your application's `onCreate()` method.

{{< tabs >}}
{{% tab "Kotlin" %}}
   ```kotlin
        val monitor = RumMonitor.Builder().build()
        GlobalRum.registerIfAbsent(monitor)
   ```
{{% /tab %}}
{{% tab "Java" %}}
   ```java
        final RumMonitor monitor = new RumMonitor.Builder().build();
        GlobalRum.registerIfAbsent(monitor);
   ```
{{% /tab %}}
{{< /tabs >}}

To track your OkHttp requests as resources, add the provided [Interceptor][9]:

{{< tabs >}}
{{% tab "Kotlin" %}}
   ```kotlin
        val okHttpClient =  OkHttpClient.Builder()
            .addInterceptor(DatadogInterceptor())
            .build()
   ```
{{% /tab %}}
{{% tab "Java" %}}
   ```java
        final OkHttpClient okHttpClient =  new OkHttpClient.Builder()
            .addInterceptor(new DatadogInterceptor())
            .build();
   ```
{{% /tab %}}
{{< /tabs >}}

This records each request processed by the `OkHttpClient` as a resource in RUM, with all the relevant information automatically filled (URL, method, status code, and error). Only the network requests that started when a view is active are tracked. To track requests when your application is in the background, [create a view manually][10].

**Note**: If you also use multiple Interceptors, call `DatadogInterceptor` first.

You can also add an `EventListener` for the `OkHttpClient` to [automatically track resource timing][11] for third-party providers and network requests. 

## Further Reading

{{< partial name="whats-next/whats-next.html" >}}

[1]: https://github.com/DataDog/dd-sdk-android
[2]: https://app.datadoghq.com/rum/application/create
[3]: https://docs.datadoghq.com/account_management/api-app-keys/#api-keys
[4]: https://docs.datadoghq.com/account_management/api-app-keys/#client-tokens
[5]: https://docs.datadoghq.com/real_user_monitoring/android/advanced_configuration/#automatically-track-views
[6]: https://docs.datadoghq.com/real_user_monitoring/android/troubleshooting/#set-tracking-consent-gdpr-compliance
[7]: https://docs.datadoghq.com/real_user_monitoring/android/advanced_configuration/#initialization-parameters
[8]: https://docs.datadoghq.com/real_user_monitoring/error_tracking/android/#upload-your-mapping-file
[9]: https://square.github.io/okhttp/interceptors/
[10]: https://docs.datadoghq.com/real_user_monitoring/android/advanced_configuration/#custom-views
[11]: https://docs.datadoghq.com/real_user_monitoring/android/advanced_configuration/#automatically-track-network-requests
[12]: https://github.com/DataDog/dd-sdk-android-gradle-plugin
