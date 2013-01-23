package org.bob.school.tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bob.school.Schule;
import org.bob.school.Schule.C;

import android.database.Cursor;
import android.os.Bundle;

public class CalendarTools {
	private static final String listViewDateFormat = "EEE, dd. MMM ''yy";
	private static final String filenameDateFormat = "yyyyMMdd_hhmm";
	private static final String monthDateFormat = "MMMM yyyy";

	public static final DateFormat LISTVIEW_DATE_FORMATER = new SimpleDateFormat(listViewDateFormat);
	public static final DateFormat FILENAME_DATE_FORMATER = new SimpleDateFormat(filenameDateFormat);
	public static final DateFormat MEDIUM_DATE_FORMATTER = SimpleDateFormat
			.getDateInstance(SimpleDateFormat.MEDIUM);
	public static final DateFormat MONTH_DATE_FORMATTER = new SimpleDateFormat(monthDateFormat);

	/**  Reset the time to 0:00.0 (i.e. 12:00.0 AM), date stays unchanged
	 * @param c calendar to reset
	 */
	public static void resetTime(Calendar c) {
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.AM_PM, Calendar.AM);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
	}

	/** Return the number of hours for a given date and course settings.
	 * @param c A cursor to course settings where this information can
	 * be retrieved.
	 * @param day The date for which to return the hours
	 * @return The number of hours, -1 if it's weekend (i.e. Saturday or Sunday)
	 */
	public static int getTodaysHours(Cursor c, Calendar day) {
		int dOfW = day.get(Calendar.DAY_OF_WEEK);
		if (dOfW >= Calendar.MONDAY && dOfW <= Calendar.FRIDAY) {
			int index = c.getColumnIndexOrThrow(C.KURS_WDAYS[dOfW - 2]);
			return c.getInt(index);
		}
		else
			// no school on Saturdays or Sundays
			return -1;
	}

	/** Return the number of hours for a given date and course settings.
	 * @param b A bundle containing ints with name "org.bob.school.hMon" ...
	 *   "org.bob.school.hFri"
	 * @param day The date for which to return the hours
	 * @return The number of hours, -1 if it's weekend (i.e. Saturday or Sunday)
	 */
	public static int getTodaysHours(Bundle b, Calendar day) {
		int dOfW = day.get(Calendar.DAY_OF_WEEK);
		if (dOfW >= Calendar.MONDAY && dOfW <= Calendar.FRIDAY) {
			return b.getInt(Schule.PREFIX + C.KURS_WDAYS[dOfW - 2]);
		}
		else
			// no school on Saturdays or Sundays
			return -1;
	}
}
