package ua.naiksoftware.rxgoogle.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import rx.SingleSubscriber;
import ua.naiksoftware.rxgoogle.BaseSingle;
import ua.naiksoftware.rxgoogle.RxGoogle;

/**
 * Created by naik on 04.06.16.
 */
public class LastLocationReceiverObservable extends BaseSingle<Location> {

    public LastLocationReceiverObservable(@NonNull RxGoogle rxGoogle, Long timeout, TimeUnit timeUnit) {
        super(rxGoogle, timeout, timeUnit);
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
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final SingleSubscriber<? super Location> subscriber) {
        subscriber.onSuccess(LocationServices.FusedLocationApi.getLastLocation(apiClient));
    }
}
