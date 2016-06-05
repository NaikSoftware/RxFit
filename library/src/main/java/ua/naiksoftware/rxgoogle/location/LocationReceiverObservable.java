package ua.naiksoftware.rxgoogle.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import ua.naiksoftware.rxgoogle.BaseObservable;
import ua.naiksoftware.rxgoogle.RxGoogle;

/**
 * Created by naik on 04.06.16.
 */
public class LocationReceiverObservable extends BaseObservable<Location> {

    private LocationRequest mLocationRequest;
    private LocationListener mLocationListener;

    public LocationReceiverObservable(@NonNull RxGoogle rxGoogle, LocationRequest locationRequest, Long timeout, TimeUnit timeUnit) {
        super(rxGoogle, timeout, timeUnit);
        mLocationRequest = locationRequest;
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final Subscriber<? super Location> subscriber) {
        if (ActivityCompat.checkSelfPermission(apiClient.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(apiClient.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            subscriber.onError(new SecurityException("Location permissions not granted"));
            return;
        }

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                subscriber.onNext(location);
            }
        };

        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, mLocationRequest, mLocationListener);
    }

    @Override
    protected void onUnsubscribed(GoogleApiClient apiClient) {
        if (mLocationListener == null) return;

        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, mLocationListener);
    }
}