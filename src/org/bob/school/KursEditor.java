package org.bob.school;

import java.sql.Date;
import java.util.Calendar;

import org.bob.school.Schule.C;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.BaseColumns;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

public class KursEditor extends PreferenceActivity implements
		OnDateSetListener, OnPreferenceChangeListener,
		OnPreferenceClickListener {
	public static final String TAG = "KursEditor";
	public static final int STATE_EDIT = 0;
	public static final int STATE_INSERT = 1;

	public static final int STATE_DATE_SETTER_START = 0;
	public static final int STATE_DATE_SETTER_END = 1;

	private Uri mUri; // .../course
	private int mDate_setter_state;

	private Calendar mEditDate; 
	private Date mStartDate;
	private Date mEndDate;
	private EditTextPreference mCourseNameEP;
	private ListPreference[] mWeekdayLP = new ListPreference[5];
	private boolean mDeleteCourse = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        // Do some setup based on the action being performed.
        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {
            // Requested to edit
            mUri = intent.getData();
            mDeleteCourse = false;
            setTitle(getText(R.string.title_kurs_edit));
        }
        else if (Intent.ACTION_INSERT.equals(action)) {
            // create a new entry in the container.
			mUri = getContentResolver().insert(intent.getData(), null);
            setTitle(getText(R.string.title_kurs_insert));

			if (mUri == null) {
				Log.e(TAG, "Failed to insert new course into "
						+ getIntent().getData());
				// nothing inserted, nothing to delete
				mDeleteCourse = false;
				finish();
				return;
			}

        } else {
            // Whoops, unknown action!  Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        // Set the layout res/layout/kurs_editor.xml
        setContentView(R.layout.save_cancel_preference);

        Cursor c = getContentResolver().query(mUri, null, null, null, null);

        // überschreibt das View-Element mit id @android:id/list, da dieses
        // das ContentView von PreferenceActivity darstellt
        addPreferencesFromResource(R.xml.kurs_edit_preferences);

        mCourseNameEP = (EditTextPreference)findPreference("name");
        mCourseNameEP.setOnPreferenceChangeListener(this);
        for(int i=0; i<5; ++i) {
        	mWeekdayLP[i] = (ListPreference)findPreference(C.KURS_WDAYS[i]);
        	mWeekdayLP[i].setOnPreferenceChangeListener(this);
        }
        
        guiUpdatePrefs(c);
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	if(isFinishing() && mDeleteCourse)
    		deleteCourse();
    }

	@Override
	public boolean onPreferenceChange(Preference p, Object newValue) {
		if(p instanceof ListPreference)
			p.setSummary(getResources().getString(R.string.sum_wd) + ": " + newValue);
		else
			p.setSummary(newValue.toString());

		return true;
	}
	// Someone clicked on a preference list element. This is only interesting
    // if a date preference element has been clicked since we have to show
    // a date picker.
	@Override
	public boolean onPreferenceClick(Preference preference) {
		boolean ret = false;
        if (preference == findPreference("startdate")) {
        	mDate_setter_state = STATE_DATE_SETTER_START;
        	mEditDate.setTime(mStartDate);
            showDatePicker();
            ret = true;
        } else if (preference == findPreference("enddate")) {
        	mDate_setter_state = STATE_DATE_SETTER_END;
        	mEditDate.setTime(mEndDate);
            showDatePicker();
            ret = true;
        }		

        return ret;
	}

    private void showDatePicker() {
		new DatePickerDialog(this, this, mEditDate.get(Calendar.YEAR),
				mEditDate.get(Calendar.MONTH), mEditDate.get(Calendar.DAY_OF_MONTH))
				.show();
    }

	@Override
	public void onDateSet(DatePicker datePicker, int year, int month, int day) {
		Preference pDateView = null;
		Calendar c = Calendar.getInstance();
		c.set(year, month, day);

		Date d = new Date(c.getTimeInMillis());

		switch(mDate_setter_state) {
		case STATE_DATE_SETTER_START:
			mStartDate = d; 
			pDateView = findPreference("startdate");
			break;
		case STATE_DATE_SETTER_END:
			mEndDate = d;
			pDateView = findPreference("enddate");
			break;
		default:
            Log.e(TAG, "Unknown state of date setter, cannot set a date");
            return;
		}

		pDateView.setSummary(DateFormat.getDateFormat(this).format(d));
	}

	// as described in the save_cancel_list_view-xml-file
	public void okClicked(View v) {
		ContentValues values = new ContentValues();
		values.put(C.KURS_NAME, mCourseNameEP.getText());
		values.put(C.KURS_SDATE, mStartDate.getTime());
		values.put(C.KURS_EDATE, mEndDate.getTime());
		for(int i=0; i<5; ++i)
			values.put(C.KURS_WDAYS[i], mWeekdayLP[i].getValue());

		getContentResolver().update(mUri, values, BaseColumns._ID + "=?", new String[] { mUri.getLastPathSegment() });
		setResult(RESULT_OK);

		mDeleteCourse = false;
		finish();
	}

	// as described in the save_cancel_list_view-xml-file
	public void cancelClicked(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}

	/** Update the GUI-Elements from the values of the current cursor 
	 */
	private void guiUpdatePrefs(Cursor c) {
		c.moveToFirst();
		String cName = c.getString(c
				.getColumnIndex(C.KURS_NAME));
		mCourseNameEP.setText(cName);
		mCourseNameEP.setSummary(cName);

		for (int i = 0; i < 5; ++i) {
			mWeekdayLP[i].setValue(c.getString(c
					.getColumnIndex(C.KURS_WDAYS[i])));
			onPreferenceChange(mWeekdayLP[i], mWeekdayLP[i].getValue());
		}

		mEditDate = Calendar.getInstance();
		if (c.isNull(c.getColumnIndex(C.KURS_SDATE)))
			mStartDate = new Date(mEditDate.getTimeInMillis());
		else
			mStartDate = new Date(c.getLong(c
					.getColumnIndex(C.KURS_SDATE)));

		mEditDate.add(Calendar.MONTH, 6);
		if (c.isNull(c.getColumnIndex(C.KURS_EDATE)))
			mEndDate = new Date(mEditDate.getTimeInMillis());
		else
			mEndDate = new Date(c.getLong(c
					.getColumnIndex(C.KURS_EDATE)));
		c.close();

		Preference pref = findPreference("startdate");
		pref.setSummary(DateFormat.getDateFormat(this).format(mStartDate));
		pref.setOnPreferenceClickListener(this);
		pref = findPreference("enddate");
		pref.setSummary(DateFormat.getDateFormat(this).format(mEndDate));
		pref.setOnPreferenceClickListener(this);
	}

	private void deleteCourse() {
		getContentResolver().delete(mUri, null, null);
	}
}
