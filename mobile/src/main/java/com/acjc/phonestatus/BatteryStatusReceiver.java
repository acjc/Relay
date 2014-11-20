package com.acjc.phonestatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import uk.co.acjc.phonestatus.common.LogUtil;
import uk.co.acjc.phonestatus.common.MessageContract;

public class BatteryStatusReceiver extends BroadcastReceiver {

    private static final String TAG = BatteryStatusReceiver.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onReceive(Context context, final Intent intent) {
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

                updateWatchNotification(mGoogleApiClient, intent);
                mGoogleApiClient.disconnect();
            }
        }).start();
    }

    public static Collection<Node> getNodes(GoogleApiClient googleApiClient) {
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        return nodes.getNodes();
    }

    public static void updateWatchNotification(GoogleApiClient googleApiClient, Intent batteryStatusIntent) {
        int batteryStatus = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        for (Node node : getNodes(googleApiClient)) {
            MessageApi.SendMessageResult result;
            if (batteryStatus == BatteryManager.BATTERY_STATUS_FULL) {
                LogUtil.d(TAG, "battery full");
                result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), MessageContract.BatteryStatus.CHARGED_KEY, null).await();
            } else if (batteryStatusIntent.getAction().equals(Intent.ACTION_POWER_CONNECTED) || batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                LogUtil.d(TAG, "battery charging");
                result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), MessageContract.BatteryStatus.CHARGING_KEY, null).await();
            } else if (batteryStatusIntent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
                LogUtil.d(TAG, "battery low");
                result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), MessageContract.BatteryStatus.BATTERY_LOW_KEY, null).await();
            } else {
                if (calculateBatteryPercentage(batteryStatusIntent) < 50) {
                    LogUtil.d(TAG, "discharging average health");
                    result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), MessageContract.BatteryStatus.DISCHARGING_AVERAGE_HEALTH_KEY, null).await();
                } else {
                    LogUtil.d(TAG, "discharging good health");
                    result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), MessageContract.BatteryStatus.DISCHARGING_GOOD_HEALTH_KEY, null).await();
                }
            }
            if (!result.getStatus().isSuccess()) {
                LogUtil.e(TAG, "failed to send Message: " + result.getStatus());
            }
        }
    }

    public static int calculateBatteryPercentage(Intent batteryStatusIntent) {
        int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (int) (100 * ((float) level / scale));
    }
}
