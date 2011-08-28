package org.bob.school;

import java.util.Calendar;

import org.bob.school.Schule.Constants;
import org.bob.school.tools.AlertDialogs;
import org.bob.school.tools.CalendarTools;
import org.bob.school.tools.SchoolTools;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

public class FehlstundeEditor extends PreferenceActivity implements
		OnDateSetListener, OnPreferenceChangeListener,
		OnPreferenceClickListener {

	/**
	 * Special intent action meaning "edit miss"
	 */
	public static final String ACTION_ADD_EDIT_MISS = "org.bob.school.action.EDIT_MISS";

	private static final int MENU_ITEM_DELETE = Menu.FIRST;

	private Uri mUri; // data: .../miss/# (MISS_ID)
	private Calendar mDatum;
	private ListPreference mMiss;
	private ListPreference mMissExcused;
	private CheckBoxPreference mCount;
	private Preference mDatePref;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		mUri = getIntent().getData();

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

		Cursor c = getContentResolver().query(mUri, null, null, null, null);
		guiUpdatePrefs(c);
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

		CalendarTools.resetTime(mDatum);
		values.put(Constants.MISS_DATUM, mDatum.getTimeInMillis());
		values.put(SchoolTools.buildMissColumn(mCount.isChecked()), mMiss.getValue());
		values.put(SchoolTools.buildMissColumn(!mCount.isChecked()), 0);
		values.put(Constants.MISS_STUNDEN_E, mMissExcused.getValue());
		getContentResolver().update(mUri, values, null, null);
		setResult(RESULT_OK);
		finish();
	}

	public void cancelClicked(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ITEM_DELETE, 0, R.string.menu_miss_delete)
				.setShortcut('1', 'i')
				.setIcon(android.R.drawable.ic_menu_delete);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_DELETE:
			AlertDialogs.createDeleteConfirmDialog(this, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getContentResolver().delete(mUri, null, null);
					finish();
				}
			}, R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_miss).show();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void guiUpdatePrefs(Cursor c) {
		c.moveToFirst();
		int miss_z = c.getInt(c.getColumnIndex(Constants.MISS_STUNDEN_Z));
		int miss_nz = c.getInt(c.getColumnIndex(Constants.MISS_STUNDEN_NZ));
		mMiss.setValue(String.valueOf(Math.max(miss_z, miss_nz)));
		mMiss.setSummary(mMiss.getValue());
		mMissExcused.setValue(c.getString(c
				.getColumnIndex(Constants.MISS_STUNDEN_E)));
		mMissExcused.setSummary(mMissExcused.getValue());
		mCount.setChecked(c.getInt(c.getColumnIndex(Constants.MISS_STUNDEN_NZ)) == 0);
		mDatum = Calendar.getInstance();
		mDatum.setTimeInMillis(c.getLong(c.getColumnIndex(Constants.MISS_DATUM)));
		mDatePref.setSummary(DateFormat.getDateFormat(this).format(
				mDatum.getTime()));
		c.close();
	}
}
