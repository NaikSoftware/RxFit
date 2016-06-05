package ua.naiksoftware.rxgoogle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.request.DataDeleteRequest;

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
public class HistoryDeleteDataSingle extends BaseSingle<Status> {

    private final DataDeleteRequest dataDeleteRequest;

    HistoryDeleteDataSingle(RxGoogle rxFit, DataDeleteRequest dataDeleteRequest, Long timeout, TimeUnit timeUnit) {
        super(rxFit, timeout, timeUnit);
        this.dataDeleteRequest = dataDeleteRequest;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final SingleSubscriber<? super Status> subscriber) {
        setupFitnessPendingResult(Fitness.HistoryApi.deleteData(apiClient, dataDeleteRequest), new StatusResultCallBack(subscriber));
    }
}