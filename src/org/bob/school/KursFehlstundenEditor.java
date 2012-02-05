package org.bob.school;

import java.util.Calendar;

import org.bob.school.Schule.C;
import org.bob.school.tools.CalendarTools;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class KursFehlstundenEditor extends Activity implements
		DatePickerDialog.OnDateSetListener {

	/**
	 * Special intent action meaning "add misses for a course"
	 */
	public static final String ACTION_ADD_COURSE_MISSES = "org.bob.school.action.ADD_COURSE_MISSES";

	private Uri mUri;         // data: .../course/# (COURSE_ID)
	private Cursor mCursor;
	private Calendar today;
	private String mCourseName;
	// private DatePicker mDatePicker;
	private ListView mPupilsList;
	private int mDayOfWeek;
	private Spinner mCourseHoursSpinner;

	private EditText mDateEditText;
	private ArrayAdapter<String> mHoursAdapter = null;

	private static class MyPupilNameListViewBinder implements
			SimpleCursorAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			// cursor has the columns _id[0], nachname[1], vorname[2],
			TextView tv = (TextView) view;

			StringBuilder sb = new StringBuilder(cursor.getString(1)).append(
					", ").append(cursor.getString(2));

			tv.setText(sb);

			return true;
		}
	}

	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUri = getIntent().getData();

		setContentView(R.layout.add_course_miss_layout);

		// get course information
		mCursor = managedQuery(mUri, null, null, null, null);
		mCursor.moveToFirst();
		mCourseName = mCursor.getString(mCursor
				.getColumnIndex(C.KURS_NAME));

		setTitle(getTitle() + ": " + mCourseName);

		today = Calendar.getInstance();

		mDateEditText = (EditText) findViewById(R.id.miss_edittext);
		mDateEditText.setText(CalendarTools.MEDIUM_DATE_FORMATTER.format(today.getTime()));
		mDateEditText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new DatePickerDialog(KursFehlstundenEditor.this,
						KursFehlstundenEditor.this, today.get(Calendar.YEAR),
						today.get(Calendar.MONTH), today
								.get(Calendar.DAY_OF_MONTH)).show();
			}
		});

		// retrieve the hours for the course for todays weekday
		// if there is no lesson today, set "1"
		mCourseHoursSpinner = (Spinner)findViewById(R.id.miss_spinner);
		mHoursAdapter = (ArrayAdapter<String>) mCourseHoursSpinner.getAdapter();

		mCourseHoursSpinner.setSelection(mHoursAdapter.getPosition(String
				.valueOf(Math.max(1, CalendarTools.getTodaysHours(mCursor)))));

		// query
		Uri uri = Uri.withAppendedPath(mUri, C.PUPIL_SEGMENT);
		Cursor c = managedQuery(uri, new String[] { C._ID,
				C.SCHUELER_NACHNAME, C.SCHUELER_VORNAME },
				null, null, SchuelerFehlstundenList.SORT_ORDER_NAME);

		mPupilsList = (ListView) findViewById(R.id.pupils_list);
		SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_multiple_choice, c,
				new String[] { C.SCHUELER_NACHNAME },
				new int[] { android.R.id.text1 });
		sca.setViewBinder(new MyPupilNameListViewBinder());
		mPupilsList.setAdapter(sca);
		mPupilsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mPupilsList.setItemsCanFocus(false);
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		today.set(year, monthOfYear, dayOfMonth);
		mDateEditText.setText(CalendarTools.MEDIUM_DATE_FORMATTER.format(today.getTime()));
	}

	// saves the misses
	public void okClicked(View v) {
		long[] checkedPupils = mPupilsList.getCheckItemIds();

		Uri uri = Uri.withAppendedPath(C.CONTENT_URI, C.MISS_SEGMENT);
		ContentValues[] values = new ContentValues[checkedPupils.length];
		int i = 0;
		for(long l : checkedPupils) {
			values[i] = new ContentValues(5);

			/* since we need to make queries selecting rows by a specific date
			 * we need to ensure that misses of the same date have the 
			 * same millisecond value (thus use a specific time of day for a
			 * date to save)
			 */			
			CalendarTools.resetTime(today);  // set time to 0
			values[i].put(C.MISS_DATUM, today.getTimeInMillis());
			// here we assume all misses are to count
			values[i].put(C.MISS_STUNDEN_Z, mHoursAdapter.getItem(mCourseHoursSpinner.getSelectedItemPosition()));
			values[i].put(C.MISS_STUNDEN_NZ, 0);  
			values[i].put(C.MISS_STUNDEN_E, 0);
			values[i++].put(C.MISS_SCHUELERID, l);
		}
		getContentResolver().bulkInsert(uri, values);
		setResult(RESULT_OK);
		finish();
	}

	public void cancelClicked(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}
}
