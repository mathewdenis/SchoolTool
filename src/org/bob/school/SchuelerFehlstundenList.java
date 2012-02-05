package org.bob.school;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.bob.school.Schule.C;
import org.bob.school.export.ExportToFile;
import org.bob.school.tools.AlertDialogs;
import org.bob.school.tools.CalendarTools;
import org.bob.school.tools.SchoolTools;
import org.bob.school.tools.StringTools;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorTreeAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SchuelerFehlstundenList extends ExpandableListActivity {
	public static final String SORT_ORDER_NAME = C.SCHUELER_NACHNAME  + " COLLATE NOCASE," + C.SCHUELER_VORNAME + " COLLATE NOCASE";
	public static final String SORT_ORDER_MISS = C.MISS_SUM_STUNDEN_Z
			+ " DESC," + C.MISS_SUM_STUNDEN_E + " ASC" + "," + SORT_ORDER_NAME;
	private static final String SORT_ORDER_DATE = C.MISS_DATUM;	

	/**
	 * Special intent action meaning "view the pupils of a given course"
	 */
	public static final String ACTION_VIEW_PUPILS = "org.bob.school.action.VIEW_PUPILS";
	// Identifiers for our menu items.
    private static final int MENU_ITEM_ADD_PUPIL = Menu.FIRST;
    private static final int MENU_ITEM_ADD_MISS = Menu.FIRST + 1;
    private static final int MENU_ITEM_EDIT_COURSE = Menu.FIRST + 2;
    private static final int MENU_ITEM_EDIT_PUPIL = Menu.FIRST + 3;
    private static final int MENU_ITEM_EDIT_MISS = Menu.FIRST + 4;
    private static final int MENU_ITEM_DELETE_COURSE = Menu.FIRST + 5;
    private static final int MENU_ITEM_DELETE_PUPIL = Menu.FIRST + 6;
    private static final int MENU_ITEM_DELETE_MISS = Menu.FIRST + 7;
    private static final int MENU_SORT_LIST = Menu.FIRST + 8;
	private static final int MENU_ITEM_EXPORT = Menu.FIRST + 9;

    public static final String TAG = "SchuelerList";

    // uri of the pupils directory to deal with
	private Uri mUri;          // data: .../course/#
	private Uri mWorkingUri;   // data: .../course/#/pupil
	private short mSortOrderCode = 0;
	private String mTitle;
	private Calendar mSDatum, mEDatum;
	
	private static class MyPupilListViewBinder implements
			SimpleCursorTreeAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor c, int columnIndex) {
			TextView tv = (TextView) view;
			int tv_color = tv.getResources().getColor(android.R.color.primary_text_dark);

			boolean b = false;
			// case: child list item
			// columns are: _id[0], datum[1], miss_count[2], miss_ex[3], miss_ncount[4]
			if (c.getColumnIndex(C.SCHUELER_NACHNAME) == -1) {
				StringBuilder sb = new StringBuilder(CalendarTools.LISTVIEW_DATE_FORMATER.format(new Date(c.getLong(1))));
				sb.append(" (")
						.append(StringTools.writeZeroIfNull(c.getString(2)))
						.append("/")
						.append(StringTools.writeZeroIfNull(c.getString(4)))
						.append("/")
						.append(StringTools.writeZeroIfNull(c.getString(3)))
						.append(")");
				tv.setText(sb);
				if(c.getInt(2)>c.getInt(3))
					tv_color = tv.getResources().getColor(R.color.color_unexcused);

				b = true;
			} // group list item, nachname to be bound
			  // columns are: _id[0], nachname[1], vorname[2], miss_sum[3], miss_ex_sum[4], miss_ncount_sum[5]
			else if (columnIndex == 1) {
				StringBuilder sb = new StringBuilder(c.getString(columnIndex));
				sb.append(" (")
				.append(StringTools.writeZeroIfNull(c.getString(3)))
				.append("/")
				.append(StringTools.writeZeroIfNull(c.getString(5)))
				.append("/")
				.append(StringTools.writeZeroIfNull(c.getString(4)))
				.append(")");
				tv.setText(sb);
				if(c.getInt(3)>c.getInt(4))
					tv_color = tv.getResources().getColor(R.color.color_unexcused);
				b = true;
			}
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

		// get course data
		Cursor c = getContentResolver().query(mUri,
				new String[] { C.KURS_NAME, C.KURS_SDATE, C.KURS_EDATE }, null, null, null);
		c.moveToFirst();
		mTitle = c.getString(c.getColumnIndex(C.KURS_NAME));
		(mSDatum = Calendar.getInstance()).setTimeInMillis(c.getLong(c.getColumnIndex(C.KURS_SDATE)));
		(mEDatum = Calendar.getInstance()).setTimeInMillis(c.getLong(c.getColumnIndex(C.KURS_EDATE)));
		c.close();

		registerForContextMenu(getExpandableListView());

		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
      	createPupilList();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Uri settings_uri = Uri.withAppendedPath(C.CONTENT_URI, C.SETTINGS_SEGMENT);
		ContentValues vals = new ContentValues();
		vals.put(C.SETTINGS_VALUE_INT, mSDatum.getTimeInMillis());
		getContentResolver().update(settings_uri, vals, C.SETTINGS_NAME + "=?", new String[] { C.STARTDATE_SUM_MISS_SETTING });
		vals.clear();
		vals.put(C.SETTINGS_VALUE_INT, mEDatum.getTimeInMillis());
		getContentResolver().update(settings_uri, vals, C.SETTINGS_NAME + "=?", new String[] { C.ENDDATE_SUM_MISS_SETTING });

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
		SimpleCursorTreeAdapter sca = new SimpleCursorTreeAdapter(this, c,
				android.R.layout.simple_expandable_list_item_2, new String[] {
						C.SCHUELER_NACHNAME, C.SCHUELER_VORNAME }, new int[] {
						android.R.id.text1, android.R.id.text2 },
				R.layout.small_expandable_list_item_1,
				new String[] { C._ID }, new int[] { android.R.id.text1 }) {

			@Override
			protected Cursor getChildrenCursor(Cursor groupCursor) {
				Uri uri = Uri.withAppendedPath(C.CONTENT_URI, C.MISS_SEGMENT);
				groupCursor.getInt(0);
				Cursor c = managedQuery(
						uri,
						new String[] { C._ID, C.MISS_DATUM, C.MISS_STUNDEN_Z,
								C.MISS_STUNDEN_E, C.MISS_STUNDEN_NZ },
						C.MISS_SCHUELERID + "= ? and " + C.MISS_DATUM
								+ " between ? and ?",
						new String[] { groupCursor.getString(0),
								String.valueOf(mSDatum.getTimeInMillis()),
								String.valueOf(mEDatum.getTimeInMillis()) },
						SORT_ORDER_DATE);
				c.setNotificationUri(getContentResolver(), uri);
				return c;
			}
		};

				
		sca.setViewBinder(new MyPupilListViewBinder());
		setListAdapter(sca);
	}

	// clicking on a list item toggles between excused and unexcused
	// misses
	@Override
	public boolean onChildClick(ExpandableListView lv, View v, int gPos, int cPos, long id) {
		super.onChildClick(lv, v, gPos, cPos, id);
		ContentValues values = new ContentValues();

		// Cursor to selected child element
		// columns are: _id[0], datum[1], miss_count[2], miss_ex[3], miss_ncount[4]
		Cursor c = (Cursor) getExpandableListAdapter().getChild(gPos, cPos);
		int miss = c.getInt(2);
		int miss_ex = c.getInt(3);

		// only toggle when the miss is to be counted
		if (c.getInt(4) == 0) {
			if (miss_ex > 0)
				// there are excused hours, so toggle miss_ex to 0
				values.put(C.MISS_STUNDEN_E, 0);
			else
				// excused hours are 0, so toggle miss_ex to the value of miss
				values.put(C.MISS_STUNDEN_E, miss);

			getContentResolver().update(SchoolTools.buildMissUri(id), values,
					BaseColumns._ID + "=?", new String[] { String.valueOf(id) });
			getContentResolver().notifyChange(mUri, null);
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ITEM_ADD_PUPIL, 0, R.string.menu_pupil_insert)
				.setShortcut('1', 'i').setIcon(R.drawable.ic_menu_add);
		menu.add(Menu.NONE, MENU_ITEM_EDIT_COURSE, 0, R.string.menu_course_edit)
				.setShortcut('2', 'e').setIcon(R.drawable.ic_menu_edit)
				.setIntent(new Intent(Intent.ACTION_EDIT, mUri));
		menu.add(Menu.NONE, MENU_ITEM_DELETE_COURSE, 0,
				R.string.menu_course_delete).setShortcut('3', 'd')
				.setIcon(R.drawable.ic_menu_delete);
		menu.add(Menu.NONE, MENU_SORT_LIST, 0, R.string.menu_sort_by_size)
				.setShortcut('4', 's');
		menu.add(Menu.NONE, MENU_ITEM_EXPORT, 0, R.string.menu_export)
				.setIcon(R.drawable.ic_menu_export).setShortcut('5', 'x');
		;

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
		soi.setEnabled(getExpandableListView().getCount() > 0);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_ADD_PUPIL:
			createAddDialog().show();
			break;
		case MENU_ITEM_DELETE_COURSE:
			AlertDialogs.createDeleteConfirmDialog(this, mUri,
					R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_course, true).show();
			break;
		case MENU_SORT_LIST:
			mSortOrderCode = (short) (1 - mSortOrderCode);
			createPupilList();
			break;
		case MENU_ITEM_EXPORT:
			File filename = new ExportToFile("kurs_fehlstunden_"
					+ mUri.getLastPathSegment() + "_")
					.append(SchuelerFehlstundenList.this,
							R.string.exportheader)
					.append(generateHtml(((CursorTreeAdapter) getExpandableListAdapter())
							.getCursor()))
					.append(SchuelerFehlstundenList.this,
							R.string.exportfooter)
					.exportFile().getAbsoluteFile();
			Toast.makeText(
					SchuelerFehlstundenList.this,
					getResources()
							.getString(
									R.string.export_as_file, filename),
					Toast.LENGTH_LONG).show();
			
			startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(
					Uri.fromFile(filename), "text/html"));
			break;
		}

		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, view, menuInfo);
    	ExpandableListView.ExpandableListContextMenuInfo info;

        info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        int ptype = ExpandableListView.getPackedPositionType(info.packedPosition);
        int pchild = ExpandableListView.getPackedPositionChild(info.packedPosition);
        int pgroup = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        String menuHeader = "";
        if(ptype == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
        	Cursor c = (Cursor) getExpandableListAdapter().getChild(pgroup, pchild);

    		// columns are: _id[0], datum[1], miss_count[2], miss_ex[3], miss_ncount[4]
			menuHeader = CalendarTools.LISTVIEW_DATE_FORMATER.format(new Date(c
					.getLong(1)));        	
			menu.add(Menu.NONE, MENU_ITEM_EDIT_MISS, 0,
					R.string.title_fehlstunde_edit).setIntent(
					new Intent(Intent.ACTION_EDIT, SchoolTools
							.buildMissUri(info.id)));
			menu.add(Menu.NONE, MENU_ITEM_DELETE_MISS, 0,
					R.string.menu_miss_delete);
        } else if(ptype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
        	Cursor c = (Cursor) getExpandableListAdapter().getGroup(pgroup);
			Uri uri = Uri.withAppendedPath(C.CONTENT_URI, C.MISS_SEGMENT).buildUpon()
					.appendQueryParameter(C.MISS_SCHUELERID, String.valueOf(info.id)).build();
			Intent intent = new Intent(Intent.ACTION_INSERT, uri);
			// put the course number into the intent
			intent.putExtra(C.CONTENT_COURSE_TYPE, mUri);

		    // columns are: _id[0], nachname[1], vorname[2], miss_sum[3], miss_ex_sum[4], miss_ncount_sum[5]
			menuHeader = c.getString(1) + ", " + c.getString(2);
			menu.add(Menu.NONE, MENU_ITEM_ADD_MISS, 0,
					R.string.menu_misses_insert).setIntent(intent);
			menu.add(Menu.NONE, MENU_ITEM_EDIT_PUPIL, 0, R.string.menu_pupil_edit);
			menu.add(Menu.NONE, MENU_ITEM_DELETE_PUPIL, 0, R.string.menu_pupil_delete);
        }
        menu.setHeaderTitle(menuHeader);
    }

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		boolean b = false;
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item
				.getMenuInfo();
		final Uri uri = ContentUris.withAppendedId(mWorkingUri, info.id);

		switch (item.getItemId()) {
		case MENU_ITEM_EDIT_PUPIL:
			Cursor c = (Cursor) getExpandableListAdapter().getGroup(
					ExpandableListView.getPackedPositionGroup(info.packedPosition));

			// columns are: _id[0], nachname[1], vorname[2], miss_sum[3], miss_ex_sum[4], miss_ncount_sum[5]
			final EditText et = new EditText(getApplicationContext());
			et.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);
			et.setText(c.getString(1) + ", " + c.getString(2));

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
									getContentResolver().update(
											uri,
											values,
											BaseColumns._ID,
											new String[] { uri
													.getLastPathSegment() });
									// notify managed cursor of this ListView
									// about the change
									getContentResolver().notifyChange(
											mUri, null);
								}
							}).setIcon(android.R.drawable.ic_media_ff)
					.setView(et).create().show();

				b = true;
			break;
		case MENU_ITEM_DELETE_PUPIL:
			AlertDialogs.createDeleteConfirmDialog(this, uri,
					R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_pupil, false).show();

			b = true;
			break;
		case MENU_ITEM_DELETE_MISS:
			final Uri uri2 = ContentUris.withAppendedId(Uri.withAppendedPath(
					C.CONTENT_URI, C.MISS_SEGMENT), info.id);
			AlertDialogs.createDeleteConfirmDialog(this, uri2,
					R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_miss, mUri).show();
			b = true;
			break;
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
									SchuelerFehlstundenList.this,
									getResources().getString(R.string.toast_pupil_add, 
															 namesToAdd.length),
									Toast.LENGTH_SHORT).show();
						}
					}
				}).setNegativeButton(android.R.string.cancel, null)
				.setIcon(android.R.drawable.ic_media_ff).setView(et)
				.create();
	}

	private StringBuilder generateHtml(Cursor c) {
		StringBuilder b = new StringBuilder();
		b.append("<table><tr><th>Name</th><th>Fehlstunden</th><th>unentschuldigt</th></tr>");
		c.moveToFirst();
		while(!c.isAfterLast()) {
			b.append("<tr><td>").append(c.getString(1)).append(", ")
			.append(c.getString(2)).append("</td><td>")
			.append(StringTools.writeZeroIfNull(c.getString(3))).append("</td><td>")
			.append(c.getInt(3)-c.getInt(4)).append("</td></tr>\n");
			c.moveToNext();
		}
		b.append("</table>");
		return b;
	}

	private String getSortOrder() {
		if(mSortOrderCode == 0)
			return SORT_ORDER_NAME;
		else
			return SORT_ORDER_MISS;
	}

}
