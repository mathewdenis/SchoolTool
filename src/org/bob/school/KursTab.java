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

	private Uri mUri;    // data:   .../course/#  (COURSE_ID)

	@Override
    protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

    	final TabHost tabHost = getTabHost();
    	final Resources res = getResources();
    	mUri = getIntent().getData();

    	// set the course name as the title of the activity
		Cursor c = getContentResolver().query(mUri,
				new String[] { C.KURS_NAME }, null, null, null);
		c.moveToFirst();
		setTitle(c.getString(0));
		c.close();

		tabHost.addTab(tabHost.newTabSpec("pupillist")
				.setIndicator(res.getString(R.string.title_kurs_list),
						res.getDrawable(R.drawable.menu_kursliste))
				.setContent(new Intent(Intent.ACTION_PICK, mUri)));

		Uri uri = Uri.withAppendedPath(mUri, C.MISS_SEGMENT);
		tabHost.addTab(tabHost.newTabSpec("misslist")
				.setIndicator(res.getString(R.string.title_kurs_fehlstunde_list),
						res.getDrawable(R.drawable.menu_fehlstunde_kursliste))
				.setContent(new Intent(Intent.ACTION_PICK, uri)));
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
