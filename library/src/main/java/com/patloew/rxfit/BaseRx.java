package com.patloew.rxfit;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/* Copyright (C) 2015 Michał Charmas (http://blog.charmas.pl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ---------------
 *
 * FILE MODIFIED by Patrick Löwenstein, 2016
 *
 */
public abstract class BaseRx<T> {
    protected static final Set<BaseRx> observableSet = new HashSet<>();

    protected final Context ctx;
    private final Api<? extends Api.ApiOptions.NotRequiredOptions>[] services;
    private final Scope[] scopes;
    private final Long timeoutTime;
    private final TimeUnit timeoutUnit;

    protected BaseRx(@NonNull RxGoogle rxFit, Long timeout, TimeUnit timeUnit) {
        this.ctx = rxFit.getContext();
        this.services = rxFit.getApis();
        this.scopes = rxFit.getScopes();

        if(timeout != null && timeUnit != null) {
            this.timeoutTime = timeout;
            this.timeoutUnit = timeUnit;
        } else {
            this.timeoutTime = RxGoogle.getDefaultTimeout();
            this.timeoutUnit = RxGoogle.getDefaultTimeoutUnit();
        }
    }

    protected BaseRx(@NonNull Context ctx, @NonNull Api<? extends Api.ApiOptions.NotRequiredOptions>[] services, Scope[] scopes) {
        this.ctx = ctx;
        this.services = services;
        this.scopes = scopes;
        timeoutTime = null;
        timeoutUnit = null;
    }

    protected final <T extends Result> void setupFitnessPendingResult(PendingResult<T> pendingResult, ResultCallback<? super T> resultCallback) {
        if(timeoutTime != null && timeoutUnit != null) {
            pendingResult.setResultCallback(resultCallback, timeoutTime, timeoutUnit);
        } else {
            pendingResult.setResultCallback(resultCallback);
        }
    }

    protected final GoogleApiClient createApiClient(ApiClientConnectionCallbacks apiClientConnectionCallbacks) {

        GoogleApiClient.Builder apiClientBuilder = new GoogleApiClient.Builder(ctx);


        for (Api<? extends Api.ApiOptions.NotRequiredOptions> service : services) {
            apiClientBuilder.addApi(service);
        }

        if(scopes != null) {
            for (Scope scope : scopes) {
                apiClientBuilder.addScope(scope);
            }
        }

        apiClientBuilder.addConnectionCallbacks(apiClientConnectionCallbacks);
        apiClientBuilder.addOnConnectionFailedListener(apiClientConnectionCallbacks);

        GoogleApiClient apiClient = apiClientBuilder.build();

        apiClientConnectionCallbacks.setClient(apiClient);

        return apiClient;
    }

    protected void onUnsubscribed(GoogleApiClient apiClient) { }

    protected abstract void handleResolutionResult(int resultCode, ConnectionResult connectionResult);

    static final void onResolutionResult(int resultCode, ConnectionResult connectionResult) {
        for(BaseRx observable : observableSet) { observable.handleResolutionResult(resultCode, connectionResult); }
        observableSet.clear();
    }

    protected abstract class ApiClientConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        protected GoogleApiClient apiClient;

        protected ApiClientConnectionCallbacks() { }

        public void setClient(GoogleApiClient client) {
            this.apiClient = client;
        }
    }
}
