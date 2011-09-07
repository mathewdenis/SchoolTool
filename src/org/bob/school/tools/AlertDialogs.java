package org.bob.school.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;

public class AlertDialogs {
	/**  Creates an AlertDialog asking for confirmation for a delete operation
	 * @param context The context of the activity to display the dialog in
	 * @param listener The DialogInterface.OnClickListener for the ok-button
	 * @param titleId The id for the string resource of the tile
	 * @param confirmMessageId The id for the string resource of the confirmation message
	 * @return An AlertDialog, readily equipped and armed
	 */
	public static AlertDialog createDeleteConfirmDialog(final Context context, OnClickListener listener, int titleId, int confirmMessageId) {
		return new AlertDialog.Builder(context)
				.setTitle(titleId)
				.setMessage(confirmMessageId)
				.setPositiveButton(android.R.string.ok, listener)
				.setNegativeButton(android.R.string.cancel, null)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.create();
	}

	/**  Creates an AlertDialog asking for confirmation for a delete operation
	 * @param activity The activity to display the dialog in
	 * @param uri The URI for the delete operation
	 * @param titleId The id for the string resource of the tile
	 * @param confirmMessageId The id for the string resource of the confirmation message
	 * @param finishActivity if true the activity finishes with RESULT_OK 
	 * @return An AlertDialog, readily equipped and armed
	 */
	public static AlertDialog createDeleteConfirmDialog(final Activity activity,
			final Uri uri, int titleId, int confirmMessageId, final boolean finishActivity) {
		return createDeleteConfirmDialog(activity, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.getContentResolver().delete(uri, null, null);
				if (finishActivity) {
					activity.setResult(Activity.RESULT_OK);
					activity.finish();
				}
			}

		}, titleId, confirmMessageId);
	}

	/**  Creates an AlertDialog asking for confirmation for a delete operation
	 * @param activity The activity to display the dialog in
	 * @param uri The URI for the delete operation
	 * @param titleId The id for the string resource of the tile
	 * @param confirmMessageId The id for the string resource of the confirmation message
	 * @param notifyUri notify this URI when the item is deleted, can be null
	 * @return An AlertDialog, readily equipped and armed
	 */
	public static AlertDialog createDeleteConfirmDialog(final Activity activity,
			final Uri uri, int titleId, int confirmMessageId, final Uri notifyUri) {
		return createDeleteConfirmDialog(activity, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.getContentResolver().delete(uri, null, null);
				if (notifyUri != null)
					activity.getContentResolver().notifyChange(notifyUri, null);
			}

		}, titleId, confirmMessageId);
	}
}
