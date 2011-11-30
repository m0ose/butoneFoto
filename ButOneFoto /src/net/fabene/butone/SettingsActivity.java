package net.fabene.butone;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SettingsActivity extends ListActivity {

	private EditText mUserText;
	private Button mButton;
	private static final int COLOR_REQUEST_CODE = 1;
	private static final int USERNAME_REQUEST_CODE = 2;
	private static final int PICTURE_REQUEST_CODE = 3;
	private static final int DELETE_ID = Menu.FIRST + 1;
	private static final int COLOR_ID = Menu.FIRST + 2;
	private static final int PICTURE_ID = Menu.FIRST + 3;
	private TagsDbAdapter mDbAdapter;
	private Cursor mTagsCursor;
	private int mAppWidgetId;
	private Uri imageUri;
	private Handler handler;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.widget_input);

		mDbAdapter = new TagsDbAdapter(this);
		mDbAdapter.open();

		updateList();

		mUserText = (EditText) findViewById(R.id.user_input);

		mButton = (Button) findViewById(R.id.submit);

		mButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				createTag();
			}
		});

		registerForContextMenu(getListView());

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt("appWidgetId");
		}
		super.onCreate(savedInstanceState);
	}

	private void updateList() {
		// Get all of the rows from the database and create the item list
		mTagsCursor = mDbAdapter.fetchAllTags();
		startManagingCursor(mTagsCursor);

		Log.d("Ben","TAGS FETCHED");

		// Create an array to specify the fields we want to display in the list (only TITLE)
		String[] from = new String[]{TagsDbAdapter.KEY_TITLE, TagsDbAdapter.KEY_COLOR};

		// and an array of the fields we want to bind those fields to (in this case just text1)
		int[] to = new int[]{R.id.list_text, R.id.list_color_icon};

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter notes = 
			new SimpleCursorAdapter(this, R.layout.list_item, mTagsCursor, from, to);
		notes.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (columnIndex == 1) { //index for title
					TextView text = (TextView) view;
					text.setText(cursor.getString(1));
				}
				else if (columnIndex == 2) { //index for color
					ImageView iconView = (ImageView) view;
					iconView.setBackgroundColor(cursor.getInt(2));
				}
				return true;
			}
		});

		setListAdapter(notes);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Cursor selectedItem = (Cursor) getListView().getItemAtPosition(position);
		int tagId = selectedItem.getInt(0); //get id from column 0
		String tagTitle = selectedItem.getString(1); //get title from column 1
		int tagColor = selectedItem.getInt(2); //get color form column 2

		mDbAdapter.updateTagWidgetId(tagId, mAppWidgetId);

		final Context context = SettingsActivity.this;

		// Push widget update to surface with newly set prefix
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		ButOneWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId, tagTitle, tagColor);

		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 1, R.string.menu_delete);
		menu.add(0, COLOR_ID, 0, R.string.menu_color);
		menu.add(0, PICTURE_ID, 2, "Take Picture");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
		case DELETE_ID:
			mDbAdapter.deleteTag(info.id);
			updateList();
			return true;
		case COLOR_ID:
			Intent colorIntent = new Intent(this, ColorActivity.class);
			colorIntent.putExtra("id", info.id);
			startActivityForResult(colorIntent, COLOR_REQUEST_CODE);
			return true;
		case PICTURE_ID:
			//define the file-name to save photo taken by Camera activity
			String fileName =  "ButoneTestPic";

			//create parameters for Intent with filename
			ContentValues values = new ContentValues();
			values.put(MediaStore.Images.Media.TITLE, fileName);
			values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera");
			//imageUri is the current activity attribute, define and save it for later usage (also in onSaveInstanceState)
			//create new Intent
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			imageUri = getContentResolver().insert(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

			intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
			intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
			startActivityForResult(intent, PICTURE_REQUEST_CODE);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.set_username:
			startActivityForResult(new Intent(this,UsernameActivity.class), USERNAME_REQUEST_CODE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case COLOR_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				Bundle goodies = data.getExtras();
				if (goodies != null) updateTagColor((int) goodies.getLong("id"),goodies.getInt("color"));
			}
			break;
		case USERNAME_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				Bundle goodies = data.getExtras();
				if (goodies != null) {
					updateUsername(goodies.getString("username"));
					updateAppWidgets();
				}
				else Toast.makeText(this, "NULL GOODIES", Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(this, "BAD RESULT CODE", Toast.LENGTH_SHORT).show();
			}
			break;
		case PICTURE_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				//showMessage("Attempting to upload image...please wait");


				if(android.os.Build.MANUFACTURER.equals("Samsung")){

					// Describe the columns you'd like to have returned. Selecting from the Thumbnails location gives you both the Thumbnail Image ID, as well as the original image ID
					String[] projection = {
							MediaStore.Images.Thumbnails._ID, // The columns we want
							MediaStore.Images.Thumbnails.IMAGE_ID,
							MediaStore.Images.Thumbnails.KIND,
							MediaStore.Images.Thumbnails.DATA};
					String selection = MediaStore.Images.Thumbnails.KIND + "=" + // Select only mini's
					MediaStore.Images.Thumbnails.MINI_KIND;

					String sort = MediaStore.Images.Thumbnails._ID + " DESC";

					//At the moment, this is a bit of a hack, as I'm returning ALL images, and just taking the latest one. There is a better way to narrow this down I think with a WHERE clause which is currently the selection variable
					Cursor myCursor = this.managedQuery(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, selection, null, sort);

					long imageId = 0l;
					long thumbnailImageId = 0l;
					String thumbnailPath = "";

					try{
						myCursor.moveToFirst();
						imageId = myCursor.getLong(myCursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID));
						thumbnailImageId = myCursor.getLong(myCursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID));
						thumbnailPath = myCursor.getString(myCursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA));
					}
					finally{myCursor.close();}

					//Create new Cursor to obtain the file Path for the large image

					String[] largeFileProjection = {
							MediaStore.Images.ImageColumns._ID,
							MediaStore.Images.ImageColumns.DATA
					};

					String largeFileSort = MediaStore.Images.ImageColumns._ID + " DESC";
					myCursor = this.managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, largeFileProjection, null, null, largeFileSort);
					String largeImagePath = "";

					try{
						myCursor.moveToFirst();

						//This will actually give yo uthe file path location of the image.
						largeImagePath = myCursor.getString(myCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
					}
					finally{myCursor.close();}
					// These are the two URI's you'll be interested in. They give you a handle to the actual images
					Uri uriLargeImage = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(imageId));
					Uri uriThumbnailImage = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, String.valueOf(thumbnailImageId));

					// I've left out the remaining code, as all I do is assign the URI's to my own objects anyways...
					String myPath = getRealPathFromURI(uriLargeImage);
					post(myPath);
				}
				else { // a normal phone
					// do it the normal way

					try
					{
						//Toast.makeText(this, "Uploading Image...", Toast.LENGTH_SHORT).show();
						String myPath = getRealPathFromURI(imageUri);
						Toast.makeText(this,  myPath , Toast.LENGTH_SHORT);
						post(myPath);
					}
					catch(Exception e)
					{
						//Toast.makeText(this, "Uploading image failed, Please try again", Toast.LENGTH_SHORT).show();
					}
				}

			}

			{
				break;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void updateTagColor(int id, int color) {
		mDbAdapter.updateTagColor(id, color);
		updateList();
	}

	private void updateUsername(String username) {
		mDbAdapter.setUsername(username);
	}

	private void createTag() {
		String text = mUserText.getText().toString();
		if (!text.equals("")) {
			mDbAdapter.createTag(text, 0xFF0000FF, -1);
			mUserText.setText(null);
			updateList();
		}
	}

	private void updateAppWidgets() {
		Cursor tags = mDbAdapter.fetchAllTags();
		startManagingCursor(tags);
		int[] appWidgetIds = new int[tags.getCount()];
		tags.moveToFirst();
		for (int i = 0; i < appWidgetIds.length; i++) {
			appWidgetIds[i] = tags.getInt(3);
			tags.moveToNext();
		}
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		ButOneWidget.updateAppWidgets(this, appWidgetManager, appWidgetIds);
	}


	public void post(String path) {
		List<NameValuePair> postContent = new ArrayList<NameValuePair>(2);  
		postContent.add(new BasicNameValuePair("key", "8034d3a78d71d73973b5f07aee2fcc68"));  
		postContent.add(new BasicNameValuePair("image", path));


		String url = "http://imgur.com/api/upload.xml";
		HttpClient httpClient = new DefaultHttpClient();
		HttpContext localContext = new BasicHttpContext();
		HttpPost httpPost = new HttpPost(url);

		try {
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

			for(int index=0; index < postContent.size(); index++) {
				if(postContent.get(index).getName().equalsIgnoreCase("image")) {
					// If the key equals to "image", we use FileBody to transfer the data
					entity.addPart(postContent.get(index).getName(), new FileBody(new File (postContent.get(index).getValue())));
				} else {
					// Normal string data
					entity.addPart(postContent.get(index).getName(), new StringBody(postContent.get(index).getValue()));
				}
			}

			httpPost.setEntity(entity);

			HttpResponse response = httpClient.execute(httpPost, localContext);
			Map<?, ?> mImgurResponse = parseResponse (response);

			Iterator<?> it = mImgurResponse.entrySet().iterator();
			while(it.hasNext()){	        	
				HashMap.Entry pairs = (HashMap.Entry)it.next();
				if(pairs.getValue()!=null)
				{
					Log.d("INFO",pairs.getKey().toString());
					Log.d("INFO", pairs.getValue().toString());
					if(pairs.getKey().toString().equals("original"))
					{
						Variables.getInstance().setURL(pairs.getValue().toString());  
					}

				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getRealPathFromURI(Uri contentUri) {

		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}



	private Map<String,String> parseResponse(HttpResponse response) {
		String xmlResponse = null;

		try {
			xmlResponse = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (xmlResponse == null) return null;

		HashMap<String, String> ret = new HashMap<String, String>();
		ret.put("original", getXMLElementValue(xmlResponse, "original_image"));
		ret.put("error", getXMLElementValue(xmlResponse, "error_msg"));
		ret.put("delete", getXMLElementValue(xmlResponse, "delete_page"));

		return ret;
	}

	private String getXMLElementValue(String xml, String elementName) {
		if (xml.indexOf(elementName) >= 0)
			return xml.substring(xml.indexOf(elementName) + elementName.length() + 1, 
					xml.lastIndexOf(elementName) - 2);
		else
			return null;
	}


	@Override
	protected void onPause() {
		super.onPause();
		updateAppWidgets();
	}

	@Override
	protected void onDestroy() {
		mDbAdapter.close();
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

}
