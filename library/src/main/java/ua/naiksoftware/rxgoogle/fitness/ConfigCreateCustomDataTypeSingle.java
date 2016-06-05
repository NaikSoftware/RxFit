package ua.naiksoftware.rxgoogle.fitness;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataTypeCreateRequest;
import com.google.android.gms.fitness.result.DataTypeResult;

import java.util.concurrent.TimeUnit;

import rx.SingleSubscriber;
import ua.naiksoftware.rxgoogle.BaseSingle;
import ua.naiksoftware.rxgoogle.RxGoogle;
import ua.naiksoftware.rxgoogle.StatusException;

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
public class ConfigCreateCustomDataTypeSingle extends BaseSingle<DataType> {

    private final DataTypeCreateRequest dataTypeCreateRequest;

    public ConfigCreateCustomDataTypeSingle(RxGoogle rxFit, DataTypeCreateRequest dataTypeCreateRequest, Long timeout, TimeUnit timeUnit) {
        super(rxFit, timeout, timeUnit);
        this.dataTypeCreateRequest = dataTypeCreateRequest;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final SingleSubscriber<? super DataType> subscriber) {
        setupFitnessPendingResult(Fitness.ConfigApi.createCustomDataType(apiClient, dataTypeCreateRequest), new ResultCallback<DataTypeResult>() {
            @Override
            public void onResult(@NonNull DataTypeResult dataTypeResult) {
                if (!dataTypeResult.getStatus().isSuccess()) {
                    subscriber.onError(new StatusException(dataTypeResult.getStatus()));
                } else {
                    subscriber.onSuccess(dataTypeResult.getDataType());
                }
            }
        });

    }
}
