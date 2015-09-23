/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.parsers;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import main.java.spatialrelex.Main;
import main.java.spatialrelex.ling.ParseTree;
import main.java.spatialrelex.ling.SENNASrl;
import main.java.spatialrelex.markup.Doc;

/**
 *
 * @author Jenny D'Souza
 */
public class StanfordParser {
    
    static StanfordCoreNLP pipeline;
    
    /**
     * Initializes the Stanford CoreNLP parser with input annotator properties
     * as a string of the desired annotator flags. 
     * For e.g., "tokenize, ssplit, pos, lemma, ner, parse". 
     */
    public static void initializeStanfordParser() {
        // Create a CoreNLP pipeline.
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
        pipeline = new StanfordCoreNLP(props);        
    }        
    
    /** 
     * Parses a given input text document using the Stanford CoreNLP parser.
     * 
     * @param document
     * @throws java.io.UnsupportedEncodingException 
     * @throws java.lang.InterruptedException 
     */
    public static void parse(Doc document) throws UnsupportedEncodingException, IOException, InterruptedException {

        // Initialize an Annotation with some text to be annotated. The text is the argument to the constructor.
        Annotation annotation = new Annotation(new String(document.text.getBytes("UTF-8"), "UTF-8"));
        // run all the selected Annotators on this text
        pipeline.annotate(annotation);

        // An Annotation is a Map and you can get and use the various analyses individually.
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        
        //returns if the annotation is empty.
        if (sentences == null || sentences.isEmpty())
            return;
        
        //map linking token offsets with their tokens annotation from the Stanford tool.        
        for (CoreMap sentence : sentences) {
            String sentenceStr = "";
            int sentenceNum = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class);
            
            Map<Integer, Integer> tokenNumStartOffset = document.sentenceTokenNumStartOffset.get(sentenceNum);
            if (tokenNumStartOffset == null)
                document.sentenceTokenNumStartOffset.put(sentenceNum, tokenNumStartOffset = new HashMap<>());
            
            Map<Integer, List<String>> startOffsetSRLRoles = new TreeMap<>();
            //extracting tokenized information from the stanford parser output.
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                sentenceStr += token.value()+" ";
                document.startOffsetIndexedWord.put(token.beginPosition(), new IndexedWord(token));
                tokenNumStartOffset.put(token.index(), token.beginPosition());
                startOffsetSRLRoles.put(token.beginPosition(), null);
            }
               
            //write the tokenized sentence to an output file
            FileOutputStream output = new FileOutputStream(Main.RESOURCES_DIR+"\\senna\\log.txt");
            output.write(sentenceStr.getBytes());
            //the semantic roles labels for the sentence are obtained by applying SENNA
            startOffsetSRLRoles = SENNASrl.getSRLRoles(startOffsetSRLRoles);
            //set the srl tags
            document.startOffsetSRLRoles.putAll(startOffsetSRLRoles);
                        
            //parse tree of the sentence
            String stanfordParseTree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class).toString();
            ParseTree parseTree = new ParseTree(stanfordParseTree);
            parseTree.convertParseTree();
            document.setSentenceParseTree(sentenceNum, parseTree);
            
            //dependency graph of the sentence
            SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);
            document.setSentenceDependencyGraph(sentenceNum, graph);
        }
    }
    
    /**
     * Creates a path string using dependency relation names and the direction symbol
     * 
     * @param edges which form a path in the dependency grammar tree
     * @param symbol which is "<" for a path going from bottom to top and ">" for a reverse path
     * @return 
     */
    public static String createPath(List<SemanticGraphEdge> edges, String symbol) {
        String path = "";
        for (SemanticGraphEdge edge : edges) {
            path = path.equals("") ? edge.getRelation().getShortName() : path+symbol+edge.getRelation().getShortName();
        }
        return path;
    }
    
    /**
     * Gets a dependency path between iw1 and iw2 if it exists.
     * 
     * @param iw1 is the Stanford data-structure indexed word posited as first node in the path
     * @param iw2 is the Stanford data-structure indexed word posited as second node in the path
     * @param graph is the dependency path
     * @return dependency path between iw1 and iw2 if it exits, else an empty string
     */
    public static String getPath(IndexedWord iw1, IndexedWord iw2, SemanticGraph graph) {
        List<SemanticGraphEdge> edges = graph.getShortestDirectedPathEdges(iw1, iw2);

        if (edges != null) 
            return createPath(edges, ">");

        edges = graph.getShortestDirectedPathEdges(iw2, iw1);
        if (edges != null) {
            Collections.reverse(edges);
            return createPath(edges, "<");
        }        
        
        return "";
    }
    
    /**
     * Gets all dependency paths between tokens of two phrases where the phrases
     * are represented by the starting offsets of their phrasal tokens in the text.
     * 
     * @param offsets1 contains a phrase's start and end offsets in text
     * @param offsets2 contains another phrase's start and end offsets in text
     * @param startOffsetIndexedWord map of start offsets to their indexed words in the entire text
     * @param graph is the dependency graph of the entire sentence
     * @return Set of dependency paths between tokens of two phrases or empty Set
     */
    public static List<String> getDependencyPaths(int[] offsets1, int[] offsets2, Map<Integer, IndexedWord> startOffsetIndexedWord, SemanticGraph graph) {
        Set<String> dependencyPaths = new HashSet<>();
        
        for (int startOffset1 = offsets1[0]; startOffset1 < offsets1[1]; startOffset1++) {
            if (!startOffsetIndexedWord.containsKey(startOffset1))
                continue;
            IndexedWord iw1 = startOffsetIndexedWord.get(startOffset1);
            for (int startOffset2 = offsets2[0]; startOffset2 < offsets2[1]; startOffset2++) {
                if (!startOffsetIndexedWord.containsKey(startOffset2))
                    continue;
                IndexedWord iw2 = startOffsetIndexedWord.get(startOffset2);
                String path = getPath(iw1, iw2, graph);
                if (!path.equals(""))
                    dependencyPaths.add(path);
            }
        }
        
        return new ArrayList<>(dependencyPaths);
    }
    
    /**
     * Gets a map that links words on one of the ends of the dependency paths to the dependency paths
     * 
     * @param offsets1
     * @param offsets2
     * @param first
     * @param startOffsetIndexedWord
     * @param graph
     * @return 
     */
    public static Map<String, List<String>> getWordLinkedDependencyPaths(int[] offsets1, int[] offsets2, boolean first, Map<Integer, IndexedWord> startOffsetIndexedWord, SemanticGraph graph) {
        Map<String, List<String>> wordLinkedDependencyPaths = new HashMap<>();
        
        for (int startOffset1 = offsets1[0]; startOffset1 < offsets1[1]; startOffset1++) {
            if (!startOffsetIndexedWord.containsKey(startOffset1))
                continue;
            IndexedWord iw1 = startOffsetIndexedWord.get(startOffset1);
            for (int startOffset2 = offsets2[0]; startOffset2 < offsets2[1]; startOffset2++) {
                if (!startOffsetIndexedWord.containsKey(startOffset2))
                    continue;                
                IndexedWord iw2 = startOffsetIndexedWord.get(startOffset2);
                
                String path = getPath(iw1, iw2, graph);
                if (path.equals(""))
                    continue;
                
                String word = first ? iw1.value() : iw2.value();
                List<String> dependencyPaths = wordLinkedDependencyPaths.get(word);
                if (dependencyPaths == null)
                    wordLinkedDependencyPaths.put(word, dependencyPaths = new ArrayList<>());
                if (!dependencyPaths.contains(path))
                    dependencyPaths.add(path);
            }
        }
        
        return wordLinkedDependencyPaths;
    }
    
    /**
     * Gets all dependency paths between tokens of two phrases where the phrases
     * are represented by the starting offsets of their phrasal tokens in the text.
     * 
     * @param offsets1 contains a phrase's start and end offsets in text
     * @param offsets2 contains another phrase's start and end offsets in text
     * @param offsets3 contains a third phrase's start and end offsets in text
     * @param startOffsetIndexedWord map of start offsets to their indexed words in the entire text
     * @param graph is the dependency graph of the entire sentence
     * @return Set of dependency paths between tokens of two phrases or empty Set
     */
    public static List<String> getDependencyPaths(int[] offsets1, int[] offsets2, int[] offsets3, 
            Map<Integer, IndexedWord> startOffsetIndexedWord, SemanticGraph graph) {
        Set<String> dependencyPaths = new HashSet<>();
        
        Map<String, List<String>> wordLinkedDependencyPaths1 = StanfordParser.getWordLinkedDependencyPaths(offsets1, offsets2, false, startOffsetIndexedWord, graph);        
        Map<String, List<String>> wordLinkedDependencyPaths2 = StanfordParser.getWordLinkedDependencyPaths(offsets2, offsets3, true, startOffsetIndexedWord, graph);
        
        Set<String> keys = new HashSet<>(wordLinkedDependencyPaths1.keySet());
        keys.retainAll(wordLinkedDependencyPaths2.keySet());
        
        for (String key : keys) {
            List<String> dependencyPaths1 = wordLinkedDependencyPaths1.get(key);
            List<String> dependencyPaths2 = wordLinkedDependencyPaths2.get(key);
            
            for (String dependencyPath1 : dependencyPaths1) 
                for (String dependencyPath2 : dependencyPaths2) 
                    dependencyPaths.add(dependencyPath1+"-"+dependencyPath2);
            
        }
        
        return new ArrayList<>(dependencyPaths);
    }    
    
}
