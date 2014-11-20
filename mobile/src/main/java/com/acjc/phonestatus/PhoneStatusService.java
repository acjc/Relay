package com.acjc.phonestatus;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import uk.co.acjc.phonestatus.common.LogUtil;
import uk.co.acjc.phonestatus.common.MessageContract;

public class PhoneStatusService extends WearableListenerService {

    private static final String TAG = PhoneStatusService.class.getSimpleName();

    public static final String PREF_BASIC_INFO_ON_CONNECT = "uk.co.acjc.phonestatus.PREF_BASIC_INFO_ON_CONNECT";
    public static final String PREF_RECENT_IMAGES_ON_CONNECT = "uk.co.acjc.phonestatus.PREF_RECENT_IMAGES_ON_CONNECT";
    private static final int LIMITED_CONNECTION = -1;

    private SharedPreferences mPreferences;
    private BatteryLevelReceiver mBatteryLevelReceiver;
    private GoogleApiClient mGoogleApiClient;
    private final PhoneStateListener mSignalStrengthListener = new CustomPhoneStateListener();
    private boolean mReceiversRegistered = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onDestroy() {
        if (mReceiversRegistered) {
            unregisterReceiver(mBatteryLevelReceiver);
            mReceiversRegistered = false;
        }
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        Log.d(TAG, "message received: " + path);
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        switch (path) {
            case MessageContract.REQUEST_PHONE_STATUS_PATH:
                if (!mReceiversRegistered) {
                    enableConnectivityReceiver();

                    // Calls onSignalStrengthsChanged with the current signal strength
                    tm.listen(mSignalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

                    mBatteryLevelReceiver = new BatteryLevelReceiver();
                    registerReceiver(mBatteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

                    sendConnectivityStatus(this, mGoogleApiClient);

                    mReceiversRegistered = true;
                }
                break;
            case MessageContract.STOP_PHONE_STATUS_PATH:
                if (mReceiversRegistered) {
                    disableConnectivityReceiver();

                    tm.listen(mSignalStrengthListener, PhoneStateListener.LISTEN_NONE);

                    unregisterReceiver(mBatteryLevelReceiver);

                    mReceiversRegistered = false;
                }
                break;
            default:
                throw new IllegalStateException("unknown message path: " + path);
        }
    }

    private void disableConnectivityReceiver() {
        ComponentName receiver = new ComponentName(this, ConnectivityReceiver.class);

        getPackageManager().setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void enableConnectivityReceiver() {
        ComponentName receiver = new ComponentName(this, ConnectivityReceiver.class);

        getPackageManager().setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void sendConnectivityStatus(Context context, GoogleApiClient googleApiClient) {
        if (googleApiClient == null || !googleApiClient.isConnected()) {
            Log.e(TAG, "GoogleApiClient was not connected");
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        int connectivity = LIMITED_CONNECTION;
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            connectivity = activeNetwork.getType();
        }

        PutDataMapRequest dataMap = PutDataMapRequest.create(MessageContract.Connectivity.CONNECTIVITY_PATH);
        dataMap.getDataMap().putLong(MessageContract.TIMESTAMP_KEY, System.currentTimeMillis());
        dataMap.getDataMap().putInt(MessageContract.Connectivity.CONNECTIVITY_KEY, connectivity);

        switch (connectivity) {
            case ConnectivityManager.TYPE_WIFI:
                WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                dataMap.getDataMap().putString(MessageContract.Connectivity.SSID_KEY, wifiInfo.getSSID());
                int wifiStrength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
                dataMap.getDataMap().putInt(MessageContract.Connectivity.WIFI_STRENGTH_KEY, wifiStrength);
                LogUtil.d(TAG, "connectivity: WIFI, SSID [" + wifiInfo.getSSID() + "], strength [" + wifiStrength + "]");
                break;
            case ConnectivityManager.TYPE_MOBILE:
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                dataMap.getDataMap().putString(MessageContract.Connectivity.MOBILE_DATA_TYPE_KEY, activeNetwork.getSubtypeName());
                int mobileDataStrength = -1;
                for (CellInfo cellInfo : tm.getAllCellInfo()) {
                    if (cellInfo instanceof CellInfoGsm) {
                        mobileDataStrength = ((CellInfoGsm) cellInfo).getCellSignalStrength().getLevel();
                        break;
                    } else if (cellInfo instanceof CellInfoLte) {
                        mobileDataStrength = ((CellInfoLte) cellInfo).getCellSignalStrength().getLevel();
                        break;
                    } else if (cellInfo instanceof CellInfoCdma) {
                        mobileDataStrength = ((CellInfoCdma) cellInfo).getCellSignalStrength().getLevel();
                        break;
                    } else if (cellInfo instanceof CellInfoWcdma) {
                        mobileDataStrength = ((CellInfoWcdma) cellInfo).getCellSignalStrength().getLevel();
                        break;
                    }
                }
                dataMap.getDataMap().putInt(MessageContract.Connectivity.MOBILE_DATA_STRENGTH_KEY, mobileDataStrength);
                LogUtil.d(TAG, "connectivity: mobile, type [" + activeNetwork.getSubtypeName() + "], strength [" + mobileDataStrength + "]");
                break;
        }
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, request).await();
    }

    public class BatteryLevelReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
                LogUtil.e(TAG, "GoogleApiClient was not connected");
                return;
            }

            int batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatus == BatteryManager.BATTERY_STATUS_FULL;
            int batteryPercentage = BatteryStatusReceiver.calculateBatteryPercentage(intent);
            LogUtil.d(TAG, "battery percentage: " + batteryPercentage);
            sendBatteryInfo(batteryPercentage, isCharging);
        }

        private void sendBatteryInfo(int batteryLevel, boolean isCharging) {
            PutDataMapRequest dataMap = PutDataMapRequest.create(MessageContract.BatteryInfo.BATTERY_LEVEL_PATH);
            dataMap.getDataMap().putLong(MessageContract.TIMESTAMP_KEY, System.currentTimeMillis());
            dataMap.getDataMap().putInt(MessageContract.BatteryInfo.BATTERY_LEVEL_KEY, batteryLevel);
            dataMap.getDataMap().putBoolean(MessageContract.BatteryInfo.BATTERY_CHARGING_KEY, isCharging);
            PutDataRequest request = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        if (mPreferences.getBoolean(PREF_BASIC_INFO_ON_CONNECT, true)) {
            HomeActivity.showBasicInfoNotification(this, mGoogleApiClient);
        }
        if (mPreferences.getBoolean(PREF_RECENT_IMAGES_ON_CONNECT, true)) {
            // show recent images notification
        }
    }

    private class CustomPhoneStateListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendConnectivityStatus(PhoneStatusService.this, mGoogleApiClient);
                }
            }).start();
        }
    }
}
