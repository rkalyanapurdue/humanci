package org.apache.storm.elasticsearch.common;

import org.apache.storm.tuple.ITuple;
import twitter4j.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TwitterWeatherEsTupleMapper implements EsTupleMapper {

    private String myIndexName;

    public TwitterWeatherEsTupleMapper(String indexName)
    {
	myIndexName = indexName;
    }

    @Override
    public String getSource(ITuple tuple) {
	try
	{
                /*Status tweet_s = (Status) tuple.getValue(0);
                Gson gson = new Gson();
                String statusJson = gson.toJson(tweet_s);
                return statusJson;*/
		return (String) tuple.getValue(0);
	}
	catch(Exception e)
	{
		e.printStackTrace();
		return null;
	}
    }

    @Override
    public String getIndex(ITuple tuple) {
        return myIndexName;
    }

    @Override
    public String getType(ITuple tuple) {
        return "tweet";
    }

    @Override
    public String getId(ITuple tuple) {
	try {
        /*Status tweet_s = (Status) tuple.getValue(0);
        return (new Long(tweet_s.getId())).toString();*/

	JsonParser parser = new JsonParser();
	JsonObject statusobj = (JsonObject)parser.parse((String) tuple.getValue(0));
	JsonElement idele = statusobj.get("id");
	return (new Long(idele.getAsLong())).toString();
	}
	catch(Exception e)
	{
		e.printStackTrace();
		return null;
	}
    }
}
