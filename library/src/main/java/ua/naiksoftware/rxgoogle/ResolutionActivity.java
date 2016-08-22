package ua.naiksoftware.rxgoogle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;

import java.util.ArrayList;
import java.util.List;

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
public class ResolutionActivity extends Activity {

    public static final String ARG_CONNECTION_RESULT = "connectionResult";
    public static final String ARG_PERMISSIONS_LIST = "permissionsList";
    public static final String ARG_PERMISSIONS_REQUEST_CODE = "permissionsRequestCode";

    private static final int REQUEST_CODE_RESOLUTION = 123;
    private static final int REQUEST_CODE_PERMISSIONS = 124;

    private static boolean resolutionShown = false;

    private List<String> permissions;
    private int permissionsRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent.hasExtra(ARG_PERMISSIONS_LIST)) { // Resolve some permissions
            permissionsRequestCode = intent.getIntExtra(ARG_PERMISSIONS_REQUEST_CODE, -1);
            permissions = intent.getStringArrayListExtra(ARG_PERMISSIONS_LIST);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                setPermissionsResultAndFinish(permissions, permissions);
                return;
            }
            Context baseContext = getApplicationContext();
            List<String> missingPermissions = new ArrayList<>(permissions.size());
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(baseContext, permission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }
            int count = missingPermissions.size();
            if (count > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(missingPermissions.toArray(new String[count]), REQUEST_CODE_PERMISSIONS);
            }
        } else { // Resolve resolution for google api client
            try {
                ConnectionResult connectionResult = intent.getParcelableExtra(ARG_CONNECTION_RESULT);
                connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
                resolutionShown = true;
            } catch (IntentSender.SendIntentException | NullPointerException e) {
                setResolutionResultAndFinish(Activity.RESULT_CANCELED);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_RESOLUTION) {
            setResolutionResultAndFinish(resultCode);
        } else {
            setResolutionResultAndFinish(Activity.RESULT_CANCELED);
        }
    }

    private void setResolutionResultAndFinish(int resultCode) {
        resolutionShown = false;
        BaseRx.onResolutionResult(resultCode, (ConnectionResult) getIntent().getParcelableExtra(ARG_CONNECTION_RESULT));
        finish();
    }

    private void setPermissionsResultAndFinish(List<String> requestedPermissions, List<String> grantedPermissions) {
        BaseRx.onPermissionsResult(permissionsRequestCode, requestedPermissions, grantedPermissions);
        finish();
    }

    static boolean isResolutionShown() {
        return resolutionShown;
    }
}
