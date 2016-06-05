package ua.naiksoftware.rxgoogle;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.SingleSubscriber;

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
public class SensorsFindDataSourcesSingle extends BaseSingle<List<DataSource>> {

    private final DataSourcesRequest dataSourcesRequest;
    private final DataType dataType;

    SensorsFindDataSourcesSingle(RxGoogle rxFit, DataSourcesRequest dataSourcesRequest, DataType dataType, Long timeout, TimeUnit timeUnit) {
        super(rxFit, timeout, timeUnit);
        this.dataSourcesRequest = dataSourcesRequest;
        this.dataType = dataType;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final SingleSubscriber<? super List<DataSource>> subscriber) {
        setupFitnessPendingResult(Fitness.SensorsApi.findDataSources(apiClient, dataSourcesRequest), new ResultCallback<DataSourcesResult>() {
            @Override
            public void onResult(@NonNull DataSourcesResult dataSourcesResult) {
                if (!dataSourcesResult.getStatus().isSuccess()) {
                    subscriber.onError(new StatusException(dataSourcesResult.getStatus()));
                } else {
                    if(dataType == null) {
                        subscriber.onSuccess(dataSourcesResult.getDataSources());
                    } else {
                        subscriber.onSuccess(dataSourcesResult.getDataSources(dataType));
                    }
                }
            }
        });
    }
}
