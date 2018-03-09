package de.cyface.synchronization;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import de.cyface.persistence.GpsPointsTable;
import de.cyface.persistence.MagneticValuePointTable;
import de.cyface.persistence.MeasuringPointsContentProvider;
import de.cyface.persistence.RotationPointTable;
import de.cyface.persistence.SamplePointTable;

/**
 * A wrapper for a <code>ContentProviderClient</code> used to provide access to one specific measurement.
 * <p>
 * ATTENTION: You must still close the provided <code>ContentProviderClient</code>.
 *
 * @author Klemens Muthmann
 * @version 1.1.0
 * @since 2.0.0
 */
public class MeasurementContentProviderClient {

    /**
     * The identifier of the measurement handled by this client.
     */
    private final long measurementIdentifier;
    /**
     * The client used to load the data to serialize from the <code>ContentProvider</code>.
     */
    private final ContentProviderClient client;

    /**
     * Creates a new completely initialized <code>MeasurementContentProviderClient</code> for one measurement, wrapping
     * a <code>ContentProviderClient</code>. The wrapped client is not closed by this object. You are still responsible
     * for closing it after you have finished communication with the <code>ContentProvider</code>.
     * 
     * @param measurementIdentifier The device wide unqiue identifier of the measurement to serialize.
     * @param client
     */
    MeasurementContentProviderClient(final long measurementIdentifier, final @NonNull ContentProviderClient client) {
        this.measurementIdentifier = measurementIdentifier;
        this.client = client;
    }

    /**
     * Loads all the geo locations for the measurement.
     *
     * @return A <code>Cursor</code> on the geo locations stored for the measurement.
     * @throws RemoteException If the content provider is not accessible.
     */
    Cursor loadGeoLocations() throws RemoteException {
        return client.query(MeasuringPointsContentProvider.GPS_POINTS_URI,
                new String[] {GpsPointsTable.COLUMN_GPS_TIME, GpsPointsTable.COLUMN_LAT, GpsPointsTable.COLUMN_LON,
                        GpsPointsTable.COLUMN_SPEED, GpsPointsTable.COLUMN_ACCURACY},
                GpsPointsTable.COLUMN_MEASUREMENT_FK + "=?",
                new String[] {Long.valueOf(measurementIdentifier).toString()}, null);
    }

    /**
     * Loads data points for the measurement. Such points might be accelerometer, gyroscope or compass points.
     *
     * @param serializer A serializer defining which kind of data points to load and how to access them.
     * @return A <code>Cursor</code> on one kind of data points stored for the measurement.
     * @throws RemoteException If the content provider is not accessible.
     */
    Cursor load3DPoint(final @NonNull Point3DSerializer serializer) throws RemoteException {
        return client.query(serializer.getTableUri(),
                new String[] {serializer.getTimestampColumnName(), serializer.getXColumnName(),
                        serializer.getYColumnName(), serializer.getZColumnName()},
                serializer.getMeasurementKeyColumnName() + "=?",
                new String[] {Long.valueOf(measurementIdentifier).toString()}, null);
    }

    /**
     * Cleans the measurement by deleting all data points (accelerations, rotations and directions). This operation can
     * not be revoked. Your data will be lost afterwards.
     *
     * @return The amount of deleted data points.
     * @throws RemoteException If the content provider is not accessible.
     */
    int cleanMeasurement() throws RemoteException {
        int ret = 0;
        ret += client.delete(MeasuringPointsContentProvider.SAMPLE_POINTS_URI,
                SamplePointTable.COLUMN_MEASUREMENT_FK + "=?",
                new String[] {Long.valueOf(measurementIdentifier).toString()});
        ret += client.delete(MeasuringPointsContentProvider.ROTATION_POINTS_URI,
                RotationPointTable.COLUMN_MEASUREMENT_FK + "=?",
                new String[] {Long.valueOf(measurementIdentifier).toString()});
        ret += client.delete(MeasuringPointsContentProvider.MAGNETIC_VALUE_POINTS_URI,
                MagneticValuePointTable.COLUMN_MEASUREMENT_FK + "=?",
                new String[] {Long.valueOf(measurementIdentifier).toString()});
        return ret;
    }
}