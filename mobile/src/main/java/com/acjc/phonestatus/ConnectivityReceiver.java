package com.acjc.phonestatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

import uk.co.acjc.phonestatus.common.LogUtil;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityReceiver.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onReceive(final Context context, Intent intent) {
        LogUtil.d(TAG, "onReceive");
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mGoogleApiClient.isConnected()) {
                    ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
                    if (!connectionResult.isSuccess()) {
                        LogUtil.e(TAG, "failed to connect to GoogleApiClient");
                        return;
                    }
                }

                PhoneStatusService.sendConnectivityStatus(context, mGoogleApiClient);
                mGoogleApiClient.disconnect();
            }
        }).start();
    }
}
