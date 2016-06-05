package ua.naiksoftware.rxgoogle;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.BleApi;
import com.google.android.gms.fitness.ConfigApi;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryApi;
import com.google.android.gms.fitness.RecordingApi;
import com.google.android.gms.fitness.SensorsApi;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.fitness.data.BleDevice;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.BleScanCallback;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.DataTypeCreateRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.request.StartBleScanRequest;
import com.google.android.gms.fitness.result.BleDevicesResult;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.result.DataTypeResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareOnlyThisForTest({ ContextCompat.class, Fitness.class, Status.class, ConnectionResult.class, DataType.class, DataSet.class, DataPoint.class, BaseRx.class })
@SuppressStaticInitializationFor("com.google.android.gms.fitness.Fitness")
public class RxGoogleTest {

    @Mock Context ctx;

    @Mock GoogleApiClient apiClient;
    @Mock Status status;
    @Mock ConnectionResult connectionResult;
    @Mock PendingResult pendingResult;

    @Mock DataType dataType;
    @Mock DataSource dataSource;
    @Mock DataSet dataSet;
    @Mock BleDevice bleDevice;
    @Mock Subscription subscription;
    @Mock SensorRequest sensorRequest;
    @Mock Session session;

    @Mock BleApi bleApi;
    @Mock ConfigApi configApi;
    @Mock HistoryApi historyApi;
    @Mock RecordingApi recordingApi;
    @Mock SensorsApi sensorsApi;
    @Mock SessionsApi sessionsApi;

    @Mock
    RxGoogle rxFit;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(Fitness.class);
        Whitebox.setInternalState(Fitness.class, bleApi);
        Whitebox.setInternalState(Fitness.class, configApi);
        Whitebox.setInternalState(Fitness.class, historyApi);
        Whitebox.setInternalState(Fitness.class, recordingApi);
        Whitebox.setInternalState(Fitness.class, sensorsApi);
        Whitebox.setInternalState(Fitness.class, sessionsApi);

        when(ctx.getApplicationContext()).thenReturn(ctx);
    }

    //////////////////
    // UTIL METHODS //
    //////////////////

    @SuppressWarnings("unchecked")
    private static <T> Subscriber<? super T> getSubscriber(BaseObservable<T> baseObservable, GoogleApiClient apiClient) {
        try {
            final Field subscriberField = BaseObservable.class.getDeclaredField("subscriptionInfoHashMap");
            subscriberField.setAccessible(true);
            return ((HashMap<GoogleApiClient, Subscriber<? super T>>) subscriberField.get(baseObservable)).get(apiClient);
        } catch(Exception e) {
            return null;
        }
    }

    // Mock GoogleApiClient connection success behaviour
    private <T> void setupBaseObservableSuccess(final BaseObservable<T> baseObservable) {
        setupBaseObservableSuccess(baseObservable, apiClient);
    }

    // Mock GoogleApiClient connection success behaviour
    private <T> void setupBaseObservableSuccess(final BaseObservable<T> baseObservable, final GoogleApiClient apiClient) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Subscriber<? super T> subscriber = ((BaseObservable.ApiClientConnectionCallbacks)invocation.getArguments()[0]).subscriber;

                doAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        baseObservable.onGoogleApiClientReady(apiClient, subscriber);
                        return null;
                    }
                }).when(apiClient).connect();

                return apiClient;
            }
        }).when(baseObservable).createApiClient(Matchers.any(BaseRx.ApiClientConnectionCallbacks.class));
    }

    // Mock GoogleApiClient resolution behaviour
    private <T> void setupBaseObservableResolution(final BaseObservable<T> baseObservable, final GoogleApiClient apiClient) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                doAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        try {
                            final Field observableSetField = BaseRx.class.getDeclaredField("observableSet");
                            observableSetField.setAccessible(true);
                            ((Set<BaseRx>)observableSetField.get(baseObservable)).add(baseObservable);
                        } catch(Exception e) { }
                        return null;
                    }
                }).when(apiClient).connect();

                return apiClient;
            }
        }).when(baseObservable).createApiClient(Matchers.any(BaseRx.ApiClientConnectionCallbacks.class));
    }

    // Mock GoogleApiClient connection success behaviour
    private <T> void setupBaseSingleSuccess(final BaseSingle<T> baseSingle) {
        setupBaseSingleSuccess(baseSingle, apiClient);
    }

    // Mock GoogleApiClient connection success behaviour
    private <T> void setupBaseSingleSuccess(final BaseSingle<T> baseSingle, final GoogleApiClient apiClient) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final SingleSubscriber<? super T> subscriber = ((BaseSingle.ApiClientConnectionCallbacks)invocation.getArguments()[0]).subscriber;

                doAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        baseSingle.onGoogleApiClientReady(apiClient, subscriber);
                        return null;
                    }
                }).when(apiClient).connect();

                return apiClient;
            }
        }).when(baseSingle).createApiClient(Matchers.any(BaseRx.ApiClientConnectionCallbacks.class));
    }

    // Mock GoogleApiClient connection error behaviour
    private <T> void setupBaseObservableError(final BaseObservable<T> baseObservable) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Subscriber<? super T> subscriber = ((BaseObservable.ApiClientConnectionCallbacks)invocation.getArguments()[0]).subscriber;

                doAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        subscriber.onError(new GoogleAPIConnectionException("Error connecting to GoogleApiClient.", connectionResult));
                        return null;
                    }
                }).when(apiClient).connect();

                return apiClient;
            }
        }).when(baseObservable).createApiClient(Matchers.any(BaseRx.ApiClientConnectionCallbacks.class));
    }

    // Mock GoogleApiClient connection error behaviour
    private <T> void setupBaseSingleError(final BaseSingle<T> baseSingle) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final SingleSubscriber<? super T> subscriber = ((BaseSingle.ApiClientConnectionCallbacks)invocation.getArguments()[0]).subscriber;

                doAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        subscriber.onError(new GoogleAPIConnectionException("Error connecting to GoogleApiClient.", connectionResult));
                        return null;
                    }
                }).when(apiClient).connect();

                return apiClient;
            }
        }).when(baseSingle).createApiClient(Matchers.any(BaseRx.ApiClientConnectionCallbacks.class));
    }

    @SuppressWarnings("unchecked")
    private void setPendingResultValue(final Result result) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultCallback)invocation.getArguments()[0]).onResult(result);
                return null;
            }
        }).when(pendingResult).setResultCallback(Matchers.<ResultCallback>any());
    }

    private static void assertError(TestSubscriber sub, Class<? extends Throwable> errorClass) {
        sub.assertError(errorClass);
        sub.assertNoValues();
        sub.assertUnsubscribed();
    }

    @SuppressWarnings("unchecked")
    private static void assertSingleValue(TestSubscriber sub, Object value) {
        sub.assertCompleted();
        sub.assertUnsubscribed();
        sub.assertValue(value);
    }


    //////////////////////
    // OBSERVABLE TESTS //
    //////////////////////


    // GoogleApiClientObservable

    @Test
    public void GoogleAPIClientObservable_Success() {
        TestSubscriber<GoogleApiClient> sub = new TestSubscriber<>();
        GoogleAPIClientSingle single = PowerMockito.spy(new GoogleAPIClientSingle(ctx, new Api[] {}, new Scope[] {}));

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, apiClient);
    }

    @Test
    public void GoogleAPIClientObservable_ConnectionException() {
        TestSubscriber<GoogleApiClient> sub = new TestSubscriber<>();
        final GoogleAPIClientSingle single = PowerMockito.spy(new GoogleAPIClientSingle(ctx, new Api[] {}, new Scope[] {}));

        setupBaseSingleError(single);
        Single.create(single).subscribe(sub);

        assertError(sub, GoogleAPIConnectionException.class);
    }

    // CheckConnectionCompletable

    @Test
    public void CheckConnectionCompletable_Success() {
        TestSubscriber<GoogleApiClient> sub = new TestSubscriber<>();
        CheckConnectionObservable observable = PowerMockito.spy(new CheckConnectionObservable(rxFit));

        setupBaseObservableSuccess(observable);
        Completable.fromObservable(Observable.create(observable)).subscribe(sub);

        sub.assertCompleted();
        sub.assertNoValues();
    }

    @Test
    public void CheckConnectionCompletable_Error() {
        TestSubscriber<GoogleApiClient> sub = new TestSubscriber<>();
        CheckConnectionObservable observable = PowerMockito.spy(new CheckConnectionObservable(rxFit));

        setupBaseObservableError(observable);
        Completable.fromObservable(Observable.create(observable)).subscribe(sub);

        sub.assertError(GoogleAPIConnectionException.class);
        sub.assertNoValues();
    }


    @Test
    public void CheckConnectionCompletable_Resolution_Success() {
        TestSubscriber<GoogleApiClient> sub1 = new TestSubscriber<>();
        TestSubscriber<GoogleApiClient> sub2 = new TestSubscriber<>();
        final GoogleApiClient apiClient2 = Mockito.mock(GoogleApiClient.class);
        final CheckConnectionObservable observable = PowerMockito.spy(new CheckConnectionObservable(rxFit));

        Completable completable = Completable.fromObservable(Observable.create(observable));

        setupBaseObservableResolution(observable, apiClient);
        completable.subscribe(sub1);

        setupBaseObservableResolution(observable, apiClient2);
        completable.subscribe(sub2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                observable.onGoogleApiClientReady(apiClient, getSubscriber(observable, apiClient));
                return null;
            }
        }).when(apiClient).connect();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                observable.onGoogleApiClientReady(apiClient2, getSubscriber(observable, apiClient2));
                return null;
            }
        }).when(apiClient2).connect();

        BaseRx.onResolutionResult(Activity.RESULT_OK, Mockito.mock(ConnectionResult.class));

        sub1.assertCompleted();
        sub1.assertNoValues();

        sub2.assertCompleted();
        sub2.assertNoValues();
    }

    @Test
    public void CheckConnectionCompletable_Resolution_Error() {
        TestSubscriber<GoogleApiClient> sub1 = new TestSubscriber<>();
        TestSubscriber<GoogleApiClient> sub2 = new TestSubscriber<>();
        final GoogleApiClient apiClient2 = Mockito.mock(GoogleApiClient.class);
        final CheckConnectionObservable observable = PowerMockito.spy(new CheckConnectionObservable(rxFit));

        Completable completable = Completable.fromObservable(Observable.create(observable));

        setupBaseObservableResolution(observable, apiClient);
        completable.subscribe(sub1);

        setupBaseObservableResolution(observable, apiClient2);
        completable.subscribe(sub2);

        BaseObservable.onResolutionResult(Activity.RESULT_CANCELED, Mockito.mock(ConnectionResult.class));

        sub1.assertError(GoogleAPIConnectionException.class);
        sub1.assertNoValues();

        sub2.assertError(GoogleAPIConnectionException.class);
        sub2.assertNoValues();
    }

    @Test
    public void CheckConnectionCompletable_Resolution_Error_ResumeNext_Resolution_Success() {
        TestSubscriber<Void> sub = new TestSubscriber<>();
        ConnectionResult connectionResult = Mockito.mock(ConnectionResult.class);
        final CheckConnectionObservable observable = PowerMockito.spy(new CheckConnectionObservable(rxFit));

        setupBaseObservableResolution(observable, apiClient);
        Completable.fromObservable(Observable.create(observable).compose(RxGoogle.OnExceptionResumeNext.with(Observable.<Void>just(null))))
                .subscribe(sub);

        when(connectionResult.hasResolution()).thenReturn(true);
        BaseObservable.onResolutionResult(Activity.RESULT_CANCELED, connectionResult);

        sub.assertError(GoogleAPIConnectionException.class);
        sub.assertNoValues();
    }

    @Test
    public void CheckConnectionCompletable_Resolution_Error_ResumeNext_NoResolution_Success() {
        TestSubscriber<Void> sub = new TestSubscriber<>();
        ConnectionResult connectionResult = Mockito.mock(ConnectionResult.class);
        final CheckConnectionObservable observable = PowerMockito.spy(new CheckConnectionObservable(rxFit));

        setupBaseObservableResolution(observable, apiClient);
        Completable.fromObservable(Observable.create(observable).compose(RxGoogle.OnExceptionResumeNext.with(Observable.<Void>just(null))))
                .subscribe(sub);

        when(connectionResult.hasResolution()).thenReturn(false);
        BaseObservable.onResolutionResult(Activity.RESULT_CANCELED, connectionResult);

        sub.assertCompleted();
        sub.assertNoValues();
    }

    @Test
    public void CheckConnectionCompletable_Resolution_Success_ResumeNext_Error() {
        TestSubscriber<Void> sub = new TestSubscriber<>();
        ConnectionResult connectionResult = Mockito.mock(ConnectionResult.class);
        final CheckConnectionObservable observable = PowerMockito.spy(new CheckConnectionObservable(rxFit));

        setupBaseObservableResolution(observable, apiClient);
        Completable.fromObservable(Observable.create(observable).compose(RxGoogle.OnExceptionResumeNext.with(Observable.<Void>just(null))))
                .subscribe(sub);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                observable.onGoogleApiClientReady(apiClient, getSubscriber(observable, apiClient));
                return null;
            }
        }).when(apiClient).connect();

        doThrow(new Error("Generic error")).when(observable).onGoogleApiClientReady(Matchers.any(GoogleApiClient.class), Matchers.any(Subscriber.class));
        BaseObservable.onResolutionResult(Activity.RESULT_OK, connectionResult);

        sub.assertError(Error.class);
        sub.assertNoValues();
    }

    /*******
     * BLE *
     *******/

    // BleClaimDeviceObservable

    @Test
    public void BleClaimDeviceObservable_BleDevice_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        BleDevice bleDevice = Mockito.mock(BleDevice.class);
        BleClaimDeviceSingle single = PowerMockito.spy(new BleClaimDeviceSingle(rxFit, bleDevice, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(bleApi.claimBleDevice(apiClient, bleDevice)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void BleClaimDeviceObservable_DeviceAddress_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        String deviceAddress = "deviceAddress";
        BleClaimDeviceSingle single = PowerMockito.spy(new BleClaimDeviceSingle(rxFit, null, deviceAddress, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(bleApi.claimBleDevice(apiClient, deviceAddress)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void BleClaimDeviceObservable_BleDevice_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        BleDevice bleDevice = Mockito.mock(BleDevice.class);
        BleClaimDeviceSingle single = PowerMockito.spy(new BleClaimDeviceSingle(rxFit, bleDevice, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(bleApi.claimBleDevice(apiClient, bleDevice)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void BleClaimDeviceObservable_DeviceAddress_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        String deviceAddress = "deviceAddress";
        BleClaimDeviceSingle single = PowerMockito.spy(new BleClaimDeviceSingle(rxFit, null, deviceAddress, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(bleApi.claimBleDevice(apiClient, deviceAddress)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }


    // BleUnclaimDeviceObservable

    @Test
    public void BleUnclaimDeviceObservable_BleDevice_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        BleDevice bleDevice = Mockito.mock(BleDevice.class);
        BleUnclaimDeviceSingle single = PowerMockito.spy(new BleUnclaimDeviceSingle(rxFit, bleDevice, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(bleApi.unclaimBleDevice(apiClient, bleDevice)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void BleUnclaimDeviceObservable_DeviceAddress_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        String deviceAddress = "deviceAddress";
        BleUnclaimDeviceSingle single = PowerMockito.spy(new BleUnclaimDeviceSingle(rxFit, null, deviceAddress, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(bleApi.unclaimBleDevice(apiClient, deviceAddress)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void BleUnclaimDeviceObservable_BleDevice_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        BleDevice bleDevice = Mockito.mock(BleDevice.class);
        BleUnclaimDeviceSingle single = PowerMockito.spy(new BleUnclaimDeviceSingle(rxFit, bleDevice, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(bleApi.unclaimBleDevice(apiClient, bleDevice)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void BleUnclaimDeviceObservable_DeviceAddress_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        String deviceAddress = "deviceAddress";
        BleUnclaimDeviceSingle single = PowerMockito.spy(new BleUnclaimDeviceSingle(rxFit, null, deviceAddress, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(bleApi.unclaimBleDevice(apiClient, deviceAddress)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // BleListClaimedDevicesObservable

    @Test
    public void BleListClaimedDevicesObservable_WithDataType_Success() {
        TestSubscriber<List<BleDevice>> sub = new TestSubscriber<>();
        BleListClaimedDevicesSingle single = PowerMockito.spy(new BleListClaimedDevicesSingle(rxFit, dataType, null, null));

        BleDevicesResult bleDevicesResult = Mockito.mock(BleDevicesResult.class);

        List<BleDevice> bleDeviceList = new ArrayList<>();
        bleDeviceList.add(bleDevice);

        when(bleDevicesResult.getClaimedBleDevices(dataType)).thenReturn(bleDeviceList);

        setPendingResultValue(bleDevicesResult);
        when(bleDevicesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(bleApi.listClaimedBleDevices(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, bleDeviceList);
    }

    @Test
    public void BleListClaimedDevicesObservable_WithDataType_StatusException() {
        TestSubscriber<List<BleDevice>> sub = new TestSubscriber<>();
        BleListClaimedDevicesSingle single = PowerMockito.spy(new BleListClaimedDevicesSingle(rxFit, dataType, null, null));

        BleDevicesResult bleDevicesResult = Mockito.mock(BleDevicesResult.class);

        List<BleDevice> bleDeviceList = new ArrayList<>();
        bleDeviceList.add(bleDevice);

        when(bleDevicesResult.getClaimedBleDevices(dataType)).thenReturn(bleDeviceList);

        setPendingResultValue(bleDevicesResult);
        when(bleDevicesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(bleApi.listClaimedBleDevices(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void BleListClaimedDevicesObservable_Success() {
        TestSubscriber<List<BleDevice>> sub = new TestSubscriber<>();
        BleListClaimedDevicesSingle single = PowerMockito.spy(new BleListClaimedDevicesSingle(rxFit, null, null, null));

        BleDevicesResult bleDevicesResult = Mockito.mock(BleDevicesResult.class);

        List<BleDevice> bleDeviceList = new ArrayList<>();
        bleDeviceList.add(bleDevice);

        when(bleDevicesResult.getClaimedBleDevices()).thenReturn(bleDeviceList);

        setPendingResultValue(bleDevicesResult);
        when(bleDevicesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(bleApi.listClaimedBleDevices(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, bleDeviceList);
    }

    @Test
    public void BleListClaimedDevicesObservable_StatusException() {
        TestSubscriber<List<BleDevice>> sub = new TestSubscriber<>();
        BleListClaimedDevicesSingle single = PowerMockito.spy(new BleListClaimedDevicesSingle(rxFit, null, null, null));

        BleDevicesResult bleDevicesResult = Mockito.mock(BleDevicesResult.class);

        List<BleDevice> bleDeviceList = new ArrayList<>();
        bleDeviceList.add(bleDevice);

        when(bleDevicesResult.getClaimedBleDevices()).thenReturn(bleDeviceList);

        setPendingResultValue(bleDevicesResult);
        when(bleDevicesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(bleApi.listClaimedBleDevices(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // BleScanObservable

    @SuppressWarnings("MissingPermission")
    @Test
    public void BleScanObservable_Success() {
        TestSubscriber<BleDevice> sub = new TestSubscriber<>();
        BleScanObservable observable = PowerMockito.spy(new BleScanObservable(rxFit, null, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(bleApi.startBleScan(Matchers.any(GoogleApiClient.class), Matchers.any(StartBleScanRequest.class))).thenReturn(pendingResult);
        when(apiClient.isConnected()).thenReturn(true);

        setupBaseObservableSuccess(observable);
        Observable.create(observable).subscribe(sub);
        getSubscriber(observable, apiClient).onNext(bleDevice);

        verify(bleApi, never()).stopBleScan(Matchers.any(GoogleApiClient.class), Matchers.any(BleScanCallback.class));
        sub.unsubscribe();
        verify(bleApi).stopBleScan(Matchers.any(GoogleApiClient.class), Matchers.any(BleScanCallback.class));

        sub.assertNoTerminalEvent();
        sub.assertValue(bleDevice);
    }

    @SuppressWarnings("MissingPermission")
    @Test
    public void BleScanObservable_StatusException() {
        TestSubscriber<BleDevice> sub = new TestSubscriber<>();
        BleScanObservable observable = PowerMockito.spy(new BleScanObservable(rxFit, null, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(bleApi.startBleScan(Matchers.any(GoogleApiClient.class), Matchers.any(StartBleScanRequest.class))).thenReturn(pendingResult);
        when(apiClient.isConnected()).thenReturn(true);

        setupBaseObservableSuccess(observable);
        Observable.create(observable).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @SuppressWarnings("MissingPermission")
    @Test
    public void BleScanObservable_SecurityException() throws Exception {
        TestSubscriber<BleDevice> sub = new TestSubscriber<>();

        PowerMockito.doThrow(new SecurityException("Missing Bluetooth Admin permission")).when(bleApi).startBleScan(Matchers.any(GoogleApiClient.class), Matchers.any(StartBleScanRequest.class));

        BleScanObservable observable = PowerMockito.spy(new BleScanObservable(rxFit, null, null, null, null));

        setupBaseObservableSuccess(observable);
        Observable.create(observable).subscribe(sub);

        assertError(sub, SecurityException.class);
    }

    /**********
     * Config *
     **********/

    // ConfigCreateCustomDataTypeObservable

    @Test
    public void ConfigCreateCustomDataTypeObservable_Success() {
        TestSubscriber<DataType> sub = new TestSubscriber<>();
        DataTypeCreateRequest dataTypeCreateRequest = Mockito.mock(DataTypeCreateRequest.class);
        DataTypeResult dataTypeResult = Mockito.mock(DataTypeResult.class);
        ConfigCreateCustomDataTypeSingle single = PowerMockito.spy(new ConfigCreateCustomDataTypeSingle(rxFit, dataTypeCreateRequest, null, null));

        setPendingResultValue(dataTypeResult);
        when(dataTypeResult.getStatus()).thenReturn(status);
        when(dataTypeResult.getDataType()).thenReturn(dataType);
        when(status.isSuccess()).thenReturn(true);
        when(configApi.createCustomDataType(apiClient, dataTypeCreateRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, dataType);
    }

    @Test
    public void ConfigCreateCustomDataTypeObservable_StatusException() {
        TestSubscriber<DataType> sub = new TestSubscriber<>();
        DataTypeCreateRequest dataTypeCreateRequest = Mockito.mock(DataTypeCreateRequest.class);
        DataTypeResult dataTypeResult = Mockito.mock(DataTypeResult.class);
        ConfigCreateCustomDataTypeSingle single = PowerMockito.spy(new ConfigCreateCustomDataTypeSingle(rxFit, dataTypeCreateRequest, null, null));

        setPendingResultValue(dataTypeResult);
        when(dataTypeResult.getStatus()).thenReturn(status);
        when(dataTypeResult.getDataType()).thenReturn(dataType);
        when(status.isSuccess()).thenReturn(false);
        when(configApi.createCustomDataType(apiClient, dataTypeCreateRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // ConfigDisableFitObservable

    @Test
    public void ConfigDisableFitObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        ConfigDisableFitSingle single = PowerMockito.spy(new ConfigDisableFitSingle(rxFit, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(configApi.disableFit(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void ConfigDisableFitObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        ConfigDisableFitSingle single = PowerMockito.spy(new ConfigDisableFitSingle(rxFit, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(configApi.disableFit(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // ConfigReadDataTypeObservable

    @Test
    public void ConfigReadDataTypeObservable_Success() {
        TestSubscriber<DataType> sub = new TestSubscriber<>();
        String dataTypeName = "dataTypeName";
        DataTypeResult dataTypeResult = Mockito.mock(DataTypeResult.class);
        ConfigReadDataTypeSingle single = PowerMockito.spy(new ConfigReadDataTypeSingle(rxFit, dataTypeName, null, null));

        setPendingResultValue(dataTypeResult);
        when(dataTypeResult.getStatus()).thenReturn(status);
        when(dataTypeResult.getDataType()).thenReturn(dataType);
        when(status.isSuccess()).thenReturn(true);
        when(configApi.readDataType(apiClient, dataTypeName)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, dataType);
    }

    @Test
    public void ConfigReadDataTypeObservable_StatusException() {
        TestSubscriber<DataType> sub = new TestSubscriber<>();
        String dataTypeName = "dataTypeName";
        DataTypeResult dataTypeResult = Mockito.mock(DataTypeResult.class);
        ConfigReadDataTypeSingle single = PowerMockito.spy(new ConfigReadDataTypeSingle(rxFit, dataTypeName, null, null));

        setPendingResultValue(dataTypeResult);
        when(dataTypeResult.getStatus()).thenReturn(status);
        when(dataTypeResult.getDataType()).thenReturn(dataType);
        when(status.isSuccess()).thenReturn(false);
        when(configApi.readDataType(apiClient, dataTypeName)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    /***********
     * History *
     ***********/

    // HistoryDeleteDataObservable

    @Test
    public void HistoryDeleteDataObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        DataDeleteRequest dataDeleteRequest = Mockito.mock(DataDeleteRequest.class);
        HistoryDeleteDataSingle single = PowerMockito.spy(new HistoryDeleteDataSingle(rxFit, dataDeleteRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(historyApi.deleteData(apiClient, dataDeleteRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void HistoryDeleteDataObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        DataDeleteRequest dataDeleteRequest = Mockito.mock(DataDeleteRequest.class);
        HistoryDeleteDataSingle single = PowerMockito.spy(new HistoryDeleteDataSingle(rxFit, dataDeleteRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(historyApi.deleteData(apiClient, dataDeleteRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // HistoryInsertDataObservable

    @Test
    public void HistoryInsertDataObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        HistoryInsertDataSingle single = PowerMockito.spy(new HistoryInsertDataSingle(rxFit, dataSet, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(historyApi.insertData(apiClient, dataSet)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void HistoryInsertDataObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        HistoryInsertDataSingle single = PowerMockito.spy(new HistoryInsertDataSingle(rxFit, dataSet, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(historyApi.insertData(apiClient, dataSet)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // HistoryReadDailyTotalObservable

    @Test
    public void HistoryReadDailyTotalObservable_Success() {
        TestSubscriber<DataSet> sub = new TestSubscriber<>();
        DailyTotalResult dailyTotalResult = Mockito.mock(DailyTotalResult.class);
        HistoryReadDailyTotalSingle single = PowerMockito.spy(new HistoryReadDailyTotalSingle(rxFit, dataType, null, null));

        setPendingResultValue(dailyTotalResult);
        when(dailyTotalResult.getStatus()).thenReturn(status);
        when(dailyTotalResult.getTotal()).thenReturn(dataSet);
        when(status.isSuccess()).thenReturn(true);
        when(historyApi.readDailyTotal(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, dataSet);
    }

    @Test
    public void HistoryReadDailyTotalObservable_StatusException() {
        TestSubscriber<DataSet> sub = new TestSubscriber<>();
        DailyTotalResult dailyTotalResult = Mockito.mock(DailyTotalResult.class);
        HistoryReadDailyTotalSingle single = PowerMockito.spy(new HistoryReadDailyTotalSingle(rxFit, dataType, null, null));

        setPendingResultValue(dailyTotalResult);
        when(dailyTotalResult.getStatus()).thenReturn(status);
        when(dailyTotalResult.getTotal()).thenReturn(dataSet);
        when(status.isSuccess()).thenReturn(false);
        when(historyApi.readDailyTotal(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // HistoryReadDataObservable

    @Test
    public void HistoryReadDataObservable_Success() {
        TestSubscriber<DataReadResult> sub = new TestSubscriber<>();
        DataReadRequest dataReadRequest = Mockito.mock(DataReadRequest.class);
        DataReadResult dataReadResult = Mockito.mock(DataReadResult.class);
        HistoryReadDataSingle single = PowerMockito.spy(new HistoryReadDataSingle(rxFit, dataReadRequest, null, null));

        setPendingResultValue(dataReadResult);
        when(dataReadResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(historyApi.readData(apiClient, dataReadRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, dataReadResult);
    }

    @Test
    public void HistoryReadDataObservable_StatusException() {
        TestSubscriber<DataReadResult> sub = new TestSubscriber<>();
        DataReadRequest dataReadRequest = Mockito.mock(DataReadRequest.class);
        DataReadResult dataReadResult = Mockito.mock(DataReadResult.class);
        HistoryReadDataSingle single = PowerMockito.spy(new HistoryReadDataSingle(rxFit, dataReadRequest, null, null));

        setPendingResultValue(dataReadResult);
        when(dataReadResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(historyApi.readData(apiClient, dataReadRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // HistoryUpdateDataObservable

    @Test
    public void HistoryUpdateDataObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        DataUpdateRequest dataUpdateRequest = Mockito.mock(DataUpdateRequest.class);
        HistoryUpdateDataSingle single = PowerMockito.spy(new HistoryUpdateDataSingle(rxFit, dataUpdateRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(historyApi.updateData(apiClient, dataUpdateRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void HistoryUpdateDataObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        DataUpdateRequest dataUpdateRequest = Mockito.mock(DataUpdateRequest.class);
        HistoryUpdateDataSingle single = PowerMockito.spy(new HistoryUpdateDataSingle(rxFit, dataUpdateRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(historyApi.updateData(apiClient, dataUpdateRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }


    /*************
     * Recording *
     *************/

    // RecordingListSubscriptionsObservable

    @Test
    public void RecordingListSubscriptionsObservable_Success() {
        TestSubscriber<List<Subscription>> sub = new TestSubscriber<>();
        RecordingListSubscriptionsSingle single = PowerMockito.spy(new RecordingListSubscriptionsSingle(rxFit, null, null, null));

        ListSubscriptionsResult listSubscriptionsResult = Mockito.mock(ListSubscriptionsResult.class);

        List<Subscription> subscriptionList = new ArrayList<>();
        subscriptionList.add(Mockito.mock(Subscription.class));

        when(listSubscriptionsResult.getSubscriptions()).thenReturn(subscriptionList);

        setPendingResultValue(listSubscriptionsResult);
        when(listSubscriptionsResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(recordingApi.listSubscriptions(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, subscriptionList);
    }

    @Test
    public void RecordingListSubscriptionsObservable_StatusException() {
        TestSubscriber<List<Subscription>> sub = new TestSubscriber<>();
        RecordingListSubscriptionsSingle single = PowerMockito.spy(new RecordingListSubscriptionsSingle(rxFit, null, null, null));

        ListSubscriptionsResult listSubscriptionsResult = Mockito.mock(ListSubscriptionsResult.class);

        List<Subscription> subscriptionList = new ArrayList<>();
        subscriptionList.add(Mockito.mock(Subscription.class));

        when(listSubscriptionsResult.getSubscriptions()).thenReturn(subscriptionList);

        setPendingResultValue(listSubscriptionsResult);
        when(listSubscriptionsResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(recordingApi.listSubscriptions(apiClient)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void RecordingListSubscriptionsObservable_WithDataType_Success() {
        TestSubscriber<List<Subscription>> sub = new TestSubscriber<>();
        RecordingListSubscriptionsSingle single = PowerMockito.spy(new RecordingListSubscriptionsSingle(rxFit, dataType, null, null));

        ListSubscriptionsResult listSubscriptionsResult = Mockito.mock(ListSubscriptionsResult.class);

        List<Subscription> subscriptionList = new ArrayList<>();
        subscriptionList.add(Mockito.mock(Subscription.class));

        when(listSubscriptionsResult.getSubscriptions()).thenReturn(subscriptionList);

        setPendingResultValue(listSubscriptionsResult);
        when(listSubscriptionsResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(recordingApi.listSubscriptions(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, subscriptionList);
    }

    @Test
    public void RecordingListSubscriptionsObservable_WithDataType_StatusException() {
        TestSubscriber<List<Subscription>> sub = new TestSubscriber<>();
        RecordingListSubscriptionsSingle single = PowerMockito.spy(new RecordingListSubscriptionsSingle(rxFit, dataType, null, null));

        ListSubscriptionsResult listSubscriptionsResult = Mockito.mock(ListSubscriptionsResult.class);

        List<Subscription> subscriptionList = new ArrayList<>();
        subscriptionList.add(subscription);

        when(listSubscriptionsResult.getSubscriptions()).thenReturn(subscriptionList);

        setPendingResultValue(listSubscriptionsResult);
        when(listSubscriptionsResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(recordingApi.listSubscriptions(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // RecordingSubscribeObservable

    @Test
    public void RecordingSubscribeObservable_DataType_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingSubscribeSingle single = PowerMockito.spy(new RecordingSubscribeSingle(rxFit, null, dataType, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(recordingApi.subscribe(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }


    @Test
    public void RecordingSubscribeObservable_DataType_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingSubscribeSingle single = PowerMockito.spy(new RecordingSubscribeSingle(rxFit, null, dataType, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(recordingApi.subscribe(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void RecordingSubscribeObservable_DataSource_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingSubscribeSingle single = PowerMockito.spy(new RecordingSubscribeSingle(rxFit, dataSource, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(recordingApi.subscribe(apiClient, dataSource)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }


    @Test
    public void RecordingSubscribeObservable_DataSource_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingSubscribeSingle single = PowerMockito.spy(new RecordingSubscribeSingle(rxFit, dataSource, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(recordingApi.subscribe(apiClient, dataSource)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // RecordingSubscribeObservable

    @Test
    public void RecordingUnsubscribeObservable_DataType_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingUnsubscribeSingle single = PowerMockito.spy(new RecordingUnsubscribeSingle(rxFit, null, dataType, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(recordingApi.unsubscribe(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }


    @Test
    public void RecordingUnsubscribeObservable_DataType_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingUnsubscribeSingle single = PowerMockito.spy(new RecordingUnsubscribeSingle(rxFit, null, dataType, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(recordingApi.unsubscribe(apiClient, dataType)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void RecordingUnsubscribeObservable_DataSource_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingUnsubscribeSingle single = PowerMockito.spy(new RecordingUnsubscribeSingle(rxFit, dataSource, null, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(recordingApi.unsubscribe(apiClient, dataSource)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void RecordingUnsubscribeObservable_DataSource_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingUnsubscribeSingle single = PowerMockito.spy(new RecordingUnsubscribeSingle(rxFit, dataSource, null, null, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(recordingApi.unsubscribe(apiClient, dataSource)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void RecordingUnsubscribeObservable_Subscription_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingUnsubscribeSingle single = PowerMockito.spy(new RecordingUnsubscribeSingle(rxFit, null, null, subscription, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(recordingApi.unsubscribe(apiClient, subscription)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void RecordingUnsubscribeObservable_Subscription_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        RecordingUnsubscribeSingle single = PowerMockito.spy(new RecordingUnsubscribeSingle(rxFit, null, null, subscription, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(recordingApi.unsubscribe(apiClient, subscription)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }


    /***********
     * Sensors *
     ***********/

    // SensorsAddDataPointIntentObservable

    @Test
    public void SensorsAddDataPointIntentObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SensorsAddDataPointIntentSingle single = PowerMockito.spy(new SensorsAddDataPointIntentSingle(rxFit, sensorRequest, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(sensorsApi.add(apiClient, sensorRequest, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void SensorsAddDataPointIntentObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SensorsAddDataPointIntentSingle single = PowerMockito.spy(new SensorsAddDataPointIntentSingle(rxFit, sensorRequest, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(sensorsApi.add(apiClient, sensorRequest, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }


    // SensorsRemoveDataPointIntentObservable

    @Test
    public void SensorsRemoveDataPointIntentObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SensorsRemoveDataPointIntentSingle single = PowerMockito.spy(new SensorsRemoveDataPointIntentSingle(rxFit, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(sensorsApi.remove(apiClient, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void SensorsRemoveDataPointIntentObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SensorsRemoveDataPointIntentSingle single = PowerMockito.spy(new SensorsRemoveDataPointIntentSingle(rxFit, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(sensorsApi.remove(apiClient, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // SensorsDataPointObservable

    @Test
    public void SensorsDataPointObservable_Success() {
        TestSubscriber<Object> sub = new TestSubscriber<>();
        DataPoint dataPoint = Mockito.mock(DataPoint.class);
        SensorsDataPointObservable observable = PowerMockito.spy(new SensorsDataPointObservable(rxFit, sensorRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(sensorsApi.add(Matchers.any(GoogleApiClient.class), Matchers.any(SensorRequest.class), Matchers.any(OnDataPointListener.class))).thenReturn(pendingResult);
        when(apiClient.isConnected()).thenReturn(true);

        setupBaseObservableSuccess(observable);
        Observable.create(observable).subscribe(sub);
        getSubscriber(observable, apiClient).onNext(dataPoint);

        verify(sensorsApi, never()).remove(Matchers.any(GoogleApiClient.class), Matchers.any(OnDataPointListener.class));
        sub.unsubscribe();
        verify(sensorsApi).remove(Matchers.any(GoogleApiClient.class), Matchers.any(OnDataPointListener.class));

        sub.assertNoTerminalEvent();
        sub.assertValue(dataPoint);
    }

    @Test
    public void SensorsDataPointObservable_StatusException() {
        TestSubscriber<DataPoint> sub = new TestSubscriber<>();
        SensorsDataPointObservable observable = PowerMockito.spy(new SensorsDataPointObservable(rxFit, sensorRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(sensorsApi.add(Matchers.any(GoogleApiClient.class), Matchers.any(SensorRequest.class), Matchers.any(OnDataPointListener.class))).thenReturn(pendingResult);

        setupBaseObservableSuccess(observable);
        Observable.create(observable).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // SensorsFindDataSourcesObservable

    @Test
    public void SensorsFindDataSourcesObservable_Success() {
        TestSubscriber<List<DataSource>> sub = new TestSubscriber<>();
        DataSourcesRequest dataSourcesRequest = Mockito.mock(DataSourcesRequest.class);
        DataSourcesResult dataSourcesResult = Mockito.mock(DataSourcesResult.class);
        SensorsFindDataSourcesSingle single = PowerMockito.spy(new SensorsFindDataSourcesSingle(rxFit, dataSourcesRequest, null, null, null));

        List<DataSource> dataSourceList = new ArrayList<>();
        dataSourceList.add(dataSource);

        when(dataSourcesResult.getDataSources()).thenReturn(dataSourceList);

        setPendingResultValue(dataSourcesResult);
        when(dataSourcesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(sensorsApi.findDataSources(apiClient, dataSourcesRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, dataSourceList);
    }

    @Test
    public void SensorsFindDataSourcesObservable_StatusException() {
        TestSubscriber<List<DataSource>> sub = new TestSubscriber<>();
        DataSourcesRequest dataSourcesRequest = Mockito.mock(DataSourcesRequest.class);
        DataSourcesResult dataSourcesResult = Mockito.mock(DataSourcesResult.class);
        SensorsFindDataSourcesSingle single = PowerMockito.spy(new SensorsFindDataSourcesSingle(rxFit, dataSourcesRequest, null, null, null));

        List<DataSource> dataSourceList = new ArrayList<>();
        dataSourceList.add(dataSource);

        when(dataSourcesResult.getDataSources()).thenReturn(dataSourceList);

        setPendingResultValue(dataSourcesResult);
        when(dataSourcesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(sensorsApi.findDataSources(apiClient, dataSourcesRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    @Test
    public void SensorsFindDataSourcesObservable_WithDataType_Success() {
        TestSubscriber<List<DataSource>> sub = new TestSubscriber<>();
        DataSourcesRequest dataSourcesRequest = Mockito.mock(DataSourcesRequest.class);
        DataSourcesResult dataSourcesResult = Mockito.mock(DataSourcesResult.class);
        SensorsFindDataSourcesSingle single = PowerMockito.spy(new SensorsFindDataSourcesSingle(rxFit, dataSourcesRequest, dataType, null, null));

        List<DataSource> dataSourceList = new ArrayList<>();
        dataSourceList.add(dataSource);

        when(dataSourcesResult.getDataSources(dataType)).thenReturn(dataSourceList);

        setPendingResultValue(dataSourcesResult);
        when(dataSourcesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(sensorsApi.findDataSources(apiClient, dataSourcesRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, dataSourceList);
    }

    @Test
    public void SensorsFindDataSourcesObservable_WithDataType_StatusException() {
        TestSubscriber<List<DataSource>> sub = new TestSubscriber<>();
        DataSourcesRequest dataSourcesRequest = Mockito.mock(DataSourcesRequest.class);
        DataSourcesResult dataSourcesResult = Mockito.mock(DataSourcesResult.class);
        SensorsFindDataSourcesSingle single = PowerMockito.spy(new SensorsFindDataSourcesSingle(rxFit, dataSourcesRequest, dataType, null, null));

        List<DataSource> dataSourceList = new ArrayList<>();
        dataSourceList.add(dataSource);

        when(dataSourcesResult.getDataSources(dataType)).thenReturn(dataSourceList);

        setPendingResultValue(dataSourcesResult);
        when(dataSourcesResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(sensorsApi.findDataSources(apiClient, dataSourcesRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }


    /************
     * Sessions *
     ************/

    // SessionInsertObservable

    @Test
    public void SessionInsertObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        SessionInsertRequest sessionInsertRequest = Mockito.mock(SessionInsertRequest.class);
        SessionInsertSingle single = PowerMockito.spy(new SessionInsertSingle(rxFit, sessionInsertRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(sessionsApi.insertSession(apiClient, sessionInsertRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void SessionInsertObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        SessionInsertRequest sessionInsertRequest = Mockito.mock(SessionInsertRequest.class);
        SessionInsertSingle single = PowerMockito.spy(new SessionInsertSingle(rxFit, sessionInsertRequest, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(sessionsApi.insertSession(apiClient, sessionInsertRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // SessionRegisterObservable

    @Test
    public void SessionRegisterObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SessionRegisterSingle single = PowerMockito.spy(new SessionRegisterSingle(rxFit, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(sessionsApi.registerForSessions(apiClient, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void SessionRegisterObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SessionRegisterSingle single = PowerMockito.spy(new SessionRegisterSingle(rxFit, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(sessionsApi.registerForSessions(apiClient, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // SessionUnregisterObservable

    @Test
    public void SessionUnregisterObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SessionUnregisterSingle single = PowerMockito.spy(new SessionUnregisterSingle(rxFit, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(sessionsApi.unregisterForSessions(apiClient, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void SessionUnregisterObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
        SessionUnregisterSingle single = PowerMockito.spy(new SessionUnregisterSingle(rxFit, pendingIntent, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(sessionsApi.unregisterForSessions(apiClient, pendingIntent)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // SessionStartObservable

    @Test
    public void SessionStartObservable_Success() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        Session session = Mockito.mock(Session.class);
        SessionStartSingle single = PowerMockito.spy(new SessionStartSingle(rxFit, session, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(true);
        when(sessionsApi.startSession(apiClient, session)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, status);
    }

    @Test
    public void SessionStartObservable_StatusException() {
        TestSubscriber<Status> sub = new TestSubscriber<>();
        SessionStartSingle single = PowerMockito.spy(new SessionStartSingle(rxFit, session, null, null));

        setPendingResultValue(status);
        when(status.isSuccess()).thenReturn(false);
        when(sessionsApi.startSession(apiClient, session)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // SessionStopObservable

    @Test
    public void SessionStopObservable_Success() {
        TestSubscriber<List<Session>> sub = new TestSubscriber<>();
        String identifier = "identifier";
        SessionStopResult sessionStopResult = Mockito.mock(SessionStopResult.class);
        SessionStopSingle single = PowerMockito.spy(new SessionStopSingle(rxFit, identifier, null, null));

        List<Session> sessionList = new ArrayList<>();
        sessionList.add(session);

        when(sessionStopResult.getSessions()).thenReturn(sessionList);

        setPendingResultValue(sessionStopResult);
        when(sessionStopResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(sessionsApi.stopSession(apiClient, identifier)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, sessionList);
    }

    @Test
    public void SessionStopObservable_StatusException() {
        TestSubscriber<List<Session>> sub = new TestSubscriber<>();
        String identifier = "identifier";
        SessionStopResult sessionStopResult = Mockito.mock(SessionStopResult.class);
        SessionStopSingle single = PowerMockito.spy(new SessionStopSingle(rxFit, identifier, null, null));

        List<Session> sessionList = new ArrayList<>();
        sessionList.add(session);

        when(sessionStopResult.getSessions()).thenReturn(sessionList);

        setPendingResultValue(sessionStopResult);
        when(sessionStopResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(sessionsApi.stopSession(apiClient, identifier)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

    // SessionReadObservable

    @Test
    public void SessionReadObservable_Success() {
        TestSubscriber<SessionReadResult> sub = new TestSubscriber<>();
        SessionReadRequest sessionReadRequest = Mockito.mock(SessionReadRequest.class);
        SessionReadResult sessionReadResult = Mockito.mock(SessionReadResult.class);
        SessionReadSingle single = PowerMockito.spy(new SessionReadSingle(rxFit, sessionReadRequest, null, null));

        setPendingResultValue(sessionReadResult);
        when(sessionReadResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(true);
        when(sessionsApi.readSession(apiClient, sessionReadRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertSingleValue(sub, sessionReadResult);
    }

    @Test
    public void SessionReadObservable_StatusException() {
        TestSubscriber<SessionReadResult> sub = new TestSubscriber<>();
        SessionReadRequest sessionReadRequest = Mockito.mock(SessionReadRequest.class);
        SessionReadResult sessionReadResult = Mockito.mock(SessionReadResult.class);
        SessionReadSingle single = PowerMockito.spy(new SessionReadSingle(rxFit, sessionReadRequest, null, null));

        setPendingResultValue(sessionReadResult);
        when(sessionReadResult.getStatus()).thenReturn(status);
        when(status.isSuccess()).thenReturn(false);
        when(sessionsApi.readSession(apiClient, sessionReadRequest)).thenReturn(pendingResult);

        setupBaseSingleSuccess(single);
        Single.create(single).subscribe(sub);

        assertError(sub, StatusException.class);
    }

}
