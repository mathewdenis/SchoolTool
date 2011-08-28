package org.bob.school.tools;

import org.bob.school.Schule.Constants;

import android.content.ContentUris;
import android.net.Uri;

public class SchoolTools {

	public static Uri buildMissUri(long id) {
		return ContentUris.withAppendedId(Uri.withAppendedPath(
				Constants.CONTENT_URI, Constants.MISS_SEGMENT), id);
	}

	public static String buildMissColumn(boolean doesMissCount) {
		return doesMissCount ? Constants.MISS_STUNDEN_Z : Constants.MISS_STUNDEN_NZ;
	}
}
