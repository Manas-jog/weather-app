

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.json.simple.*;

/**
 * Servlet implementation class YWeather
 */
//@WebServlet("/YWeather")
public class YWeather extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String location = URLEncoder.encode(request.getParameter("location"), "UTF-8");
		String type = request.getParameter("type");
		String tempUnit = request.getParameter("tempUnit");
		
		response.setContentType("text/json");
		PrintWriter out = response.getWriter();

		String baseURI = "http://default-environment-qn4k3x5ktr.elasticbeanstalk.com/";
		String uriParameters = ("?location=").concat(location);
		uriParameters = uriParameters.concat(("&type=").concat(type));
		uriParameters = uriParameters.concat(("&tempUnit=").concat(tempUnit));
		
		URL weatherXML = new URL(baseURI.concat(uriParameters));
		URLConnection connection = weatherXML.openConnection();
		connection.setAllowUserInteraction(false);
		InputStream weatherStream = weatherXML.openStream();
		
		SAXBuilder xmlBuilder = new SAXBuilder();
		
		try
		{
			Document xmlDoc = xmlBuilder.build(weatherStream);
			
			Element weather = xmlDoc.getRootElement();
			List weatherInformation = weather.getChildren();
			
			if (weatherInformation.isEmpty())
			{
				JSONObject errorJSON = new JSONObject();
				errorJSON.put("error", "Zero results found!");
				out.println(errorJSON.toJSONString());
				return;
			}
			
			Element feedXML = (Element)weatherInformation.get(0);
			String feed = feedXML.getText();
			
			Element linkXML = (Element)weatherInformation.get(1);
			String link = linkXML.getText();
			
			Element locationXML = (Element)weatherInformation.get(2);
			String city = locationXML.getAttributeValue("city");
			String region = locationXML.getAttributeValue("region");
			String country = locationXML.getAttributeValue("country");
			
			Element unitsXML = (Element)weatherInformation.get(3);
			String temperatureUnit = unitsXML.getAttributeValue("temperature");
			
			Element conditionXML = (Element)weatherInformation.get(4);
			String temperatureText = conditionXML.getAttributeValue("text");
			String temperature = conditionXML.getAttributeValue("temp");
			
			Element imageXML = (Element)weatherInformation.get(5);
			String imageURL = imageXML.getText();
			
			ArrayList fday = new ArrayList();
			ArrayList flow = new ArrayList();
			ArrayList fhigh = new ArrayList();
			ArrayList ftext = new ArrayList();
			
			
			for(int i=6; i<11; i++)
			{
				Element forecastXML = (Element)weatherInformation.get(i);
				fday.add(forecastXML.getAttributeValue("day"));
				flow.add(forecastXML.getAttributeValue("low"));
				fhigh.add(forecastXML.getAttributeValue("high"));
				ftext.add(forecastXML.getAttributeValue("text"));
			}

			//now to create json
			
			JSONArray forecastListJSON = new JSONArray();
			
			for(int i=0; i<5; i++)
			{
				JSONObject forecastJSON = new JSONObject();
				forecastJSON.put("text", ftext.get(i));
				forecastJSON.put("high", fhigh.get(i));
				forecastJSON.put("day", fday.get(i));
				forecastJSON.put("low", flow.get(i));
				forecastListJSON.add(forecastJSON);
			}
			
			JSONObject conditionJSON = new JSONObject();
			conditionJSON.put("text", temperatureText);
			conditionJSON.put("temp", temperature);
			
			JSONObject locationJSON = new JSONObject();
			locationJSON.put("region", region);
			locationJSON.put("country", country);
			locationJSON.put("city", city);
			
			JSONObject unitsJSON = new JSONObject();
			unitsJSON.put("temperature", temperatureUnit);
			
			//final weather json object
			JSONObject weatherJSON = new JSONObject();
			weatherJSON.put("forecast", forecastListJSON);
			weatherJSON.put("condition", conditionJSON);
			weatherJSON.put("location", locationJSON);
			weatherJSON.put("link", link);
			weatherJSON.put("img", imageURL);
			weatherJSON.put("feed", feed);
			weatherJSON.put("units", unitsJSON);
			
			//write json object to output
			String output = weatherJSON.toString();
			String correctedOutput = output.replaceAll("\\\\", "");
			out.println(correctedOutput);
		}
		
		catch(Exception ex)
		{
			JSONObject exceptionJSON = new JSONObject();
			exceptionJSON.put("error", "exception");
			exceptionJSON.put("message", ex.getMessage());
			out.println(exceptionJSON.toJSONString());
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
