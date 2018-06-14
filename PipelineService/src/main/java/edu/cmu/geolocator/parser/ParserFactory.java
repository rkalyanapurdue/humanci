package edu.cmu.geolocator.parser;

import edu.cmu.geolocator.parser.Universal.MTNERParser;
import edu.cmu.geolocator.parser.english.EnglishParser;
import edu.cmu.geolocator.parser.english.EnglishStanfordNERParser;
import edu.cmu.geolocator.parser.english.EnglishRuleSTBDParser;
import edu.cmu.geolocator.parser.english.EnglishRuleToponymParser;
import edu.cmu.geolocator.parser.spanish.SpanishParser;
import edu.cmu.geolocator.parser.spanish.SpanishRuleSTBDParser;
import edu.cmu.geolocator.parser.spanish.SpanishRuleToponymParser;

public class ParserFactory {
  private static EnglishParser enparser;
  private static SpanishParser esparser;
  public static EnglishParser getEnAggrParser(){
    if(enparser==null)
      enparser = new EnglishParser(false);
    return enparser;
  }
  public static SpanishParser getEsAggrParser(){
    if(esparser==null)
      esparser = new SpanishParser(false);
    return esparser;
  }
  public static MTNERParser getEnNERParser() {
      try {
	System.out.println("Returning MT NER parser");
        return MTNERParser.getInstance("en");
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return null;
  }

  /*public static EnglishStanfordNERParser getEnNERParser() {
      try {
	System.out.println("Returning Stanford NER parser");
        return EnglishStanfordNERParser.getInstance();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return null;
  }*/

  public static EnglishRuleSTBDParser getEnSTBDParser() {
      return EnglishRuleSTBDParser.getInstance();
  }
  
  public static EnglishRuleToponymParser getEnToponymParser() {
      return EnglishRuleToponymParser.getInstance();
  }

  public static MTNERParser getEsNERParser() {
    try {
      return MTNERParser.getInstance("es");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
}

public static SpanishRuleSTBDParser getEsSTBDParser() {
    return SpanishRuleSTBDParser.getInstance();
}

public static SpanishRuleToponymParser getEsToponymParser() {
    return SpanishRuleToponymParser.getInstance();
}

}
