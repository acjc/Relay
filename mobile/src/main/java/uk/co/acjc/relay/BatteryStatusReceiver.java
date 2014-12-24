package uk.co.acjc.relay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import uk.co.acjc.relay.common.LogUtil;
import uk.co.acjc.relay.common.MessageContract;

public class BatteryStatusReceiver extends BroadcastReceiver {

    private static final String TAG = BatteryStatusReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, final Intent intent) {
        LogUtil.d(TAG, "onReceive");

        final GoogleApiClient client = HomeActivity.getClient(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!HomeActivity.blockingConnect(client)) {
                    return;
                }

                updateWatchNotification(client, intent);

                client.disconnect();
            }
        }).start();
    }

    public static void updateWatchNotification(GoogleApiClient client, Intent batteryStatusIntent) {
        int batteryStatus = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        for (Node node : HomeActivity.getNodes(client)) {
            MessageApi.SendMessageResult result;
            if (batteryStatus == BatteryManager.BATTERY_STATUS_FULL) {
                LogUtil.d(TAG, "battery full");
                result = Wearable.MessageApi.sendMessage(client, node.getId(), MessageContract.BatteryStatus.CHARGED_KEY, null).await();
            } else if (batteryStatusIntent.getAction().equals(Intent.ACTION_POWER_CONNECTED) || batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                LogUtil.d(TAG, "battery charging");
                result = Wearable.MessageApi.sendMessage(client, node.getId(), MessageContract.BatteryStatus.CHARGING_KEY, null).await();
            } else if (batteryStatusIntent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
                LogUtil.d(TAG, "battery low");
                result = Wearable.MessageApi.sendMessage(client, node.getId(), MessageContract.BatteryStatus.BATTERY_LOW_KEY, null).await();
            } else {
                if (calculateBatteryPercentage(batteryStatusIntent) < 50) {
                    LogUtil.d(TAG, "discharging average health");
                    result = Wearable.MessageApi.sendMessage(client, node.getId(), MessageContract.BatteryStatus.DISCHARGING_AVERAGE_HEALTH_KEY, null).await();
                } else {
                    LogUtil.d(TAG, "discharging good health");
                    result = Wearable.MessageApi.sendMessage(client, node.getId(), MessageContract.BatteryStatus.DISCHARGING_GOOD_HEALTH_KEY, null).await();
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
