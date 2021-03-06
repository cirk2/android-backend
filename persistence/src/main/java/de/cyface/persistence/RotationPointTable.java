/*
 * Created on at 10:30.
 */
package de.cyface.persistence;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * <p>
 * A table for rotation points from the android gyroscope sensor.
 * </p>
 *
 * @author Klemens Muthmann
 * @version 1.1.0
 * @since 1.0.0
 */
public class RotationPointTable extends AbstractCyfaceMeasurementTable {
    /**
     * <p>
     * Creates a new completely initialized {@code RotationPointTable} using "rotation_points" as table name.
     * </p>
     */
    protected RotationPointTable() {
        super("rotation_points");
    }

    /**
     * <p>
     * Logging tag for Android logging.
     * </p>
     */
    static final String TAG = "RotationPointsTable";
    /**
     * <p>
     * Column name for the column storing the gyroscope value in X direction in [unit?] using the device coordinate system.
     * </p>
     */
    public static final String COLUMN_RX = "rx";
    /**
     * <p>
     * Column name for the column storing the gyroscope value in Y direction in [unit?] using the device coordinate system.
     * </p>
     */
    public static final String COLUMN_RY = "ry";
    /**
     * <p>
     * Column name for the column storing the gyroscope value in Z direction in [unit?] using the device coordinate system.
     * </p>
     */
    public static final String COLUMN_RZ = "rz";
    /**
     * <p>
     * Column name for the column storing the timestamp this point was captured at in milliseconds since 01.01.1970 (UNIX timestamp format).
     * </p>
     */
    public static final String COLUMN_TIME = "time";
    /**
     * <p>
     * Column name for the foreign key to the measurement this point belongs to.
     * </p>
     */
    public static final String COLUMN_MEASUREMENT_FK = "measurement_fk";
    /**
     * <p>
     * Column name for the column storing either a '1' if this point has been synchronized with a Cyface server and '0' otherwise.
     * </p>
     */
    public static final String COLUMN_IS_SYNCED = "is_synced";
    /**
     * <p>
     * An array containing all columns from this table in default order.
     * </p>
     */
    static final String[] COLUMNS = {BaseColumns._ID, COLUMN_RX, COLUMN_RY,
            COLUMN_RZ, COLUMN_TIME, COLUMN_MEASUREMENT_FK, COLUMN_IS_SYNCED};


    @Override protected String getCreateStatement() {
        return "CREATE TABLE " + getName() + "(" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_RX + " REAL NOT NULL, " + COLUMN_RY + " REAL NOT NULL, "
                + COLUMN_RZ + " REAL NOT NULL, " + COLUMN_TIME + " INTEGER NOT NULL, "
                + COLUMN_MEASUREMENT_FK + " INTEGER NOT NULL, " + COLUMN_IS_SYNCED + " INTEGER NOT NULL DEFAULT 0);";
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 3:
                // nothing to do here
                // no break, thus, the upgrade process continues with the next incremental upgrade step ->
            /*case X:
                Log.w(TAG, "Upgrading " + getName() + " from version X to {X+1}"); // For some reason this does not show up in log even though it's called
                db.execSQL(SQL_QUERY_HERE_FOR_UPGRADES_FROM_X_to_X+1);*/
        }
    }

    @Override
    protected String[] getDatabaseTableColumns() {
        return COLUMNS;
    }
}
