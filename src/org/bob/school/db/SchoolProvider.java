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
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class SchoolProvider extends ContentProvider {
	public static final String DATABASE_NAME = "schooltool.db";
	public static final int DATABASE_VERSION = 3;
	public static final String TAG = "SchoolProvider";

	private static final int COURSE = 1;
	private static final int COURSE_ID = 2;
	private static final int COURSE_PUPIL = 3; // .../course/#/pupil
	private static final int PUPIL_ID = 4;
	private static final int MISS = 5;
	private static final int MISS_ID = 6;
	private static final int COURSE_MISS = 7;
	private static final int SETTINGS = 8;

	// Table and View create statements
	private static final String SCHUELER_TABLE_CREATE = "create table "
			+ C.SCHUELER_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.SCHUELER_NACHNAME
			+ " text not null, " + C.SCHUELER_VORNAME + " text not null,"
			+ C.SCHUELER_KURSID + " integer not null," + "foreign key ("
			+ C.SCHUELER_KURSID + ") references " + C.KURS_TABLE + "("
			+ BaseColumns._ID + ") ON DELETE CASCADE)";

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
					" integer not null") + ")";

	private static final String MISS_TABLE_CREATE = "create table "
			+ C.MISS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.MISS_DATUM
			+ " integer not null, " + C.MISS_STUNDEN_Z + " integer not null, "
			+ C.MISS_STUNDEN_NZ + " integer not null, " + C.MISS_STUNDEN_E
			+ " integer not null, " + C.MISS_SCHUELERID + " integer not null, "
			+ C.MISS_GRUND + " integer not null default 0, " + C.MISS_BEMERKUNG
			+ " text, " + "foreign key (" + C.MISS_SCHUELERID + ") references "
			+ C.SCHUELER_TABLE + "(" + BaseColumns._ID
			+ ") ON DELETE CASCADE, " + "unique (" + C.MISS_DATUM + ", "
			+ C.MISS_SCHUELERID + ", " + C.MISS_GRUND + "))";

	private static final String MISS_VIEW_CREATE = "create view "
			+ C.PUPIL_MISS_VIEW + " as select " + C.SCHUELER_TABLE + "."
			+ BaseColumns._ID + " AS " + BaseColumns._ID + "," + "sum("
			+ C.MISS_STUNDEN_Z + ") AS " + C.MISS_SUM_STUNDEN_Z + "," + "sum("
			+ C.MISS_STUNDEN_NZ + ") AS " + C.MISS_SUM_STUNDEN_NZ + ","
			+ "sum(" + C.MISS_STUNDEN_E + ") AS " + C.MISS_SUM_STUNDEN_E
			+ " from " + C.SCHUELER_TABLE + " join " + C.MISS_TABLE + " on ("
			+ C.SCHUELER_TABLE + "." + BaseColumns._ID + "="
			+ C.MISS_SCHUELERID + ")" + " where " + C.MISS_DATUM
			+ " between (select " + C.SETTINGS_VALUE_INT + " from "
			+ C.SETTINGS_TABLE + " where " + C.SETTINGS_NAME + "='"
			+ C.STARTDATE_SUM_MISS_SETTING + "') and "
			+ "coalesce((select " + C.SETTINGS_VALUE_INT + " from "
			+ C.SETTINGS_TABLE + " where " + C.SETTINGS_NAME + "='"
			+ C.ENDDATE_SUM_MISS_SETTING + "'), (select max(" + C.MISS_DATUM
			+ ") from " + C.MISS_TABLE + "))" + " group by " + C.SCHUELER_TABLE
			+ "." + BaseColumns._ID + " order by " + C.MISS_SUM_STUNDEN_Z
			+ " desc, " + C.MISS_SUM_STUNDEN_E + " desc";

	private static final String SETTINGS_TABLE_CREATE = "create table "
			+ C.SETTINGS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.SETTINGS_NAME
			+ " text, " + C.SETTINGS_VALUE_INT + " integer, "
			+ C.SETTINGS_VALUE_TEXT + " text)";			

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
					upgrade1(db);
				}
				if(oldVersion < 3) {
					upgrade2(db);
				}
				// Log.w(TAG, "Upgrading database from version " + oldVersion +
				// " to "
				// + newVersion + ", which will destroy all old data");
				// db.execSQL("DROP TABLE IF EXISTS " + C.PUPIL_MISS_VIEW);
				// db.execSQL("DROP TABLE IF EXISTS " + C.MISS_TABLE);
				// db.execSQL("DROP TABLE IF EXISTS " + C.SCHUELER_TABLE);
				// db.execSQL("DROP TABLE IF EXISTS " + C.KURS_TABLE);
				// onCreate(db);
				// }
			}

			private void upgrade1(SQLiteDatabase db) {
				db.beginTransaction();
				db.execSQL("ALTER TABLE versaeumnis ADD grund integer not null default 0");
				db.execSQL("ALTER TABLE versaeumnis ADD bemerkung text");
				try {
					db.execSQL("CREATE UNIQUE INDEX datum_schueler_grund_uq ON"
							+ " versaeumnis (datum, schuelerid, grund)");
					db.setTransactionSuccessful();
				} catch (SQLiteConstraintException e) {
					Log.e(TAG, e.getMessage());
				}
				db.endTransaction();
			}

			private void upgrade2(SQLiteDatabase db) {
				db.beginTransaction();
				try {
					db.execSQL(SETTINGS_TABLE_CREATE);
					db.execSQL("INSERT INTO " + C.SETTINGS_TABLE + "("
							+ C.SETTINGS_NAME + "," + C.SETTINGS_VALUE_INT
							+ ")" + " VALUES " + "('"
							+ C.STARTDATE_SUM_MISS_SETTING + "'," + "0)");
					db.execSQL("INSERT INTO " + C.SETTINGS_TABLE + "("
							+ C.SETTINGS_NAME + "," + C.SETTINGS_VALUE_INT
							+ ")" + " VALUES " + "('"
							+ C.ENDDATE_SUM_MISS_SETTING + "'," + "null)");

					db.execSQL("DROP VIEW " + C.PUPIL_MISS_VIEW);
					db.execSQL(MISS_VIEW_CREATE);
					db.setTransactionSuccessful();
				} catch (SQLiteException e) {
					Log.e(TAG, e.getMessage());
				} finally {
					db.endTransaction();
				}
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL("PRAGMA foreign_keys=ON;");
				db.execSQL(KURS_TABLE_CREATE);
				db.execSQL(SCHUELER_TABLE_CREATE);
				db.execSQL(MISS_TABLE_CREATE);
				db.execSQL(MISS_VIEW_CREATE);
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
		Log.d(TAG, qb.buildQuery(projection, selection, selectionArgs, groupBy, null, sortOrder, null));

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

		Long now = Long.valueOf(System.currentTimeMillis());

		switch (mUriMatcher.match(uri)) {
		case COURSE:
			tableName = C.KURS_TABLE;
			if (!values.containsKey(C.KURS_NAME))
				values.put(
						C.KURS_NAME,
						Resources.getSystem().getString(
								android.R.string.untitled));
			if (!values.containsKey(C.KURS_SDATE))
				values.put(C.KURS_SDATE, now);
			if (!values.containsKey(C.KURS_EDATE))
				// now + half a year
				values.put(C.KURS_EDATE, now + 15552000000L);
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
		String id = uri.getLastPathSegment();
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
		default:
			throw new IllegalArgumentException(
					"SchoolProvider.delete: Unknown or unsupported URI " + uri);
		}
		count = db.delete(tableName, C._ID + "=" + id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
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

		default:
			throw new IllegalArgumentException(
					"SchoolProvider.getType: Unknown URI " + uri);
		}
	}
}