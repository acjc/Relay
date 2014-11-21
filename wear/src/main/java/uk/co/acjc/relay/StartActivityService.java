package uk.co.acjc.relay;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

import uk.co.acjc.relay.common.MessageContract;

public class StartActivityService extends WearableListenerService {

    private static final String TAG = StartActivityService.class.getSimpleName();

    private static final int PHONE_STATUS_NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        Log.d(TAG, "message received: " + path);
        switch (path) {
            case MessageContract.BatteryStatus.CHARGED_KEY:
                updateNotification(R.string.charged, R.drawable.charging);
                break;
            case MessageContract.BatteryStatus.CHARGING_KEY:
                updateNotification(R.string.charging, R.drawable.charging);
                break;
            case MessageContract.BatteryStatus.DISCHARGING_GOOD_HEALTH_KEY:
                updateNotification(R.string.discharging, R.drawable.good_health);
                break;
            case MessageContract.BatteryStatus.DISCHARGING_AVERAGE_HEALTH_KEY:
                updateNotification(R.string.discharging, R.drawable.average_health);
                break;
            case MessageContract.BatteryStatus.BATTERY_LOW_KEY:
                updateNotification(this, R.string.low_battery, R.drawable.low_health, NotificationCompat.PRIORITY_HIGH);
                break;
            default:
                throw new IllegalStateException("unknown message path: " + path);
        }
    }

    private void updateNotification(@StringRes int contentText, @DrawableRes int background) {
        updateNotification(this, contentText, background, NotificationCompat.PRIORITY_LOW);
    }

    public static void updateNotification(Context context, @StringRes int contentId, @DrawableRes int backgroundId, int priority) {
        Intent notificationIntent = new Intent(context, PhoneStatusCard.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(contentId))
                .setSmallIcon(R.drawable.status_launcher)
                .setDefaults(0)
                .setLocalOnly(true)
                .setPriority(priority)
                .extend(new NotificationCompat.WearableExtender()
                        .setDisplayIntent(notificationPendingIntent)
                        .setBackground(BitmapFactory.decodeResource(context.getResources(), backgroundId))
                        .setCustomSizePreset(NotificationCompat.WearableExtender.SIZE_LARGE));

        if (priority >= NotificationCompat.PRIORITY_HIGH) {
            notification.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(PHONE_STATUS_NOTIFICATION_ID, notification.build());
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        updateNotification(R.string.disconnected, R.drawable.low_health);
    }
}
