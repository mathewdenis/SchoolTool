package org.bob.school;

import org.bob.school.Schule.C;
import org.bob.school.tools.AlertDialogs;

import android.app.ListActivity;
import android.content.ContentUris;
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

public class KurseList extends ListActivity {
	private static final String DEFAULT_SORT_ORDER_KURS = C.KURS_NAME;
    // Identifiers for our menu items.
	private static final int MENU_ITEM_VIEW = Menu.FIRST;
	private static final int MENU_ITEM_ADD = Menu.FIRST + 1;
	private static final int MENU_ITEM_ADD_MISSES = Menu.FIRST + 2;
	private static final int MENU_ITEM_EDIT = Menu.FIRST + 3;
	private static final int MENU_ITEM_DELETE = Menu.FIRST + 4;

    private Uri mUri;  // data: .../course  (COURSE)
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUri = Uri.withAppendedPath(C.CONTENT_URI, C.COURSE_SEGMENT);

		// if the activity has just been started, set the content uri
		// of our content provider as data in the intent 
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(mUri);
        }

        registerForContextMenu(this.getListView());

		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
		createCourseList();
	}

	private void createCourseList() {
		Cursor c = managedQuery(mUri, new String[] { C._ID,
				C.KURS_NAME }, null, null, DEFAULT_SORT_ORDER_KURS);

		setListAdapter(new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, c,
				new String[] { C.KURS_NAME },
				new int[] { android.R.id.text1 }));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

        Uri uri = ContentUris.withAppendedId(mUri, id);
        startActivity(new Intent(KursTab.ACTION_VIEW_COURSE, uri));	
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// This is our one standard application action -- inserting a
		// new course into the list.
		menu.add(Menu.NONE, MENU_ITEM_ADD, 0, R.string.menu_course_insert)
				.setShortcut('1', 'i').setIcon(android.R.drawable.ic_menu_add)
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
		menu.add(Menu.NONE, MENU_ITEM_VIEW, 0, R.string.menu_course_view)
				.setIntent(new Intent(KursTab.ACTION_VIEW_COURSE, uri));		
		menu.add(Menu.NONE, MENU_ITEM_ADD_MISSES, 0, R.string.menu_misses_insert)
		.setIntent(new Intent(KursFehlstundenEditor.ACTION_ADD_COURSE_MISSES, uri));		
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
					R.string.dialog_confirm_delete_course, true).show();
			return true;
		}
		return false;

	}
}