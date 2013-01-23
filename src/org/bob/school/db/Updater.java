package org.bob.school.db;

import java.util.Calendar;

import org.bob.school.Schule.C;
import org.bob.school.tools.CalendarTools;
import org.bob.school.tools.StringTools;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.provider.BaseColumns;
import android.util.Log;

class Updater {
	private static String TAG = "Updater";

	static final String SCHUELER_TABLE_CREATE = "create table "
			+ C.SCHUELER_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.SCHUELER_NACHNAME
			+ " text not null, " + C.SCHUELER_VORNAME + " text not null,"
			+ C.SCHUELER_KURSID + " integer not null, foreign key ("
			+ C.SCHUELER_KURSID + ") references " + C.KURS_TABLE + "("
			+ BaseColumns._ID + ") ON DELETE CASCADE)";

	static final String KURS_TABLE_CREATE = "create table "
			+ C.KURS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.KURS_NAME
			+ " text not null, " + C.KURS_SDATE + " integer, "
			+ C.KURS_EDATE + " integer, "
			+ StringTools.arrayToString(C.KURS_WDAYS, ", ", null,
					" integer not null") + ")";

	static final String MISS_TABLE_CREATE = "create table "
			+ C.MISS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.MISS_DATUM
			+ " integer not null, " + C.MISS_STUNDEN_Z + " integer not null, "
			+ C.MISS_STUNDEN_NZ + " integer not null, " + C.MISS_STUNDEN_E
			+ " integer not null, " + C.MISS_SCHUELERID + " integer not null, "
			+ C.MISS_GRUND + " integer not null default 0, " + C.MISS_BEMERKUNG
			+ " text, foreign key (" + C.MISS_SCHUELERID + ") references "
			+ C.SCHUELER_TABLE + "(" + BaseColumns._ID
			+ ") ON DELETE CASCADE, " + "unique (" + C.MISS_DATUM + ", "
			+ C.MISS_SCHUELERID + ", " + C.MISS_GRUND + "))";

	static final String SETTINGS_TABLE_CREATE = "create table "
			+ C.SETTINGS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.SETTINGS_NAME
			+ " text, " + C.SETTINGS_VALUE_INT + " integer, "
			+ C.SETTINGS_VALUE_TEXT + " text)";

	static final String MISS_VIEW_CREATE = "create view "
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

	static final String COURSE_DATES_CREATE = "create table "
			+ C.KURS_TERMINE_TABLE + " (" + BaseColumns._ID
			+ " integer primary key autoincrement, " + C.KURS_TERMINE_KURSID
			+ " integer, " + C.KURS_TERMINE_DATE + " integer, "
			+ C.KURS_TERMINE_STUNDEN + " integer, foreign key ("
			+ C.KURS_TERMINE_KURSID + ") references " + C.KURS_TABLE + "("
			+ BaseColumns._ID + ") ON DELETE CASCADE, unique ("
			+ C.KURS_TERMINE_KURSID + ", " + C.KURS_TERMINE_DATE + "))";

	static void upgrade1(SQLiteDatabase db) {
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

	static void upgrade2(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			db.execSQL(SETTINGS_TABLE_CREATE);
			initialSettingInsertions(db);
			db.execSQL("DROP VIEW " + C.PUPIL_MISS_VIEW);
			db.execSQL(MISS_VIEW_CREATE);
			db.setTransactionSuccessful();
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		} finally {
			db.endTransaction();
		}
	}

	static void upgrade3(SQLiteDatabase db) {
		Calendar cal = Calendar.getInstance();
		long endtime;
		int dayOfWeek;
		int h;
		ContentValues cv;
		db.beginTransaction();
		try {
			db.execSQL(COURSE_DATES_CREATE);

			// set dates for all courses
			Cursor c = db.query(C.KURS_TABLE, null, null, null, null, null, null);			
			c.moveToFirst();
			while(!c.isAfterLast()) {
				cal.setTimeInMillis(c.getLong(c.getColumnIndex(C.KURS_SDATE)));
				endtime = c.getLong(c.getColumnIndex(C.KURS_EDATE));

				while(cal.getTimeInMillis() < endtime) {
					Log.d(TAG, "date: " + CalendarTools.LISTVIEW_DATE_FORMATER.format(cal.getTime()));
					// insert into kurs_termine table
					if((dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK)+5) % 7) < 5 && 
						// monday = 0, tuesday = 1, ...
						(h = c.getInt(c.getColumnIndex(C.KURS_WDAYS[dayOfWeek])))>0) {
						cv = new ContentValues();
						cv.put(C.KURS_TERMINE_KURSID,
								c.getInt(c.getColumnIndex(BaseColumns._ID)));
						cv.put(C.KURS_TERMINE_DATE, cal.getTimeInMillis());
						cv.put(C.KURS_TERMINE_STUNDEN, h);
						db.insertOrThrow(C.KURS_TERMINE_TABLE, null, cv);
					}
					cal.add(Calendar.DATE, 1);
				}
				c.moveToNext();
			}
			db.setTransactionSuccessful();
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		} finally {
			db.endTransaction();
		}
	}

	static void initialSettingInsertions(SQLiteDatabase db) {
		db.execSQL("INSERT INTO " + C.SETTINGS_TABLE + "("
				+ C.SETTINGS_NAME + "," + C.SETTINGS_VALUE_INT
				+ ")" + " VALUES " + "('"
				+ C.STARTDATE_SUM_MISS_SETTING + "'," + "0)");
		db.execSQL("INSERT INTO " + C.SETTINGS_TABLE + "("
				+ C.SETTINGS_NAME + "," + C.SETTINGS_VALUE_INT
				+ ")" + " VALUES " + "('"
				+ C.ENDDATE_SUM_MISS_SETTING + "'," + "null)");
	}
}