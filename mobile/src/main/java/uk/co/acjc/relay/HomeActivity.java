package uk.co.acjc.relay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import uk.co.acjc.relay.common.LogUtil;

public class HomeActivity extends ActionBarActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    public static GoogleApiClient getClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    public static boolean blockingConnect(GoogleApiClient client) {
        ConnectionResult connectionResult = client.blockingConnect(30, TimeUnit.SECONDS);
        if (!connectionResult.isSuccess()) {
            LogUtil.e(TAG, "failed to connect to GoogleApiClient");
            return false;
        }
        return true;
    }

    public static Collection<Node> getNodes(GoogleApiClient client) {
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(client).await();
        return nodes.getNodes();
    }

    public static void showBasicInfoNotification(final Context context, final GoogleApiClient googleApiClient) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (googleApiClient == null || !googleApiClient.isConnected()) {
                    return;
                }
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent intent = context.registerReceiver(null, filter);
                BatteryStatusReceiver.updateWatchNotification(googleApiClient, intent);
            }
        }).start();
    }

    public static class HomeFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks {

        private static final long ANIMATION_START_DELAY = 2000;

        private SharedPreferences mPreferences;
        private GoogleApiClient mGoogleApiClient;

        private long mAnimationDuration;

        @InjectView(R.id.toolbar) Toolbar mToolbar;
        @InjectView(R.id.toolbar_shadow) View mToolbarShadow;
        @InjectView(R.id.basic_info_notification_shown_container) View mBasicInfoNotificationShownContainer;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_home, container, false);
            ButterKnife.inject(this, root);

            ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbar);
            ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            actionBar.setDisplayShowTitleEnabled(true);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mToolbarShadow.setVisibility(View.VISIBLE);
            }

            return root;
        }

        @OnClick(R.id.basic_info_button)
        void onClick() {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                showBasicInfoNotification(getActivity(), mGoogleApiClient);
                animateShownNotificationIcon(mBasicInfoNotificationShownContainer);
            } else {
                Toast.makeText(getActivity(), R.string.wearable_not_connected, Toast.LENGTH_SHORT).show();
            }
        }

        private void animateShownNotificationIcon(View iconContainer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                revealShownNotificationIcon(iconContainer);
            } else {
                iconContainer.setAlpha(1f);
                iconContainer.setVisibility(View.VISIBLE);
                iconContainer.animate().alpha(0).setStartDelay(ANIMATION_START_DELAY).setDuration(mAnimationDuration);
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void revealShownNotificationIcon(final View iconContainer) {
            Animator reveal = ViewAnimationUtils.createCircularReveal(
                    iconContainer,
                    iconContainer.getWidth() / 2,
                    iconContainer.getHeight() / 2,
                    0,
                    iconContainer.getWidth());
            reveal.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    iconContainer.setAlpha(1f);
                    iconContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    iconContainer.animate().alpha(0).setStartDelay(ANIMATION_START_DELAY).setDuration(mAnimationDuration);
                }
            });
            reveal.start();
        }

        @OnClick(R.id.basic_info_overflow)
        void onBasicInfoOverflowClick(View v) {
            showCardOverflowPopup(v, BasicInfoSender.PREF_BASIC_INFO_ON_CONNECT);
        }

        @OnClick(R.id.recent_images_overflow)
        void onRecentImagesOverflowClick(View v) {
            showCardOverflowPopup(v, BasicInfoSender.PREF_RECENT_IMAGES_ON_CONNECT);
        }

        private void showCardOverflowPopup(View root, final String prefKey) {
            PopupMenu popup = new PopupMenu(getActivity(), root);
            popup.getMenuInflater().inflate(R.menu.menu_card_overflow, popup.getMenu());
            MenuItem onConnect = popup.getMenu().findItem(R.id.card_overflow_on_connect);
            final boolean isChecked = mPreferences.getBoolean(prefKey, true);
            onConnect.setChecked(isChecked);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.card_overflow_on_connect:
                            mPreferences.edit().putBoolean(prefKey, !isChecked).apply();
                            break;
                        default:
                            throw new IllegalStateException("unknown menu item: " + item.getTitle());
                    }
                    return false;
                }
            });
            popup.show();
        }

        @Override
        public void onConnected(Bundle bundle) {
            showBasicInfoNotification(getActivity(), mGoogleApiClient);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            LogUtil.d(TAG, "onConnectionSuspended: " + cause);
        }
    }
}
