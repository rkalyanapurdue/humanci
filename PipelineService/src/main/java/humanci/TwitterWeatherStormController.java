package humanci;

import java.util.concurrent.Future;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;

@RestController
public class TwitterWeatherStormController {

    private final AtomicLong counter = new AtomicLong();
    private static final String indexTemplate = "index100%d";

    private final TwitterWeatherStorm tws;

    public TwitterWeatherStormController(TwitterWeatherStorm tws) {
	this.tws = tws;
    }

    @CrossOrigin()
    @RequestMapping("/tracktweets")
    public String twitterWeatherStorm(@RequestParam(value="keywords") String keywords,
				      @RequestParam(value="alertTypes") String alertTypes,
			 	      @RequestParam(value="endTime") String endTime,
				      @RequestParam(value="geoBounds") String geoBounds) throws Exception {
	String indexName = String.format(indexTemplate,counter.incrementAndGet());

	//we could also use indexExists which looks for an actual index in Elasticsearch
	//but it is too slow, requiring a node to be created each time
	//currently trying to create a node once for each tws object
        while(tws.indexExists(indexName))
        {
		indexName = String.format(indexTemplate,counter.incrementAndGet());
        }

	System.out.println("================ About to start tracking tweets =================");
	Future<Boolean> ret = tws.trackTweets(indexName, keywords, alertTypes, endTime, geoBounds);
	System.out.println("================ Return immediately =================");
	return indexName;
    }

    @CrossOrigin()
    @RequestMapping("/stoptracking")
    public String stopTracking(@RequestParam(value="index") String index) {
    
	Jedis jedis = new Jedis("localhost");
	jedis.set(index,"stop");
	long count = tws.indexCount(index);
	System.out.println("================ About to stop tracking tweets =================");
   	System.out.println("Returning "+String.valueOf(count));
	System.out.println("================ Done =================");
			
	return String.valueOf(count);

    }
	
}
