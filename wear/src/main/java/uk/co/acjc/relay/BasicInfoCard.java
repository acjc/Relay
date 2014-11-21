package uk.co.acjc.relay;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.acjc.relay.common.MessageContract;

public class BasicInfoCard extends Activity implements DataApi.DataListener, NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = BasicInfoCard.class.getSimpleName();

    @InjectView(R.id.disconnected) TextView mDisconnected;
    @InjectView(R.id.loading) View mLoading;
    @InjectView(R.id.battery) TextView mBatteryStatus;
    @InjectView(R.id.connectivity) TextView mConnectivity;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        ButterKnife.inject(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
    }

    private Collection<Node> getNodes() {
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        return nodes.getNodes();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        mConnectivity.setVisibility(View.GONE);
        mBatteryStatus.setVisibility(View.GONE);
        mLoading.setVisibility(View.VISIBLE);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        stopStatusUpdates();
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
                switch (path) {
                    case MessageContract.BatteryInfo.BATTERY_LEVEL_PATH:
                        boolean isCharging = dataMap.getBoolean(MessageContract.BatteryInfo.BATTERY_CHARGING_KEY);
                        int batteryPercentage = dataMap.getInt(MessageContract.BatteryInfo.BATTERY_LEVEL_KEY);
                        Log.d(TAG, "battery level changed: " + batteryPercentage);
                        setBatteryInfo(batteryPercentage, isCharging);
                        break;
                    case MessageContract.Connectivity.CONNECTIVITY_PATH:
                        final int connectivity = dataMap.getInt(MessageContract.Connectivity.CONNECTIVITY_KEY);
                        final String ssid = dataMap.getString(MessageContract.Connectivity.SSID_KEY);
                        final int wifiStrength = dataMap.getInt(MessageContract.Connectivity.WIFI_STRENGTH_KEY);
                        final String mobileDataType = dataMap.getString(MessageContract.Connectivity.MOBILE_DATA_TYPE_KEY);
                        final int mobileDataStrength = dataMap.getInt(MessageContract.Connectivity.MOBILE_DATA_STRENGTH_KEY);
                        Log.d(TAG, "connectivity changed: connectivity [" + connectivity + "], wifi strength [" + wifiStrength + "], ssid [" + ssid + "], mobile data strength [" + mobileDataStrength + "]");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDisconnected.setVisibility(View.GONE);
                                mLoading.setVisibility(View.GONE);
                                mConnectivity.setVisibility(View.VISIBLE);
                                if (connectivity == ConnectivityManager.TYPE_WIFI) {
                                    setWifiConnectivity(ssid, wifiStrength);
                                } else if (connectivity == ConnectivityManager.TYPE_MOBILE) {
                                    setMobileConnectivity(mobileDataType, mobileDataStrength);
                                } else {
                                    mConnectivity.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_error_red_24dp), null, null, null);
                                    mConnectivity.setText(getString(R.string.limited));
                                }
                            }
                        });
                        break;
                    default:
                        throw new IllegalStateException("unknown data path: " + path);
                }
            }
        }
    }

    private void setBatteryInfo(final int batteryPercentage, final boolean isCharging) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mDisconnected.setVisibility(View.GONE);
                mLoading.setVisibility(View.GONE);
                mBatteryStatus.setVisibility(View.VISIBLE);
                mBatteryStatus.setText(getString(R.string.battery_percentage, batteryPercentage));
                Drawable batteryIcon = isCharging ? getResources().getDrawable(R.drawable.battery_level_charging) : getResources().getDrawable(R.drawable.battery_level_discharging);
                batteryIcon.setLevel(batteryPercentage);
                mBatteryStatus.setCompoundDrawablesWithIntrinsicBounds(batteryIcon, null, null, null);
            }
        });
    }

    private void setMobileConnectivity(String mobileDataType, int mobileDataStrength) {
        Drawable mobileDataIcon = getResources().getDrawable(R.drawable.mobile_data_signal);
        mobileDataIcon.setLevel(mobileDataStrength);
        mConnectivity.setCompoundDrawablesWithIntrinsicBounds(mobileDataIcon, null, null, null);
        mConnectivity.setText(mobileDataType);
    }

    private void setWifiConnectivity(String ssid, int wifiStrength) {
        Drawable wifiIcon = getResources().getDrawable(R.drawable.wifi_signal);
        wifiIcon.setLevel(wifiStrength);
        mConnectivity.setCompoundDrawablesWithIntrinsicBounds(wifiIcon, null, null, null);
        mConnectivity.setText(ssid);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        Wearable.NodeApi.addListener(mGoogleApiClient, BasicInfoCard.this);
        Wearable.DataApi.addListener(mGoogleApiClient, BasicInfoCard.this);
        requestStatusUpdates();
    }

    private void requestStatusUpdates() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            Log.e(TAG, "GoogleApiClient was not connected");
            return;
        }

        Log.d(TAG, "requestStatusUpdates");

        new Thread(new Runnable() {
            @Override
            public void run() {
                Collection<Node> nodes = getNodes();
                if (nodes == null || nodes.isEmpty()) {
                    showDisconnected();
                    return;
                }

                for (Node node : nodes) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), MessageContract.REQUEST_BASIC_INFO_PATH, null).await();
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "failed to send Message: " + result.getStatus());
                    }
                }
            }
        }).start();
    }

    private void stopStatusUpdates() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            return;
        }

        Log.d(TAG, "stopStatusUpdates");

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Node node : getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), MessageContract.STOP_BASIC_INFO_PATH, null).await();
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "failed to send Message: " + result.getStatus());
                    }
                }
                Wearable.DataApi.removeListener(mGoogleApiClient, BasicInfoCard.this);
                Wearable.NodeApi.removeListener(mGoogleApiClient, BasicInfoCard.this);
                mGoogleApiClient.disconnect();
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG, "onPeerConnected: " + node);
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "onPeerDisconnected: " + node);
        showDisconnected();
    }

    private void showDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBatteryStatus.setVisibility(View.GONE);
                mConnectivity.setVisibility(View.GONE);
                mLoading.setVisibility(View.GONE);
                mDisconnected.setVisibility(View.VISIBLE);
            }
        });
    }
}
