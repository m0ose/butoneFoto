package net.fabene.butone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TagsDbAdapter {

	public static final String KEY_WIDGETID = "widget_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_COLOR = "color";
    public static final String KEY_ROWID = "_id";
    public static final int USERNAME_ID = -2; //the username entry has this special widget id
    
    private static final String TAG = "TagsDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table tags (_id integer primary key autoincrement, title text not null, color integer not null, widget_id integer not null);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "tags";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS tags");
            onCreate(db);
        }
    }

    public TagsDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the tags database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public TagsDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        addDefaultUsername();
        return this;
    }
    
    public long addDefaultUsername() {
    	ContentValues defaultUserValues = new ContentValues();
    	defaultUserValues.put(KEY_TITLE, "AnonymousUser"+Math.round(1000*Math.random()));
    	defaultUserValues.put(KEY_COLOR, -1);
    	defaultUserValues.put(KEY_WIDGETID, USERNAME_ID);
    	return mDb.insert(DATABASE_TABLE, null, defaultUserValues);
    }

    public void close() {
        mDbHelper.close();
    }

    public long createTag(String title, int color, int widgId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_COLOR, color);
        initialValues.put(KEY_WIDGETID, widgId);
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public boolean deleteTag(long rowId) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

//    querying sqlite databases returns a cursor
//    that points to the first item in the query result
    
    public Cursor fetchAllTags() {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE, KEY_COLOR, KEY_WIDGETID}, KEY_WIDGETID + "!=" + USERNAME_ID, null, null, null, null);
    }

    public Cursor fetchTag(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE, KEY_COLOR, KEY_WIDGETID}, KEY_ROWID + "=" + rowId,
            		null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    public Cursor fetchTagOfWidget(int widgId) throws SQLException {

        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE, KEY_COLOR, KEY_WIDGETID}, KEY_WIDGETID + "=" + widgId,
            		null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    public boolean setUsername(String username) {
    	ContentValues args = new ContentValues();
        args.put(KEY_TITLE, username);
        return mDb.update(DATABASE_TABLE, args, KEY_WIDGETID + "=" + USERNAME_ID, null) > 0;
    }
    
    public Cursor getUsername() throws SQLException {
    	Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE, KEY_COLOR, KEY_WIDGETID}, KEY_WIDGETID + "=" + USERNAME_ID,
            		null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    public boolean updateTagColor(long rowId, int color) {
        ContentValues args = new ContentValues();
        args.put(KEY_COLOR, color);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public boolean clearTagsWithWidgetId(int widgId) {
    	ContentValues args = new ContentValues();
        args.put(KEY_WIDGETID, -1);
    	return mDb.update(DATABASE_TABLE, args, KEY_WIDGETID + "=" + widgId, null) > 0;
    }
    
    public boolean updateTagWidgetId(long rowId, int widgId) {
    	clearTagsWithWidgetId(widgId);
    	
    	ContentValues args = new ContentValues();
        args.put(KEY_WIDGETID, widgId);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
