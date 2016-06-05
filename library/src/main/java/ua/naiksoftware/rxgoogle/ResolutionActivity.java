package ua.naiksoftware.rxgoogle;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;

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

    protected static final String ARG_CONNECTION_RESULT = "connectionResult";

    private static final int REQUEST_CODE_RESOLUTION = 123;

    private static boolean resolutionShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null) {
            handleIntent();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
    }

    private void handleIntent() {
        try {
            ConnectionResult connectionResult = getIntent().getParcelableExtra(ARG_CONNECTION_RESULT);
            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
            resolutionShown = true;
        } catch (IntentSender.SendIntentException|NullPointerException e) {
            setResolutionResultAndFinish(Activity.RESULT_CANCELED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_RESOLUTION) {
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

    static boolean isResolutionShown() {
        return resolutionShown;
    }
}
