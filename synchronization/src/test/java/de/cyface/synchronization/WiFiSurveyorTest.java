package de.cyface.synchronization;

import static android.os.Build.VERSION_CODES.KITKAT;
import static de.cyface.synchronization.TestUtils.ACCOUNT_TYPE;
import static de.cyface.synchronization.TestUtils.AUTHORITY;
import static de.cyface.synchronization.WiFiSurveyor.SYNC_INTERVAL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import de.cyface.utils.Validate;

/**
 * Tests the correct functionality of the <code>WiFiSurveyor</code> class.
 * <p>
 * Robolectric is used to emulate the Android context. The tests are currently executed explicitly with KITKAT SDK
 * as we did not yet port the tests to the newer Robolectric version (or we failed to).
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 1.1.4
 * @since 2.0.0
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = KITKAT) // Because these Roboelectric tests don't run on newer SDKs
public class WiFiSurveyorTest {

    /**
     * The Robolectric shadow used for the Android <code>ConnectivityManager</code>.
     */
    private ShadowConnectivityManager shadowConnectivityManager;
    /**
     * An object of the class under test.
     */
    private WiFiSurveyor oocut;
    /**
     * The Android test <code>Context</code> to use for testing.
     */
    private Context context;

    /**
     * Initializes the properties for each test case individually.
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        ConnectivityManager connectivityManager = getConnectivityManager();
        shadowConnectivityManager = Shadows.shadowOf(connectivityManager);
        oocut = new WiFiSurveyor(context, connectivityManager, AUTHORITY, ACCOUNT_TYPE);
    }

    /**
     * Tests that WiFi connectivity is detected correctly.
     *
     * @throws SynchronisationException This should not happen in the test environment. Occurs if no Android
     *             <code>Context</code> is available.
     */
    @Test
    public void testWifiConnectivity() throws SynchronisationException {

        Account account = oocut.createAccount("test", null);
        oocut.startSurveillance(account);
        ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);

        switchWiFiConnection(false);
        assertThat(oocut.isConnectedToSyncableNetwork(), is(equalTo(false)));

        switchWiFiConnection(true);
        assertThat(oocut.isConnectedToSyncableNetwork(), is(equalTo(true)));
        assertThat(oocut.isPeriodicSyncEnabled(), is(equalTo(true)));
        ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY);
    }

    /**
     * Tests if mobile and WiFi connectivity is detected correctly if both are allowed.
     *
     */
    @Test
    public void testMobileConnectivity() throws SynchronisationException {

        Account account = oocut.createAccount("test", null);
        oocut.startSurveillance(account);
        ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);

        switchMobileConnection(false);
        switchWiFiConnection(false);
        oocut.setSyncOnUnMeteredNetworkOnly(false);
        assertThat(oocut.isConnectedToSyncableNetwork(), is(equalTo(false)));

        switchMobileConnection(true);
        assertThat(oocut.isConnectedToSyncableNetwork(), is(equalTo(true)));
        assertThat(oocut.isPeriodicSyncEnabled(), is(equalTo(true)));
        ContentResolver.removePeriodicSync(account, AUTHORITY, Bundle.EMPTY);
    }

    /**
     * @return An appropriate <code>ConnectivityManager</code> from Robolectric.
     */
    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Switches the simulated state of the active network connection to either WiFi on or off.
     *
     * @param enabled If <code>true</code>, the connection is switched to on; if <code>false</code> it is switched to
     *            off.
     */
    private void switchWiFiConnection(final boolean enabled) {
        NetworkInfo networkInfoShadow = ShadowNetworkInfo.newInstance(
                enabled ? NetworkInfo.DetailedState.CONNECTED : NetworkInfo.DetailedState.DISCONNECTED,
                ConnectivityManager.TYPE_WIFI, 0, true,
                enabled ? NetworkInfo.State.CONNECTED : NetworkInfo.State.DISCONNECTED);
        shadowConnectivityManager.setNetworkInfo(ConnectivityManager.TYPE_WIFI, networkInfoShadow);
        if (enabled) {
            shadowConnectivityManager.setActiveNetworkInfo(networkInfoShadow);
        } else {
            shadowConnectivityManager.setActiveNetworkInfo(null);
        }
        Intent broadcastIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        broadcastIntent.putExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, enabled);
        context.sendBroadcast(broadcastIntent);
    }

    /**
     * Switches the simulated state of the active network connection to either mobile on or off.
     *
     * @param enabled If <code>true</code>, the connection is switched to on; if <code>false</code> it is switched to
     *            off.
     */
    private void switchMobileConnection(final boolean enabled) {
        NetworkInfo networkInfoShadow = ShadowNetworkInfo.newInstance(
                enabled ? NetworkInfo.DetailedState.CONNECTED : NetworkInfo.DetailedState.DISCONNECTED,
                ConnectivityManager.TYPE_MOBILE, 0, true,
                enabled ? NetworkInfo.State.CONNECTED : NetworkInfo.State.DISCONNECTED);
        shadowConnectivityManager.setNetworkInfo(ConnectivityManager.TYPE_MOBILE, networkInfoShadow);
        if (enabled) {
            shadowConnectivityManager.setActiveNetworkInfo(networkInfoShadow);
        } else {
            shadowConnectivityManager.setActiveNetworkInfo(null);
        }
        Intent broadcastIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        context.sendBroadcast(broadcastIntent);
    }

}
