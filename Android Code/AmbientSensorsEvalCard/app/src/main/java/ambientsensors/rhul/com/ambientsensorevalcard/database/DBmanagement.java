package ambientsensors.rhul.com.ambientsensorevalcard.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ambientsensors.rhul.com.ambientsensorevalcard.enums.SensorEnum;

public class DBmanagement extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "StoreData.db";
    private final String COLUMN_ID = "id";
    private final String COLUMN_PLACE = "place";
    private final String COLUMN_INIT_TIME = "init_time";
    private final String COLUMN_CANCELLED = "cancelled";
    private final String COLUMN_DATA = "data";
    private final String COLUMN_SHARED_ID = "shared_id";
    private final String TYPE_TEXT = "text";
    private final String TYPE_INT = "integer";
    private final String SQL_CREATE_TABLE = "create table";
    private final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ";
    private String tableName;

    public DBmanagement(Context context, String tableName) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.tableName = tableName;
    }

    public void writeToDB(String place, String initTime, String id, String data) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_PLACE, place);
        values.put(COLUMN_INIT_TIME, initTime);
        values.put(COLUMN_SHARED_ID, id);
        values.put(COLUMN_DATA, data);

        db.insertOrThrow(tableName, null, values);

        db.close();
    }

    public int getLastEntryNumber() {
        String query = "SELECT * FROM " + tableName + " WHERE " + COLUMN_CANCELLED + " != 1";
        int entryNo = -1;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        cursor.moveToLast();
        if (cursor.getCount() > 0)
            entryNo = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        cursor.close();

        db.close();

        return entryNo;
    }

    /**
     * Returns the number of entries in the DB that do not have status cancelled
     *
     * @return number of not-cancelled measurements
     */
    public int getTotalActiveEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        int total = (int) DatabaseUtils.queryNumEntries(db, tableName, COLUMN_CANCELLED + " = ? ",
                new String[]{"0"});

        db.close();

        return total;
    }

    /**
     * The total number of entries for a given location
     *
     * @param loc the location
     * @return number of entries in the specified location
     */
    public int getTotalActiveEntriesInLocation(String loc) {
        String query = "SELECT * FROM " + tableName + " WHERE " + COLUMN_CANCELLED + " != 1 AND "
                + COLUMN_PLACE + " == \"" + loc + "\"";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        int cnt = cursor.getCount();

        cursor.close();

        db.close();

        return cnt;
    }

    /**
     * Sets the value of an entry to cancelled. ATTENTION: does not delete the line
     *
     * @param entryNo the id of the row
     */
    public void deleteEntry(int entryNo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_CANCELLED, 1);

        db.update(tableName, values, COLUMN_ID + " = ? ", new String[]{Integer.toString(entryNo)});

        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (SensorEnum i : SensorEnum.values()) {
            String createDB = SQL_CREATE_TABLE + " " + i.name() + "( " + COLUMN_ID + " " + TYPE_INT
                    + " primary key" + ", " + COLUMN_PLACE + " " + TYPE_TEXT + ", "
                    + COLUMN_INIT_TIME + " " + TYPE_TEXT + ", " + COLUMN_CANCELLED
                    + " " + TYPE_INT + " default 0, " + COLUMN_SHARED_ID + " " + TYPE_TEXT + ", "
                    + COLUMN_DATA + " " + TYPE_TEXT + " )";

            db.execSQL(createDB);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES + " " + tableName);
        onCreate(db);
    }
}
