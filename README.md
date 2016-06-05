# Reactive Google API Library for Android

[![Release](https://jitpack.io/v/NaikSoftware/RxGoogle.svg)](https://jitpack.io/#NaikSoftware/RxGoogle)

This library wraps the Google API in [RxJava](https://github.com/ReactiveX/RxJava) Observables and Singles. No more managing GoogleApiClients! Also, the authorization process handled by the lib.

# Usage

Initialize RxGoogle once, preferably in your Application `onCreate()` via `RxGoogle.init(...)`. Make sure to include all the APIs and Scopes that you need for your app. RxGoogle its entry point to Google API, for example, instead of `Fitness.HistoryApi.readData(apiClient, dataReadRequest)` you can use `RxGoogle.Fit.History.read(dataReadRequest)`. Make sure to have the Location and Body Sensors permission from Marshmallow on, if they are needed by your Fit API requests. If the user didn’t already authorize your app for using fitness data, the lib handles showing the authorization dialog.

Example:

```java
RxGoogle.init(
        context,
        new Api[] { Fitness.HISTORY_API },
        new Scope[] { new Scope(Scopes.FITNESS_ACTIVITY_READ) }
);

DataReadRequest dataReadRequest = new DataReadRequest.Builder()
	    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
	    .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
	    .bucketBySession(1, TimeUnit.MINUTES)
	    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
	    .build();

RxGoogle.Fit.History.read(dataReadRequest)
        .flatMapObservable(dataReadResult -> Observable.from(dataReadResult.getBuckets()))
        .subscribe(bucket -> {
        	/* do something */
        });
```

An `OnExceptionResumeNext` Transformer is available in the lib, which resumes with another Single/Observable when an Exception is thrown, except when the exception was a GoogleAPIConnectionException which was caused by an unresolved resolution.

An optional global default timeout for all Fit API requests made through the library can be set via `RxGoogle.setDefaultTimeout(...)`. In addition, timeouts can be set when creating a new Observable by providing timeout parameters, e.g. `RxGoogle.Fit.History.read(dataReadRequest, 15, TimeUnit.SECONDS)`. These parameters override the default timeout. When a timeout occurs, a StatusException is provided via `onError()`. The RxJava timeout operators can be used instead, but these do not cancel the Fit API request immediately.

You can also obtain a `Single<GoogleApiClient>`, which connects on subscribe and disconnects on unsubscribe via `GoogleAPIClientSingle.create(...)`.

The following Exceptions are thrown in the lib and provided via `onError()`:

* `StatusException`: When the call to the Fit API was not successful or timed out
* `GoogleAPIConnectionException`: When connecting to the GoogleAPIClient was not successful and the resolution (if available) was also not successful (e.g. when the user does not authorize your app to use fitness data). Resolutions are not handled when using `GoogleAPIClientObservable`.
* `GoogleAPIConnectionSuspendedException`: When the GoogleApiClient connection was suspended.
* `SecurityException`: When you try to call a Fit API without proper permissions.

# Sample

A basic sample app is available in the `sample` project. You need to create an OAuth 2.0 Client ID for the sample app, see the [guide in the Fit API docs](https://developers.google.com/fit/android/get-api-key).

# Setup

The lib is available on JitPack. Add the following to your `build.gradle`:

	allprojects {
    	repositories {
    		...
    		maven { url "https://jitpack.io" }
    	}
    }
	
	dependencies {
	    compile 'com.github.NaikSoftware:RxFit:{latest version}'
	}

# Credits

 - The code for managing the GoogleApiClient is taken from the [Android-ReactiveLocation](https://github.com/mcharmas/Android-ReactiveLocation) library by Michał Charmas, which is licensed under the Apache License, Version 2.0.
 - The Fit API and most of code taken from [RxFit](https://github.com/patloew/RxFit) library by Patrick Löwenstein, which is licensed under the Apache License, Version 2.0.

# License

	Copyright 2016 Nickolay Savchenko

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.