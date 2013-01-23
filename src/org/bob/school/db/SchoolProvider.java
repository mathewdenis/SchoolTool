package org.bob.school.db;

import java.util.HashMap;
import java.util.Map;

import org.bob.school.R;
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
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class SchoolProvider extends ContentProvider {
	public static final String DATABASE_NAME = "schooltool.db";
	public static final int DATABASE_VERSION = 4;
	public static final String TAG = "SchoolProvider";

	private static final int COURSE = 1;
	private static final int COURSE_ID = 2;
	private static final int COURSE_PUPIL = 3; // .../course/#/pupil
	private static final int PUPIL_ID = 4;
	private static final int MISS = 5;
	private static final int MISS_ID = 6;
	private static final int COURSE_MISS = 7;
	private static final int SETTINGS = 8;
	private static final int COURSE_CALENDAR = 9;

	// Table and View create statements

	private SQLiteOpenHelper db_helper;
	private static final UriMatcher mUriMatcher;

	static {
		StringBuilder b = new StringBuilder(C.SETTINGS_SEGMENT);
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), SETTINGS);
		
		b = new StringBuilder(C.COURSE_SEGMENT);
		// .../course
		mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE);

		b.append("/#");
		// .../course/#
		mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE_ID);

		StringBuilder b2 = new StringBuilder(C.MISS_SEGMENT);
		// .../miss
		mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), MISS);
		b2.append("/#");
		// .../miss/#
		mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), MISS_ID);
		b2 = new StringBuilder(b).append("/").append(C.MISS_SEGMENT);
		// .../course/#/miss
		mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), COURSE_MISS);
		b2 = new StringBuilder(b).append("/").append(C.CALENDAR_SEGMENT);
		// .../course/#/calendar
		mUriMatcher.addURI(Schule.AUTHORITY, b2.toString(), COURSE_CALENDAR);

		b.append("/").append(C.PUPIL_SEGMENT);
		// .../course/#/pupil
		mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), COURSE_PUPIL);
		b.append("/#");
		// .../course/#/pupil/#
		mUriMatcher.addURI(Schule.AUTHORITY, b.toString(), PUPIL_ID);
	}

	@Override
	public boolean onCreate() {
		db_helper = new SQLiteOpenHelper(getContext(), DATABASE_NAME, null,
				DATABASE_VERSION) {
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion,
					int newVersion) {
				Log.i(TAG, "Upgrading DB from version " + oldVersion + " to "
						+ newVersion);
				if (oldVersion < 2) {
					Updater.upgrade1(db);
				}
				if(oldVersion < 3) {
					Updater.upgrade2(db);
				}
				if(oldVersion < 4) {
					Updater.upgrade3(db);
				}
			}
			
			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL("PRAGMA foreign_keys=ON;");
				db.execSQL(Updater.KURS_TABLE_CREATE);
				db.execSQL(Updater.SCHUELER_TABLE_CREATE);
				db.execSQL(Updater.MISS_TABLE_CREATE);
				// version 3
				db.execSQL(Updater.SETTINGS_TABLE_CREATE);
				Updater.initialSettingInsertions(db);
				db.execSQL(Updater.MISS_VIEW_CREATE);
				// version 4
				db.execSQL(Updater.COURSE_DATES_CREATE);
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
		SQLiteDatabase db = db_helper.getReadableDatabase();

        Map<String, String> projMap = new HashMap<String, String>();

        String groupBy = null;

		switch (mUriMatcher.match(uri)) {
		case COURSE: // path is .../course
			qb.setTables(C.KURS_TABLE);
			if("1".equals(uri.getQueryParameter(C.QUERY_PUPIL_COUNT))) {
				String[] myProjection = new String[projection.length+1];
				projMap.put(C._ID, C.KURS_TABLE + "." + C._ID);

				groupBy = StringTools.arrayToString(projection, projMap, ",", "", "");

				for(int i=0; i<projection.length; ++i)
					myProjection[i] = projection[i];
				myProjection[projection.length] = "COUNT(" + C.SCHUELER_NACHNAME + ")";
				projection = myProjection;
				fillProjMap(projMap, projection);
				qb.setProjectionMap(projMap);

					
				qb.setTables(C.KURS_TABLE + " LEFT JOIN " + C.SCHUELER_TABLE
						+ " ON " + C.KURS_TABLE + "." + C._ID + "="
						+ C.SCHUELER_KURSID);
			}
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
								+ C.MISS_SUM_STUNDEN_E,
						"sum(" + C.MISS_STUNDEN_NZ + ") AS "
								+ C.MISS_SUM_STUNDEN_NZ};
			}

			break;
		case MISS_ID: // path is .../miss/#
			qb.setTables(C.MISS_TABLE);
			qb.appendWhere(C._ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("SchoolProvider.query: Unknown URI " + uri);
		}
		Log.d(TAG, qb.buildQuery(projection, selection, groupBy, null, sortOrder, null));

		return qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);
	}

	private void setMissSumsJoin(SQLiteQueryBuilder qb, Uri uri,
			String[] projection, Map<String, String> projMap) {
		qb.setTables(C.SCHUELER_TABLE + " LEFT JOIN " + C.PUPIL_MISS_VIEW
				+ " ON (" + C.SCHUELER_TABLE + "." + C._ID + "="
				+ C.PUPIL_MISS_VIEW + "." + C._ID + ")");

		// use a projection map for ambiguous column names
		projMap.put(C._ID, C.SCHUELER_TABLE + "." + C._ID);
		/*
		 * fill the projection map with identity mappings for all columns used
		 * in the projection
		 */
		fillProjMap(projMap, projection);
		qb.setProjectionMap(projMap);
	}

	private void fillProjMap(Map<String, String> projMap, String[] projection) {
		for (String proj : projection)
			if (!projMap.containsKey(proj))
				projMap.put(proj, proj);

	}

	@Override
	public Uri insert(Uri uri, ContentValues contentValues) throws SQLException {
		SQLiteDatabase db = db_helper.getWritableDatabase();
		String tableName = null;
		ContentValues values;

		if (contentValues != null)
			values = new ContentValues(contentValues);
		else
			values = new ContentValues();

		switch (mUriMatcher.match(uri)) {
		case COURSE:
			tableName = C.KURS_TABLE;
			if (!values.containsKey(C.KURS_NAME))
				values.put(
						C.KURS_NAME,
						Resources.getSystem().getString(
								android.R.string.untitled));
			for (String s : C.KURS_WDAYS)
				if (!values.containsKey(s))
					values.put(s, 0);
			break;
		case COURSE_PUPIL:
			tableName = C.SCHUELER_TABLE;
			if (!values.containsKey(C.SCHUELER_NACHNAME)
					|| !values.containsKey(C.SCHUELER_VORNAME))
				throw new SQLException("Failed to insert pupil row into " + uri
						+ ". Missing name information.");
			else
				values.put(C.SCHUELER_KURSID, uri.getPathSegments().get(1));
			break;
		case MISS:
			tableName = C.MISS_TABLE;
			if (!values.containsKey(C.MISS_DATUM)
					|| !values.containsKey(C.MISS_STUNDEN_Z)
					|| !values.containsKey(C.MISS_STUNDEN_NZ)
					|| !values.containsKey(C.MISS_STUNDEN_E)
					|| !values.containsKey(C.MISS_SCHUELERID))
				throw new SQLException("Failed to insert miss row into " + uri
						+ ". Missing non-null information.");
			break;
		case COURSE_ID:
			tableName = C.KURS_TABLE;
			values.put(C._ID, uri.getLastPathSegment());
			break;
		case PUPIL_ID:
			tableName = C.SCHUELER_TABLE;
			values.put(C._ID, uri.getLastPathSegment());
			break;
		case MISS_ID:
			tableName = C.MISS_TABLE;
			values.put(C._ID, uri.getLastPathSegment());
			break;
		}

		long rowId = db.insert(tableName, null, values);

		if (rowId > 0) {
			Uri noteUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(noteUri, null);
			return noteUri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) throws SQLException {
		SQLiteDatabase db = db_helper.getWritableDatabase();
		SQLiteStatement insert;
		db.beginTransaction();
		ContentValues cur_val  = null;
		int numInserts = 0;
		try {
			switch (mUriMatcher.match(uri)) {
			case MISS:
				insert = db.compileStatement("INSERT INTO " + C.MISS_TABLE
						+ " (" + C.MISS_DATUM + "," + C.MISS_STUNDEN_Z + ","
						+ C.MISS_STUNDEN_NZ + "," + C.MISS_STUNDEN_E + ","
						+ C.MISS_SCHUELERID + ") VALUES (?,?,?,?,?)");
				for (ContentValues v : values) {
					cur_val = v;
					insert.bindLong(1, v.getAsLong(C.MISS_DATUM));
					insert.bindLong(2, v.getAsInteger(C.MISS_STUNDEN_Z));
					insert.bindLong(3, v.getAsInteger(C.MISS_STUNDEN_NZ));
					insert.bindLong(4, v.getAsInteger(C.MISS_STUNDEN_E));
					insert.bindLong(5, v.getAsLong(C.MISS_SCHUELERID));
					numInserts += insert.executeInsert();
				}
				break;
			case COURSE_PUPIL:
				insert = db.compileStatement("INSERT INTO " + C.SCHUELER_TABLE
						+ "(" + C.SCHUELER_NACHNAME + "," + C.SCHUELER_VORNAME
						+ "," + C.SCHUELER_KURSID + ") VALUES (?,?,?)");
				for (ContentValues v : values) {
					cur_val = v;
					insert.bindString(1, v.getAsString(C.SCHUELER_NACHNAME));
					insert.bindString(2, v.getAsString(C.SCHUELER_VORNAME));
					insert.bindString(3, uri.getPathSegments().get(1));
					numInserts += insert.executeInsert();
				}
				break;
			default:
				throw new UnsupportedOperationException("Unsupported uri: "
						+ uri);
			}
			db.setTransactionSuccessful();
		} catch (SQLiteConstraintException ce) {
			Log.e(TAG, "bulkInsert constraint violated: " + uri, ce);
			Log.e(TAG, "data: " + cur_val);
			Toast.makeText(
					this.getContext(),
					getContext().getResources().getString(
							R.string.toast_bulk_insert_constraint_violation),
					Toast.LENGTH_LONG).show();
		} finally {
			db.endTransaction();
		}
		return numInserts;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) throws SQLException {
		SQLiteDatabase db = db_helper.getWritableDatabase();
		String tableName;
		int count;
		switch (mUriMatcher.match(uri)) {
		case COURSE_ID:
			tableName = C.KURS_TABLE;
			break;
		case PUPIL_ID:
			tableName = C.SCHUELER_TABLE;
			break;
		case MISS_ID:
			tableName = C.MISS_TABLE;
			break;
		case SETTINGS:
			tableName = C.SETTINGS_TABLE;
			break;
		default:
			throw new IllegalArgumentException(
					"SchoolProvider.update: Unknown URI " + uri);
		}
		count = db.update(tableName, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = db_helper.getWritableDatabase();
		int count;
		String tableName;
		selection = C._ID + "=" + uri.getLastPathSegment()
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");

		switch (mUriMatcher.match(uri)) {
		case COURSE_ID:
			tableName = C.KURS_TABLE;
			break;
		case PUPIL_ID:
			tableName = C.SCHUELER_TABLE;
			break;
		case MISS_ID:
			tableName = C.MISS_TABLE;
			break;
		case COURSE_MISS:
			tableName = C.MISS_TABLE;
			selection = C.MISS_DATUM + "= ? AND " +
					C.MISS_SCHUELERID + " IN (SELECT " + C._ID + " FROM " +
					C.SCHUELER_TABLE + " WHERE " +  C.SCHUELER_KURSID + "= ?)";
			break;
		default:
			throw new IllegalArgumentException(
					"SchoolProvider.delete: Unknown or unsupported URI " + uri);
		}

		Log.d(TAG,
				"DELETE FROM  " + tableName + " WHERE " + selection
						+ " WITH ARGUMENTS ["
						+ StringTools.arrayToString(selectionArgs, ",") + "]");
		count = db.delete(tableName, selection, selectionArgs);

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case COURSE:
			return Schule.CONTENT_COURSES_TYPE;

		case COURSE_ID:
			return Schule.CONTENT_COURSE_TYPE;

		case COURSE_PUPIL:
			return Schule.CONTENT_PUPILS_TYPE;

		case PUPIL_ID:
			return Schule.CONTENT_PUPIL_TYPE;

		case MISS:
			return Schule.CONTENT_MISSES_TYPE;

		case MISS_ID:
			return Schule.CONTENT_MISS_TYPE;

		case COURSE_MISS:
			return Schule.CONTENT_COURSE_MISS_TYPE;

		case COURSE_CALENDAR:
			return Schule.CONTENT_COURSE_CALENDAR_TYPE;

		default:
			throw new IllegalArgumentException(
					"SchoolProvider.getType: Unknown URI " + uri);
		}
	}
}