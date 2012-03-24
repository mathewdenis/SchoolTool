package org.bob.school;

import java.util.Calendar;

import org.bob.school.Schule.C;
import org.bob.school.tools.AlertDialogs;
import org.bob.school.tools.CalendarTools;
import org.bob.school.tools.SchoolTools;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.BaseColumns;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

public class FehlstundeEditor extends PreferenceActivity implements
		OnDateSetListener, OnPreferenceChangeListener,
		OnPreferenceClickListener {

	/**
	 * Special intent action meaning "edit miss"
	 */
//	public static final String ACTION_ADD_MISS = "org.bob.school.action.ADD_MISS";

	private static final int MENU_ITEM_DELETE = Menu.FIRST;

	// data: .../miss/# (MISS_ID)  (ACTION_EDIT)
	//       .../miss (MISS)  (ACTION_INSERT)
	private Uri mUri;
	private Calendar mDatum;
	private ListPreference mMiss;
	private ListPreference mMissExcused;
	private CheckBoxPreference mCount;
	private Preference mDatePref;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Intent intent = getIntent();

		mUri = intent.getData();

		setContentView(R.layout.save_cancel_preference);
		addPreferencesFromResource(R.xml.fehlstunde_edit_preferences);
		mMiss = (ListPreference) findPreference("miss");
		mMiss.setOnPreferenceChangeListener(this);
		mMissExcused = (ListPreference) findPreference("miss_excused");
		mMissExcused.setOnPreferenceChangeListener(this);
		mCount = (CheckBoxPreference) findPreference("count");

		mCount.setOnPreferenceChangeListener(this);

		mDatePref = findPreference("date");
		mDatePref.setOnPreferenceClickListener(this);
		mDatum = Calendar.getInstance();
		Cursor c = null;

		if (Intent.ACTION_EDIT.equals(intent.getAction()))
			c = getContentResolver().query(mUri, null, null, null, null);

		guiUpdatePrefs(c);
		if(c != null)
			c.close();
	}
	// Someone clicked on a preference list element. This is only interesting
	// if a date preference element has been clicked since we have to show
	// a date picker.
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == mDatePref) {
			showDatePicker();
			return true;
		}
		return false;
	}

	private void showDatePicker() {
		new DatePickerDialog(this, this, mDatum.get(Calendar.YEAR),
				mDatum.get(Calendar.MONTH), mDatum.get(Calendar.DAY_OF_MONTH))
				.show();
	}

	@Override
	public boolean onPreferenceChange(Preference p, Object newValue) {
		if (p instanceof ListPreference) {
			// This doesn't work that way....

			// mMiss cannot be greater than mMissExcused
			// if ("miss_excused".equals(p.getKey())
			// && Integer.parseInt(mMiss.getValue()) < Integer
			// .parseInt(newValue.toString()))

			p.setSummary(newValue.toString());
		} else if (p == mCount) {
			// if the "count this" checkbox is unchecked
			// set missExcused to 0, because uncounted misses are not
			// excused anyway
			if (!(Boolean) newValue) {
				mMissExcused.setValue("0");
				mMissExcused.setSummary("0");
			}
		}

		return true;
	}

	@Override
	public void onDateSet(DatePicker datePicker, int year, int month, int day) {
		mDatum.set(year, month, day);
		mDatePref.setSummary(DateFormat.getDateFormat(this).format(
				mDatum.getTime()));
	}

	public void okClicked(View v) {
		ContentValues values = new ContentValues();

		/* since we need to make queries selecting rows by a specific date
		 * we need to ensure that misses of the same date have the 
		 * same millisecond value (thus use a specific time of day for a
		 * date to save)
		 */
		CalendarTools.resetTime(mDatum);
		values.put(C.MISS_DATUM, mDatum.getTimeInMillis());
		values.put(SchoolTools.buildMissColumn(mCount.isChecked()), mMiss.getValue());
		values.put(SchoolTools.buildMissColumn(!mCount.isChecked()), 0);
		values.put(C.MISS_STUNDEN_E, mMissExcused.getValue());

		try {
			if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
				values.put(C.MISS_SCHUELERID,
						mUri.getQueryParameter(C.MISS_SCHUELERID));
				Uri uri = getContentResolver().insert(mUri, values);
				getIntent().setData(uri);
				setResult(RESULT_OK, getIntent());
			} else
				getContentResolver().update(mUri, values, BaseColumns._ID + "=?", new String[] { mUri.getLastPathSegment() });
		} catch (SQLiteConstraintException ce) {
			Toast.makeText(
					this,
					getResources().getString(
							R.string.toast_update_miss_constraint_violation),
					Toast.LENGTH_LONG).show();
		}

		setResult(RESULT_OK);
		finish();
	}

	public void cancelClicked(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}

	private void guiUpdatePrefs(Cursor c) {
		if (c == null) {
			c = getContentResolver()
					.query((Uri) getIntent().getParcelableExtra(
							C.CONTENT_COURSE_TYPE), null, null, null, null);
			c.moveToFirst();
			String todaysHours = String.valueOf(Math.max(1,
					CalendarTools.getTodaysHours(c, Calendar.getInstance())));
			mMiss.setValue(todaysHours);
			mMiss.setSummary(todaysHours);
			mCount.setChecked(true);
			mMissExcused.setValue("0");
			mMissExcused.setSummary("0");
		} else {
			c.moveToFirst();
			int miss_z = c.getInt(c.getColumnIndex(C.MISS_STUNDEN_Z));
			int miss_nz = c.getInt(c.getColumnIndex(C.MISS_STUNDEN_NZ));
			mMiss.setValue(String.valueOf(Math.max(miss_z, miss_nz)));
			mMiss.setSummary(mMiss.getValue());
			mMissExcused.setValue(c.getString(c
					.getColumnIndex(C.MISS_STUNDEN_E)));
			mMissExcused.setSummary(mMissExcused.getValue());
			mCount.setChecked(c.getInt(c.getColumnIndex(C.MISS_STUNDEN_NZ)) == 0);
			mDatum.setTimeInMillis(c.getLong(c.getColumnIndex(C.MISS_DATUM)));
		}
		mDatePref.setSummary(DateFormat.getDateFormat(this).format(
				mDatum.getTime()));
	}
}
