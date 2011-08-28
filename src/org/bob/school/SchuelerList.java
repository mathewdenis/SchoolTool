package org.bob.school;

import java.util.regex.Pattern;

import org.bob.school.Schule.C;
import org.bob.school.tools.AlertDialogs;
import org.bob.school.tools.StringTools;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SchuelerList extends ListActivity {
	public static final String SORT_ORDER_NAME = C.SCHUELER_NACHNAME + "," + C.SCHUELER_VORNAME;
	public static final String SORT_ORDER_MISS = C.MISS_SUM_STUNDEN_Z
			+ " DESC," + C.MISS_SUM_STUNDEN_E + " ASC" + "," + SORT_ORDER_NAME;

	/**
	 * Special intent action meaning "view the pupils of a given course"
	 */
	public static final String ACTION_VIEW_PUPILS = "org.bob.school.action.VIEW_PUPILS";
	// Identifiers for our menu items.
    public static final int MENU_ITEM_SHOW_PUPIL_MISSES = Menu.FIRST + 1;
    public static final int MENU_ITEM_ADD = Menu.FIRST + 1;
    public static final int MENU_ITEM_EDIT = Menu.FIRST + 2;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 3;
    public static final int MENU_ITEM_MISSES = Menu.FIRST + 4;
    public static final int MENU_SORT_LIST = Menu.FIRST + 5;

    public static final String TAG = "SchuelerList";

    // uri of the pupils directory to deal with
	private Uri mUri;          // data: .../course/#
	private Uri mWorkingUri;   // data: .../course/#/pupil
	private short mSortOrderCode = 0;
	
	private static class MyPupilListViewBinder implements
			SimpleCursorAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor c, int columnIndex) {
			/* cursor has the columns
				_id[0], nachname[1], vorname[2], sum_miss_z[3], sum_miss_ex[4], sum_miss_nz[5] */
			TextView tv = (TextView) view;
			int tv_color = tv.getResources().getColor(android.R.color.primary_text_dark);

			boolean b = false;

			if (columnIndex == 1) {             // text1
				StringBuilder sb = new StringBuilder(c.getString(1))
						.append(" (")
						.append(StringTools.writeZeroIfNull(c.getString(3)))
						.append("/")
						.append(StringTools.writeZeroIfNull(c.getString(5)))
						.append("/")
						.append(StringTools.writeZeroIfNull(c.getString(4)))
						.append(")");

				tv.setText(sb);
				b = true;
			}
			if(c.getInt(3)!=c.getInt(4))
				tv_color = tv.getResources().getColor(R.color.color_unexcused);
//			else
//			tv_color = tv.getResources().getColor(R.color.color_excused);

			tv.setTextColor(tv_color);

			return b;
		}
	}
	
	/** Called when the activity is first created
     */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUri = getIntent().getData();
		mWorkingUri = Uri.withAppendedPath(mUri, C.PUPIL_SEGMENT);

		// get course name
		Cursor c = getContentResolver().query(mUri,
				new String[] { C.KURS_NAME }, null, null, null);

		c.moveToFirst();
		setTitle(getTitle() + ": " + c.getString(c.getColumnIndex(C.KURS_NAME)));
		c.close();

		registerForContextMenu(getListView());

		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
      	createPupilList();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getContentResolver().notifyChange(mUri, null);
	}

	private void createPupilList() {
		Uri uri = mWorkingUri
				.buildUpon()
				.appendQueryParameter(C.QUERY_SUM_MISS, "1").build();

		Cursor c = managedQuery(uri, new String[] { C._ID,
				C.SCHUELER_NACHNAME, C.SCHUELER_VORNAME,
				C.MISS_SUM_STUNDEN_Z,
				C.MISS_SUM_STUNDEN_E,
				C.MISS_SUM_STUNDEN_NZ}, null, null,
				getSortOrder());

		c.setNotificationUri(getContentResolver(), mUri);
		SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_2, c,
				new String[] { C.SCHUELER_NACHNAME,
						C.SCHUELER_VORNAME }, new int[] {
						android.R.id.text1, android.R.id.text2 });
		sca.setViewBinder(new MyPupilListViewBinder());
		setListAdapter(sca);
	}

	private String getSortOrder() {
		if(mSortOrderCode == 0)
			return SORT_ORDER_NAME;
		else
			return SORT_ORDER_MISS;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroying SchuelerList activity");
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
 
        Uri uri = ContentUris.withAppendedId(mWorkingUri, id);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ITEM_ADD, 0, R.string.menu_pupil_insert)
				.setShortcut('1', 'i').setIcon(android.R.drawable.ic_menu_add);
		menu.add(Menu.NONE, MENU_ITEM_EDIT, 0, R.string.menu_course_edit)
				.setShortcut('2', 'e').setIcon(android.R.drawable.ic_menu_edit)
				.setIntent(new Intent(Intent.ACTION_EDIT, mUri));
		menu.add(Menu.NONE, MENU_ITEM_DELETE, 0, R.string.menu_course_delete)
				.setShortcut('3', 'd').setIcon(android.R.drawable.ic_menu_delete);
		menu.add(Menu.NONE, MENU_SORT_LIST, 0, R.string.menu_sort_by_size)
				.setShortcut('4', 's');

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem soi = menu.getItem(3);
		if(mSortOrderCode==0) {
			soi.setIcon(android.R.drawable.ic_menu_sort_by_size);
			soi.setTitle(R.string.menu_sort_by_size);
		}
		else {
			soi.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
			soi.setTitle(R.string.menu_sort_alph);
		}

		// only enable if list is non-empty 
		soi.setEnabled(getListView().getCount() > 0);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case MENU_ITEM_ADD :
			createAddDialog().show();
			break;
		case MENU_ITEM_DELETE :
			AlertDialogs.createDeleteConfirmDialog(this, mUri, R.string.dialog_confirm_delete_title, 
					R.string.dialog_confirm_delete_course, true).show();
			break;
		case MENU_SORT_LIST :
			mSortOrderCode = (short)(1-mSortOrderCode);
			createPupilList();
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info;

        info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Cursor c = (Cursor) getListAdapter().getItem(info.position);

        // Setup the menu header
		menu.setHeaderTitle(c.getString(c
				.getColumnIndex(C.SCHUELER_NACHNAME))
				+ ", "
				+ c.getString(c.getColumnIndex(C.SCHUELER_VORNAME)));

		// Add a menu item to delete the note
		Uri uri = ContentUris.withAppendedId(mWorkingUri, info.id);
		menu.add(Menu.NONE, MENU_ITEM_SHOW_PUPIL_MISSES, 0,
				R.string.menu_misses_show)
				.setIntent(new Intent(Intent.ACTION_VIEW, uri));
		menu.add(Menu.NONE, MENU_ITEM_EDIT, 0, R.string.menu_pupil_edit);
		menu.add(Menu.NONE, MENU_ITEM_DELETE, 0, R.string.menu_pupil_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		boolean b = false;

		AdapterView.AdapterContextMenuInfo info =
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final Uri uri = ContentUris.withAppendedId(mWorkingUri, info.id);

		Cursor c = (Cursor)getListAdapter().getItem(info.position);

		switch (item.getItemId()) {
		case MENU_ITEM_EDIT:
			final EditText et = new EditText(getApplicationContext());
			et.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);
			et.setText(c.getString(c.getColumnIndex(C.SCHUELER_NACHNAME)) + ", "
					+ c.getString(c.getColumnIndex(C.SCHUELER_VORNAME)));

			new AlertDialog.Builder(this)
					.setTitle(R.string.menu_pupil_edit)
					.setMessage(R.string.dialog_pupil_edit)
					.setPositiveButton(android.R.string.ok,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String[] name = et.getText().toString()
											.trim().split("\\s*,\\s*", 2);
									ContentValues values = new ContentValues(2);
									values.put(C.SCHUELER_NACHNAME,
											name[0]);
									values.put(C.SCHUELER_VORNAME,
											name[1]);
									getContentResolver().update(uri, values,
											null, null);
									// notify managed cursor of this ListView
									// about the change
									getContentResolver().notifyChange(
											mUri, null);
								}
							}).setIcon(android.R.drawable.ic_media_ff)
					.setView(et).create().show();

				b = true;
			break;
		case MENU_ITEM_DELETE:
			AlertDialogs.createDeleteConfirmDialog(this, uri,
					R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_pupil, false).show();

			b = true;
		}
		return b;
	}

	private AlertDialog createAddDialog() {
		final EditText et = new EditText(getApplicationContext());
		et.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);

		return new AlertDialog.Builder(this)
				.setTitle(R.string.menu_pupil_insert)
				.setMessage(R.string.dialog_pupil_add_format)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {

					/* Insert all the students from the text field into the
					 * list.
					 */
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Pattern kommaPattern = Pattern.compile("\\s*,\\s*");
						String[] namesToAdd = et.getText().toString().trim().split("\\s*;\\s*");
						ContentValues[] values = new ContentValues[namesToAdd.length];
						int i=0;
						for(String s : namesToAdd) {
							String[] nameSep = kommaPattern.split(s, 2);
							values[i] = new ContentValues();
							values[i].put(C.SCHUELER_NACHNAME, nameSep[0]);
							values[i++].put(C.SCHUELER_VORNAME, nameSep[1]);
						}
						if(i>0) {
							getContentResolver().bulkInsert(mWorkingUri, values);
							Toast.makeText(
									SchuelerList.this,
									getResources().getString(R.string.toast_pupil_add, 
															 namesToAdd.length),
									Toast.LENGTH_SHORT).show();
						}
					}
				}).setNegativeButton(android.R.string.cancel, null)
				.setIcon(android.R.drawable.ic_media_ff).setView(et)
				.create();
	}
}
