package org.bob.school.tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalendarTools {
	private static final String dateFormat = "EEE, dd. MMM ''yy";

	public static final DateFormat dateFormatter = new SimpleDateFormat(dateFormat);

	/**  Reset the time to 0:00.0, date stays unchanged
	 * @param c calendar to reset
	 */
	public static void resetTime(Calendar c) {
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.AM_PM, Calendar.AM);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
	}
}
