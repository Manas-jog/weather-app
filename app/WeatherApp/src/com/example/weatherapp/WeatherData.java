package com.example.weatherapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class WeatherData {
	public String imgURL;
	public String temperatureType;
	public String feed;
	public String link;
	public String country;
	public String region;
	public String city;
	public String temperatureText;
	public String temperature;
	
	public List<Forecast> forecasts;
	
	public WeatherData(String json) {
		JSONObject data = (JSONObject) JSONValue.parse(json);
		
		imgURL = (String) data.get("img");
		
		JSONObject units = (JSONObject) data.get("units"); 
		temperatureType = (String) units.get("temperature");
		
		feed = (String) data.get("feed");
		
		link = (String) data.get("link");
		
		JSONArray forecastsJSON = (JSONArray) data.get("forecast");
		forecasts = new ArrayList<Forecast>();
		
		for(Iterator<Object> i = forecastsJSON.iterator(); i.hasNext();  ) {
			JSONObject forecastJSON = (JSONObject) i.next();
			Forecast forecast = new Forecast();
			forecast.high = (String) forecastJSON.get("high");
			forecast.low = (String) forecastJSON.get("low");
			forecast.day = (String) forecastJSON.get("day");
			forecast.text = (String) forecastJSON.get("text");
			forecasts.add(forecast);
		}
		
		JSONObject locationJSON = (JSONObject) data.get("location");
		country = (String) locationJSON.get("country");
		region = (String) locationJSON.get("region");
		city = (String) locationJSON.get("city");
		
		JSONObject conditionJSON = (JSONObject) data.get("condition");
		temperatureText = (String) conditionJSON.get("text");
		temperature = (String) conditionJSON.get("temp");
		 
	}
	
	static public class Forecast {
		public String high;
		public String day;
		public String text;
		public String low;
		
		public void display() {
			System.out.println(high);
			System.out.println(low);
			System.out.println(text);
			System.out.println(day);
		}
	}	
	
}