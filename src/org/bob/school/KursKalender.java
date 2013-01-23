package org.bob.school;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.bob.school.Schule.C;
import org.bob.school.tools.CalendarTools;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import com.exina.android.calendar.CalendarAdapter;
import com.exina.android.calendar.CalendarView;
import com.exina.android.calendar.Cell;

public class KursKalender extends Activity implements CalendarView.OnCellTouchListener {

	@SuppressWarnings("unused")
	private static final String TAG = "KursKalender";
	private GestureDetector mGestureDetector;
	

	private CalendarView mView;
	private TextView mMonth;
	private Calendar mSDatum, mEDatum;
	private Uri mUri;    // data:   .../course/#/calendar  (COURSE_CALENDAR)
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_layout);
        mUri = getIntent().getData();

        mView = (CalendarView)findViewById(R.id.calendar);
        mView.setOnCellTouchListener(this);
        
        mMonth = (TextView)findViewById(R.id.monthTextView);
        mMonth.setText(CalendarTools.MONTH_DATE_FORMATTER.format(mView.getDate().getTime()));

        // get marked dates
        Bundle b = getIntent().getExtras();
		(mSDatum = Calendar.getInstance()).setTimeInMillis(b.getLong(Schule.PREFIX + C.KURS_SDATE));
		(mEDatum = Calendar.getInstance()).setTimeInMillis(b.getLong(Schule.PREFIX + C.KURS_EDATE));
		CalendarTools.resetTime(mSDatum);
		CalendarTools.resetTime(mEDatum);

		Calendar c;
		Set<Long> markedDates = new HashSet<Long>();
		for (int i = 0; i < 5; ++i)
			if (b.getInt(Schule.PREFIX + C.KURS_WDAYS[i]) > 0) {
				c = (Calendar) mSDatum.clone();
				// roll c to next currently executed weekday
				c.add(Calendar.DATE,
						(i - mSDatum.get(Calendar.DAY_OF_WEEK) + 9) % 7);
				while (c.getTimeInMillis() <= mEDatum.getTimeInMillis()) {
					markedDates.add(c.getTimeInMillis());
					c.add(Calendar.DATE, 7);
				}

			}

        mView.setAdapter(new CalendarAdapter(markedDates));

		mGestureDetector = new GestureDetector(this,
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						boolean ret = false;

						if (Math.abs(velocityX) > Math.abs(velocityY)
								&& Math.abs(e1.getX() - e2.getX()) > 120) {
							mView.changeMonth(velocityX < 0);
							mMonth.setText(CalendarTools.MONTH_DATE_FORMATTER
									.format(mView.getDate().getTime()));
							ret = true;
						}
						return ret;
					}

					@Override
					public boolean onSingleTapConfirmed(MotionEvent ev) {
						Rect r = new Rect();
						mView.getHitRect(r);
						ev.offsetLocation(-r.left, -r.top);
						mView.handleTouchEvent(ev);

						return true;
					}
				});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean done = mGestureDetector.onTouchEvent(event);

		// If we return false from a ACTION_DOWN-event
		// we won't get notifications about following gestures or
		// ACTION_UP events. So we need to consume the ACTION_DOWN
		// event here by returning true.
		if (event.getAction() == MotionEvent.ACTION_DOWN)
			done = true;

		return done;
	}

	@Override
	public void onTouch(Cell cell) {
		Calendar c = cell.getDate();
		Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(
				C.CONTENT_URI, C.COURSE_SEGMENT), mUri
				.getPathSegments().get(1));

		Intent i = new Intent(KursFehlstundenEditor.ACTION_EDIT_COURSE_MISSES,
				uri);
		i.putExtras(getIntent().getExtras());
		i.putExtra(Schule.DATE_EXTRA, c.getTimeInMillis());
		startActivity(i);
	}
}
