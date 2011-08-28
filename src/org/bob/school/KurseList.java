package org.bob.school;

import org.bob.school.Schule.C;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class KurseList extends ListActivity {
	private static final String DEFAULT_SORT_ORDER_KURS = C.KURS_NAME;
    // Identifiers for our menu items.
    public static final int MENU_ITEM_ADD = Menu.FIRST;
    public static final int MENU_ITEM_EDIT = Menu.FIRST + 1;
    public static final int MENU_ITEM_PICK = Menu.FIRST + 2;

    private Uri mUri;
    
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
        startActivity(new Intent(Intent.ACTION_VIEW, uri));	
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        final long id = getSelectedItemId();

        menu.removeGroup(Menu.CATEGORY_CONTAINER);

        // bad code: depending on edit-mode (when there are devices without
        // a keyboard) -- actually, this is unnecessary
        // if something is selected, display pick and edit menu
        if(id != Long.MIN_VALUE) {
            Uri uri = ContentUris.withAppendedId(mUri, id);
			menu.add(Menu.CATEGORY_CONTAINER, MENU_ITEM_EDIT, 0,
					R.string.menu_course_edit).setShortcut('2', 'e')
					.setIcon(android.R.drawable.ic_menu_edit)
					.setIntent(new Intent(Intent.ACTION_EDIT, uri));

			menu.add(Menu.CATEGORY_CONTAINER, MENU_ITEM_PICK, 0,
					R.string.menu_course_pick).setShortcut('3', 'p')
					.setIcon(android.R.drawable.ic_menu_info_details)
					.setIntent(new Intent(Intent.ACTION_PICK, uri));
        }

    	return super.onPrepareOptionsMenu(menu);
    }
}