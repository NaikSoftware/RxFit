package ua.naiksoftware.rxgoogle.location;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
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

    @Override
    protected ArrayList<String> getRequiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissions;
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    @Override
    protected void onGoogleApiClientReady(final GoogleApiClient apiClient, final Subscriber<? super Location> subscriber) {

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                subscriber.onNext(location);
            }
        };

        LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .build();

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(apiClient, locationSettingsRequest);
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // Unnecessary check
                        if (ActivityCompat.checkSelfPermission(apiClient.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(apiClient.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            subscriber.onError(new SecurityException("Location permissions not granted"));
                            return;
                        }
                        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, mLocationRequest, mLocationListener);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        resolveStatus(status);
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }

            }
        });
    }

    @Override
    protected void onUnsubscribed(GoogleApiClient apiClient) {
        if (mLocationListener == null) return;

        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, mLocationListener);
    }
}