package net.phfactor.meltdown.providers;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/*
 * New 10/24/13 - starting v2 of Meltdown, done as ContentProviders with an eye towards 
 * a migration into a SyncService.
 */
public class ItemProvider extends ContentProvider
{
	static final String TAG = "MeltdownItemProvider";
	public static final Uri URI = Uri.parse("content://net.phfactor.meltdown.itemprovider");
	static final String SINGLE_RECORD = "vnd.android.cursor.item/vnd.phfactor.meltdown.item";
	static final String MULTIPLE_RECORDS = "vnd.android.cursor.dir/vnd.phfactor.meltdown.items";
	public static final String AUTHORITY = "net.phfactor.meltdown.itemprovider";
	
	private dbHelper helper;
	private static final String DATABASE = "rss_items.db";
	private static final String TABLE = "items";
	private static final int DB_VERSION = 3;
	private static final String C_ID = "_id";
    public static final String C_IS_READ = "is_read";
    public static final String C_TITLE = "title";
    public static final String C_HTML = "html";
    public static final String C_URL = "url";
    public static final String C_FEVER_ID = "fever_id";
    public static final String C_CREATED_ON = "created_on";
    public static final String C_FEED_ID = "feed_id";
    public static final String C_IS_SAVED = "is_saved";
    public static final String C_AUTHOR = "author";
    
    public static final String SORT_ORDER = C_CREATED_ON + " DESC";
	
	public ItemProvider()
	{
		// As per docs, do init in onCreate, not in constructor.
		helper = null;
	}

	@Override
	public boolean onCreate()
	{
		/* https://developer.android.com/reference/android/content/ContentProvider.html
		 * suggests deferring the helper open until query is called, so we just return true
		 * to denote success here.
		 */
		Log.d(TAG, "CP created");
		return true;
	}
	@Override
	public String getType(Uri uri)
	{
		return this.getId(uri) < 0 ? SINGLE_RECORD : MULTIPLE_RECORDS;
	}

	private long getId(Uri uri)
	{
		String lastPathSegment = uri.getLastPathSegment();
		if (lastPathSegment != null)
		{
			try
			{
				return Long.parseLong(lastPathSegment);
			}
			catch (NumberFormatException e)
			{
				// no help here
			}
		}
		return -1;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		// As per docs, defer initialization to first call
		if (helper == null)
			helper = new dbHelper(getContext(), DB_VERSION);
		
		long id = helper.getWritableDatabase().insertOrThrow(TABLE, null, values);
		if (id == -1)
			throw new RuntimeException(String.format("%s: failed to insert %s to %s, reason unknown",
					TAG, values, uri));
		else
			return ContentUris.withAppendedId(uri, id);
	}
	
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		if (helper == null)
			helper = new dbHelper(getContext(), DB_VERSION);
		
		long id = this.getId(uri);
		SQLiteDatabase db = helper.getWritableDatabase();
		if (id < 0)
		{
			return db.update(TABLE, values, selection, selectionArgs);
		}
		else
		{
			return db.update(TABLE, values, C_ID + "=" + id, null);
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		if (helper == null)
			helper = new dbHelper(getContext(), DB_VERSION);

		long id = this.getId(uri);

		SQLiteDatabase db = helper.getWritableDatabase();
		if (id < 0)
		{
			return db.delete(TABLE, selection, selectionArgs);
		}
		else
		{
			return db.delete(TABLE, C_ID + "=" + id, null);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		if (helper == null)
			helper = new dbHelper(getContext(), DB_VERSION);

		Log.d(TAG, "query started");
		
		long id = this.getId(uri);
		SQLiteDatabase db = helper.getReadableDatabase();
		if (id < 0)
			return db.query(TABLE, projection, selection, selectionArgs, null, null, sortOrder, null);
		else
			return db.query(TABLE, projection, C_ID + "=" + id, null, null, null, null, null);
	}

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     * 
     * @note this is from the Google iosched (Google IO 2012) code.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException 
    {
    	Long tzero = System.currentTimeMillis();
    	
    	Log.i(TAG, "Bulk ops called");
		if (helper == null)
			helper = new dbHelper(getContext(), DB_VERSION);
        final SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) 
            {
            	try {
            		results[i] = operations.get(i).apply(this, results, i);
            	}
            	catch (SQLiteConstraintException sqe)
            	{
            		//Log.d(TAG, "ignoring constraint");
            	}
            }
            db.setTransactionSuccessful();
            Long deltat = System.currentTimeMillis() - tzero;
            
            Double ops_per_sec = operations.size() / ((float) deltat / 1000.0);
            Log.i(TAG, String.format("%d msec to do %d ops, %7.2f ops per second", deltat, operations.size(), ops_per_sec));
            return results;
            
        } finally {
            db.endTransaction();
        }
        
        
    }
    
	class dbHelper extends SQLiteOpenHelper
	{
		public dbHelper(Context ctx, int version_number)
		{
			super(ctx, DATABASE, null, version_number);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			try {
				db.execSQL("create table " + TABLE + " ( "+ C_ID +" int primary key, "
						+ C_IS_READ + " boolean,"
						+ C_TITLE + " text,"
						+ C_URL + " text,"
						+ C_HTML + " text,"
						+ C_FEVER_ID + " int unique,"
					    + C_CREATED_ON + " long,"
					    + C_FEED_ID + " int,"
					    + C_IS_SAVED + " boolean,"
					    + C_AUTHOR + " text)");
				
				// Want multiple indices to make mark-as-read and such faster.
				// See http://stackoverflow.com/questions/2767571/create-a-indexed-column-in-sqlite
				db.execSQL("create index is_read_idx ON " + TABLE + "(" + C_IS_READ + ")");
				db.execSQL("create index feed_id_idx ON " + TABLE + "(" + C_FEED_ID + ")");
				db.execSQL("create index fever_id_idx ON " + TABLE + "(" + C_FEVER_ID + ")");
				
				Log.i(TAG, "Database created");
			} catch (SQLException e) 
			{
				Log.e(TAG, "Unable to create helper, fatal error", e);
				e.printStackTrace();
			}				
		}	

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			try {
				db.execSQL("drop table " + TABLE);
				Log.w(TAG, "helper erased");
				this.onCreate(db);
			} catch (SQLException e)
			{
				Log.e(TAG, "Unable to upgrade helper, fatal error", e);
				e.printStackTrace();
			}
		}
	}
}
