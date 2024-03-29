package org.bob.school.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.bob.school.tools.CalendarTools;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class ExportToFile {
	
	private String mPrefix;
	private StringBuilder exportBuilder;
	private char buf[] = new char[200];

	public ExportToFile(String filePrefix) {
		mPrefix = filePrefix;
		exportBuilder = new StringBuilder();
	}

	public ExportToFile append(Context context, int resourceId) {
		try {
			InputStreamReader isr = new InputStreamReader(context.getAssets()
					.open(context.getString(resourceId)));
			int l;
			while ((l = isr.read(buf)) > -1)
				exportBuilder.append(buf, 0, l);
			isr.close();
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		return this;
	}

	public ExportToFile append(StringBuilder b) {
		exportBuilder.append(b);
		return this;
	}

	/** Write export to file and return the file object. 
	 * @return File object of export file
	 */
	public File exportFile() {
		File fname = null;
		try {
			File root = Environment.getExternalStorageDirectory();
			
			if (root.canWrite()) {
				fname = new File(root, mPrefix + CalendarTools.FILENAME_DATE_FORMATER.format(new Date()) + ".html");
				FileOutputStream f = new FileOutputStream(fname);
				f.write(exportBuilder.toString().getBytes());
				f.close();
//				Intent i = new Intent(Intent.ACTION_VIEW);
//
//				i.setDataAndType(Uri.fromFile(fname), "text/html");
//				startActivity(i);
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("ExportToFile", "Failed to write to " + fname.getAbsolutePath() + ": " + e.getMessage());
		}
		return fname;
	}
}
