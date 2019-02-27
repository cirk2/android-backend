package de.cyface.synchronization;

import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static de.cyface.synchronization.Constants.TAG;

import java.lang.ref.WeakReference;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.cyface.utils.Validate;

/**
 * An instance of this class is responsible for surveying the state of the devices WiFi connection. If WiFi is active,
 * data is going to be synchronized continuously.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 4.0.0
 * @since 2.0.0
 */
public class WiFiSurveyor extends BroadcastReceiver {

    /**
     * The number of seconds in one minute. This value is used to calculate the data synchronisation interval.
     */
    private static final long SECONDS_PER_MINUTE = 60L;
    /**
     * The data synchronisation interval in minutes.
     * <p>
     * There is no particular reason for choosing 60 minutes. It seems reasonable and can be changed in the future.
     */
    private static final long SYNC_INTERVAL_IN_MINUTES = 60L;
    /**
     * Since we need to specify the sync interval in seconds, this constant transforms the interval in minutes to
     * seconds using {@link #SECONDS_PER_MINUTE}.
     */
    public static final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;
    /**
     * The <code>Account</code> currently used for data synchronization or <code>null</code> if no such
     * <code>Account</code> has been set.
     */
    private Account currentSynchronizationAccount;
    /**
     * The current Android context (i.e. Activity or Service).
     */
    private WeakReference<Context> context;
    /**
     * A flag that might be queried to see whether synchronization is active or not. This is <code>true</code> if
     * synchronization is active and <code>false</code> otherwise.
     */
    private boolean synchronizationIsActive;
    /**
     * If <code>true</code> the <code>MovebisDataCapturingService</code> synchronizes data only if
     * connected to a WiFi network; if <code>false</code> it synchronizes as soon as a data connection is
     * available. The second option might use up the users data plan rapidly so use it sparingly.
     */
    private boolean syncOnUnMeteredNetworkOnly;
    /**
     * The <code>ContentProvider</code> authority used by this service to store and read data. See the
     * <a href="https://developer.android.com/guide/topics/providers/content-providers.html">Android documentation</a>
     * for further information.
     */
    private final String authority;
    /**
     * A <code>String</code> identifying the account type of the accounts to use for data synchronization.
     */
    private final String accountType;
    /**
     * The Android <code>ConnectivityManager</code> used to check the device's current connection status.
     */
    final ConnectivityManager connectivityManager;
    /**
     * A callback which handles changes on the connectivity starting at API {@link Build.VERSION_CODES#LOLLIPOP}
     */
    private NetworkCallback networkCallback;

    /**
     * Creates a new completely initialized <code>WiFiSurveyor</code> within the current Android context.
     *
     * @param context The current Android context (i.e. Activity or Service).
     * @param connectivityManager The Android <code>ConnectivityManager</code> used to check the device's current
     *            connection status.
     * @param authority The <code>ContentProvider</code> authority used by this service to store and read data. See the
     *            <a href="https://developer.android.com/guide/topics/providers/content-providers.html">Android
     *            documentation</a>
     *            for further information.
     * @param accountType A <code>String</code> identifying the account type of the accounts to use for data
     *            synchronization.
     */
    public WiFiSurveyor(final @NonNull Context context, final @NonNull ConnectivityManager connectivityManager,
            final @NonNull String authority, final @NonNull String accountType) {
        this.context = new WeakReference<>(context);
        this.connectivityManager = connectivityManager;
        this.syncOnUnMeteredNetworkOnly = true;
        this.authority = authority;
        this.accountType = accountType;
    }

    /**
     * Starts the WiFi* connection status surveillance. If a WiFi connection is active data synchronization is started.
     * If the WiFi goes back down synchronization is deactivated.
     * <p>
     * The method also schedules an immediate synchronization run after the WiFi has been connected.
     * <p>
     * ATTENTION: If you use this method do not forget to call {@link #stopSurveillance()}, at some time in the future
     * or you will waste system resources.
     * <p>
     * ATTENTION: Starting at version {@link Build.VERSION_CODES#LOLLIPOP} and higher instead of expecting only "WiFi"
     * connections as "not metered" we use the {@link NetworkCapabilities#NET_CAPABILITY_NOT_METERED} as synonym as
     * suggested by Android.
     *
     * @param account Starts surveillance of the WiFi connection status for this account.
     * @throws SynchronisationException If no current Android <code>Context</code> is available.
     */
    public void startSurveillance(final @NonNull Account account) throws SynchronisationException {
        if (context.get() == null) {
            throw new SynchronisationException("No valid context available!");
        }

        currentSynchronizationAccount = account;
        scheduleSyncNow(); // Needs to be called after currentSynchronizationAccount is set

        // Roboelectric is currently only testing the deprecated code, see class documentation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
            if (syncOnUnMeteredNetworkOnly) {
                // Cleaner is "NET_CAPABILITY_NOT_METERED" but this is not yet available on the client (unclear why)
                requestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            }
            networkCallback = new NetworkCallback(this, currentSynchronizationAccount, authority);
            connectivityManager.registerNetworkCallback(requestBuilder.build(), networkCallback);
        } else {
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.get().registerReceiver(this, intentFilter);
        }
    }

    /**
     * Stops surveillance of the devices connection status. This frees up all used system resources.
     *
     * @throws SynchronisationException If no current Android <code>Context</code> is available.
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // Used by CyfaceDataCapturingService
    public void stopSurveillance() throws SynchronisationException {
        if (context.get() == null) {
            throw new SynchronisationException("No valid context available!");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (networkCallback == null) {
                Log.w(TAG, "Unable to unregister NetworkCallback because it's null.");
                return;
            }
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } else {
            try {
                context.get().unregisterReceiver(this);
            } catch (IllegalArgumentException e) {
                throw new SynchronisationException(e);
            }
        }
    }

    /**
     * Schedules data synchronization for right now. This does not mean synchronization is going to start immediately.
     * The Android system still decides when it is convenient.
     */
    public void scheduleSyncNow() {
        if (currentSynchronizationAccount == null) {
            Log.w(TAG, "scheduleSyncNow aborted, not account available.");
            return;
        }

        if (isConnected()) {
            final Bundle params = new Bundle();
            params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(currentSynchronizationAccount, authority, params);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (currentSynchronizationAccount == null) {
            Log.e(TAG, "No account for data synchronization registered with this service. Aborting synchronization.");
            return;
        }

        Validate.notNull(intent.getAction());
        final boolean connectivityChanged = intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION);
        if (!connectivityChanged) {
            return;
        }

        final boolean connectionLost = synchronizationIsActive() && !isConnected();
        final boolean connectionEstablished = !synchronizationIsActive() && isConnected();
        if (connectionEstablished) {
            if (!ContentResolver.getMasterSyncAutomatically()) {
                Log.d(TAG, "onCapabilitiesChanged: master sync is disabled. Aborting.");
                return;
            }

            // Enable auto-synchronization - periodic flag is always pre set for all account by us
            Log.v(TAG, "onReceive: setSyncAutomatically.");
            ContentResolver.setSyncAutomatically(currentSynchronizationAccount, authority, true);
            synchronizationIsActive = true;

        } else if (connectionLost) {

            Log.v(TAG, "onReceive: setSyncAutomatically to false");
            ContentResolver.setSyncAutomatically(currentSynchronizationAccount, authority, false);
            synchronizationIsActive = false;
        }
    }

    /**
     * Deletes a Cyface account from the Android {@code Account} system. Does silently nothing if no such
     * <code>Account</code> exists.
     * <p>
     * <b>ATTENTION:</b> SDK implementing apps which cannot use this method to remove an account need to call
     * {@code ContentResolver#removePeriodicSync()} themselves.
     *
     * @param username The username of the account to delete.
     */
    @SuppressWarnings("unused") // {@link MovebisDataCapturingService} uses this to deregister a token
    public void deleteAccount(final @NonNull String username) {
        final AccountManager accountManager = AccountManager.get(context.get());
        final Account account = new Account(username, accountType);

        if (!ContentResolver.getPeriodicSyncs(account, authority).isEmpty()) {
            ContentResolver.removePeriodicSync(account, authority, Bundle.EMPTY);
        }

        synchronized (this) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccount(account, null, null);
            } else {
                accountManager.removeAccountExplicitly(account);
            }
            currentSynchronizationAccount = null;
        }
    }

    /**
     * Creates a new {@code Account} which is required for the {@link WiFiSurveyor} to work.
     *
     * @param username The username of the account to be created.
     * @param password The password of the account to be created. May be null if a custom {@link CyfaceAuthenticator} is
     *            used instead of a LoginActivity to return tokens as in {@code MovebisDataCapturingService}.
     * @return The created {@code Account}
     */
    @NonNull
    @SuppressWarnings("unused") // Is used by MovebisDataCapturingService
    public Account createAccount(@NonNull final String username, @Nullable final String password) {

        final AccountManager accountManager = AccountManager.get(context.get());
        final Account newAccount = new Account(username, accountType);

        synchronized (this) {
            Validate.isTrue(accountManager.addAccountExplicitly(newAccount, password, Bundle.EMPTY));
            Validate.isTrue(accountManager.getAccountsByType(accountType).length == 1);
            Log.v(TAG, "New account added");

            makeAccountSyncable(newAccount, true);
        }

        return newAccount;
    }

    /**
     * Sets up an already existing {@code Account} to work with the {@link WiFiSurveyor}.
     * <p>
     * <b>ATTENTION:</b> SDK implementing apps need to use this method if they cannot use
     * {@link WiFiSurveyor#createAccount(String, String)}.
     * <p>
     * This has the following reasons:
     * - {@code ContentResolver#addPeriodicSync()} is always registered until {@link WiFiSurveyor#deleteAccount(String)}
     * is called
     * - {@code ContentResolver#setSyncAutomatically()} is automatically updated via {@link NetworkCallback}s and
     * defines if a connection is available which can be used for synchronization (dependent on
     * {@link WiFiSurveyor#setSyncOnUnMeteredNetworkOnly(boolean)}). Using this instead of the periodicSync flag fixed
     * MOV-535.
     * - {@code ContentResolver#setIsSyncable()} is used to disable synchronization manually and completely
     *
     * @param account The {@code Account} to be used for synchronization
     * @param enabled True if the synchronization should be enabled
     */
    @SuppressWarnings("unused") // Used by CyfaceDataCapturingService
    public void makeAccountSyncable(@NonNull final Account account, boolean enabled) {

        // Synchronization can be disabled via {@link #setSyncEnabled()}
        ContentResolver.setIsSyncable(account, authority, enabled ? 1 : 0);

        // PeriodicSync must always be on and is removed in {@code #removeAccount()}
        ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, SYNC_INTERVAL);
    }

    /**
     * This method retrieves an <code>Account</code> from the Android account system. If the <code>Account</code>
     * does not exist it throws an IllegalStateException as we require the default SDK using apps to have exactly one
     * account created in advance.
     *
     * @return The only <code>Account</code> existing
     */
    public Account getAccount() {
        final AccountManager accountManager = AccountManager.get(context.get());
        final Account[] cyfaceAccounts = accountManager.getAccountsByType(accountType);
        if (cyfaceAccounts.length == 0) {
            throw new IllegalStateException("No cyface account exists.");
        }
        if (cyfaceAccounts.length > 1) {
            throw new IllegalStateException("More than one cyface account exists.");
        }
        return cyfaceAccounts[0];
    }

    /**
     * Checks whether the device is connected with a syncable network (WiFi if {@link #syncOnUnMeteredNetworkOnly}).
     *
     * @return <code>true</code> if a syncable connection is available; <code>false</code> otherwise.
     */
    public boolean isConnected() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Validate.notNull(connectivityManager);
            final Network activeNetwork = connectivityManager.getActiveNetwork();
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities == null) {
                // This happened on Xiaomi Mi A2 Android 9.0 in the morning after capturing during the night
                return false;
            }

            final boolean isNotMeteredNetwork = networkCapabilities.hasCapability(NET_CAPABILITY_NOT_METERED);
            final boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            final boolean isSyncableNetwork = isConnected && (isNotMeteredNetwork || !syncOnUnMeteredNetworkOnly);

            Log.v(TAG, "isConnected: " + isSyncableNetwork + " (" + (isNotMeteredNetwork ? "not" : "") + " metered)"
                    + " (" + (syncOnUnMeteredNetworkOnly ? "" : "disabled") + " syncOnUnMeteredNetworkOnly)");
            return isSyncableNetwork;
        } else {
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

            final boolean isWifiNetwork = activeNetworkInfo != null
                    && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
            final boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            final boolean isSyncableNetwork = isConnected && (isWifiNetwork || !syncOnUnMeteredNetworkOnly);

            Log.v(TAG, "isConnected: " + isSyncableNetwork + " (" + (isWifiNetwork ? "not" : "") + " wifi)" + " ("
                    + (syncOnUnMeteredNetworkOnly ? "" : "disabled") + " syncOnUnMeteredNetworkOnly)");
            return isSyncableNetwork;
        }
    }

    /**
     * @return A flag that might be queried to see whether synchronization is active or not. This is <code>true</code>
     *         if synchronization is active and <code>false</code> otherwise.
     */
    public boolean synchronizationIsActive() {
        return synchronizationIsActive;
    }

    /**
     * Sets whether synchronization should happen only on
     * {@code android.net.NetworkCapabilities#NET_CAPABILITY_NOT_METERED}
     * networks or on all networks.
     * <p>
     * For Android devices lower than {@code android.os.Build.VERSION_CODES.LOLLIPOP}
     * {@code ConnectivityManager.TYPE_WIFI}
     * is used as a synonym for the more general "not metered" capability.
     *
     * @param state If {@code true} the {@link WiFiSurveyor} synchronizes data only if connected to a
     *            {@code android.net.NetworkCapabilities#NET_CAPABILITY_NOT_METERED} network; if
     *            {@code false} it synchronizes as soon as a data connection is available. The second option might use
     *            up the users data plan rapidly so use it sparingly. The default value is {@code true}.
     * @throws SynchronisationException If no current Android <code>Context</code> is available.
     */
    public void setSyncOnUnMeteredNetworkOnly(final boolean state) throws SynchronisationException {
        syncOnUnMeteredNetworkOnly = state;

        // This is required to update the NetworkCallback filter and the setSyncAutomatically state
        stopSurveillance();
        startSurveillance(currentSynchronizationAccount);
    }

    void setSynchronizationIsActive(boolean synchronizationIsActive) {
        this.synchronizationIsActive = synchronizationIsActive;
    }

    /**
     * @return True if synchronization is enabled
     */
    public boolean isSyncEnabled() {
        return ContentResolver.getIsSyncable(currentSynchronizationAccount, authority) == 1;
    }

    /**
     * Allows to enable or disable synchronization completely.
     *
     * @param enabled True if synchronization should be enabled
     */
    public void setSyncEnabled(final boolean enabled) {
        ContentResolver.setIsSyncable(currentSynchronizationAccount, authority, enabled ? 1 : 0);
    }
}
