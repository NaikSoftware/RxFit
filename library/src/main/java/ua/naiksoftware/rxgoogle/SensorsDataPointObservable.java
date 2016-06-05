package ua.naiksoftware.rxgoogle;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;

import java.util.concurrent.TimeUnit;

import rx.Subscriber;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
public class SensorsDataPointObservable extends BaseObservable<DataPoint> {

    private final SensorRequest sensorRequest;
    private OnDataPointListener dataPointListener = null;

    SensorsDataPointObservable(RxGoogle rxFit, SensorRequest sensorRequest, Long timeout, TimeUnit timeUnit) {
        super(rxFit, timeout, timeUnit);
        this.sensorRequest = sensorRequest;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final Subscriber<? super DataPoint> subscriber) {
        dataPointListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                subscriber.onNext(dataPoint);
            }
        };

        setupFitnessPendingResult(Fitness.SensorsApi.add(apiClient, sensorRequest, dataPointListener), new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (!status.isSuccess()) {
                    subscriber.onError(new StatusException(status));
                }
            }
        });
    }

    @Override
    protected void onUnsubscribed(GoogleApiClient apiClient) {
        Fitness.SensorsApi.remove(apiClient, dataPointListener);
    }
}
