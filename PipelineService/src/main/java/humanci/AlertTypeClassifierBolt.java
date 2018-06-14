package humanci;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import twitter4j.*;
import twitter4j.conf.*;

import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import java.util.Iterator;
import java.util.logging.*;
import java.util.regex.*;
import java.io.*;
import java.nio.charset.Charset;

import cc.mallet.classify.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.util.*;

public class AlertTypeClassifierBolt implements IRichBolt {

   private OutputCollector collector;

   private String classifierFile = "/mallet/maxent.classifier";

   private Map<String, Integer> alertTypeMap;

   private String[] alertKinds;

   public AlertTypeClassifierBolt(String alertTypes)
   {
        String delim = "[,]";
        this.alertKinds = alertTypes.split(delim);

	this.alertTypeMap = new HashMap<String, Integer>();
	this.alertTypeMap.put("Special Weather Statement",new Integer(0));
	this.alertTypeMap.put("Red Flag Warning",new Integer(1));
	this.alertTypeMap.put("Beach Hazards Statement",new Integer(2));
	this.alertTypeMap.put("Flash Flood Watch",new Integer(3));
	this.alertTypeMap.put("Flood Warning",new Integer(4));
	this.alertTypeMap.put("Coastal Flood Warning",new Integer(5));
	this.alertTypeMap.put("Coastal Flood Advisory",new Integer(6));
	this.alertTypeMap.put("Rip Current Statement",new Integer(7));
	this.alertTypeMap.put("Hurricane Warning",new Integer(8));
	this.alertTypeMap.put("Hurricane Local Statement",new Integer(9));
	this.alertTypeMap.put("Tropical Storm Warning",new Integer(10));
	this.alertTypeMap.put("Wind Advisory",new Integer(11));
	this.alertTypeMap.put("High Surf Advisory",new Integer(12));
	this.alertTypeMap.put("Flood Watch",new Integer(13));
	this.alertTypeMap.put("Flood Advisory",new Integer(14));
	this.alertTypeMap.put("Hurricane Watch",new Integer(15));
	this.alertTypeMap.put("Flash Flood Warning",new Integer(16));
	this.alertTypeMap.put("Tornado Watch",new Integer(17));
	this.alertTypeMap.put("High Wind Warning",new Integer(18));
	this.alertTypeMap.put("Coastal Flood Watch",new Integer(19));
	this.alertTypeMap.put("Air Quality Alert",new Integer(20));
	this.alertTypeMap.put("High Wind Watch",new Integer(21));
	this.alertTypeMap.put("Freeze Watch",new Integer(22));
	this.alertTypeMap.put("Frost Advisory",new Integer(23));
	this.alertTypeMap.put("Freeze Warning",new Integer(24));
	this.alertTypeMap.put("Hard Freeze Warning",new Integer(25));
	this.alertTypeMap.put("Winter Weather Advisory",new Integer(26));
	this.alertTypeMap.put("Fire Weather Watch",new Integer(27));
	this.alertTypeMap.put("Hydrologic Outlook",new Integer(28));
	this.alertTypeMap.put("Lake Wind Advisory",new Integer(29));
	this.alertTypeMap.put("High Surf Warning",new Integer(30));
   }

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
       this.collector = collector;
   }
  
   @Override
   public void execute(Tuple tuple) {

	JsonParser parser = new JsonParser();
   	JsonObject tweetJson = (JsonObject)parser.parse((String) tuple.getValue(0));
   	String text = ((JsonElement) tweetJson.get("text")).getAsString();

	// Read classifier from file
	Classifier classifier = null;

	try {

		//Get file from resources folder
		Resource classifierResc = new ClassPathResource(classifierFile);
	
		ObjectInputStream ois =
			new ObjectInputStream (classifierResc.getInputStream());
		
		classifier = (Classifier) ois.readObject();
		ois.close();
	} catch (Exception e) {
		throw new IllegalArgumentException("Problem loading classifier from file " + classifierFile +
						   ": " + e.getMessage());
	}
		
	String tweets[] = {text};

	Iterator<Instance> stringArrIterator = new StringArrayIterator(tweets);
	Iterator <Instance> iterator = classifier.getInstancePipe().newIteratorFrom(stringArrIterator);
	
	classifier.getInstancePipe().getDataAlphabet().stopGrowth();
	classifier.getInstancePipe().getTargetAlphabet().stopGrowth();

	Instance instance = iterator.next();
		
	Labeling labeling = classifier.classify(instance).getLabeling();

	double score = 0.0;
	int indx;

	for(int i=0; i<alertKinds.length; i++)
	{
		if(alertTypeMap.containsKey(alertKinds[i]))
		{
	    		indx = ((Integer)alertTypeMap.get(alertKinds[i])).intValue();
			score += labeling.valueAtLocation(indx);
		}
	}
	score = score/alertKinds.length;

	tweetJson.addProperty("relevance",new Float(score));

        String tweetJsonStr = tweetJson.toString();
        List<Object> out = new ArrayList<Object>();
        out.add(tweetJsonStr);
        this.collector.emit(out);

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
