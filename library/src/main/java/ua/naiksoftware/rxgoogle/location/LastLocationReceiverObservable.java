package ua.naiksoftware.rxgoogle.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

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

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final SingleSubscriber<? super Location> subscriber) {
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

        subscriber.onSuccess(LocationServices.FusedLocationApi.getLastLocation(apiClient));
    }
}
