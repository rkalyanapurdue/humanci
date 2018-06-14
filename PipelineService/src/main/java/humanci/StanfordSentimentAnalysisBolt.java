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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

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

import org.ejml.simple.SimpleMatrix;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;

public class StanfordSentimentAnalysisBolt implements IRichBolt {

  private OutputCollector collector;

  private static final NumberFormat NF = new DecimalFormat("0.0000");

  private static final String[] sentimentTypes = {"Very Negative", "Negative", "Neutral", "Positive", "Very Positive"};

  public StanfordSentimentAnalysisBolt() {} // static methods

   @Override
   public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
       this.collector = collector;
   }


  /**
   * Sets the labels on the tree to be the indices of the nodes.
   * Starts counting at the root and does a postorder traversal.
   */
  static int setIndexLabels(Tree tree, int index) {
    if (tree.isLeaf()) {
      return index;
    }

    tree.label().setValue(Integer.toString(index));
    index++;
    for (Tree child : tree.children()) {
      index = setIndexLabels(child, index);
    }
    return index;
  }

  /**
   * Outputs the scores from the tree.  Counts the tree nodes the
   * same as setIndexLabels.
   */
  static int outputTreeRootScores(PrintStream out, Tree tree) {
    
    int index = 0;

    if (tree.isLeaf()) {
      return index;
    }

    SimpleMatrix vector = RNNCoreAnnotations.getPredictions(tree);
    for (int i = 0; i < vector.getNumElements(); ++i) {
      out.print("  " + NF.format(vector.get(i)));
    }
    return index;
  }

  /**
   * Outputs a tree as probabilities at the root node for the 5 sentiment categories
   */
  static void outputTree(PrintStream out, CoreMap sentence) {
    Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
    Tree copy = tree.deepCopy();
    setIndexLabels(copy, 0);
    outputTreeRootScores(out, tree);
  }

  static final String DEFAULT_TLPP_CLASS = "edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams";

  @Override
  public void execute(Tuple tuple) {

   JsonObject tweetJson = (JsonObject) tuple.getValueByField("tweet");

   String text = ((JsonElement) tweetJson.get("text")).getAsString();

   String parserModel = null;
   String sentimentModel = null;

   boolean filterUnknown = false;

   String tlppClass = DEFAULT_TLPP_CLASS;

   // We construct two pipelines.  One handles tokenization, if
   // necessary.  The other takes tokenized sentences and converts
   // them to sentiment trees.
   Properties pipelineProps = new Properties();
   Properties tokenizerProps = null;
   
   pipelineProps.setProperty("annotators", "parse, sentiment");
   pipelineProps.setProperty("enforceRequirements", "false");
   tokenizerProps = new Properties();
   tokenizerProps.setProperty("annotators", "tokenize, ssplit");
   tokenizerProps.setProperty("ssplit.eolonly", "true");

   StanfordCoreNLP tokenizer = (tokenizerProps == null) ? null : new StanfordCoreNLP(tokenizerProps);
   StanfordCoreNLP pipeline = new StanfordCoreNLP(pipelineProps);

   String tweetJsonStr = tweetJson.toString();
   List<Object> out = new ArrayList<Object>();
   out.add(tweetJsonStr);

   boolean emitted = false;

   text = text.trim();
   if (text.length() > 0) {
     Annotation annotation = tokenizer.process(text);
     pipeline.annotate(annotation);
     for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) 
     {
       //set sentiment scores in JsonObject
       Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
       Tree copy = tree.deepCopy();
       setIndexLabels(copy, 0);
       if (!tree.isLeaf()) 
	{
    	  SimpleMatrix vector = RNNCoreAnnotations.getPredictions(tree);
	  if(vector.getNumElements() > 0)
	  {
	    JsonObject sentimentJson = new JsonObject();
    	    for (int i = 0; i < vector.getNumElements(); ++i) 
	    {
	        sentimentJson.addProperty(sentimentTypes[i],new Float(NF.format(vector.get(i))));
	    }
	    tweetJson.add("sentiment",sentimentJson);
	    emitted = true;
	    String tweetJsonStr1 = tweetJson.toString();
	    List<Object> out1 = new ArrayList<Object>();
	    out1.add(tweetJsonStr1);
            this.collector.emit(out1);
          }  
        }
     }
   }

   if(!emitted)
   {
     //couldn't determine sentiment, so just emit original JsonObject
     this.collector.emit(out);
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
