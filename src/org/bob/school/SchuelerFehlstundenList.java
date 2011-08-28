package org.bob.school;

import java.util.Date;

import org.bob.school.Schule.Constants;
import org.bob.school.tools.AlertDialogs;
import org.bob.school.tools.CalendarTools;
import org.bob.school.tools.SchoolTools;
import org.bob.school.tools.StringTools;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SchuelerFehlstundenList extends Activity implements OnItemClickListener, OnHierarchyChangeListener {
	private static final String DEFAULT_SORT_ORDER_MISSES = Constants.MISS_DATUM;

	/**
	 * Special intent action meaning "add misses for a pupil"
	 */
	public static final String ACTION_ADD_PUPIL_MISSES = "org.bob.school.action.ADD_PUPIL_MISSES";
//	private static final String TAG = "SchuelerFehlstunden";

	private Uri mUri;      // data: .../course/#/pupil/# (PUPIL_ID)
	private ListView mMissList;
	private TextView mCourseName;
	private TextView mPupilName;

	private static final int MENU_ITEM_EDIT_MISS = Menu.FIRST;
	private static final int MENU_ITEM_DELETE_MISS = Menu.FIRST + 1;

	private static class DateBinder implements SimpleCursorAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor c, int columnIndex) {
			TextView tv = (TextView) view;

			/* cursor has the columns
			_id[0], miss_datum[1], miss_count[2], miss_ex[3], miss_ncount[4] */
			if(c.getInt(4)==0 && c.getInt(2) != c.getInt(3))
				tv.setTextColor(tv.getResources().getColor(R.color.color_unexcused));
//			else if(c.getInt(4)==0)
//				tv.setTextColor(tv.getResources().getColor(R.color.color_excused));

			if (columnIndex == 1) // view describes the date
				tv.setText(CalendarTools.dateFormatter.format(new Date(c
						.getLong(columnIndex))));
			else // view describes the misses
				tv.setText(c.getString(2) + "/" +  c.getString(4) + "/"
						+ c.getShort(3));

			return true;
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Cursor c;

		setContentView(R.layout.pupil_miss_layout);

		mUri = getIntent().getData();
		mPupilName = (TextView) findViewById(R.id.pupil_name);
		mCourseName = (TextView) findViewById(R.id.course_name);
		mMissList = (ListView) findViewById(R.id.miss_list);

		// get course name
		Uri uri = Uri.withAppendedPath(Constants.CONTENT_URI, Constants.COURSE_SEGMENT);
		uri = Uri.withAppendedPath(uri, mUri.getPathSegments().get(1));
		c = getContentResolver().query(uri,
				new String[] { Constants.KURS_NAME }, null, null, null);
		c.moveToFirst();
		mCourseName.setText(c.getString(0));
		mPupilName.setText(R.string.title_fehlstunde_edit);

		c.close();

		// pupil information cursor


		writePupilsNameAndMissSum();

		registerForContextMenu(mMissList);
		mMissList.setOnItemClickListener(this);

		uri = Uri.withAppendedPath(Constants.CONTENT_URI, Constants.MISS_SEGMENT);
		c = managedQuery(uri, new String[] { Constants._ID,
				Constants.MISS_DATUM, Constants.MISS_STUNDEN_Z,
				Constants.MISS_STUNDEN_E,
				Constants.MISS_STUNDEN_NZ},
				Constants.MISS_SCHUELERID + "= ?",
				new String[] { mUri.getLastPathSegment() },
				DEFAULT_SORT_ORDER_MISSES);
		c.setNotificationUri(getContentResolver(), uri);

		SimpleCursorAdapter ca = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_2, c, new String[] {
						Constants.MISS_DATUM, Constants.MISS_STUNDEN_Z },
				new int[] { android.R.id.text1, android.R.id.text2 });
		ca.setViewBinder(new DateBinder());

		mMissList.setAdapter(ca);
		mMissList.setOnHierarchyChangeListener(this);
	}


	// clicking on a list item toggles between excused and unexcused
	// misses
	@Override
	public void onItemClick(AdapterView<?> l, View view, int position, long id) {
		ContentValues values = new ContentValues();

		// Cursor to selected element
		Cursor c = (Cursor) mMissList.getAdapter().getItem(position);
		int miss = c.getInt(c.getColumnIndex(Constants.MISS_STUNDEN_Z));
		int miss_ex = c.getInt(c.getColumnIndex(Constants.MISS_STUNDEN_E));

		// only toggle when the miss is to be counted
		if (c.getInt(c.getColumnIndex(Constants.MISS_STUNDEN_NZ)) == 0) {
			if (miss_ex > 0)
				// there are excused hours, so toggle miss_ex to 0
				values.put(Constants.MISS_STUNDEN_E, 0);
			else
				// excused hours are 0, so toggle miss_ex to the value of miss
				values.put(Constants.MISS_STUNDEN_E, miss);

			getContentResolver().update(SchoolTools.buildMissUri(id), values, null, null);
			writePupilsNameAndMissSum();
		}
		// old version of onItemClick (opens the miss editor)
		//		startActivity(new Intent(FehlstundeEditor.ACTION_ADD_EDIT_MISS, uri));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ITEM_DELETE_MISS, 0, R.string.menu_pupil_delete)
		.setShortcut('1', 'i').setIcon(android.R.drawable.ic_menu_delete);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case MENU_ITEM_DELETE_MISS :
			AlertDialogs.createDeleteConfirmDialog(this, mUri,
					R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_pupil, true).show();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info;

        info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Cursor cursor = (Cursor) mMissList.getAdapter().getItem(info.position);

        // Setup the menu header
		menu.setHeaderTitle(CalendarTools.dateFormatter.format(new Date(cursor
				.getLong(cursor.getColumnIndex(Constants.MISS_DATUM)))));

		// Edit or delete the miss
		menu.add(Menu.NONE, MENU_ITEM_EDIT_MISS, 0, R.string.title_fehlstunde_edit)
				.setIntent(new Intent(FehlstundeEditor.ACTION_ADD_EDIT_MISS,
								      SchoolTools.buildMissUri(info.id)));
		menu.add(Menu.NONE, MENU_ITEM_DELETE_MISS, 0, R.string.menu_miss_delete);
    }

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		boolean b = false;

		AdapterView.AdapterContextMenuInfo info =
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {
		case MENU_ITEM_DELETE_MISS:
			final Uri uri = ContentUris.withAppendedId(Uri.withAppendedPath(
					Constants.CONTENT_URI, Constants.MISS_SEGMENT), info.id);
			AlertDialogs.createDeleteConfirmDialog(this, uri,
					R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_miss, false).show();
			b = true;
			break;
		}
		return b;
	}

	@Override
	public void onChildViewAdded(View vParent, View vChild) {
		writePupilsNameAndMissSum();
	}

	@Override
	public void onChildViewRemoved(View vParent, View vChild) {
		writePupilsNameAndMissSum();
	}

	private void writePupilsNameAndMissSum() {
		// get pupil name and misses sums
		Cursor c = getContentResolver().query(
				mUri.buildUpon()
						.appendQueryParameter(Constants.QUERY_SUM_MISS, "1")
						.build(),
				new String[] { Constants.SCHUELER_NACHNAME,
						Constants.SCHUELER_VORNAME, Constants.MISS_SUM_STUNDEN_Z,
						Constants.MISS_SUM_STUNDEN_E, Constants.MISS_SUM_STUNDEN_NZ }, null, null,
				null);

		c.moveToFirst();
		setTitle(new StringBuilder(c.getString(0))
						.append(", ")
						.append(c.getString(1))
						.append(" (")
						.append(StringTools.writeZeroIfNull(c
								.getString(2)))
						.append("/")
						.append(StringTools.writeZeroIfNull(c
								.getString(4)))
						.append("/")
						.append(StringTools.writeZeroIfNull(c
								.getString(3))).append(")"));
		c.close();
	}
}
