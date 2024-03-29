package org.bob.school;

import org.bob.school.Schule.C;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TabHost;

public class KursTab extends TabActivity {

	public static final String ACTION_VIEW_COURSE = "org.bob.school.action.VIEW_COURSE";

	private Uri mUri;    // data:   .../course/#  (COURSE_ID)

	@Override
    protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

    	final TabHost tabHost = getTabHost();
    	final Resources res = getResources();
    	mUri = getIntent().getData();
    	Bundle b = getIntent().getExtras();

    	// set the course name as the title of the activity
		Cursor c = getContentResolver().query(mUri,
				new String[] { C.KURS_NAME }, null, null, null);
		c.moveToFirst();
		setTitle(c.getString(0));
		c.close();

		Uri uri = Uri.withAppendedPath(mUri, C.MISS_SEGMENT);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.putExtras(b);
		intent.addCategory(Intent.CATEGORY_TAB);
		tabHost.addTab(tabHost.newTabSpec("misslist")
				.setIndicator(res.getString(R.string.title_kurs_fehlstunde_list),
						res.getDrawable(R.drawable.menu_fehlstunde_kursliste))
				.setContent(intent));

		intent = new Intent(Intent.ACTION_VIEW, mUri);
		intent.putExtras(b);
		intent.addCategory(Intent.CATEGORY_TAB);
		tabHost.addTab(tabHost
				.newTabSpec("pupillist")
				.setIndicator(
						res.getString(R.string.title_kurs_list),
						res.getDrawable(R.drawable.menu_fehlstunden_schuelerliste))
				.setContent(intent));

		uri = Uri.withAppendedPath(mUri, C.CALENDAR_SEGMENT);
		intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.putExtras(b);
		intent.addCategory(Intent.CATEGORY_TAB);
		tabHost.addTab(tabHost
				.newTabSpec("course_calendar")
				.setIndicator(
						res.getString(R.string.title_kurs_kalender),
						res.getDrawable(R.drawable.menu_fehlstunden_schuelerliste))
				.setContent(intent));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
	}
}
