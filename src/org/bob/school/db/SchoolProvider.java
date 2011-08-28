package org.bob.school.db;

import java.util.HashMap;
import java.util.Map;

import org.bob.school.Schule;
import org.bob.school.Schule.Constants;
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

	private static final class SchuelerDAO {
		// Begin DB specific
		public static final String TABLE_CREATE = "create table "
				+ Constants.SCHUELER_TABLE + " (" + BaseColumns._ID
				+ " integer primary key autoincrement, "
				+ Constants.SCHUELER_NACHNAME + " text not null, "
				+ Constants.SCHUELER_VORNAME + " text not null,"
				+ Constants.SCHUELER_KURSID + " integer not null,"
				+ "foreign key (" + Constants.SCHUELER_KURSID + ") references "
				+ Constants.KURS_TABLE + "(" + BaseColumns._ID + ") ON DELETE CASCADE);";
		// End DB specific
	}

	private static final class KursDAO {
		// Begin DB specific
		public static final String TABLE_CREATE = "create table " + Constants.KURS_TABLE
		+ " (" + BaseColumns._ID + " integer primary key autoincrement, "
		+ Constants.KURS_NAME + " text not null, "
		+ Constants.KURS_SDATE + " integer, "
		+ Constants.KURS_EDATE + " integer, "
		+ StringTools.arrayToString(Constants.KURS_WDAYS, ", ", null, " integer not null")
		+ ");";
		// End DB specific
	}

	private static final class MissDAO {
		// Begin DB specific
		public static final String TABLE_CREATE = "create table " + Constants.MISS_TABLE
		+ " (" + BaseColumns._ID + " integer primary key autoincrement, "
		+ Constants.MISS_DATUM + " integer not null, "
		+ Constants.MISS_STUNDEN_Z + " integer not null, "
		+ Constants.MISS_STUNDEN_NZ + " integer not null, "
		+ Constants.MISS_STUNDEN_E + " integer not null, "
		+ Constants.MISS_SCHUELERID + " integer not null, "
		+ "foreign key (" + Constants.MISS_SCHUELERID + ") references "
		+ Constants.SCHUELER_TABLE + "(" + BaseColumns._ID + ") ON DELETE CASCADE);";
		// End DB specific
	}
// 08-24 18:52:08.891: ERROR/Database(3300): Failure 1 (ambiguous column name: _id) on 0x2953f8 when preparing 'create view versaeumnis_sum as select schueler._id AS _id,sum(stunden) AS sum_stunden,sum(entschuldigt) AS sum_entschuldigt from schueler join versaeumnis on (_id=schuelerid) where zaehlen=1 group by _id order by sum_stunden desc, sum_entschuldigt desc'.

	private static final class PupilMissOverViewDAO {
		// Begin DB specific
		public static final String VIEW_CREATE = "create view " + Constants.PUPIL_MISS_VIEW
		+ " as select " + Constants.SCHUELER_TABLE + "." + BaseColumns._ID + " AS " + BaseColumns._ID + ","
		+ "sum(" + Constants.MISS_STUNDEN_Z + ") AS " + Constants.MISS_SUM_STUNDEN_Z + "," 
		+ "sum(" + Constants.MISS_STUNDEN_NZ + ") AS " + Constants.MISS_SUM_STUNDEN_NZ + "," 
		+ "sum(" + Constants.MISS_STUNDEN_E + ") AS " + Constants.MISS_SUM_STUNDEN_E 
		+ " from " + Constants.SCHUELER_TABLE + " join " + Constants.MISS_TABLE
		+ " on (" +  Constants.SCHUELER_TABLE + "." + BaseColumns._ID + "=" + Constants.MISS_SCHUELERID + ")"
		+ " group by " + Constants.SCHUELER_TABLE + "." + BaseColumns._ID
		+ " order by " + Constants.MISS_SUM_STUNDEN_Z + " desc, " + Constants.MISS_SUM_STUNDEN_E + " desc";
		// End DB specific
	}

	private SQLiteOpenHelper db_helper;
	private static final UriMatcher mUriMatcher;

    static {
    	StringBuilder b = new StringBuilder(Constants.COURSE_SEGMENT);
    	mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    	//  .../course
    	mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE);

    	b.append("/#");
    	//  .../course/#
    	mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE_ID);

    	StringBuilder b2 = new StringBuilder(Constants.MISS_SEGMENT);
    	//  .../miss
    	mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), MISS);
    	b2.append("/#");
    	//  .../miss/#    	
    	mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), MISS_ID);
    	b2 = new StringBuilder(b).append("/").append(Constants.MISS_SEGMENT);
    	// .../course/#/miss
    	mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), COURSE_MISS);

    	b.append("/").append(Constants.PUPIL_SEGMENT);
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
	            db.execSQL("DROP TABLE IF EXISTS " + Constants.PUPIL_MISS_VIEW);
	            db.execSQL("DROP TABLE IF EXISTS " + Constants.MISS_TABLE);	            db.execSQL("DROP TABLE IF EXISTS " + Constants.MISS_TABLE);	            db.execSQL("DROP TABLE IF EXISTS " + Constants.MISS_TABLE);
	            db.execSQL("DROP TABLE IF EXISTS " + Constants.SCHUELER_TABLE);
	            db.execSQL("DROP TABLE IF EXISTS " + Constants.KURS_TABLE);
	            onCreate(db);
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL("PRAGMA foreign_keys=ON;");
				db.execSQL(KursDAO.TABLE_CREATE);
				db.execSQL(SchuelerDAO.TABLE_CREATE);
				db.execSQL(MissDAO.TABLE_CREATE);
				db.execSQL(PupilMissOverViewDAO.VIEW_CREATE);
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
			qb.setTables(Constants.KURS_TABLE);
			break;
		case COURSE_ID: // path is .../course/#
			qb.setTables(Constants.KURS_TABLE);
			qb.appendWhere(Constants._ID + "=" + uri.getLastPathSegment());
			break;
		case COURSE_PUPIL: // path is .../course/#/pupil(?QUERY_SUM_MISS=1)
			if ("1".equals(uri.getQueryParameter(Constants.QUERY_SUM_MISS)))
				setMissSumsJoin(qb, uri, projection, projMap);
			else
				qb.setTables(Constants.SCHUELER_TABLE);

			qb.appendWhere(Constants.SCHUELER_KURSID + "="
					+ uri.getPathSegments().get(1));
			
			break;
		case PUPIL_ID: // path is .../course/#/pupil/#(?QUERY_SUM_MISS=1)
			if ("1".equals(uri.getQueryParameter(Constants.QUERY_SUM_MISS)))
				setMissSumsJoin(qb, uri, projection, projMap);
			else
				qb.setTables(Constants.SCHUELER_TABLE);

			qb.appendWhere(Constants.SCHUELER_TABLE + "." + Constants._ID + "="
					+ uri.getLastPathSegment());
			break;
		case MISS: // path is .../miss
			qb.setTables(Constants.MISS_TABLE);
			break;
		case COURSE_MISS: // path is .../course/#/miss
			qb.setTables(Constants.MISS_TABLE + " JOIN " + Constants.SCHUELER_TABLE
					+ " ON ("
					+ Constants.SCHUELER_TABLE + "." + Constants._ID + "="
					+ Constants.MISS_SCHUELERID + ")" );

			qb.appendWhere(Constants.SCHUELER_KURSID + "="
					+ uri.getPathSegments().get(1));

			if ("1".equals(uri
					.getQueryParameter(Constants.QUERY_DISTINCT_DATES_WITH_ID_HACK))) {
				groupBy = Constants.MISS_DATUM;
				projection = new String[] {
						"max(" + Constants.MISS_TABLE + "." + Constants._ID
								+ ") AS " + Constants._ID,
						Constants.MISS_DATUM,
						"sum(" + Constants.MISS_STUNDEN_Z + ") AS "
								+ Constants.MISS_SUM_STUNDEN_Z,
						"sum(" + Constants.MISS_STUNDEN_E + ") AS "
								+ Constants.MISS_SUM_STUNDEN_E };
			}

			break;
		case MISS_ID: // path is .../miss/#
			qb.setTables(Constants.MISS_TABLE);
			qb.appendWhere(Constants._ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("SchoolProvider.query: Unknown URI " + uri);
		}
		Log.d(TAG, qb.buildQuery(projection, selection, null, null, null, null, null));
		
		SQLiteDatabase db = db_helper.getReadableDatabase();

		return qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);
	}

	private void setMissSumsJoin(SQLiteQueryBuilder qb, Uri uri, String[] projection, Map<String, String> projMap) {
		qb.setTables(Constants.SCHUELER_TABLE + " LEFT JOIN "
				+ Constants.PUPIL_MISS_VIEW + " ON ("
				+ Constants.SCHUELER_TABLE + "." + Constants._ID + "="
				+ Constants.PUPIL_MISS_VIEW + "." + Constants._ID + ")");

		// use a projection map for ambiguous column names
		projMap.put(Constants._ID, Constants.SCHUELER_TABLE + "." + Constants._ID);
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
			table = Constants.KURS_TABLE;
			if (!values.containsKey(Constants.KURS_NAME))
				values.put(Constants.KURS_NAME, Resources.getSystem()
						.getString(android.R.string.untitled));
			if (!values.containsKey(Constants.KURS_SDATE))
				values.put(Constants.KURS_SDATE, now);
			if (!values.containsKey(Constants.KURS_EDATE))
				                             // now + half a year
				values.put(Constants.KURS_EDATE, now + 15552000000L);
			for(String s : Constants.KURS_WDAYS)
				if(!values.containsKey(s))
					values.put(s, 0);
			break;
		case COURSE_PUPIL:
			table = Constants.SCHUELER_TABLE;
			if (!values.containsKey(Constants.SCHUELER_NACHNAME)
					|| !values.containsKey(Constants.SCHUELER_VORNAME))
				throw new SQLException("Failed to insert pupil row into " + uri
						+ ". Missing name information.");
			else
				values.put(Constants.SCHUELER_KURSID, uri.getPathSegments().get(1));
			break;
		case MISS:
			table = Constants.MISS_TABLE;
			if (!values.containsKey(Constants.MISS_DATUM)
					|| !values.containsKey(Constants.MISS_STUNDEN_Z)
					|| !values.containsKey(Constants.MISS_STUNDEN_NZ)
					|| !values.containsKey(Constants.MISS_STUNDEN_E)
					|| !values.containsKey(Constants.MISS_SCHUELERID))
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
            count = db.update(Constants.KURS_TABLE, values, Constants._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        case PUPIL_ID:
            count = db.update(Constants.SCHUELER_TABLE, values, Constants._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
        	break;
        case MISS_ID:
            count = db.update(Constants.MISS_TABLE, values, Constants._ID + "=" + id
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
            count = db.delete(Constants.KURS_TABLE, Constants._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        case PUPIL_ID:
            count = db.delete(Constants.SCHUELER_TABLE, Constants._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
        case MISS_ID:
            count = db.delete(Constants.MISS_TABLE, Constants._ID + "=" + id
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
        	return Constants.CONTENT_COURSES_TYPE;

        case COURSE_ID:
        	return Constants.CONTENT_COURSE_TYPE;

        case COURSE_PUPIL:
        	return Constants.CONTENT_PUPILS_TYPE;

        case PUPIL_ID:
        	return Constants.CONTENT_PUPIL_TYPE;

        case MISS:
        	return Constants.CONTENT_MISSES_TYPE;

        case MISS_ID:
        	return Constants.CONTENT_MISS_TYPE;

        case COURSE_MISS:
        	return Constants.CONTENT_COURSE_MISS_TYPE;

        default:
            throw new IllegalArgumentException("SchoolProvider.getType: Unknown URI " + uri);
        }
	}
}