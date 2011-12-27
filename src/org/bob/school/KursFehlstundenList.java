package org.bob.school;

import java.io.File;
import java.util.Date;

import org.bob.school.Schule.C;
import org.bob.school.export.ExportToFile;
import org.bob.school.tools.AlertDialogs;
import org.bob.school.tools.CalendarTools;
import org.bob.school.tools.SchoolTools;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class KursFehlstundenList extends Activity implements OnChildClickListener {
	private static final String DEFAULT_SORT_ORDER_DATE = C.MISS_DATUM;
	private static final String DEFAULT_SORT_ORDER_NAME = C.SCHUELER_NACHNAME
			+ " COLLATE NOCASE," + C.SCHUELER_VORNAME + " COLLATE NOCASE";

	private static final int MENU_ITEM_ADD_MISSES = Menu.FIRST;
	private static final int MENU_ITEM_EDIT_MISS = Menu.FIRST + 1;
	private static final int MENU_ITEM_DELETE_MISS = Menu.FIRST + 2;
	private static final int MENU_ITEM_DELETE_MISS_ALL = Menu.FIRST + 3;
	private static final int MENU_ITEM_EXPORT = Menu.FIRST + 4;

	private Uri mUri; // .../course/#/miss (COURSE_MISS)
	private ExpandableListView mExpListView;

	private static class MySimpleCursorTreeAdapter extends SimpleCursorTreeAdapter {
		private Activity mActivity;
		private Uri mUri;

		public MySimpleCursorTreeAdapter(Activity activity, Uri uri, Cursor cursor, int groupLayout, String[] groupFrom, int[] groupTo, int childLayout, String[] childFrom, int[] childTo) {
			super(activity, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
			mActivity = activity;
			mUri = uri;
		}

		@Override
		public Cursor getChildrenCursor(Cursor groupCursor) {
			Cursor c = mActivity.managedQuery(mUri, new String[] {
					C.MISS_TABLE + "." + C._ID,
					C.SCHUELER_NACHNAME,
					C.SCHUELER_VORNAME, C.MISS_STUNDEN_Z,
					C.MISS_STUNDEN_E,
					C.MISS_STUNDEN_NZ },
					C.MISS_DATUM + "=?",
					new String[] { groupCursor.getString(groupCursor
							.getColumnIndex(C.MISS_DATUM)) },
					DEFAULT_SORT_ORDER_NAME);
			c.setNotificationUri(mActivity.getContentResolver(), mUri);
			return c;
		}
	}

	public static class DateBinder implements
			SimpleCursorTreeAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor c, int columnIndex) {
			TextView tv = (TextView) view;
			int tv_color = tv.getResources().getColor(
					android.R.color.primary_text_dark);
			// in this case, the columns are (_id [0], datum [1], miss_count_sum [2], miss_ex_sum [3], miss_ncount_sum [4])
			if (c.getColumnIndex(C.SCHUELER_NACHNAME) == -1) {
				tv.setText(CalendarTools.LISTVIEW_DATE_FORMATER.format(new Date(c.getLong(1))));

				// set text color appropriately dependening on whether all
				// misses are excused
				if(c.getInt(2)!=c.getInt(3))
					tv_color = tv.getResources().getColor(R.color.color_unexcused);
//				else
//					tv_color = tv.getResources().getColor(R.color.color_excused);
			}
			// here, columns are (_id [0], nachname [1], vorname [2], miss_count [3], miss_ex [4], miss_ncount[5])
			else {
				if (columnIndex == 1)
					tv.setText(c.getString(1) + ", " + c.getString(2));
				else
					tv.setText(c.getString(3) + "/" + c.getString(5) + "/"
							+ c.getString(4));

				if (c.getInt(5) == 0 && c.getInt(3) != c.getInt(4))
					tv_color = tv.getResources().getColor(R.color.color_unexcused);
//				else if (c.getInt(5) == 0)
//					tv_color = tv.getResources().getColor(R.color.color_excused);
			}
			tv.setTextColor(tv_color);
			return true;
		}
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		Cursor c;
		mUri = getIntent().getData();

		/* Important: It might seem more reasonable to use an ExpandableListActivity
		 * all the way to simplify some coding.
		 * However, this activity is used in a TabView along with another ListActivity.
		 *
		 * When a state save occurs (e.g. by changing the orientation of the device)
		 * both states, the list view's and the expandable list view's state would than
		 * be saved under the same id (android:id/list), i.e. one state object will
		 * be overwritten. Now when the ExpandableListView is to be restored the
		 * previously saved state object is taken out the container under it's
		 * (unfortunately not unique) id. The state object is casted to
		 * ExpandableListView$StateSaved, however it is of type ListView$SavedState
		 * and the activity is going to crash with a ClassCastException.
		 * See issue 2732 and issue 3443 in the android issue tracker
		 * (http://code.google.com/p/android/issues).
		 */
		mExpListView = new ExpandableListView(this);
		mExpListView.setId(R.id.expandableList);
		mExpListView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		setContentView(mExpListView);

		// append course name to activity title
		Uri uri = Uri.withAppendedPath(C.CONTENT_URI,
				C.COURSE_SEGMENT);
		uri = Uri.withAppendedPath(uri, mUri.getPathSegments().get(1));
		c = getContentResolver().query(uri,
				new String[] { C.KURS_NAME }, null, null, null);
		c.moveToFirst();
		setTitle(getTitle() + ": "
				+ c.getString(c.getColumnIndex(C.KURS_NAME)));
		c.close();

		registerForContextMenu(mExpListView);
		mExpListView.setOnChildClickListener(this);

		// dirty hack's projection is { max(miss._id), miss.datum, miss_sum, miss_ex_sum }

		c = managedQuery(
				mUri.buildUpon()
						.appendQueryParameter(
								C.QUERY_DISTINCT_DATES_WITH_ID_HACK,
								"1").build(), null, null, null,
				DEFAULT_SORT_ORDER_DATE);
		c.setNotificationUri(getContentResolver(), mUri);

		// set the expandable list adapter
		MySimpleCursorTreeAdapter cta = new MySimpleCursorTreeAdapter(this,
				mUri, c, android.R.layout.simple_expandable_list_item_1,
				new String[] { C._ID }, new int[] { android.R.id.text1 },
				android.R.layout.simple_expandable_list_item_2, new String[] {
						C.SCHUELER_NACHNAME, C.MISS_STUNDEN_Z }, new int[] {
						android.R.id.text1, android.R.id.text2 });
		cta.setViewBinder(new DateBinder());
		mExpListView.setAdapter(cta);
	}

	@Override
	public boolean onChildClick(ExpandableListView parentView, View child, int groupPos, int childPos, long id) {
		ContentValues values = new ContentValues();

		// Cursor to selected element
		Cursor c = (Cursor) mExpListView.getExpandableListAdapter().getChild(
				groupPos, childPos);
		int miss = c.getInt(c.getColumnIndex(C.MISS_STUNDEN_Z));
		int miss_ex = c.getInt(c.getColumnIndex(C.MISS_STUNDEN_E));

		// only toggle when the miss is to be counted
		if (c.getInt(c.getColumnIndex(C.MISS_STUNDEN_NZ)) == 0) {
			if (miss_ex > 0)
				// there are excused hours, so toggle miss_ex to 0
				values.put(C.MISS_STUNDEN_E, 0);
			else
				// excused hours are 0, so toggle miss_ex to the value of miss
				values.put(C.MISS_STUNDEN_E, miss);

			getContentResolver().update(SchoolTools.buildMissUri(id), values,
					null, null);
			getContentResolver().notifyChange(mUri, null);
		}
		return true;
		// old version of onItemClick (opens the miss editor)
		//		startActivity(new Intent(FehlstundeEditor.ACTION_ADD_EDIT_MISS, uri));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(
				C.CONTENT_URI, C.COURSE_SEGMENT), mUri
				.getPathSegments().get(1));

		menu.add(Menu.NONE, MENU_ITEM_ADD_MISSES, 0,
				R.string.menu_misses_insert)
				.setShortcut('1', 'a')
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
				.setIntent(
						new Intent(
								KursFehlstundenEditor.ACTION_ADD_COURSE_MISSES,
								uri));
		menu.add(Menu.NONE, MENU_ITEM_EXPORT, 0, R.string.menu_export);

		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_EXPORT:
			MySimpleCursorTreeAdapter cta = ((MySimpleCursorTreeAdapter)mExpListView.getExpandableListAdapter());
			Cursor c = cta.getCursor();
			c.moveToFirst();

			StringBuilder b = new StringBuilder();
			b.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\" /></head><body>");
			while(!c.isAfterLast()) {
				b.append("<div><p><strong>")
						.append(CalendarTools.LISTVIEW_DATE_FORMATER
								.format(new Date(c.getLong(1))))
						.append("</strong></p><p>");
				Cursor cc = cta.getChildrenCursor(c);
				cc.moveToFirst();
				while(!cc.isAfterLast()) {
					b.append("<span style=\"");
					if(cc.getInt(4)==0 && cc.getInt(3)>0) {
						b.append("color:red;");
					}
					b.append("\">");
					b.append(cc.getString(1)).append(", ").append(cc.getString(2));
					if(cc.getInt(3)>0) {
						b.append(" (").append(cc.getString(3)).append("h");
						if(cc.getInt(4)==cc.getInt(3))
							b.append(",e");
						else if(cc.getInt(4)>0)
							b.append(",").append(cc.getInt(4)).append("h e");
						else
							b.append(",ue");
						b.append(")");
					} else if(cc.getInt(5)>0)
						b.append(" (").append(cc.getString(5)).append("h,SV)");
					b.append("</span>");
					cc.moveToNext();
					if(!cc.isAfterLast())
						b.append("; ");
				}
				b.append("</p></div>");
				c.moveToNext();
			}
			b.append("</body></html>");
			File filename = 
			new ExportToFile("tage_fehlstunden_" + mUri.getPathSegments().get(1) + "_").exportFile(b);
			Toast.makeText(
					KursFehlstundenList.this,
					getResources().getString(R.string.export_as_file,
							filename.getAbsoluteFile()), Toast.LENGTH_SHORT)
					.show();
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
        	Cursor c = (Cursor) mExpListView.getExpandableListAdapter().getChild(pgroup, pchild);
			menuHeader = c.getString(c
					.getColumnIndex(C.SCHUELER_NACHNAME))
					+ ", " + c.getString(c.getColumnIndex(C.SCHUELER_VORNAME));

			// Edit or delete the miss
			menu.add(Menu.NONE, MENU_ITEM_EDIT_MISS, 0, R.string.title_fehlstunde_edit)
					.setIntent(new Intent(Intent.ACTION_EDIT,
									      SchoolTools.buildMissUri(info.id)));
			menu.add(Menu.NONE, MENU_ITEM_DELETE_MISS, 0,
					R.string.menu_miss_delete);
		} else if(ptype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
        	Cursor c = (Cursor) mExpListView.getExpandableListAdapter().getGroup(pgroup);

			menuHeader = CalendarTools.LISTVIEW_DATE_FORMATER.format(new Date(c
					.getLong(c.getColumnIndex(C.MISS_DATUM))));

			menu.add(Menu.NONE, MENU_ITEM_DELETE_MISS_ALL, 0, R.string.menu_miss_delete_all);
		}
        // set the menu header title
        menu.setHeaderTitle(menuHeader);
    }

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		boolean b = false;

		final ExpandableListView.ExpandableListContextMenuInfo info =
			(ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();

		final Uri uri = Uri.withAppendedPath(C.CONTENT_URI, C.MISS_SEGMENT);
		
        switch(item.getItemId()) {
        case MENU_ITEM_DELETE_MISS :
        	final Uri modUri = ContentUris.withAppendedId(uri, info.id);

			AlertDialogs.createOKCancelDialog(this, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					getContentResolver().delete(modUri, null, null);

					// notify the managed cursor of the ExpandableListView
					// that its content has changed (notice: mUri, not modUri)
					getContentResolver().notifyChange(mUri, null);
				}
			}, R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_miss).show();
			b = true;
        	break;

		case MENU_ITEM_DELETE_MISS_ALL:
			AlertDialogs.createOKCancelDialog(
					this,
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							int pgroup = ExpandableListView
									.getPackedPositionGroup(info.packedPosition);
							int childCount = mExpListView.getExpandableListAdapter()
									.getChildrenCount(pgroup);
							for (int i = 0; i < childCount; ++i)
								getContentResolver()
										.delete(ContentUris.withAppendedId(uri,
												mExpListView.getExpandableListAdapter()
														.getChildId(pgroup, i)),
												null, null);

							// see above for explanation
							getContentResolver().notifyChange(mUri, null);

							Toast.makeText(
									KursFehlstundenList.this,
									getResources().getString(R.string.toast_miss_delete,
															 childCount),
									Toast.LENGTH_SHORT).show();
						}
					}, R.string.dialog_confirm_delete_title,
					R.string.dialog_confirm_delete_miss_all).show();
			b = true;
			break;
		}

        return b;
	}	
}