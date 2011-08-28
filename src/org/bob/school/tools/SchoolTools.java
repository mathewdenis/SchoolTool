package org.bob.school.tools;

import org.bob.school.Schule.C;

import android.content.ContentUris;
import android.net.Uri;

public class SchoolTools {

	public static Uri buildMissUri(long id) {
		return ContentUris.withAppendedId(Uri.withAppendedPath(
				C.CONTENT_URI, C.MISS_SEGMENT), id);
	}

	public static String buildMissColumn(boolean doesMissCount) {
		return doesMissCount ? C.MISS_STUNDEN_Z : C.MISS_STUNDEN_NZ;
	}
}
