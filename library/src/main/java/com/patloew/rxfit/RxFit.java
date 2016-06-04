package com.patloew.rxfit;

import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.data.BleDevice;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.DataTypeCreateRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.SessionReadResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Function;
import rx.functions.Functions;

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
 * limitations under the License.
 *
 * -----------------------------
 *
 * Factory for Google Fit API observables. Make sure to include all the APIs
 * and Scopes that you need for your app. Also make sure to have the Location
 * and Body Sensors permission on Marshmallow, if they are needed by your
 * Fit API requests.
 */
public class RxFit {

    private static RxFit instance = null;

    private static Long timeoutTime = null;
    private static TimeUnit timeoutUnit = null;

    private final Context ctx;
    private final Api<? extends Api.ApiOptions.NotRequiredOptions>[] apis;
    private final Scope[] scopes;

    /* Initializes the singleton instance of RxFit
     *
     * @param ctx Context.
     * @param apis An array of Fitness APIs to be used in your app.
     * @param scopes An array of the Scopes to be requested for your app.
     */
    public static void init(@NonNull Context ctx, @NonNull Api<? extends Api.ApiOptions.NotRequiredOptions>[] apis, @NonNull Scope[] scopes) {
        if(instance == null) { instance = new RxFit(ctx, apis, scopes); }
    }

    /* Set a default timeout for all requests to the Fit API made in the lib.
     * When a timeout occurs, onError() is called with a StatusException.
     */
    public static void setDefaultTimeout(long time, @NonNull TimeUnit timeUnit) {
        if(timeUnit != null) {
            timeoutTime = time;
            timeoutUnit = timeUnit;
        } else {
            throw new IllegalArgumentException("timeUnit parameter must not be null");
        }
    }

    /* Reset the default timeout.
     */
    public static void resetDefaultTimeout() {
        timeoutTime = null;
        timeoutUnit = null;
    }

    /* Gets the singleton instance of RxFit, after it was initialized.
     */
    private static RxFit get() {
        if(instance == null) { throw new IllegalStateException("RxFit not initialized"); }
        return instance;
    }


    private RxFit(@NonNull Context ctx, @NonNull Api<? extends Api.ApiOptions.NotRequiredOptions>[] apis, @NonNull Scope[] scopes) {
        this.ctx = ctx.getApplicationContext();
        this.apis = apis;
        this.scopes = scopes;
    }

    Context getContext() {
        return ctx;
    }

    Api<? extends Api.ApiOptions.NotRequiredOptions>[] getApis() {
        return apis;
    }

    Scope[] getScopes() {
        return scopes;
    }

    static Long getDefaultTimeout() {
        return timeoutTime;
    }

    static TimeUnit getDefaultTimeoutUnit() {
        return timeoutUnit;
    }


    /* Can be used to check whether connection to Fit API was successful.
     * For example, a wear app might need to be notified, if the user
     * allowed accessing fitness data (which means that the connection
     * was successful). As an alternative, use doOnCompleted(...) and
     * doOnError(...) on any other RxFit Observable.
     *
     * This Completable completes if the connection was successful.
     */
    public static Completable checkConnection() {
        return Completable.fromObservable(Observable.create(new CheckConnectionObservable(RxFit.get())));
    }


    public static class Ble {

        private Ble() { }

        // claimDevice

        public static Single<Status> claimDevice(@NonNull BleDevice bleDevice) {
            return claimDeviceInternal(bleDevice, null, null, null);
        }

        public static Single<Status> claimDevice(@NonNull BleDevice bleDevice, long timeout, @NonNull TimeUnit timeUnit) {
            return claimDeviceInternal(bleDevice, null, timeout, timeUnit);
        }

        public static Single<Status> claimDevice(@NonNull String deviceAddress) {
            return claimDeviceInternal(null, deviceAddress, null, null);
        }

        public static Single<Status> claimDevice(@NonNull String deviceAddress, long timeout, @NonNull TimeUnit timeUnit) {
            return claimDeviceInternal(null, deviceAddress, timeout, timeUnit);
        }

        private static Single<Status> claimDeviceInternal(BleDevice bleDevice, String deviceAddress, Long timeout, TimeUnit timeUnit) {
            return Single.create(new BleClaimDeviceSingle(RxFit.get(), bleDevice, deviceAddress, timeout, timeUnit));
        }

        // getClaimedDevices

        public static Observable<BleDevice> getClaimedDevices() {
            return getClaimedDeviceListInternal(null, null, null);
        }

        public static Observable<BleDevice> getClaimedDevices(long timeout, @NonNull TimeUnit timeUnit) {
            return getClaimedDeviceListInternal(null, timeout, timeUnit);
        }

        public static Observable<BleDevice> getClaimedDevices(DataType dataType) {
            return getClaimedDeviceListInternal(dataType, null, null);
        }

        private static Observable<BleDevice> getClaimedDevicesInternal(DataType dataType, long timeout, @NonNull TimeUnit timeUnit) {
            return getClaimedDeviceListInternal(dataType, timeout, timeUnit);
        }

        private static Observable<BleDevice> getClaimedDeviceListInternal(DataType dataType, Long timeout, TimeUnit timeUnit) {
            return Single.create(new BleListClaimedDevicesSingle(RxFit.get(), dataType, timeout, timeUnit))
                    .flatMapObservable(new Func1<List<BleDevice>, Observable<BleDevice>>() {
                        @Override
                        public Observable<BleDevice> call(List<BleDevice> bleDevices) {
                            return Observable.from(bleDevices);
                        }
                    });
        }

        // scan

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan() {
            return scanInternal(null, null, null, null);
        }

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan(long timeout, @NonNull TimeUnit timeUnit) {
            return scanInternal(null, null, timeout, timeUnit);
        }

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan(@NonNull DataType... dataTypes) {
            return scanInternal(dataTypes, null, null, null);
        }

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan(@NonNull DataType[] dataTypes, long timeout, @NonNull TimeUnit timeUnit) {
            return scanInternal(dataTypes, null, timeout, timeUnit);
        }

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan(int stopTimeSecs) {
            return scanInternal(null, stopTimeSecs, null, null);
        }

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan(int stopTimeSecs, long timeout, @NonNull TimeUnit timeUnit) {
            return scanInternal(null, stopTimeSecs, timeout, timeUnit);
        }

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan(@NonNull DataType[] dataTypes, int stopTimeSecs) {
            return scanInternal(dataTypes, stopTimeSecs, null, null);
        }

        @RequiresPermission("android.permission.BLUETOOTH_ADMIN")
        public static Observable<BleDevice> scan(@NonNull DataType[] dataTypes, int stopTimeSecs, long timeout, @NonNull TimeUnit timeUnit) {
            return scanInternal(dataTypes, stopTimeSecs, timeout, timeUnit);
        }

        @SuppressWarnings("MissingPermission")
        private static Observable<BleDevice> scanInternal(DataType[] dataTypes, Integer stopTimeSecs, Long timeout, TimeUnit timeUnit) {
            return Observable.create(new BleScanObservable(RxFit.get(), dataTypes, stopTimeSecs, timeout, timeUnit));
        }

        // unclaim Device

        public static Single<Status> unclaimDevice(@NonNull BleDevice bleDevice) {
            return unclaimDeviceInternal(bleDevice, null, null, null);
        }

        public static Single<Status> unclaimDevice(@NonNull BleDevice bleDevice, long timeout, @NonNull TimeUnit timeUnit) {
            return unclaimDeviceInternal(bleDevice, null, timeout, timeUnit);
        }

        public static Single<Status> unclaimDevice(@NonNull String deviceAddress) {
            return unclaimDeviceInternal(null, deviceAddress, null, null);
        }

        public static Single<Status> unclaimDevice(@NonNull String deviceAddress, long timeout, @NonNull TimeUnit timeUnit) {
            return unclaimDeviceInternal(null, deviceAddress, timeout, timeUnit);
        }

        private static Single<Status> unclaimDeviceInternal(BleDevice bleDevice, String deviceAddress, Long timeout, TimeUnit timeUnit) {
            return Single.create(new BleUnclaimDeviceSingle(RxFit.get(), bleDevice, deviceAddress, timeout, timeUnit));
        }

    }


    public static class Config {

        private Config() { }

        // createCustomDataType

        public static Single<DataType> createCustomDataType(@NonNull DataTypeCreateRequest dataTypeCreateRequest) {
            return createCustomDataTypeInternal(dataTypeCreateRequest, null, null);
        }

        public static Single<DataType> createCustomDataType(@NonNull DataTypeCreateRequest dataTypeCreateRequest, long timeout, @NonNull TimeUnit timeUnit) {
            return createCustomDataTypeInternal(dataTypeCreateRequest, timeout, timeUnit);
        }

        private static Single<DataType> createCustomDataTypeInternal(DataTypeCreateRequest dataTypeCreateRequest, Long timeout, TimeUnit timeUnit) {
            return Single.create(new ConfigCreateCustomDataTypeSingle(RxFit.get(), dataTypeCreateRequest, timeout, timeUnit));
        }

        // disableFit

        public static Single<Status> disableFit() {
            return disableFitInternal(null, null);
        }

        public static Single<Status> disableFit(long timeout, @NonNull TimeUnit timeUnit) {
            return disableFitInternal(timeout, timeUnit);
        }

        private static Single<Status> disableFitInternal(Long timeout, TimeUnit timeUnit) {
            return Single.create(new ConfigDisableFitSingle(RxFit.get(), timeout, timeUnit));
        }

        // readDataType

        public static Single<DataType> readDataType(@NonNull String dataTypeName) {
            return readDataTypeInternal(dataTypeName, null, null);
        }

        public static Single<DataType> readDataType(@NonNull String dataTypeName, long timeout, @NonNull TimeUnit timeUnit) {
            return readDataTypeInternal(dataTypeName, timeout, timeUnit);
        }

        private static Single<DataType> readDataTypeInternal(String dataTypeName, Long timeout, TimeUnit timeUnit) {
            return Single.create(new ConfigReadDataTypeSingle(RxFit.get(), dataTypeName, timeout, timeUnit));
        }

    }


   public static class History {

       private History() { }

       // delete

       public static Single<Status> delete(@NonNull DataDeleteRequest dataDeleteRequest) {
           return deleteInternal(dataDeleteRequest, null, null);
       }

       public static Single<Status> delete(@NonNull DataDeleteRequest dataDeleteRequest, long timeout, @NonNull TimeUnit timeUnit) {
           return deleteInternal(dataDeleteRequest, timeout, timeUnit);
       }

       private static Single<Status> deleteInternal(DataDeleteRequest dataDeleteRequest, Long timeout, TimeUnit timeUnit) {
           return Single.create(new HistoryDeleteDataSingle(RxFit.get(), dataDeleteRequest, timeout, timeUnit));
       }

       // insert

       public static Single<Status> insert(@NonNull DataSet dataSet) {
           return insertInternal(dataSet, null, null);
       }

       public static Single<Status> insert(@NonNull DataSet dataSet, long timeout, @NonNull TimeUnit timeUnit) {
           return insertInternal(dataSet, timeout, timeUnit);
       }

       private static Single<Status> insertInternal(DataSet dataSet, Long timeout, TimeUnit timeUnit) {
           return Single.create(new HistoryInsertDataSingle(RxFit.get(), dataSet, timeout, timeUnit));
       }

       // readDailyTotal

       public static Single<DataSet> readDailyTotal(@NonNull DataType dataType) {
           return readDailyTotalInternal(dataType, null, null);
       }

       public static Single<DataSet> readDailyTotal(@NonNull DataType dataType, long timeout, @NonNull TimeUnit timeUnit) {
           return readDailyTotalInternal(dataType, timeout, timeUnit);
       }

       private static Single<DataSet> readDailyTotalInternal(DataType dataType, Long timeout, TimeUnit timeUnit) {
           return Single.create(new HistoryReadDailyTotalSingle(RxFit.get(), dataType, timeout, timeUnit));
       }

       // read

       public static Single<DataReadResult> read(@NonNull DataReadRequest dataReadRequest) {
           return readInternal(dataReadRequest, null, null);
       }

       public static Single<DataReadResult> read(@NonNull DataReadRequest dataReadRequest, long timeout, @NonNull TimeUnit timeUnit) {
           return readInternal(dataReadRequest, timeout, timeUnit);
       }

       private static Single<DataReadResult> readInternal(DataReadRequest dataReadRequest, Long timeout, TimeUnit timeUnit) {
           return Single.create(new HistoryReadDataSingle(RxFit.get(), dataReadRequest, timeout, timeUnit));
       }

       // update

       public static Single<Status> update(@NonNull DataUpdateRequest dataUpdateRequest) {
           return updateInternal(dataUpdateRequest, null, null);
       }

       public static Single<Status> update(@NonNull DataUpdateRequest dataUpdateRequest, long timeout, @NonNull TimeUnit timeUnit) {
           return updateInternal(dataUpdateRequest, timeout, timeUnit);
       }

       private static Single<Status> updateInternal(DataUpdateRequest dataUpdateRequest, Long timeout, TimeUnit timeUnit) {
           return Single.create(new HistoryUpdateDataSingle(RxFit.get(), dataUpdateRequest, timeout, timeUnit));
       }

   }


    public static class Recording {

        private Recording() { }

        // listSubscriptions

        public static Observable<Subscription> listSubscriptions() {
            return listSubscriptionsInternal(null, null, null);
        }

        public static Observable<Subscription> listSubscriptions(long timeout, @NonNull TimeUnit timeUnit) {
            return listSubscriptionsInternal(null, timeout, timeUnit);
        }

        public static Observable<Subscription> listSubscriptions(DataType dataType) {
            return listSubscriptionsInternal(dataType, null, null);
        }

        public static Observable<Subscription> listSubscriptions(DataType dataType, long timeout, @NonNull TimeUnit timeUnit) {
            return listSubscriptionsInternal(dataType, timeout, timeUnit);
        }

        private static Observable<Subscription> listSubscriptionsInternal(DataType dataType, Long timeout, TimeUnit timeUnit) {
            return Single.create(new RecordingListSubscriptionsSingle(RxFit.get(), dataType, timeout, timeUnit)).flatMapObservable(new Func1<List<Subscription>, Observable<? extends Subscription>>() {
                        @Override
                        public Observable<? extends Subscription> call(List<Subscription> subscriptions) {
                            return Observable.from(subscriptions);
                        }
                    });
        }

        // subscribe

        public static Single<Status> subscribe(@NonNull DataSource dataSource) {
            return subscribeInternal(dataSource, null, null, null);
        }

        public static Single<Status> subscribe(@NonNull DataSource dataSource, long timeout, @NonNull TimeUnit timeUnit) {
            return subscribeInternal(dataSource, null, timeout, timeUnit);
        }

        public static Single<Status> subscribe(@NonNull DataType dataType) {
            return subscribeInternal(null, dataType, null, null);
        }

        public static Single<Status> subscribe(@NonNull DataType dataType, long timeout, @NonNull TimeUnit timeUnit) {
            return subscribeInternal(null, dataType, timeout, timeUnit);
        }

        private static Single<Status> subscribeInternal(DataSource dataSource, DataType dataType, Long timeout, TimeUnit timeUnit) {
            return Single.create(new RecordingSubscribeSingle(RxFit.get(), dataSource, dataType, timeout, timeUnit));
        }

        // unsubscribe

        public static Single<Status> unsubscribe(@NonNull DataSource dataSource) {
            return unsubscribeInternal(dataSource, null, null, null, null);
        }

        public static Single<Status> unsubscribe(@NonNull DataSource dataSource, long timeout, @NonNull TimeUnit timeUnit) {
            return unsubscribeInternal(dataSource, null, null, timeout, timeUnit);
        }

        public static Single<Status> unsubscribe(@NonNull DataType dataType) {
            return unsubscribeInternal(null, dataType, null, null, null);
        }

        public static Single<Status> unsubscribe(@NonNull DataType dataType, long timeout, @NonNull TimeUnit timeUnit) {
            return unsubscribeInternal(null, dataType, null, timeout, timeUnit);
        }

        public static Single<Status> unsubscribe(@NonNull Subscription subscription) {
            return unsubscribeInternal(null, null, subscription, null, null);
        }

        public static Single<Status> unsubscribe(@NonNull Subscription subscription, long timeout, @NonNull TimeUnit timeUnit) {
            return unsubscribeInternal(null, null, subscription, timeout, timeUnit);
        }

        private static Single<Status> unsubscribeInternal(DataSource dataSource, DataType dataType, Subscription subscription, Long timeout, TimeUnit timeUnit) {
            return Single.create(new RecordingUnsubscribeSingle(RxFit.get(), dataSource, dataType, subscription, timeout, timeUnit));
        }

    }


    public static class Sensors {

        private Sensors() { }

        // addDataPointIntent

        public static Single<Status> addDataPointIntent(@NonNull SensorRequest sensorRequest, @NonNull PendingIntent pendingIntent) {
            return addDataPointIntentInternal(sensorRequest, pendingIntent, null, null);
        }

        public static Single<Status> addDataPointIntent(@NonNull SensorRequest sensorRequest, @NonNull PendingIntent pendingIntent, long timeout, @NonNull TimeUnit timeUnit) {
            return addDataPointIntentInternal(sensorRequest, pendingIntent, timeout, timeUnit);
        }

        private static Single<Status> addDataPointIntentInternal(SensorRequest sensorRequest, PendingIntent pendingIntent, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SensorsAddDataPointIntentSingle(RxFit.get(), sensorRequest, pendingIntent, timeout, timeUnit));
        }

        // removeDataPointIntent

        public static Single<Status> removeDataPointIntent(@NonNull PendingIntent pendingIntent) {
            return removeDataPointIntentInternal(pendingIntent, null, null);
        }

        public static Single<Status> removeDataPointIntent(@NonNull PendingIntent pendingIntent, long timeout, @NonNull TimeUnit timeUnit) {
            return removeDataPointIntentInternal(pendingIntent, timeout, timeUnit);
        }

        private static Single<Status> removeDataPointIntentInternal(PendingIntent pendingIntent, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SensorsRemoveDataPointIntentSingle(RxFit.get(), pendingIntent, timeout, timeUnit));
        }

        // getDataPoints

        public static Observable<DataPoint> getDataPoints(@NonNull SensorRequest sensorRequest) {
            return getDataPointsInternal(sensorRequest, null, null);
        }

        public static Observable<DataPoint> getDataPoints(@NonNull SensorRequest sensorRequest, long timeout, @NonNull TimeUnit timeUnit) {
            return getDataPointsInternal(sensorRequest, timeout, timeUnit);
        }

        private static Observable<DataPoint> getDataPointsInternal(SensorRequest sensorRequest, Long timeout, TimeUnit timeUnit) {
            return Observable.create(new SensorsDataPointObservable(RxFit.get(), sensorRequest, timeout, timeUnit));
        }

        // findDataSources

        public static Observable<DataSource> findDataSources(@NonNull DataSourcesRequest dataSourcesRequest) {
            return findDataSourcesInternal(dataSourcesRequest, null, null, null);
        }

        public static Observable<DataSource> findDataSources(@NonNull DataSourcesRequest dataSourcesRequest, long timeout, @NonNull TimeUnit timeUnit) {
            return findDataSourcesInternal(dataSourcesRequest, null, timeout, timeUnit);
        }

        public static Observable<DataSource> findDataSources(@NonNull DataSourcesRequest dataSourcesRequest, DataType dataType) {
            return findDataSourcesInternal(dataSourcesRequest, dataType, null, null);
        }

        public static Observable<DataSource> findDataSources(@NonNull DataSourcesRequest dataSourcesRequest, DataType dataType, long timeout, @NonNull TimeUnit timeUnit) {
            return findDataSourcesInternal(dataSourcesRequest, dataType, timeout, timeUnit);
        }

        private static Observable<DataSource> findDataSourcesInternal(DataSourcesRequest dataSourcesRequest, DataType dataType, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SensorsFindDataSourcesSingle(RxFit.get(), dataSourcesRequest, dataType, timeout, timeUnit)).flatMapObservable(new Func1<List<DataSource>, Observable<? extends DataSource>>() {
                @Override
                public Observable<? extends DataSource> call(List<DataSource> dataSources) {
                    return Observable.from(dataSources);
                }
            });
        }

    }


    public static class Sessions {

        private Sessions() { }

        // insert

        public static Single<Status> insert(@NonNull SessionInsertRequest sessionInsertRequest) {
            return insertInternal(sessionInsertRequest, null, null);
        }

        public static Single<Status> insert(@NonNull SessionInsertRequest sessionInsertRequest, long timeout, @NonNull TimeUnit timeUnit) {
            return insertInternal(sessionInsertRequest, timeout, timeUnit);
        }

        private static Single<Status> insertInternal(SessionInsertRequest sessionInsertRequest, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SessionInsertSingle(RxFit.get(), sessionInsertRequest, timeout, timeUnit));
        }

        // read

        public static Single<SessionReadResult> read(@NonNull SessionReadRequest sessionReadRequest) {
            return readInternal(sessionReadRequest, null, null);
        }

        public static Single<SessionReadResult> read(@NonNull SessionReadRequest sessionReadRequest, long timeout, @NonNull TimeUnit timeUnit) {
            return readInternal(sessionReadRequest, timeout, timeUnit);
        }

        private static Single<SessionReadResult> readInternal(SessionReadRequest sessionReadRequest, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SessionReadSingle(RxFit.get(), sessionReadRequest, timeout, timeUnit));
        }

        // registerForSessions

        public static Single<Status> registerForSessions(@NonNull PendingIntent pendingIntent) {
            return registerForSessionsInternal(pendingIntent, null, null);
        }

        public static Single<Status> registerForSessions(@NonNull PendingIntent pendingIntent, long timeout, @NonNull TimeUnit timeUnit) {
            return registerForSessionsInternal(pendingIntent, timeout, timeUnit);
        }

        private static Single<Status> registerForSessionsInternal(PendingIntent pendingIntent, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SessionRegisterSingle(RxFit.get(), pendingIntent, timeout, timeUnit));
        }

        // unregisterForSessions

        public static Single<Status> unregisterForSessions(@NonNull PendingIntent pendingIntent) {
            return unregisterForSessionsInternal(pendingIntent, null, null);
        }

        public static Single<Status> unregisterForSessions(@NonNull PendingIntent pendingIntent, long timeout, @NonNull TimeUnit timeUnit) {
            return unregisterForSessionsInternal(pendingIntent, timeout, timeUnit);
        }

        private static Single<Status> unregisterForSessionsInternal(PendingIntent pendingIntent, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SessionUnregisterSingle(RxFit.get(), pendingIntent, timeout, timeUnit));
        }

        // start

        public static Single<Status> start(@NonNull Session session) {
            return startInternal(session, null, null);
        }

        public static Single<Status> start(@NonNull Session session, long timeout, @NonNull TimeUnit timeUnit) {
            return startInternal(session, timeout, timeUnit);
        }

        private static Single<Status> startInternal(Session session, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SessionStartSingle(RxFit.get(), session, timeout, timeUnit));
        }

        // stop

        public static Observable<Session> stop(@NonNull String identifier) {
            return stopInternal(identifier, null, null);
        }

        public static Observable<Session> stop(@NonNull String identifier, long timeout, @NonNull TimeUnit timeUnit) {
            return stopInternal(identifier, timeout, timeUnit);
        }

        private static Observable<Session> stopInternal(String identifier, Long timeout, TimeUnit timeUnit) {
            return Single.create(new SessionStopSingle(RxFit.get(), identifier, timeout, timeUnit)).flatMapObservable(new Func1<List<Session>, Observable<? extends Session>>() {
                @Override
                public Observable<? extends Session> call(List<Session> sessions) {
                    return Observable.from(sessions);
                }
            });
        }


    }


    public static class Components {

        public static <T extends BaseObservable<R>, R> Observable<R> create(Func1<RxFit, T> constructor) {
            return Observable.create(constructor.call(get()));
        }

        public static <T extends BaseSingle<R>, R> Single<R> createSingle(Func1<RxFit, T> constructor) {
            return Single.create(constructor.call(get()));
        }
    }


    /* Transformer that behaves like onExceptionResumeNext(Observable o), but propagates
     * a GoogleAPIConnectionException, which was caused by an unsuccessful resolution.
     * This can be helpful if you want to resume with another RxFit Observable when
     * an Exception occurs, but don't want to show the resolution dialog multiple times.
     *
     * An example use case: Fetch fitness data with server queries enabled, but provide
     * a timeout. When an exception occurs (e.g. timeout), switch to cached fitness data.
     * Using this Transformer prevents showing the authorization dialog twice, if the user
     * denys access for the first read. See MainActivity in sample project.
     */
    public static class OnExceptionResumeNext {

        private OnExceptionResumeNext() { }

        public static <T, R extends T> Observable.Transformer<T, T> with(final Observable<R> other) {
            return new Observable.Transformer<T, T>() {
                @Override
                public Observable<T> call(Observable<T> source) {
                    return source.onErrorResumeNext(new Func1<Throwable, Observable<R>>() {
                        @Override
                        public Observable<R> call(Throwable throwable) {
                            if (!(throwable instanceof Exception) || (throwable instanceof GoogleAPIConnectionException && ((GoogleAPIConnectionException) throwable).wasResolutionUnsuccessful())) {
                                throw Exceptions.propagate(throwable);
                            }

                            return other;
                        }
                    });
                }
            };
        }

        public static <T, R extends T> Single.Transformer<T, T> with(final Single<R> other) {
            return new Single.Transformer<T, T>() {
                @Override
                public Single<T> call(Single<T> source) {
                    return source.onErrorResumeNext(new Func1<Throwable, Single<R>>() {
                        @Override
                        public Single<R> call(Throwable throwable) {
                            if (!(throwable instanceof Exception) || (throwable instanceof GoogleAPIConnectionException && ((GoogleAPIConnectionException) throwable).wasResolutionUnsuccessful())) {
                                throw Exceptions.propagate(throwable);
                            }

                            return other;
                        }
                    });
                }
            };
        }
    }

}
