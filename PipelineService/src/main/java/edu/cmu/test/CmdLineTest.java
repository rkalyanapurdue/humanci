package edu.cmu.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.io.*;
import java.nio.charset.Charset;

import edu.cmu.geolocator.GlobalParam;
import edu.cmu.geolocator.coder.CoderFactory;
import edu.cmu.geolocator.model.CandidateAndFeature;
import edu.cmu.geolocator.model.LocEntityAnnotation;
import edu.cmu.geolocator.model.Tweet;
import edu.cmu.geolocator.parser.ParserFactory;

public class CmdLineTest {

  public static void main(String argv[]) throws Exception {
    GlobalParam.setGazIndex("/Users/rkalyana/RCAC/Humanitarian_CI/geolocator/geolocator-2.0/geo-locator/GazIndex");
    String fileName = "raw_tweet.txt";
    InputStream fis = new FileInputStream(fileName );
    BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
    String s = null;
    s = br.readLine();
    br.close();
    br = null;
    fis = null;
    /*InputStreamReader isr = new InputStreamReader(System.in);
    BufferedReader br = new BufferedReader(isr);
    while ((s = br.readLine()) != null) {*/
      if(!s.isEmpty())
      {
	//System.out.println("You typed : "+s);
        Tweet tweet = new Tweet(s);
        List<LocEntityAnnotation> topos = ParserFactory.getEnAggrParser().parse(tweet);               
        List<CandidateAndFeature> resolved = CoderFactory.getMLGeoCoder().resolve(tweet, "debug");
        for (LocEntityAnnotation topo : topos) {
          System.out.println(topo.getTokenString() + " " + topo.getNEType());
        }
        if((null!= resolved) && !resolved.isEmpty())
        {
          for (CandidateAndFeature code : resolved) {
             System.out.println(code.getAsciiName() + "" + code.getCountry() + "" + code.getLatitude()
                     + "" + code.getLongitude());
          }
        }
        else
        {
	  System.out.println("Could not geolocate tweet");
        }
      }
      else
      {
        System.exit(0);
      }
    //}
  }

}
