package com.example.weatherapp;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weatherapp.WeatherData.Forecast;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.WebDialog;

import org.json.simple.*;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends Activity {
	
	private List<TextView> weatherTextViews = new ArrayList<TextView>();
	public WeatherData weatherData;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		TableLayout table = (TableLayout)findViewById(R.id.weatherDataTable);
		
		for (int i = 1; i <= 5; i++) {
			
			TableRow row = new TableRow(this);
			table.addView(row);
			
			for (int col = 0; col < 4; col++) {
				TextView dataText = new TextView(this);
				
				dataText.setTextAppearance(this, R.style.DefaultTableCell);
				
				Resources res = getResources();
				Drawable background;
				
				if (i % 2 == 0)
					background = res.getDrawable(R.drawable.table_cell_light_blue);
				else
					background = res.getDrawable(R.drawable.table_cell_white);
				
				dataText.setBackground(background);
				dataText.setText("Test");
		
				TableRow.LayoutParams params;
				
				if (col == 1)
					params = new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 2.0f);
				else
					params = new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
				
				if (col == 2)
					dataText.setTextColor(Color.parseColor("#E3C800"));
				else if (col == 3)
					dataText.setTextColor(Color.parseColor("#028ACF"));
				
				dataText.setLayoutParams(params);
				dataText.setPadding(2, 2, 2, 2);
				dataText.setGravity(Gravity.CENTER_HORIZONTAL);
				dataText.setId(4 * (i - 1) + col + 1);
				row.addView(dataText);
				weatherTextViews.add(dataText);
			}
			
		}
		
		hideDynamicView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	  super.onActivityResult(requestCode, resultCode, data);
	  Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
	
	public void shareCurrentWeather(View view) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		String[] items = {"Post Current Weather", "Cancel"};
		alertDialogBuilder.setTitle("Post to Facebook")
			.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) 
						postWeather(true);
				}
			});
		alertDialogBuilder.show();
	}
	
	public void shareWeatherForecast(View view) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		String[] items = {"Post Weather Forecast", "Cancel"};
		alertDialogBuilder.setTitle("Post to Facebook")
			.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) 
						postWeather(false);
				}
			});
		alertDialogBuilder.show();
	}
	
	public void postWeather(boolean shareCurrentWeather) {
		Session.StatusCallback statusCallback = new SessionStatusCallback(this, shareCurrentWeather);
		Session session = Session.getActiveSession();
		
		try {
			if (session != null && !session.isOpened() && !session.isClosed()) {
				session.openForRead(new Session.OpenRequest(this)
					.setPermissions(Arrays.asList("basic_info"))
					.setCallback(statusCallback));
			}
			else {
				Session.openActiveSession(this, true, statusCallback);
			}	
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private class SessionStatusCallback implements Session.StatusCallback {
		private Activity context;
		private boolean shareCurrentWeather;
		
		public SessionStatusCallback(Activity context, boolean shareCurrentWeather) {
			this.context = context;
			this.shareCurrentWeather = shareCurrentWeather;
		}
		
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			if (state.isOpened()) {
				if (exception == null) {
					List<String> permissions = session.getPermissions();
					System.out.println(permissions.toString());
					if (permissions.contains("publish_actions"))
						publishFeedDialog(shareCurrentWeather);
					else {
						Session.NewPermissionsRequest newPermissionsRequest = new Session
							   .NewPermissionsRequest(context, Arrays.asList("publish_actions"));
						session.requestNewPublishPermissions(newPermissionsRequest);
					}
				}
			}
		}
		
		private void publishFeedDialog(boolean shareCurrentWeather) {
		    WeatherData data = ((MainActivity)context).weatherData;
		    
			Bundle params = new Bundle();
			
			params.putString("name", data.city.concat(", ").concat(data.region).concat(", ").concat(data.country));
		    params.putString("link", data.feed);
		    
			if (shareCurrentWeather) {
				params.putString("caption", "The current condition for ".concat(data.city).concat(" is ").concat(data.temperatureText).concat("."));
			    
			    String description = "Temperature is ".concat(data.temperature).concat("\uu00B0")
			    		.concat(data.temperatureType).concat(".");
			    params.putString("description", description);
			    params.putString("picture", data.imgURL);
			}
			else {
				params.putString("caption", "Weather Forecast for ".concat(data.city));
				String description = "";
				for (Iterator<WeatherData.Forecast> i = data.forecasts.iterator(); i.hasNext(); ) {
					WeatherData.Forecast forecast = i.next();
					description = description
							.concat(forecast.day).concat(": ")
							.concat(forecast.text).concat(", ")
							.concat(forecast.high).concat("/")
							.concat(forecast.low).concat("\uu00B0").concat(data.temperatureType)
							.concat("; ");
				}
				params.putString("description", description);
				params.putString("picture", "http://www-scf.usc.edu/~csci571/2013Fall/hw8/weather.jpg");
			}
			
		    JSONObject properties = new JSONObject();
		    JSONObject lookAtDetails = new JSONObject();
		    lookAtDetails.put("text", "here");
		    lookAtDetails.put("href", weatherData.link);
		    properties.put("Look at details", lookAtDetails);
		    params.putString("properties", properties.toJSONString());
		    
		    WebDialog feedDialog = (
		        new WebDialog.FeedDialogBuilder(context,
		            Session.getActiveSession(),
		            params))
		        .setOnCompleteListener(new WebDialog.OnCompleteListener() {

		            @Override
		            public void onComplete(Bundle values,
		                FacebookException error) {
		                if (error == null) {
		                    // When the story is posted, echo the success
		                    // and the post Id.
		                    final String postId = values.getString("post_id");
		                    if (postId != null) {
		                        Toast.makeText(context,
		                            "Posted story, id: "+postId,
		                            Toast.LENGTH_SHORT).show();
		                    } else {
		                        // User clicked the Cancel button
		                        Toast.makeText(context.getApplicationContext(), 
		                            "Publish cancelled", 
		                            Toast.LENGTH_SHORT).show();
		                    }
		                } else if (error instanceof FacebookOperationCanceledException) {
		                    // User clicked the "x" button
		                    Toast.makeText(context.getApplicationContext(), 
		                        "Publish cancelled", 
		                        Toast.LENGTH_SHORT).show();
		                } else {
		                    // Generic, ex: network error
		                    Toast.makeText(context.getApplicationContext(), 
		                        "Error posting story", 
		                        Toast.LENGTH_SHORT).show();
		                }
		            }

		        })
		        .build();
		    feedDialog.show();
		}
	}
	
	//when search button is clicked.
	public void displayWeatherInfo(View view) {
		//hide dynamic view
		hideDynamicView();
		
		//get location
		EditText location = (EditText) findViewById(R.id.locationEditText);
		String searchQuery = location.getText().toString().replaceAll("\\s+", " ");
		
		String errorMessage = "";
		boolean hasError;
		boolean locationIsCity = false;
		
		//is the search query empty?
		if (hasError = searchQuery.isEmpty())
			errorMessage = "Please enter a location.";
		else {
			//is search (valid/invalid) a zip code or city?
			Pattern digitPattern = Pattern.compile("^\\s?\\d+\\s?$");
			locationIsCity = !digitPattern.matcher(searchQuery).matches();
			
			//is zip code/ city valid?
			Pattern zipPattern = Pattern.compile("^\\s?\\d{5,5}\\s?$");
			Pattern cityPattern = Pattern.compile("^\\s?\\w[\\w\\s'-\\.]*,(?=.*\\w)[\\w\\s]+,?(?=.*\\w)[\\w\\s]+$");
			
			if (locationIsCity == false) {
				if (zipPattern.matcher(searchQuery).matches())
					hasError = false;
				else {
					hasError = true;
					errorMessage = "Please enter a valid 5 digit zip code";
				}
			}
			else {
				if (cityPattern.matcher(searchQuery).matches())
					hasError = false;
				else {
					hasError = true;
					errorMessage = "Invalid input. Valid input types are Los Angeles, CA or Los Angeles, CA, USA";
				}
			}
		}
		
		if (hasError) {
			AlertDialog.Builder invalidInputDialogBuilder = new AlertDialog.Builder(this);
			invalidInputDialogBuilder
				.setTitle("Query Error")
				.setMessage(errorMessage)
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//do nothing
					}
				})
				.show();
		}
		
		else {
			String url = "http://cs-server.usc.edu:24684/examples/servlet/weathersearch?location=";
			
			try {
				url = url.concat(URLEncoder.encode(searchQuery, "UTF-8"));
			}
			catch(Exception e) {
				Log.d("Exception", e.getMessage()); //We must never reach here!
			}
			
			if (locationIsCity)
				url = url.concat("&type=city");
			else
				url = url.concat("&type=zip&");

			//check if F or C was selected
			RadioButton f = (RadioButton) findViewById(R.id.fahrenheitRadioButton);
			
			if (f.isChecked())
				url = url.concat("&tempUnit=f");
			else //no need for c, radiogroup ensures it
				url = url.concat("&tempUnit=c");
				
			new RESTCallTask().execute(this, url);
		}
	}
	
	private void hideDynamicView() {
		RelativeLayout dynamicLayout = (RelativeLayout) findViewById(R.id.relativeLayout1);
		dynamicLayout.setVisibility(View.INVISIBLE);
		
		TableLayout dynamicTable = (TableLayout) findViewById(R.id.weatherDataTable);
		dynamicTable.setVisibility(View.INVISIBLE);
		
		TextView shareWeather1 = (TextView) findViewById(R.id.ShareCurrentTextView);
		shareWeather1.setVisibility(View.INVISIBLE);
		
		TextView shareWeather2 = (TextView) findViewById(R.id.ShareForecastTextView);
		shareWeather2.setVisibility(View.INVISIBLE);
	}
	
	private class RESTCallTask extends AsyncTask<Object, Integer, String> {
		private MainActivity context;
		
		protected String doInBackground(Object... params) {
			context = (MainActivity) params[0];
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet((String)params[1]);
			HttpResponse response;
			
			try
			{
				response = httpclient.execute(httpget);
				HttpEntity entity = response.getEntity();
				String result = EntityUtils.toString(entity);
				return result;
			}
			
			catch(Exception ex)
			{
				String message = ex.getMessage();
				return message;
			}
		}

		protected void onPostExecute(String json) {
			//check if json contains error message.
			JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
			String errorMessage = (String) jsonObj.get("error");
			
			if (errorMessage != null) {
				AlertDialog.Builder cannotFind = new AlertDialog.Builder(context);
				cannotFind
					.setTitle("Not Found")
					.setMessage("The location you queried does not exist!")
					.setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//do nothing
						}
					})
					.show();
				return;
			}
			
			WeatherData weatherData = new WeatherData(json);
			
			//update UI
			TextView cityText = (TextView) findViewById(R.id.cityTextView);
			cityText.setText(weatherData.city);
			
			TextView regionText = (TextView) findViewById(R.id.regionTextView);
			String region = weatherData.region;
			region = region.concat(", ").concat(weatherData.country);
			regionText.setText(region);
			
			(new DownloadImageTask()).execute(weatherData.imgURL);
			
			TextView weatherText = (TextView) findViewById(R.id.weatherTextView);
			weatherText.setText(weatherData.temperatureText);
			
			TextView temperature = (TextView) findViewById(R.id.temperatureTextView);
			String temp = weatherData.temperature;
			temp = temp.concat("\u00B0").concat(weatherData.temperatureType);
			temperature.setText(temp);
			
			int id = 1;
			TableLayout weatherDataTable = (TableLayout) findViewById(R.id.weatherDataTable);
			for (Iterator<Forecast> i = weatherData.forecasts.iterator(); i.hasNext(); ) {
				Forecast forecast = i.next();
				
				TextView dayTextView = (TextView) weatherDataTable.findViewById(id);
				dayTextView.setText(forecast.day);

				TextView descriptionTextView = (TextView) weatherDataTable.findViewById(id + 1);
				descriptionTextView.setText(forecast.text);
				
				TextView highTextView = (TextView) weatherDataTable.findViewById(id + 2);
				highTextView.setText(forecast.high.concat("\u00B0").concat(weatherData.temperatureType));
				
				TextView lowTextView = (TextView) weatherDataTable.findViewById(id + 3);
				lowTextView.setText(forecast.low.concat("\u00B0").concat(weatherData.temperatureType));
				
				id += 4;
			}
			
			context.weatherData = weatherData;
		}
		
		private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
			protected Bitmap doInBackground(String... params) {
				try {
					URL imageURL = new URL(params[0]);
					Bitmap imageData = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
					return imageData;
				}
				catch (Exception e) {
					System.out.println(e.getMessage());
				}
				return null;
			}
			
			protected void onPostExecute(Bitmap image) {
				ImageView weatherImage = (ImageView) findViewById(R.id.weatherImageView);
				weatherImage.setImageBitmap(image);
				showDynamicView();
			}
			
			private void showDynamicView() {
				RelativeLayout dynamicLayout = (RelativeLayout) findViewById(R.id.relativeLayout1);
				dynamicLayout.setVisibility(View.VISIBLE);
				
				TableLayout dynamicTable = (TableLayout) findViewById(R.id.weatherDataTable);
				dynamicTable.setVisibility(View.VISIBLE);
				
				TextView shareWeather1 = (TextView) findViewById(R.id.ShareCurrentTextView);
				shareWeather1.setVisibility(View.VISIBLE);
				
				TextView shareWeather2 = (TextView) findViewById(R.id.ShareForecastTextView);
				shareWeather2.setVisibility(View.VISIBLE);
			}
		
		}
	}

}



