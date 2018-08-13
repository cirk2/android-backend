package de.cyface.datacapturing;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.NonNull;

import de.cyface.datacapturing.exception.SetupException;
import de.cyface.datacapturing.ui.CapturingNotification;
import de.cyface.synchronization.SynchronisationException;

/**
 * An implementation of a <code>DataCapturingService</code> using a dummy Cyface account for data synchronization.
 *
 * @author Klemens Muthmann
 * @version 4.0.0
 * @since 2.0.0
 */
public final class CyfaceDataCapturingService extends DataCapturingService {
    /**
     * Default dummy account used for data synchronization.
     */
    private final static String ACCOUNT = "default_account";

    /**
     * Creates a new completely initialized {@link DataCapturingService}.
     *
     * @param context The context (i.e. <code>Activity</code>) handling this service.
     * @param contentResolver Resolver used to access the content provider for storing measurements.
     * @param authority The <code>ContentProvider</code> authority used to identify the content provider used by this
     *            <code>DataCapturingService</code>. You should use something world wide unqiue, like your domain, to
     *            avoid collisions between different apps using the Cyface SDK.
     * @param dataUploadServerAddress The server address running an API that is capable of receiving data captured by
     *            this service.
     * @throws SetupException If writing the components preferences or registering the dummy user account fails.
     */
    public CyfaceDataCapturingService(final @NonNull Context context, final @NonNull ContentResolver contentResolver,
            final @NonNull String authority, final @NonNull String dataUploadServerAddress) throws SetupException {
        super(context, contentResolver, authority, dataUploadServerAddress);
        try {
            Account account = getWiFiSurveyor().getOrCreateAccount(ACCOUNT);
            getWiFiSurveyor().startSurveillance(account);
        } catch (SynchronisationException e) {
            throw new SetupException(e);
        }
    }
}
