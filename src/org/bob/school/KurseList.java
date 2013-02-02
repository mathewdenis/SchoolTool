package org.bob.school;

import org.bob.school.Schule.C;
import org.bob.school.tools.AlertDialogs;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class KurseList extends ListActivity {
	private static final String DEFAULT_SORT_ORDER_KURS = C.KURS_NAME;
    // Identifiers for our menu items.
	private static final int MENU_ITEM_VIEW = Menu.FIRST;
	private static final int MENU_ITEM_ADD = Menu.FIRST + 1;
	private static final int MENU_ITEM_ADD_MISSES = Menu.FIRST + 2;
	private static final int MENU_ITEM_EDIT = Menu.FIRST + 3;
	private static final int MENU_ITEM_DELETE = Menu.FIRST + 4;

    private Uri mUri;  // data: .../course  (COURSE)
    private static class KursListViewBinder implements SimpleCursorAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			// column fields: _id[0], kursname[1], count(pupils)[2]
			((TextView) view).setText(cursor.getString(1) + " ("
					+ cursor.getInt(2) + ")");
			return true;
		}

    }

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mUri = Uri.withAppendedPath(C.CONTENT_URI, C.COURSE_SEGMENT);

		// if the activity has just been started, set the content uri
		// of our content provider as data in the intent 
        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(mUri);

        registerForContextMenu(this.getListView());
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
		createCourseList();
	}

	private void createCourseList() {
		Uri uri = mUri.buildUpon()
				.appendQueryParameter(C.QUERY_PUPIL_COUNT, "1").build();
		Cursor c = new CursorLoader(this, uri, new String[] { C._ID,
				C.KURS_NAME }, null, null, DEFAULT_SORT_ORDER_KURS).loadInBackground();
		c.setNotificationUri(getContentResolver(), mUri);

		SimpleCursorAdapter sca = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, c,
				new String[] { C.KURS_NAME },
				new int[] { android.R.id.text1 });
		sca.setViewBinder(new KursListViewBinder());
		setListAdapter(sca);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// append course name to activity title
        Bundle b = buildExtrasBundle(id);
        Intent i = new Intent(KursTab.ACTION_VIEW_COURSE, ContentUris.withAppendedId(mUri, id));
        i.putExtras(b);
        
        startActivity(i);	
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// This is our one standard application action -- inserting a
		// new course into the list.
		menu.add(Menu.NONE, MENU_ITEM_ADD, 0, R.string.menu_course_insert)
				.setShortcut('1', 'i').setIcon(R.drawable.ic_menu_add)
				.setIntent(new Intent(Intent.ACTION_INSERT, mUri));

		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info;

        info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Cursor c = (Cursor) getListAdapter().getItem(info.position);

        // Setup the menu header
		menu.setHeaderTitle(c.getString(c.getColumnIndex(C.KURS_NAME)));

		// Add a menu item to delete the note
		Uri uri = ContentUris.withAppendedId(mUri, info.id);

		Intent i = new Intent(KursTab.ACTION_VIEW_COURSE, uri);
		i.putExtras(buildExtrasBundle(info.id));
		menu.add(Menu.NONE, MENU_ITEM_VIEW, 0, R.string.menu_course_view)
				.setIntent(i);

		i = new Intent(KursFehlstundenEditor.ACTION_ADD_COURSE_MISSES, uri);
		i.putExtras(buildExtrasBundle(info.id));
		menu.add(Menu.NONE, MENU_ITEM_ADD_MISSES, 0, R.string.menu_misses_insert)
				.setIntent(i);

		menu.add(Menu.NONE, MENU_ITEM_EDIT, 0, R.string.menu_course_edit)
				.setIntent(new Intent(Intent.ACTION_EDIT, uri));

		menu.add(Menu.NONE, MENU_ITEM_DELETE, 0, R.string.menu_course_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);

		AdapterView.AdapterContextMenuInfo info =
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		Uri uri = ContentUris.withAppendedId(mUri, info.id);

		switch (item.getItemId()) {
		case MENU_ITEM_DELETE:
			AlertDialogs.createDeleteConfirmDialog(this, uri,
					R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_course, mUri).show();
			return true;
		}
		return false;

	}

	private Bundle buildExtrasBundle(long id) {
        Bundle b = new Bundle();

        Cursor c = getContentResolver().query(ContentUris.withAppendedId(mUri, id), null, null, null, null);
		c.moveToFirst();
		b.putString(Schule.PREFIX + C.KURS_NAME, c.getString(c.getColumnIndex(C.KURS_NAME)));
		b.putLong(Schule.PREFIX + C.KURS_SDATE, c.getLong(c.getColumnIndex(C.KURS_SDATE)));
		b.putLong(Schule.PREFIX + C.KURS_EDATE, c.getLong(c.getColumnIndex(C.KURS_EDATE)));

		for (int i = 0; i < 5; ++i)
			b.putInt(Schule.PREFIX + C.KURS_WDAYS[i], c.getInt(c.getColumnIndex(C.KURS_WDAYS[i])));

		c.close();
		return b;
	}
}