package uk.co.acjc.relay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.api.GoogleApiClient;

import uk.co.acjc.relay.common.LogUtil;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        LogUtil.d(TAG, "onReceive");

        final GoogleApiClient client = HomeActivity.getClient(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!HomeActivity.blockingConnect(client)) {
                    return;
                }

                BasicInfoSender.sendConnectivityStatus(context, client);

                client.disconnect();
            }
        }).start();
    }
}
