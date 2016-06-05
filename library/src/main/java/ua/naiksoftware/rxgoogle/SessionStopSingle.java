package ua.naiksoftware.rxgoogle;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.result.SessionStopResult;

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
public class SessionStopSingle extends BaseSingle<List<Session>> {

    private final String identifier;

    SessionStopSingle(RxGoogle rxFit, String identifier, Long timeout, TimeUnit timeUnit) {
        super(rxFit, timeout, timeUnit);
        this.identifier = identifier;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final SingleSubscriber<? super List<Session>> subscriber) {
        setupFitnessPendingResult(Fitness.SessionsApi.stopSession(apiClient, identifier), new ResultCallback<SessionStopResult>() {
            @Override
            public void onResult(@NonNull SessionStopResult sessionStopResult) {
                if (!sessionStopResult.getStatus().isSuccess()) {
                    subscriber.onError(new StatusException(sessionStopResult.getStatus()));
                } else {
                    subscriber.onSuccess(sessionStopResult.getSessions());
                }
            }
        });
    }
}
