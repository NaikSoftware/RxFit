package ua.naiksoftware.rxgoogle.fitness;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Subscription;

import java.util.concurrent.TimeUnit;

import rx.SingleSubscriber;
import ua.naiksoftware.rxgoogle.BaseSingle;
import ua.naiksoftware.rxgoogle.RxGoogle;
import ua.naiksoftware.rxgoogle.StatusResultCallBack;

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
public class RecordingUnsubscribeSingle extends BaseSingle<Status> {

    private final DataSource dataSource;
    private final DataType dataType;
    private final Subscription subscription;

    public RecordingUnsubscribeSingle(RxGoogle rxFit, DataSource dataSource, DataType dataType, Subscription subscription, Long timeout, TimeUnit timeUnit) {
        super(rxFit, timeout, timeUnit);
        this.dataSource = dataSource;
        this.dataType = dataType;
        this.subscription = subscription;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final SingleSubscriber<? super Status> subscriber) {
        ResultCallback<Status> resultCallback = new StatusResultCallBack(subscriber);

        if(dataSource != null) {
            setupPendingResult(Fitness.RecordingApi.unsubscribe(apiClient, dataSource), resultCallback);
        } else if(dataType != null) {
            setupPendingResult(Fitness.RecordingApi.unsubscribe(apiClient, dataType), resultCallback);
        } else {
            setupPendingResult(Fitness.RecordingApi.unsubscribe(apiClient, subscription), resultCallback);
        }
    }
}
