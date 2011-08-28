package org.bob.school.db;

import java.util.HashMap;
import java.util.Map;

import org.bob.school.Schule;
import org.bob.school.Schule.C;
import org.bob.school.tools.StringTools;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class SchoolProvider extends ContentProvider {
	public static final String DATABASE_NAME = "schooltool.db";
	public static final int DATABASE_VERSION = 1;
	public static final String TAG = "SchoolProvider";

	private static final int COURSE = 1;
	private static final int COURSE_ID = 2;
	private static final int COURSE_PUPIL = 3; //  .../course/#/pupil
	private static final int PUPIL_ID = 4;
	private static final int MISS = 5;
	private static final int MISS_ID = 6;
	private static final int COURSE_MISS = 7;

	// Table and View create statements
	private static final String SCHUELER_TABLE_CREATE = "create table "
			+ C.SCHUELER_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.SCHUELER_NACHNAME
			+ " text not null, " + C.SCHUELER_VORNAME + " text not null,"
			+ C.SCHUELER_KURSID + " integer not null," + "foreign key ("
			+ C.SCHUELER_KURSID + ") references " + C.KURS_TABLE + "("
			+ BaseColumns._ID + ") ON DELETE CASCADE);";

	private static final String KURS_TABLE_CREATE = "create table "
			+ C.KURS_TABLE
			+ " ("
			+ BaseColumns._ID
			+ " integer primary key autoincrement, "
			+ C.KURS_NAME
			+ " text not null, "
			+ C.KURS_SDATE
			+ " integer, "
			+ C.KURS_EDATE
			+ " integer, "
			+ StringTools.arrayToString(C.KURS_WDAYS, ", ", null,
					" integer not null") + ");";

	private static final String MISS_TABLE_CREATE = "create table "
			+ C.MISS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.MISS_DATUM
			+ " integer not null, " + C.MISS_STUNDEN_Z + " integer not null, "
			+ C.MISS_STUNDEN_NZ + " integer not null, " + C.MISS_STUNDEN_E
			+ " integer not null, " + C.MISS_SCHUELERID + " integer not null, "
			+ "foreign key (" + C.MISS_SCHUELERID + ") references "
			+ C.SCHUELER_TABLE + "(" + BaseColumns._ID
			+ ") ON DELETE CASCADE);";

	private static final String VIEW_CREATE = "create view " + C.PUPIL_MISS_VIEW
			+ " as select " + C.SCHUELER_TABLE + "." + BaseColumns._ID + " AS "
			+ BaseColumns._ID + "," + "sum(" + C.MISS_STUNDEN_Z + ") AS "
			+ C.MISS_SUM_STUNDEN_Z + "," + "sum(" + C.MISS_STUNDEN_NZ + ") AS "
			+ C.MISS_SUM_STUNDEN_NZ + "," + "sum(" + C.MISS_STUNDEN_E + ") AS "
			+ C.MISS_SUM_STUNDEN_E + " from " + C.SCHUELER_TABLE + " join "
			+ C.MISS_TABLE + " on (" + C.SCHUELER_TABLE + "." + BaseColumns._ID
			+ "=" + C.MISS_SCHUELERID + ")" + " group by " + C.SCHUELER_TABLE
			+ "." + BaseColumns._ID + " order by " + C.MISS_SUM_STUNDEN_Z
			+ " desc, " + C.MISS_SUM_STUNDEN_E + " desc";

	private SQLiteOpenHelper db_helper;
	private static final UriMatcher mUriMatcher;

    static {
    	StringBuilder b = new StringBuilder(C.COURSE_SEGMENT);
    	mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    	//  .../course
    	mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE);

    	b.append("/#");
    	//  .../course/#
    	mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE_ID);

    	StringBuilder b2 = new StringBuilder(C.MISS_SEGMENT);
    	//  .../miss
    	mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), MISS);
    	b2.append("/#");
    	//  .../miss/#    	
    	mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), MISS_ID);
    	b2 = new StringBuilder(b).append("/").append(C.MISS_SEGMENT);
    	// .../course/#/miss
    	mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), COURSE_MISS);

    	b.append("/").append(C.PUPIL_SEGMENT);
    	//  .../course/#/pupil
    	mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE_PUPIL);
    	b.append("/#");
    	//  .../course/#/pupil/#
    	mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), PUPIL_ID);
    }

	@Override
	public boolean onCreate() {
		db_helper = new SQLiteOpenHelper(getContext(), DATABASE_NAME, null,
				DATABASE_VERSION) {
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion,
					int newVersion) {
	            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	                    + newVersion + ", which will destroy all old data");
	            db.execSQL("DROP TABLE IF EXISTS " + C.PUPIL_MISS_VIEW);
	            db.execSQL("DROP TABLE IF EXISTS " + C.MISS_TABLE);	            db.execSQL("DROP TABLE IF EXISTS " + C.MISS_TABLE);	            db.execSQL("DROP TABLE IF EXISTS " + C.MISS_TABLE);
	            db.execSQL("DROP TABLE IF EXISTS " + C.SCHUELER_TABLE);
	            db.execSQL("DROP TABLE IF EXISTS " + C.KURS_TABLE);
	            onCreate(db);
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL("PRAGMA foreign_keys=ON;");
				db.execSQL(KURS_TABLE_CREATE);
				db.execSQL(SCHUELER_TABLE_CREATE);
				db.execSQL(MISS_TABLE_CREATE);
				db.execSQL(VIEW_CREATE);
			}

			@Override
			public void onOpen(SQLiteDatabase db) {
				super.onOpen(db);
				if (!db.isReadOnly()) {
					// Enable foreign key constraints
					db.execSQL("PRAGMA foreign_keys=ON;");
				}
			}
		};
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        Map<String, String> projMap = new HashMap<String, String>();

        String groupBy = null;

		switch (mUriMatcher.match(uri)) {
		case COURSE: // path is .../course
			qb.setTables(C.KURS_TABLE);
			break;
		case COURSE_ID: // path is .../course/#
			qb.setTables(C.KURS_TABLE);
			qb.appendWhere(C._ID + "=" + uri.getLastPathSegment());
			break;
		case COURSE_PUPIL: // path is .../course/#/pupil(?QUERY_SUM_MISS=1)
			if ("1".equals(uri.getQueryParameter(C.QUERY_SUM_MISS)))
				setMissSumsJoin(qb, uri, projection, projMap);
			else
				qb.setTables(C.SCHUELER_TABLE);

			qb.appendWhere(C.SCHUELER_KURSID + "="
					+ uri.getPathSegments().get(1));
			
			break;
		case PUPIL_ID: // path is .../course/#/pupil/#(?QUERY_SUM_MISS=1)
			if ("1".equals(uri.getQueryParameter(C.QUERY_SUM_MISS)))
				setMissSumsJoin(qb, uri, projection, projMap);
			else
				qb.setTables(C.SCHUELER_TABLE);

			qb.appendWhere(C.SCHUELER_TABLE + "." + C._ID + "="
					+ uri.getLastPathSegment());
			break;
		case MISS: // path is .../miss
			qb.setTables(C.MISS_TABLE);
			break;
		case COURSE_MISS: // path is .../course/#/miss
			qb.setTables(C.MISS_TABLE + " JOIN " + C.SCHUELER_TABLE
					+ " ON ("
					+ C.SCHUELER_TABLE + "." + C._ID + "="
					+ C.MISS_SCHUELERID + ")" );

			qb.appendWhere(C.SCHUELER_KURSID + "="
					+ uri.getPathSegments().get(1));

			if ("1".equals(uri
					.getQueryParameter(C.QUERY_DISTINCT_DATES_WITH_ID_HACK))) {
				groupBy = C.MISS_DATUM;
				projection = new String[] {
						"max(" + C.MISS_TABLE + "." + C._ID
								+ ") AS " + C._ID,
						C.MISS_DATUM,
						"sum(" + C.MISS_STUNDEN_Z + ") AS "
								+ C.MISS_SUM_STUNDEN_Z,
						"sum(" + C.MISS_STUNDEN_E + ") AS "
								+ C.MISS_SUM_STUNDEN_E };
			}

			break;
		case MISS_ID: // path is .../miss/#
			qb.setTables(C.MISS_TABLE);
			qb.appendWhere(C._ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("SchoolProvider.query: Unknown URI " + uri);
		}
		Log.d(TAG, qb.buildQuery(projection, selection, null, null, null, null, null));
		
		SQLiteDatabase db = db_helper.getReadableDatabase();

		return qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);
	}

	private void setMissSumsJoin(SQLiteQueryBuilder qb, Uri uri, String[] projection, Map<String, String> projMap) {
		qb.setTables(C.SCHUELER_TABLE + " LEFT JOIN "
				+ C.PUPIL_MISS_VIEW + " ON ("
				+ C.SCHUELER_TABLE + "." + C._ID + "="
				+ C.PUPIL_MISS_VIEW + "." + C._ID + ")");

		// use a projection map for ambiguous column names
		projMap.put(C._ID, C.SCHUELER_TABLE + "." + C._ID);
		/* fill the projection map with identity mappings for all columns
		   used in the projection */
		fillProjMap(projMap, projection);
		qb.setProjectionMap(projMap);
	}

	private void fillProjMap(Map<String, String> projMap, String[] projection) {
		for(String proj : projection)
			if(!projMap.containsKey(proj))
				projMap.put(proj, proj);
		
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentValues) {
		SQLiteDatabase db = db_helper.getWritableDatabase();
		String table = null;
		ContentValues values;

		if (contentValues != null)
			values = new ContentValues(contentValues);
		else
			values = new ContentValues();

		Long now = Long.valueOf(System.currentTimeMillis());

		switch (mUriMatcher.match(uri)) {
		case COURSE:
			table = C.KURS_TABLE;
			if (!values.containsKey(C.KURS_NAME))
				values.put(C.KURS_NAME, Resources.getSystem()
						.getString(android.R.string.untitled));
			if (!values.containsKey(C.KURS_SDATE))
				values.put(C.KURS_SDATE, now);
			if (!values.containsKey(C.KURS_EDATE))
				                             // now + half a year
				values.put(C.KURS_EDATE, now + 15552000000L);
			for(String s : C.KURS_WDAYS)
				if(!values.containsKey(s))
					values.put(s, 0);
			break;
		case COURSE_PUPIL:
			table = C.SCHUELER_TABLE;
			if (!values.containsKey(C.SCHUELER_NACHNAME)
					|| !values.containsKey(C.SCHUELER_VORNAME))
				throw new SQLException("Failed to insert pupil row into " + uri
						+ ". Missing name information.");
			else
				values.put(C.SCHUELER_KURSID, uri.getPathSegments().get(1));
			break;
		case MISS:
			table = C.MISS_TABLE;
			if (!values.containsKey(C.MISS_DATUM)
					|| !values.containsKey(C.MISS_STUNDEN_Z)
					|| !values.containsKey(C.MISS_STUNDEN_NZ)
					|| !values.containsKey(C.MISS_STUNDEN_E)
					|| !values.containsKey(C.MISS_SCHUELERID))
				throw new SQLException("Failed to insert miss row into " + uri
						+ ". Missing non-null information.");
			break;
		}

		long rowId = db.insert(table, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(uri, rowId);
        	getContext().getContentResolver().notifyChange(uri, null);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = db_helper.getWritableDatabase();
        int count;
        String id = uri.getLastPathSegment();
        switch (mUriMatcher.match(uri)) {
        case COURSE_ID:
            count = db.update(C.KURS_TABLE, values, C._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        case PUPIL_ID:
            count = db.update(C.SCHUELER_TABLE, values, C._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
        	break;
        case MISS_ID:
            count = db.update(C.MISS_TABLE, values, C._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("SchoolProvider.update: Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = db_helper.getWritableDatabase();
        int count;
        String id = uri.getLastPathSegment();
        switch (mUriMatcher.match(uri)) {
        case COURSE_ID:
            count = db.delete(C.KURS_TABLE, C._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        case PUPIL_ID:
            count = db.delete(C.SCHUELER_TABLE, C._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        case MISS_ID:
            count = db.delete(C.MISS_TABLE, C._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("SchoolProvider.delete: Unknown or unsupported URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	@Override
	public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
        case COURSE:
        	return C.CONTENT_COURSES_TYPE;

        case COURSE_ID:
        	return C.CONTENT_COURSE_TYPE;

        case COURSE_PUPIL:
        	return C.CONTENT_PUPILS_TYPE;

        case PUPIL_ID:
        	return C.CONTENT_PUPIL_TYPE;

        case MISS:
        	return C.CONTENT_MISSES_TYPE;

        case MISS_ID:
        	return C.CONTENT_MISS_TYPE;

        case COURSE_MISS:
        	return C.CONTENT_COURSE_MISS_TYPE;

        default:
            throw new IllegalArgumentException("SchoolProvider.getType: Unknown URI " + uri);
        }
	}
}