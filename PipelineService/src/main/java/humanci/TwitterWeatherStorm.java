package humanci;

import java.util.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.IRichBolt;

import org.elasticsearch.node.*;
import org.elasticsearch.client.*;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.settings.ImmutableSettings;

import org.apache.storm.elasticsearch.bolt.EsIndexBolt;
import org.apache.storm.elasticsearch.common.EsConfig;
import org.apache.storm.elasticsearch.common.EsTupleMapper;
import org.apache.storm.elasticsearch.common.TwitterWeatherEsTupleMapper;

import java.util.concurrent.Future;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import redis.clients.jedis.Jedis;

@Service
public class TwitterWeatherStorm {

    private String consumerKey = "";
    private String consumerSecret = "";
    private String accessToken = "";
    private String accessTokenSecret = "";

    private final RestTemplate restTemplate;

    private Node node;
    private Client client;

    public TwitterWeatherStorm(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
	
        this.node = NodeBuilder.nodeBuilder()
				.clusterName("elasticsearch")
				.settings(ImmutableSettings.settingsBuilder().put("http.enabled", false))
				.data(false)
				.client(true)
				.node();

        this.client = this.node.client();
    }

    private boolean stopTracking(String indexName)
    {
	//get value from redis	for "indexName"
	Jedis jedis = new Jedis("localhost");
	if(null != jedis.get(indexName))
	{
		if("stop".equals(jedis.get(indexName)))
			return true;
		else
			return false;
	}
	return true;
    }

    public boolean indexExistsInRedis(String indexName)
    {
	Jedis jedis = new Jedis("localhost");
	if(null != jedis.get(indexName))
		return true;
	else
		return false;
    }

    public long indexCount(String indexName)
    {
	IndicesStatusResponse response = this.client.admin().indices()
						.prepareStatus(indexName).execute().actionGet();
        return response.getIndex(indexName).getDocs().getNumDocs();
    }

    public boolean indexExists(String indexName) throws Exception
    {
	boolean exists = this.client.admin().indices()
    				.prepareExists(indexName)
    				.execute().actionGet().isExists();

	return exists;
	
    }

    private boolean prepareIndex(String indexName) throws Exception
    {
	boolean exists = indexExists(indexName);

	if(!exists)
	{
        	CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);
       		CreateIndexResponse response = createIndexRequestBuilder.execute().actionGet();
        	if(response.isAcknowledged())
		{
        		PutMappingResponse putMappingResponse = this.client.admin().indices()
                        	                                .preparePutMapping(indexName)
                                               	        	.setType("tweet")
                                                        	.setSource(XContentFactory.jsonBuilder().prettyPrint()
                                                        	.startObject()
                                                        	.startObject("tweet")
                                                        	.startObject("properties")
                                                       		.startObject("coordinates").field("type", "geo_point").field("lat_lon", true).endObject()
                                                        	.endObject()
                                                        	.endObject()
                                                        	.endObject())
                                                        	.execute().actionGet();
			if(!putMappingResponse.isAcknowledged())
			{
				//this.node.close();
				return false;
			}
			else
			{
        			putMappingResponse = this.client.admin().indices()
                        	                                .preparePutMapping(indexName)
                                               	        	.setType("tweet")
                                                        	.setSource(XContentFactory.jsonBuilder().prettyPrint()
                                                        	.startObject()
                                                        	.startObject("tweet")
                                                        	.startObject("properties")
                                                       		.startObject("sentiment").field("type", "object")
								.startObject("properties")
								.startObject("Very Negative").field("type", "float").endObject()
								.startObject("Negative").field("type","float").endObject()
								.startObject("Neutral").field("type","float").endObject()
								.startObject("Positive").field("type","float").endObject()
								.startObject("Very Positive").field("type","float").endObject()
								.endObject()
								.endObject()
                                                        	.endObject()
                                                        	.endObject()
                                                        	.endObject())
                                                        	.execute().actionGet();
				if(!putMappingResponse.isAcknowledged())
				{
					//this.node.close();
					return false;
				}
				else
				{
        				putMappingResponse = this.client.admin().indices()
                        	        	                        .preparePutMapping(indexName)
                                               		        	.setType("tweet")
                                                        		.setSource(XContentFactory.jsonBuilder().prettyPrint()
                                                        		.startObject()
                                                        		.startObject("tweet")
                                                        		.startObject("properties")
                                                       			.startObject("relevance").field("type", "float")
									.endObject()
									.endObject()
									.endObject()
									.endObject())
									.execute().actionGet();
					if(!putMappingResponse.isAcknowledged())
					{
						return false;
					}
				}
			}
				
		}
		else
		{
			//this.node.close();
			return false;
		}
	}

        //this.node.close();
	return true;
	
    }

    @Async
    public Future<Boolean> trackTweets(String indexName, String keywords, String alertTypes, String endTime, String geoBounds) throws Exception{

	String delim = "[,]";
	String[] keyWords = keywords.split(delim); //parse comma separate string

	if(prepareIndex(indexName))
	{
   
		//set a flag in Redis for this index 
		Jedis jedis = new Jedis("localhost");
    		jedis.set(indexName,"start");

		Config config = new Config();
	
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("twitter-spout", new TwitterSampleSpout(consumerKey,
									 consumerSecret, accessToken, accessTokenSecret, keyWords));

		//ES 1.6
		EsConfig esConfig = new EsConfig("elasticsearch", new String[]{"localhost:9300"});
		EsTupleMapper tupleMapper = new TwitterWeatherEsTupleMapper(indexName);
		System.out.println("Created config, now create Elasticsearch bolt");
		EsIndexBolt indexBolt = new EsIndexBolt(esConfig, tupleMapper);
		System.out.println("Created index bolt");

                builder.setBolt("twitter-geolocator-bolt", new GeoLocatorBolt(geoBounds))
                        .shuffleGrouping("twitter-spout");

                builder.setBolt("twitter-stanford-sentiment-bolt", new StanfordSentimentAnalysisBolt())
                        .shuffleGrouping("twitter-geolocator-bolt");

                builder.setBolt("twitter-alert-classifier-bolt", new AlertTypeClassifierBolt(alertTypes))
                        .shuffleGrouping("twitter-stanford-sentiment-bolt");

                builder.setBolt("twitter-es-bolt",indexBolt).shuffleGrouping("twitter-alert-classifier-bolt");

		//builder.setBolt("twitter-es-bolt",indexBolt).shuffleGrouping("twitter-spout");

		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology(indexName, config,
				       builder.createTopology());

		System.out.println("Submitted topology");

		//run until endTime
		//more complex: allow users to stop the topology
		//how will this thread behave in that case ?

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); 
		LocalDateTime endDateTime = LocalDateTime.parse(endTime, formatter);
		LocalDateTime currentDateTime = LocalDateTime.now();

		while(!stopTracking(indexName) && endDateTime.isAfter(currentDateTime))
		{
		     System.out.println("Still tracking...");
		     Thread.sleep(1000);
		     currentDateTime = LocalDateTime.now();		     
		}
	        if(stopTracking(indexName) || endDateTime.isBefore(currentDateTime))	
		{
		     System.out.println("shutdown cluster");
		     cluster.shutdown();
		}

		return new AsyncResult<>(Boolean.TRUE);
    	}
	else
	{
		System.out.println("Error creating or retrieving index");
		return new AsyncResult<>(Boolean.FALSE);
	}
    }
}
