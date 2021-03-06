package de.cyface.persistence;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * <p>
 * A table for magnetic value points from the android magnetometer sensor (with hard iron calibration).
 * </p>
 */
public class MagneticValuePointTable extends AbstractCyfaceMeasurementTable {
    /**
     * <p>
     * Creates a new completely initialized {@code MagneticValuePointTable} using "magnetic_value_points" as table name.
     * </p>
     */
    protected MagneticValuePointTable() {
        super("magnetic_value_points");
    }

    /**
     * <p>
     * Logging tag for Android logging.
     * </p>
     */
    static final String TAG = "MagneticValuePointTable";
    /**
     * <p>
     * Column name for the column storing the magnetometer value in X direction in μT using the device coordinate system.
     * </p>
     */
    public static final String COLUMN_MX = "mx";
    /**
     * <p>
     * Column name for the column storing the magnetometer value in Y direction in μT using the device coordinate system.
     * </p>
     */
    public static final String COLUMN_MY = "my";
    /**
     * <p>
     * Column name for the column storing the magnetometer value in Z direction in μT using the device coordinate system.
     * </p>
     */
    public static final String COLUMN_MZ = "mz";
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
    static final String[] COLUMNS = {BaseColumns._ID, COLUMN_MX, COLUMN_MY,
            COLUMN_MZ, COLUMN_TIME, COLUMN_MEASUREMENT_FK, COLUMN_IS_SYNCED};


    @Override protected String getCreateStatement() {
        return "CREATE TABLE " + getName() + "(" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_MX + " REAL NOT NULL, " + COLUMN_MY + " REAL NOT NULL, "
                + COLUMN_MZ + " REAL NOT NULL, " + COLUMN_TIME + " INTEGER NOT NULL, "
                + COLUMN_MEASUREMENT_FK + " INTEGER NOT NULL, " + COLUMN_IS_SYNCED + " INTEGER NOT NULL DEFAULT 0);";
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading " + getName() + " from version " + oldVersion + " to " + newVersion + " ...");
        switch(oldVersion) {
            case 3:
                Log.w(TAG, "Upgrading " + getName() + " from version 3 to 4"); // For some reason this does not show up in log even though it's called
                onCreate(database);
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
