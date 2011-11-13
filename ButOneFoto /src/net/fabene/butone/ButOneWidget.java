package net.fabene.butone;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

public class ButOneWidget extends AppWidgetProvider {
	
	private static TagsDbAdapter mDbAdapter;
	
	private static String username = "";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		updateAppWidgets(context, appWidgetManager, appWidgetIds);
	}
		
	public static RemoteViews buildUpdate(Context context, int appWidgetId, String tagTitle) {
		Intent settingsIntent = new Intent(context, SettingsActivity.class);
		settingsIntent.putExtra("appWidgetId", appWidgetId);
		settingsIntent.setAction("" + Math.random()); //something to do with making sure it doesn't reuse an old intent
		
		Intent postIntent = new Intent(context, PostService.class);
		postIntent.putExtra("tag", tagTitle);
		postIntent.putExtra("username", username);
		postIntent.setAction("" + Math.random());
		
		PendingIntent pendingPostIntent = PendingIntent.getService(context, 0, postIntent, 0);
		PendingIntent pendingSettingsIntent = PendingIntent.getActivity(context, 0, settingsIntent, 0);
	
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_view);
		updateViews.setOnClickPendingIntent(R.id.widget, pendingSettingsIntent);
		updateViews.setOnClickPendingIntent(R.id.post_button, pendingPostIntent);
		return updateViews;
	}

	public static void updateAppWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		mDbAdapter = new TagsDbAdapter(context);
		mDbAdapter.open();
		
		Cursor userCursor = mDbAdapter.getUsername();
		if (userCursor.getColumnCount() >= 4 && userCursor.getCount() > 0) username = userCursor.getString(1);
		else Log.d("Ben", "couldn't get username properly");
		
		String tagTitle;
		int tagColor;
		
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            Cursor linkedTag = mDbAdapter.fetchTagOfWidget(appWidgetId);
            if (linkedTag.getColumnCount() >= 4 && linkedTag.getCount() > 0) {
	            tagTitle = linkedTag.getString(1);
	            tagColor = linkedTag.getInt(2);
            }
            else {
            	tagTitle = "";
            	tagColor = 0xFFFFFFFF;
            }
            updateAppWidget(context, appWidgetManager, appWidgetId, tagTitle, tagColor);
            linkedTag.close();
        }
        mDbAdapter.close();
	}
	
	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String title, int color) {		
		RemoteViews updateViews = buildUpdate(context, appWidgetId, title);
		
		
//		sadly, there doesn't seem to be a way to retrieve the
//		image button from the widget, so you can't just set a
//		tint value or something clean like that. this was the
//		best solution I came up with, ugly as it is
		
		switch(color) {
		case 0xFF000000:
	        updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_black);
			break;
		case 0xFFFF0000:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_red);
			break;
		case 0xFF00FF00:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_green);
			break;
		case 0xFF0000FF:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_blue);
			break;
		case 0xFFFFFF00:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_yellow);
			break;
		case 0xFFFF00FF:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_purple);
			break;
		case 0xFF00FFFF:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_teal);
			break;
		case 0xFFFFFFFF:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_white);
			break;
		case 0xFFAAAAAA:
			updateViews.setImageViewResource(R.id.post_button, R.drawable.button_bg_grey);
			break;
		}
		
		if (!title.equals("")) updateViews.setTextViewText(R.id.message, title);
		else updateViews.setTextViewText(R.id.message, "no tag assigned");
		
        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
    }
}
