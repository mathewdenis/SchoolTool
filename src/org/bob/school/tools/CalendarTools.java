package org.bob.school.tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bob.school.Schule.C;

import android.database.Cursor;

public class CalendarTools {
	private static final String dateFormat = "EEE, dd. MMM ''yy";

	public static final DateFormat dateFormatter = new SimpleDateFormat(dateFormat);

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

	/** Return the number of hours for today's day of week (today is
	 * the runtime date) for the given course settings
	 * @param c A cursor to course settings where this information can
	 * be retrieved.
	 * @return The number of hours, -1 if it's weekend (i.e. Saturday or Sunday)
	 */
	public static int getTodaysHours(Cursor c) {
		int dOfW = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		if (dOfW >= Calendar.MONDAY && dOfW <= Calendar.FRIDAY) {
			int index = c.getColumnIndexOrThrow(C.KURS_WDAYS[dOfW - 2]);
			return c.getInt(index);
		}
		else
			// no school on Saturdays or Sundays
			return -1;
	}
}
