package org.bob.school.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.bob.school.tools.CalendarTools;

import android.os.Environment;
import android.util.Log;

public class ExportToFile {
	
	private String mPrefix;

	public ExportToFile(String filePrefix) {
		mPrefix = filePrefix;
	}

	public File exportFile(StringBuilder exportBuffer) {
		File fname = null;
		try {
			File root = Environment.getExternalStorageDirectory();
			
			if (root.canWrite()) {
				fname = new File(root, mPrefix + CalendarTools.FILENAME_DATE_FORMATER.format(new Date()) + ".html");
				FileOutputStream f = new FileOutputStream(fname);
				f.write(exportBuffer.toString().getBytes());
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
