package org.sandalov.gsmalarmmanager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	private static final int RESULT_SETTINGS = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent i = new Intent(this, SettingsActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			return true;
		}
		if (id == R.id.action_exit) {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment implements
			OnClickListener {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			Button b_arm = (Button) rootView.findViewById(R.id.button_arm);
			Button b_disarm = (Button) rootView
					.findViewById(R.id.button_disarm);
			Button b_check = (Button) rootView.findViewById(R.id.button_check);
			Button b_relay_on = (Button) rootView
					.findViewById(R.id.button_relay_on);
			Button b_relay_off = (Button) rootView
					.findViewById(R.id.button_relay_off);
			b_arm.setOnClickListener(this);
			b_disarm.setOnClickListener(this);
			b_check.setOnClickListener(this);
			b_relay_on.setOnClickListener(this);
			b_relay_off.setOnClickListener(this);
			return rootView;
		}

		@Override
		public void onClick(View v) {

			Context c = getActivity();

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(c);
			String alarmPass = prefs.getString("alarm_password", "");
			String alarmNum = prefs.getString("alarm_number", "");
			boolean alertsOn = prefs.getBoolean("alerts", true);
			boolean smsOverInternet = prefs.getBoolean("sms_provider", false);
			String apiKey = prefs.getString("smsru_apikey", "");

			String smsText = alarmPass;

			switch (v.getId()) {
			case R.id.button_arm:
				smsText += "#" + "1#1#";
				break;
			case R.id.button_disarm:
				smsText += "#" + "1#0#";
				break;
			case R.id.button_check:
				smsText += "#" + "13#1#";
				break;
			case R.id.button_relay_on:
				smsText += "#" + "94#1#";
				break;
			case R.id.button_relay_off:
				smsText += "#" + "94#0#";
				break;
			}

			sendSmsDialog(c, alarmNum, smsText, alertsOn, smsOverInternet,
					apiKey);
		}
	}

	public static void showAlert(final Context c, String title, String message,
			final String sms, final String phoneNo,
			final boolean smsOverInternet, final String apiKey) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(c);
		alertDialogBuilder.setTitle(title);
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// if this button is clicked, close current
								// activity
								sendSms(c, phoneNo, sms, smsOverInternet,
										apiKey);
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// if this button is clicked, just close
						// the dialog box and do nothing
					}
				});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	public static void sendSmsDialog(Context c, String phoneNo, String sms,
			boolean alertsOn, boolean smsOverInternet, String apiKey) {
		if (alertsOn) {
			String alertText = "To: " + phoneNo + "\nMessage: " + sms;
			if (smsOverInternet) {
				alertText += "\n (over internet)";
			}
			showAlert(c, "Send SMS?", alertText, sms, phoneNo, smsOverInternet,
					apiKey);
		} else {
			sendSms(c, phoneNo, sms, smsOverInternet, apiKey);
		}

	}

	public static void sendSms(Context c, String phoneNo, String sms,
			boolean smsOverInternet, String apiKey) {

		if ((smsOverInternet) && (apiKey.length() > 0)) {
			sendSmsInternet(c, phoneNo, sms, apiKey);
		} else {
			sendSmsAndroid(c, phoneNo, sms);
		}
	}

	public static void sendSmsAndroid(Context c, String phoneNo, String sms) {
		try {
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage(phoneNo, null, sms, null, null);
			Toast.makeText(c, "SMS Sent!", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Toast.makeText(c, "SMS faild, please try again later!",
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	public static void sendSmsInternet(Context c, String phoneNo, String sms,
			String apiKey) {
		
		phoneNo = phoneNo.replaceAll("[^\\d.]", "");
		
		String urlString = "";
		try {
			urlString = "http://sms.ru/sms/send?api_id=" + apiKey + "&to="
					+ phoneNo + "&text=" + URLEncoder.encode(sms, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new CallAPI().execute(urlString);

	}

	private static class CallAPI extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			String urlString = params[0]; // URL to call
			String httpresp = "";
			InputStream in = null;

			// HTTP Get
			try {
				URL url = new URL(urlString);
				HttpURLConnection urlConnection = (HttpURLConnection) url
						.openConnection();
				in = new BufferedInputStream(urlConnection.getInputStream());
			} catch (Exception e) {
				System.out.println(e.getMessage());
				return e.getMessage();
			}

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append((line + "\n"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			httpresp = sb.toString();
			return httpresp;
		}
	} // end CallAPI

}
