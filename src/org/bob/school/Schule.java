package org.bob.school;

import android.net.Uri;
import android.provider.BaseColumns;

public class Schule {
	public static final String AUTHORITY = "org.bob.provider.school";

	public static final class C implements BaseColumns {
		/**
		 * The content:// style URI
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

		/** The URI-segment identifying a course
		 */
		public static final String COURSE_SEGMENT = "course";

		/** The URI-segment identifying a pupil
		 */
		public static final String PUPIL_SEGMENT = "pupil";

		/** The URI-segment identifying a miss
		 */
		public static final String MISS_SEGMENT = "miss";

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of
		 * courses.
		 */
		public static final String CONTENT_COURSES_TYPE = "vnd.android.cursor.dir/vnd.bob.course";

		/**
		 * The MIME type of a {@link #CONTENT_URI} of a single course item.
		 */
		public static final String CONTENT_COURSE_TYPE = "vnd.android.cursor.item/vnd.bob.course";

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of
		 * pupils.
		 */
		public static final String CONTENT_PUPILS_TYPE = "vnd.android.cursor.dir/vnd.bob.pupil";

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a single pupil item.
		 */
		public static final String CONTENT_PUPIL_TYPE = "vnd.android.cursor.item/vnd.bob.pupil";

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of course misses 
		 */
		public static final String CONTENT_MISSES_TYPE = "vnd.android.cursor.dir/vnd.bob.miss";

		/**
		 * The MIME type of {@link #CONTENT_URI} of a single miss for a pupil
		 */
		public static final String CONTENT_MISS_TYPE = "vnd.android.cursor.item/vnd.bob.miss";

		/**
		 * The MIME type of {@link #CONTENT_URI} of a single miss for a pupil
		 */
		public static final String CONTENT_COURSE_MISS_TYPE = "vnd.android.cursor.dir/vnd.bob.course_miss";

		/** This query parameter enables a really dirty hack to get a cursor
		 *  with an _id column for each distinct date in the miss list.
		 *  The _id column is needed to use the cursor in an {@link android.widget.CursorTreeAdapter} which
		 *  is a superclass of {@link android.widget.SimpleCursorTreeAdapter} used in {@link KursFehlstundenList}.
		 */
		public static final String QUERY_DISTINCT_DATES_WITH_ID_HACK = "distinctDatesWithIdHack";

		/** Query parameter to also select the sums of the miss hours.
		 */
		public static final String QUERY_SUM_MISS = "querySumMiss";

//		public static final String _ALT_ID = "_id2";

		public static final String SCHUELER_TABLE = "schueler";
		public static final String SCHUELER_NACHNAME = "nachname";
		public static final String SCHUELER_VORNAME = "vorname";
		public static final String SCHUELER_KURSID = "kursid";

		public static final String KURS_TABLE = "kurs";
		public static final String KURS_NAME = "name";
		public static final String KURS_SDATE = "startdatum";
		public static final String KURS_EDATE = "enddatum";
		public static final String[] KURS_WDAYS = { "hMon", "hTue", "hWed", "hThu", "hFri" };

		public static final String MISS_TABLE = "versaeumnis";
		public static final String MISS_DATUM = "datum";
		public static final String MISS_STUNDEN_Z = "std_z";    // zaehlen
		public static final String MISS_STUNDEN_NZ = "std_nz";  // nicht zaehlen
		public static final String MISS_STUNDEN_E = "std_e";    // entschuldigt
		public static final String MISS_SCHUELERID = "schuelerid";
		public static final String MISS_GRUND = "grund";
		public static final String MISS_BEMERKUNG = "bemerkung";

		public static final String PUPIL_MISS_VIEW = "schueler_versaeumnis_sum";
		public static final String MISS_SUM_STUNDEN_Z = "sum_std_z";
		public static final String MISS_SUM_STUNDEN_NZ = "sum_std_nz";
		public static final String MISS_SUM_STUNDEN_E = "sum_std_e";
	}
}
