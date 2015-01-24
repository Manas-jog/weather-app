<?php

class Places
{
	private $xml;

	public function __construct($xml)
	{
		$this->xml = $xml;
	}

	public function getNumberOfPlaces()
	{
		return $this->xml->count();
	}

	public function getAllWoeids()
	{
		$woeids = array();
		if ($this->xml->getName() === "concordance")
			$woeids[] = (string)$this->xml->woeid;
		else
		{
			foreach ($this->xml->children() as $child) 
			{
				if ($child->getName() === "place")
					$woeids[] = $child->woeid;
			}
		}

		return $woeids;
	}

}


class YWeather
{
	private $xml;
	private $ns = 'http://xml.weather.yahoo.com/ns/rss/1.0';
	private $geo = 'http://www.w3.org/2003/01/geo/wgs84_pos#';

	public function __construct($xml)
	{
		$this->xml = $xml;
	}

	public function isValid()
	{
		if ((string)$this->xml->channel->item->title === "City not found")
			return false;
		return true;
	}

	public function getWeatherIcon()
	{
		$weatherDescription = (string)$this->xml->channel->item->description;
		$matches = null;
	    preg_match('/<\s*img\s*src\s*=\s*\"(.+?)\"/', $weatherDescription, $matches);
		$final = str_replace('<img src="', "", $matches[0]);
		$final = trim($final, '"');
		return $final;
	}

	public function getTemperatureCondition()
	{
		$item = $this->xml->channel->item;
		$condition = $this->getChildByNS($item, $this->ns, "condition");
		return $this->formatValue($this->getAttributeValue($condition, "text"));
	}
	
	public function getTemperature()
	{
		$item = $this->xml->channel->item;
		$condition = $this->getChildByNS($item, $this->ns, "condition");
		return $this->formatValue($this->getAttributeValue($condition, "temp"));	
	}

	public function getTemperatureUnit()
	{
		$units = $this->getChildByNS($this->xml->channel, $this->ns, "units");
		return $this->formatValue($this->getAttributeValue($units, "temperature"));
	}

	public function getCity()
	{
		$location = $this->getChildByNS($this->xml->channel, $this->ns, "location");
		return $this->formatValue($this->getAttributeValue($location, "city"));
	}

	public function getRegion()
	{
		$location = $this->getChildByNS($this->xml->channel, $this->ns, "location");
		return $this->formatValue($this->getAttributeValue($location, "region"));
	}

	public function getCountry()
	{
		$location = $this->getChildByNS($this->xml->channel, $this->ns, "location");
		return $this->formatValue($this->getAttributeValue($location, "country"));
	}	

	public function getLatitude()
	{
		$item = $this->xml->channel->item;
		$temp = (string)$this->getChildByNS($item, $this->geo, "lat");
		return $this->formatValue($temp);
	}

	public function getLongitude()
	{
		$item = $this->xml->channel->item;
		$temp = (string)$this->getChildByNS($item, $this->geo, "long");
		return $this->formatValue($temp);
	}

	public function getLinkDetails()
	{
		return $this->xml->channel->link;
	}

	public function getForecast()
	{		//return array
		$forecast = array();

		foreach ($this->xml->channel->item->children($this->ns) as $child) 
		{
			if ($child->getName() === 'forecast')
				$forecast[] = $child;
		}

		$formattedForecast = array();

		foreach ($forecast as $element) {

				$formattedForecast[] = [
					"day" => $this->getAttributeValue($element, 'day'),
					"low" => $this->getAttributeValue($element, 'low'),
					"high" => $this->getAttributeValue($element, 'high'),
					"text" => $this->getAttributeValue($element, 'text'),
				];
		}

		return $formattedForecast;
	}

	private function formatValue($value)
	{
		if (!$value || $value == "")
			return null;
		else
			return $value;
	}

	private function getChildByNS($element, $namespace, $nodeName)
	{
		foreach ($element->children($namespace) as $child) 
		{
			if ($child->getName() === $nodeName)
				return $child;
		}

		return null;
	}	

	private function getAttributeValue($element, $attrName)
	{
		foreach ($element->attributes() as $key => $value) 
		{
			if ($key === $attrName)
				return $value;
		}
	}
}



class WeatherXML
{
	private $icon;
	private $temperatureCondition;
	private $temperature;
	private $temperatureUnit;
	private $city;
	private $region;
	private $country;
	private $latitude;
	private $longitude;
	private $linkDetails;
	private $url;
	private $imageURL;
	private $valid;
	private $forecastList;

	public function __construct($yWeather, $url)
	{
		$this->valid = $yWeather->isValid();

		if ($this->valid)
		{
			$this->icon = $yWeather->getWeatherIcon();
			$this->temperatureCondition = $yWeather->getTemperatureCondition();
			$this->temperature = $yWeather->getTemperature();
			$this->temperatureUnit = $yWeather->getTemperatureUnit();
			$this->city = $yWeather->getCity();
			$this->region = $yWeather->getRegion();
			$this->country = $yWeather->getCountry();
			$this->latitude = $yWeather->getLatitude();
			$this->longitude = $yWeather->getLongitude();
			$this->linkDetails = $yWeather->getLinkDetails();
			$this->forecastList = $yWeather->getForecast();
			$this->url = str_replace('&', '&amp;', $url);
		}
	}
	
	public function getXML()
	{
		$xml = "<weather>";

		if ($this->valid)
		{
			$xml = $xml . 
				'<feed>' . $this->url . '</feed>' .
				'<link>' . $this->linkDetails . '</link>' .
				'<location city="' . $this->city . '" region="' . $this->region . '" country="' . $this->country . '" />' .
				'<units temperature="' . $this->temperatureUnit . '"/>' .
				'<condition text="' . $this->temperatureCondition . '" temp="' . $this->temperature . '"/>' .
				'<img>' . $this->icon . '</img>';

			foreach($this->forecastList as $forecast)
			{
				$xml = $xml . '<forecast day="' . $forecast["day"] . '" low="' . $forecast["low"] . '" high="' . $forecast["high"] . '" text="' . $forecast["text"] . '"/>';
			}

			$xml = $xml . '</weather>';
		}

		return $xml;
	}

	public function canDisplay()
	{
		return $this->valid;
	}

}

if (isset($_GET["location"]) && isset($_GET["type"]) && isset($_GET["tempUnit"]))
{
	$base_uri = "http://where.yahooapis.com/v1/";

	if($_GET["type"] === "city")
	{
		$request_uri = 'places$and' . "(.q('" . urlencode($_GET["location"]) . "'),.type(7));start=0;count=5?appid=dj0yJmk9RVYxYTZHR1VNTUZNJmQ9WVdrOVVXUkZZVzVFTm5FbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1kNQ--";
	}

	else if($_GET["type"] == "zip")
	{
		$request_uri = "concordance/usps/" . $_GET["location"] . "?appid=dj0yJmk9RVYxYTZHR1VNTUZNJmQ9WVdrOVVXUkZZVzVFTm5FbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1kNQ";
	}

	$data = @simplexml_load_file($base_uri . $request_uri) or die('<weather></weather>');
	$places = new Places($data);

	//Set up base request URL for weather
	$base_uri = "http://weather.yahooapis.com/forecastrss?w=";
	$requestTemperatureUnit = $_GET["tempUnit"];

	$woeid = $places->getAllWoeids()[0];
	$request_uri = $woeid . "&u=" . $requestTemperatureUnit;
	$weather = simplexml_load_file($base_uri . $request_uri);		
	$yWeather = new YWeather($weather);
	$weatherXML = new WeatherXML($yWeather, $base_uri . $request_uri);

	if ($weatherXML->canDisplay())
		echo $weatherXML->getXML();

	else
	{
		echo '<weather></weather>';
	}
}
