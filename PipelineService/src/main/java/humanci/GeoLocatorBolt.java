package humanci;

import java.util.HashMap;
import java.util.Map;

import twitter4j.*;
import twitter4j.conf.*;

import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.charset.Charset;
import java.lang.Number;

import edu.cmu.geolocator.GlobalParam;
import edu.cmu.geolocator.coder.CoderFactory;
import edu.cmu.geolocator.model.CandidateAndFeature;
import edu.cmu.geolocator.model.LocEntityAnnotation;
import edu.cmu.geolocator.model.Tweet;
import edu.cmu.geolocator.parser.ParserFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class GeoLocatorBolt implements IRichBolt {
    private OutputCollector collector; 
    private Double latMin, latMax, lonMin, lonMax;

    public GeoLocatorBolt(String inGeoBounds)
    {
	//parse inGeoBounds to extract min,max lat, lon	
	String delims = "[,]";
	String tokens[] = inGeoBounds.split(delims);
	latMin = new Double(tokens[0]);
	lonMin = new Double(tokens[1]);
	latMax = new Double(tokens[2]);
	lonMax = new Double(tokens[3]);
    }

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
       this.collector = collector;
   }

   private boolean withinBounds(Double latitude, Double longitude)
   {
	if((latMin <= latitude) && (latitude <= latMax) && (lonMin <= longitude) && (longitude <= lonMax))
		return true;
	else
		return false;
   }

   @Override
   public void execute(Tuple tuple) {
       Status tweet_s = (Status) tuple.getValueByField("tweet");
       if(null != tweet_s.getGeoLocation())
	{
		if(this.withinBounds(new Double(tweet_s.getGeoLocation().getLatitude()), new Double(tweet_s.getGeoLocation().getLongitude())))
		{
			Gson gson1 = new Gson();
			JsonObject statusJson1 = ((JsonObject) ((JsonElement) gson1.toJsonTree(tweet_s)).getAsJsonObject());
			statusJson1.remove("coordinates");
			JsonArray latlonarr1 = new JsonArray();
			latlonarr1.add((Number)new Double(tweet_s.getGeoLocation().getLongitude()));
			latlonarr1.add((Number)new Double(tweet_s.getGeoLocation().getLatitude()));
			statusJson1.add("coordinates",latlonarr1); 
			String statusJsonStr1 = statusJson1.toString();
			List<Object> out1 = new ArrayList<Object>();
			//out1.add(statusJsonStr1);
			out1.add(statusJson1);
			this.collector.emit(out1);
		}
	}
	else
	{
       		GlobalParam.setGazIndex("path to GazIndex");
       		GlobalParam.setGeoNames("path to GeoNames");
       		String s = tweet_s.getText();
       		Tweet tweet = new Tweet(tweet_s);
       		try
       		{
	    		List<LocEntityAnnotation> topos = ParserFactory.getEnAggrParser().parse(tweet);
	    		List<CandidateAndFeature> resolved = CoderFactory.getMLGeoCoder().resolve(tweet, "debug");
	    		if((null!= resolved) && !resolved.isEmpty())
	        	{
	    	   		for (CandidateAndFeature code : resolved) {
				    if(this.withinBounds(new Double(code.getLatitude()), new Double(code.getLongitude())))
				    {
				    	String latlon = (new Double(code.getLatitude())).toString()+","+(new Double(code.getLongitude())).toString();
				    	Gson gson = new Gson();
				    	JsonObject statusJson = ((JsonObject) ((JsonElement) gson.toJsonTree(tweet_s)).getAsJsonObject());
				    	String statusJsonStr = statusJson.toString();
				    	statusJson.remove("coordinates");
				    	JsonArray latlonarr = new JsonArray();
				    	latlonarr.add((Number)new Double(code.getLongitude()));
				    	latlonarr.add((Number)new Double(code.getLatitude()));
				    	statusJson.add("coordinates",latlonarr);
				    	statusJsonStr = statusJson.toString();
				    	List<Object> out = new ArrayList<Object>();
				    	//out.add(statusJsonStr);
				    	out.add(statusJson);
				    	this.collector.emit(out);
				    }
	    	   		}
			}
       		}
		catch (Exception e) { e.printStackTrace();}
	}
   }

   @Override
   public void cleanup() {


   }

   @Override
   public void declareOutputFields(OutputFieldsDeclarer declarer) {
       declarer.declare(new Fields("tweet"));
   }
    
   @Override
   public Map<String, Object> getComponentConfiguration() {
       return null;
   }
    
}

