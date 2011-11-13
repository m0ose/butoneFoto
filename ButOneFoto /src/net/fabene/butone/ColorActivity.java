package net.fabene.butone;

import java.io.File;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

public class ColorActivity extends Activity {
	
	GridView mGrid;
	Bundle returnInfo;
	long id;
	int requestNum = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
		setContentView(R.layout.color_input);
        id = getIntent().getLongExtra("id", -1);
        if (id == -1) {
        	finish();
        }
        mGrid = (GridView) findViewById(R.id.myGrid);
        mGrid.setAdapter(new ColorsAdapter());
        super.onCreate(savedInstanceState);
	}
	
	private class ColorsAdapter extends BaseAdapter {

		int[] mColors = new int[]{0xFFFF0000,0xFF00FF00,0xFF0000FF,0xFFFFFF00,0xFFFF00FF,0xFF00FFFF,0xFFFFFFFF,0xFF000000,0xFFAAAAAA};
		
        private GradientDrawable mDrawable;
		
		public final int getCount() {
            return mColors.length;
        }

        public final Object getItem(int position) {
            return mColors[position];
        }

        public final long getItemId(int position) {
            return position;
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			
			ImageView i;
			int curCol = mColors[position];

            mDrawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                                             new int[] { curCol, curCol, curCol });
            mDrawable.setShape(GradientDrawable.RECTANGLE);
            mDrawable.setCornerRadii(new float[]{5,5,5,5,5,5,5,5});
            mDrawable.setGradientRadius((float)(Math.sqrt(2) * 60));
			
			if (convertView == null) {
                i = new ImageView(ColorActivity.this);
                i.setScaleType(ImageView.ScaleType.FIT_CENTER);
                i.setLayoutParams(new GridView.LayoutParams(50, 50));
            } else {
                i = (ImageView) convertView;
            }
			i.setOnClickListener(new MyClickListener(curCol));
            i.setImageDrawable(mDrawable);
            return i;
		}
		
		private class MyClickListener implements OnClickListener {

			int color;
			Intent returnIntent;
			
			
			public MyClickListener(int color) {
				this.color = color;
				returnIntent = new Intent();
			}
			
			
			
			public void onClick(View v) {
				
				returnIntent.putExtra("color", color);
				returnIntent.putExtra("id", id);
				setResult(RESULT_OK, returnIntent);
				finish();
			}
			
		}
	}
}
