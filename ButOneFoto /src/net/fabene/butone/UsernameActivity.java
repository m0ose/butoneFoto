package net.fabene.butone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UsernameActivity extends Activity {

	EditText mUserText;
	Button mButton;
	Intent returnIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.username_input);		
		mUserText = (EditText) findViewById(R.id.input_username);
        mButton = (Button) findViewById(R.id.submit_username); 
        mButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				submitUsername();
			}
		});
		super.onCreate(savedInstanceState);
	}
	
	private void submitUsername() {
		String username = mUserText.getText().toString();
        if (!username.equals("")) {
        	returnIntent = new Intent();
        	returnIntent.putExtra("username", username);
			setResult(RESULT_OK, returnIntent);
			finish();
        }
        else {
        	Toast.makeText(this, "Invalid username.", Toast.LENGTH_SHORT).show();
        }
	}
	
}
