/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.ling;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import main.java.parsers.StanfordParser;
import main.java.spatialrelex.Main;
import main.java.spatialrelex.markup.Doc;
import main.java.spatialrelex.markup.SpatialElement;
import main.java.spatialrelex.markup.SpatialRelation;

/**
 *
 * @author Jenny D'Souza
 */
public class Features {
    
    public String parseStringFeature = "";
    
    private String concatenatedLemmaStr = "";
    private String concatenatedWordStr = "";
    private String lexicalPatternStr = "";
    private List<String> wordsInBetween = new ArrayList<>();
    private List<String> wordsOfSe3 = new ArrayList<>();
    private List<String> dependencyPathsSe1Se3Se2 = new ArrayList<>();
    private List<String> dependencyPathsSe1Se2 = new ArrayList<>();
    private List<String> dependencyPathsSe3Se2 = new ArrayList<>();
    private List<String> se3WordLinkedWithDependencyPathsSe3Se2 = new ArrayList<>();
    //se1 WordNet synsets
    //se2 WordNet synsets
    //se1 WordNet hypernyms
    //se2 WordNet hypernyms    
    //se1 srl
    //se2 srl
    //se3 srl
    private List<String> sharedGeneralInquirerClasses = new ArrayList<>();
    private List<String> sharedVerbNetClasses = new ArrayList<>();
    private String orderOfElements = "";
    //order as "r3-r2-r1"
    private String distanceSe1Se2 = "";
    private String distanceSe1Se3 = "";    
    private String distanceSe2Se3 = "";
    private String binnedDistanceSe1Se2Se3 = "";
    //se 1 Types
    //se 2 Types
    //se 3 Types
    //se 1 Roles
    //se 2 Roles
    //se 3 Roles

    public void setUniFV(Doc document, SpatialRelation sr) throws IOException {        
        int sentence = -1;
        
        //add elements extracted in previous sieves here
        
        //setting the map makes it easier to determine the order of the spatial elements in text
        Map<Integer, SpatialElement> startOffsetSpatialElement = new TreeMap<>();
        String[] roles = sr.rolesString.split("\\-");
        if (sr.se1 != null) {
            sr.se1.role = roles[0];
            startOffsetSpatialElement.put(sr.se1.start, sr.se1);
            sentence = document.startOffsetIndexedWord.get(sr.se1.start).sentIndex();
        }
        if (sr.se2 != null) {
            sr.se2.role = sr.se1 == null ? roles[0] : roles[1];
            startOffsetSpatialElement.put(sr.se2.start, sr.se2);
            sentence = document.startOffsetIndexedWord.get(sr.se2.start).sentIndex();
        }
        if (sr.se3 != null) {
            sr.se3.role = roles[roles.length-1];
            startOffsetSpatialElement.put(sr.se3.start, sr.se3);        
        }

        SemanticGraph graph = document.sentenceDependencyGraph.get(sentence);
        Map<Integer, IndexedWord> startOffsetIndexedWord = document.startOffsetIndexedWord;        
                
        this.concatenatedLemmaStr = getConcatLemmaStr(sr.se1, sr.se2, sr.se3);
        this.concatenatedWordStr = getConcatWordStr(sr.se1, sr.se2, sr.se3);
        this.lexicalPatternStr = getLexicalPatternStr(startOffsetIndexedWord, startOffsetSpatialElement);
        this.wordsInBetween = getWordsInBetween(startOffsetIndexedWord, startOffsetSpatialElement);
        this.wordsOfSe3 = getWordsOfSpatialElement(sr.se3);
        this.dependencyPathsSe1Se3Se2 = getDependencyPaths(sr.se1, sr.se3, sr.se2, startOffsetIndexedWord, graph);
        this.dependencyPathsSe1Se2 = getDependencyPaths(sr.se1, sr.se2, startOffsetIndexedWord, graph);
        this.dependencyPathsSe3Se2 = getDependencyPaths(sr.se3, sr.se2, startOffsetIndexedWord, graph);
        this.se3WordLinkedWithDependencyPathsSe3Se2 = getWordLinkedDependencyPaths(sr.se3, sr.se2, startOffsetIndexedWord, graph);
        this.sharedGeneralInquirerClasses = getSharedGeneralInquirerCategories(sr.se1, sr.se2);
        this.sharedVerbNetClasses = getSharedVerbNetClasses(sr.se1, sr.se2);
        this.orderOfElements = getOrderOfElementsInText(sr.se1, sr.se2, sr.se3, startOffsetSpatialElement);
        this.distanceSe1Se2 = getDistanceBetweenSpatialElements(sr.se1, sr.se2);
        this.distanceSe1Se3 = getDistanceBetweenSpatialElements(sr.se1, sr.se3);        
        this.distanceSe2Se3 = getDistanceBetweenSpatialElements(sr.se2, sr.se3);
        this.binnedDistanceSe1Se2Se3 = getBinnedDistance(distanceSe1Se2, distanceSe1Se3, distanceSe2Se3);
        
        //add elements extracted in preceding sieves
        if (!sr.type.equals("LINK") && !sr.type.equals("MOVELINK") && !SpatialRelation.ORDERED_SIEVES.isEmpty()) {
            List<String> precedingSieves = SpatialRelation.getPrecedingSieves(sr.type);
                    
            Map<String, List<String>> roleOtherElement = Main.train ? document.triggerMoverRoleOtherElements.get(document.filename+"-"+sr.se1.id+"-"+sr.se2.id) :
                    SpatialRelation.fileTriggerMoverRoleOtherElement.get(document.filename+"-"+sr.se1.id+"-"+sr.se2.id);
            
            if (!precedingSieves.isEmpty() && roleOtherElement != null) {
                for (String precedingSieve : precedingSieves) {
                    if (!roleOtherElement.containsKey(precedingSieve))
                        continue;
                    List<String> otherElements = roleOtherElement.get(precedingSieve);
                                        
                    for (String otherElement : otherElements) {
                        SpatialElement se = document.idSpatialElement.get(otherElement);
                        
                        se = !se.role.toUpperCase().equals(precedingSieve) ? new SpatialElement(precedingSieve.toLowerCase(), se.start, se.end, se.startToken, se.endToken) : se;
                        
                        startOffsetSpatialElement.put(se.start, se);
                    }
                }
            }            
        }
        
        this.parseStringFeature = document.sentenceParseTree.get(sentence).getParseTreeFeature(startOffsetSpatialElement, document.sentenceTokenNumStartOffset.get(sentence), startOffsetIndexedWord);
    }
        
    /**
     * Gets concatenated lemma strings of the spatial elements.
     * 
     * @param se1 is spatial element posited in the first role of the relation triplet.
     * @param se2 is spatial element posited in the second role of the relation triplet.
     * @param se3 is spatial element posited in the third role of the relation triplet.
     * @return String which is the concatenated lemma string of the spatial elements.
     */
    public static String getConcatLemmaStr(SpatialElement se1, SpatialElement se2, SpatialElement se3) {        
        
        String concatLemmaStr = se1 == null ? "null" : se1.lemmaText;
        concatLemmaStr += se2 == null ? "-null" : "-"+se2.lemmaText;
        concatLemmaStr += se3 == null ? "-null" : "-"+se3.lemmaText;
        
        return concatLemmaStr;
    }
    
    /**
     * Get concatenated word strings of the spatial elements.
     * 
     * @param se1 is spatial element posited in the first role of the relation triplet.
     * @param se2 is spatial element posited in the second role of the relation triplet.
     * @param se3 is spatial element posited in the third role of the relation triplet.
     * @return String which is the concatenated word string of the spatial elements.
     */
    public static String getConcatWordStr(SpatialElement se1, SpatialElement se2, SpatialElement se3) {
        
        String concatWordStr = se1 == null ? "null" : se1.text;
        concatWordStr += se2 == null ? "-null" : "-"+se2.text;
        concatWordStr += se3 == null ? "-null" : "-"+se3.text;
        
        return concatWordStr;
    }    
    
    /**
     * Gets lexical pattern containing spatial element roles and words in between.
     * 
     * @param startOffsetIndexedWord is a map containing all tokens of the document
     * to which the spatial element belongs.
     * @param startOffsetSpatialElement is a sorted map linking the starting offset positions
     * of the two or three spatial elements to the spatial elements.
     * @return String which is the lexical pattern.
     */
    public static String getLexicalPatternStr(Map<Integer, IndexedWord> startOffsetIndexedWord, 
            Map<Integer, SpatialElement> startOffsetSpatialElement) {
        String lexicalPattern = "";
        
        int start = -1;
        int end = -1;
        
        for (int startOffset : startOffsetSpatialElement.keySet()) {
            SpatialElement se = startOffsetSpatialElement.get(startOffset);
            if (start == -1)
                start = se.end;
            else if (end == -1) {
                end = se.start;
                
                String substring = "";
                int i = start;
                while (i < end) {
                    if (!startOffsetIndexedWord.containsKey(i)) {
                        i++;
                        continue;
                    }
                    IndexedWord iw = startOffsetIndexedWord.get(i);
                    substring += iw.value()+" ";
                    i++;
                }
                substring = substring.trim();
                
                if (!substring.equals(""))
                    lexicalPattern += "_"+substring;
                start = se.end;
                end = -1;
            }
            lexicalPattern = lexicalPattern.equals("") ? se.role : lexicalPattern+"_"+se.role;
        }
                
        return lexicalPattern;
    }
    
    /**
     * Gets a list containing all words in between the spatial elements.
     * 
     * @param startOffsetIndexedWord is a map which links the starting offset positions
     * of the tokens in the text to Stanford CoreNLP IndexedWord objects of the tokens.
     * @param startOffsetSpatialElement is a sorted map linking the starting offset positions
     * of the two or three spatial elements to the spatial element objects.
     * @return list of words in between the spatial elements.
     */
    public static List<String> getWordsInBetween(Map<Integer, IndexedWord> startOffsetIndexedWord, Map<Integer, SpatialElement> startOffsetSpatialElement) {
        List<String> wordsInBetweenList = new ArrayList<>();
        
        int start = -1;
        int end = -1;
        
        for (int startOffset : startOffsetSpatialElement.keySet()) {
            SpatialElement se = startOffsetSpatialElement.get(startOffset);
            if (start == -1)
                start = se.end;
            else if (end == -1) {
                end = se.start;
                for (int i = start; i < end; i++) {
                    if (!startOffsetIndexedWord.containsKey(i))
                        continue;
                    wordsInBetweenList = setList(wordsInBetweenList, startOffsetIndexedWord.get(i).value());
                }
                start = se.end;
                end = -1;
            }
        }
        
        return wordsInBetweenList;
    } 
    
    /**
     * Gets a list of all words of a spatial element.
     * 
     * @param se the spatial element's words to return
     * @return list of all words of the spatial element.
     */
    public static List<String> getWordsOfSpatialElement(SpatialElement se) {
        List<String> wordsOfSpatialElement = new ArrayList<>();
        if (se == null)
            return wordsOfSpatialElement;
        
        String[] spatialElementWordsArr = se.text.split("\\s+");
        for (String spatialElementWord : spatialElementWordsArr)
            wordsOfSpatialElement = setList(wordsOfSpatialElement, spatialElementWord);
        
        return wordsOfSpatialElement;
    }
    
    /**
     * Get dependency paths between three spatial elements.
     * 
     * @param firstSEOnPath is the first spatial element on the path.
     * @param secondSEOnPath is the second spatial element on the path.
     * @param thirdSEOnPath is the third spatial element on the path.
     * @param startOffsetIndexedWord is a map which links the starting offset positions
     * of the tokens in the text to Stanford CoreNLP IndexedWord objects of the tokens.
     * @param graph is the dependency graph
     * @return dependency paths between the three elements on the path.
     */
    public static List<String> getDependencyPaths(SpatialElement firstSEOnPath, SpatialElement secondSEOnPath, SpatialElement thirdSEOnPath, 
            Map<Integer, IndexedWord> startOffsetIndexedWord, SemanticGraph graph) {
        List<String> dependencyPaths = new ArrayList<>();
        
        if (secondSEOnPath == null)
            return dependencyPaths;
        else if (thirdSEOnPath == null) {
            int[] offsets1 = new int[2];    offsets1[0] = firstSEOnPath.start;  offsets1[1] = firstSEOnPath.end;
            int[] offsets2 = new int[2];    offsets2[0] = secondSEOnPath.start; offsets2[1] = secondSEOnPath.end;
            dependencyPaths = StanfordParser.getDependencyPaths(offsets1, offsets2, startOffsetIndexedWord, graph);
        }
        else if (firstSEOnPath == null) {
            int[] offsets1 = new int[2];    offsets1[0] = secondSEOnPath.start;  offsets1[1] = secondSEOnPath.end;
            int[] offsets2 = new int[2];    offsets2[0] = thirdSEOnPath.start; offsets2[1] = thirdSEOnPath.end;
            dependencyPaths = StanfordParser.getDependencyPaths(offsets1, offsets2, startOffsetIndexedWord, graph);
        }
        else {
            int[] offsets1 = new int[2];    offsets1[0] = firstSEOnPath.start;  offsets1[1] = firstSEOnPath.end;
            int[] offsets2 = new int[2];    offsets2[0] = secondSEOnPath.start; offsets2[1] = secondSEOnPath.end;            
            int[] offsets3 = new int[2];    offsets3[0] = thirdSEOnPath.start; offsets3[1] = thirdSEOnPath.end;
            dependencyPaths = StanfordParser.getDependencyPaths(offsets1, offsets2, offsets3, startOffsetIndexedWord, graph);
        }
        
        return dependencyPaths;
    }
    
    /**
     * Get dependency paths between two spatial elements.
     * 
     * @param firstSEOnPath is the first spatial element.
     * @param secondSEOnPath is the second spatial element.
     * @param startOffsetIndexedWord is a map which links the starting offset positions
     * of the tokens in the text to Stanford CoreNLP IndexedWord objects of the tokens.
     * @param graph is the dependency graph.
     * @return dependency paths between the two elements on the path.
     */
    public static List<String> getDependencyPaths(SpatialElement firstSEOnPath, SpatialElement secondSEOnPath, 
            Map<Integer, IndexedWord> startOffsetIndexedWord, SemanticGraph graph) {
        List<String> dependencyPaths = new ArrayList<>();
        
        if (firstSEOnPath == null || secondSEOnPath == null)
            return dependencyPaths;

        int[] offsets1 = new int[2];    offsets1[0] = firstSEOnPath.start;  offsets1[1] = firstSEOnPath.end;
        int[] offsets2 = new int[2];    offsets2[0] = secondSEOnPath.start; offsets2[1] = secondSEOnPath.end;
        dependencyPaths = StanfordParser.getDependencyPaths(offsets1, offsets2, startOffsetIndexedWord, graph);        
        
        return dependencyPaths;
    }
    
    /**
     * Get dependency paths between two spatial elements with the first element's word linked to the paths.
     * 
     * @param firstSEOnPath is the first spatial element.
     * @param secondSEOnPath is the second spatial element
     * @param startOffsetIndexedWord is a map which links the starting offset positions
     * of the tokens in the text to Stanford CoreNLP IndexedWord objects of the tokens.
     * @param graph is the dependency graph.
     * @return dependency paths between two spatial elements with the first element's word linked to the paths.
     */
    public static List<String> getWordLinkedDependencyPaths(SpatialElement firstSEOnPath, SpatialElement secondSEOnPath,
            Map<Integer, IndexedWord> startOffsetIndexedWord, SemanticGraph graph) {
        List<String> dependencyPaths = new ArrayList<>();
        
        if (firstSEOnPath == null || secondSEOnPath == null)
            return dependencyPaths;        

        int[] offsets1 = new int[2];    offsets1[0] = firstSEOnPath.start;  offsets1[1] = firstSEOnPath.end;
        int[] offsets2 = new int[2];    offsets2[0] = secondSEOnPath.start; offsets2[1] = secondSEOnPath.end;        
        Map<String, List<String>> wordLinkedDependencyPaths = StanfordParser.getWordLinkedDependencyPaths(offsets1, offsets2, true, startOffsetIndexedWord, graph);
        
        for (String word : wordLinkedDependencyPaths.keySet()) {
            List<String> paths = wordLinkedDependencyPaths.get(word);
            for (String path : paths) {
                if (dependencyPaths.contains(word+"-"+path))
                    continue;
                dependencyPaths.add(word+"-"+path);
            }
        }
        
        return dependencyPaths;
    }
    
    /**
     * Get General Inquirer categories in common between the General Inquirer categories of two spatial elements.
     * 
     * @param se1 is the first spatial element.
     * @param se2 is the second spatial element.
     * @return share General Inquirer categories between two spatial elements.
     */
    public static List<String> getSharedGeneralInquirerCategories(SpatialElement se1, SpatialElement se2) {
        List<String> sharedGeneralInquirerCategories = new ArrayList<>();
        
        if (se1 == null || se2 == null || (se1.generalInquirerCategories.isEmpty() && se2.generalInquirerCategories.isEmpty()))
            return sharedGeneralInquirerCategories;

        sharedGeneralInquirerCategories.addAll(se1.generalInquirerCategories);
        sharedGeneralInquirerCategories.retainAll(se2.generalInquirerCategories);        
        
        return sharedGeneralInquirerCategories;
    }
    
    /**
     * Get verbnet classes in common between the verbnet classes of two spatial elements.
     * 
     * @param se1 is the first spatial element.
     * @param se2 is the second spatial element.
     * @return shared verbnet classes between two spatial elements.
     */
    public static List<String> getSharedVerbNetClasses(SpatialElement se1, SpatialElement se2) {
        List<String> sharedVerbNetClasses = new ArrayList<>();
        
        if (se1 == null || se2 == null || (se1.verbNetClasses.isEmpty() && se2.verbNetClasses.isEmpty()))
            return sharedVerbNetClasses;
        
        sharedVerbNetClasses.addAll(se1.verbNetClasses);
        sharedVerbNetClasses.retainAll(se2.verbNetClasses);
        
        return sharedVerbNetClasses;
    }
    
    /**
     * Get order of spatial elements in text.
     * 
     * @param se1 is the first participant in the relation.
     * @param se2 is the second participant in the relation.
     * @param se3 is the third participant in the relation.
     * @param startOffsetSpatialElement is a sorted map linking the starting offset positions
     * of the two or three spatial elements to the spatial element objects.
     * @return String reflecting the order of the spatial elements.
     */
    public static String getOrderOfElementsInText(SpatialElement se1, SpatialElement se2, SpatialElement se3, 
            Map<Integer, SpatialElement> startOffsetSpatialElement) {
        String orderOfElements = "";
        for (int startOffset : startOffsetSpatialElement.keySet()) {
            SpatialElement se = startOffsetSpatialElement.get(startOffset);
            String participant = se1 != null && se1.start == se.start ? "r1" : 
                    se2 != null && se2.start == se.start ? "r2" : "r3";
            orderOfElements = orderOfElements.equals("") ? participant : orderOfElements+"-"+participant;
        }
        return orderOfElements;
    }
    
    /**
     * Gets the relative distance between two spatial elements.
     * 
     * @param se1 is the first spatial element.
     * @param se2 is the second spatial element.
     * @return String the relative distance between the spatial elements.
     */
    public static String getDistanceBetweenSpatialElements(SpatialElement se1, SpatialElement se2) {        
        if (se1 == null || se2 == null)
            return "null";
        
        String distance = se1.startToken < se2.startToken ? 
                Integer.toString(se2.startToken - se1.endToken) : 
                Integer.toString(se1.startToken - se2.endToken);
        
        distance = se1.startToken < se2.startToken ? "-"+distance : distance;
        
        return distance;
    }
    
    /**
     * Gets concatenated string of binned distances between SE1, SE2, and SE3.
     * 
     * @param se1Se2 relative distance between SE1 and SE2.
     * @param se1Se3 relative distance between SE1 and SE3.
     * @param se2Se3 relative distance between SE2 and SE3.
     * @return String with binned distances.
     */
    public static String getBinnedDistance(String se1Se2, String se1Se3, String se2Se3) {
        String se1Se2Bound = se1Se2.equals("null") ? "null" : getBound(Integer.parseInt(se1Se2));
        String se1Se3Bound = se1Se3.equals("null") ? "null" : getBound(Integer.parseInt(se1Se3));
        String se2Se3Bound = se2Se3.equals("null") ? "null" : getBound(Integer.parseInt(se2Se3));
        return se1Se2Bound+"_"+se1Se3Bound+"_"+se2Se3Bound;
    }       
    
    /**
     * Gets the binned upper bound of the value from bins of size 10.
     * 
     * @param value to get binned bound for.
     * @return String binned upper bound of the value from bins of size 10.
     */
    public static String getBound(int value) {
        String bound = "";
        int temp_bound = -1;
        int prev_temp_bound = -1;
        for (int i = 1; i <= BIN_BOUNDS_MAP.size(); i++) {
            temp_bound = BIN_BOUNDS_MAP.get(i);
            if (i == 1) {
                if (value <= temp_bound) {
                    bound = Integer.toString(temp_bound);
                    break;
                }
            }
            else if (i == BIN_BOUNDS_MAP.size() && (value > temp_bound)) {
                bound = ">80";
                break;
            }
            else if (value > prev_temp_bound && value <= temp_bound) {
                bound = Integer.toString(temp_bound);
                break;
            }
            prev_temp_bound = temp_bound;
        }
        return bound;
    }   
    
    /**
     * Sets list with values.
     * 
     * @param list to set.
     * @param values to add to list.
     * @return List with values added.
     */
    public static List<String> setList (List<String> list, List<String> values) {
        for (String value : values)
            list = setList(list, value);
        return list;
    }
    
    /**
     * Sets list with value.
     * 
     * @param list to set.
     * @param value to add.
     * @return List with value added.
     */
    public static List<String> setList (List<String> list, String value) {
        if (!list.contains(value))
            list.add(value);
        return list;
    }    
    
    /**
     * Sets universal features lists.
     * 
     * @param sr is the spatial relation object with features to add to the features lists.
     */
    public static void setUniFeatureLists(SpatialRelation sr) {
        concatenatedLemmaStrs = setList(concatenatedLemmaStrs, sr.features.concatenatedLemmaStr);
        concatenatedWordStrs = setList(concatenatedWordStrs, sr.features.concatenatedWordStr);
        lexicalPatternStrs = setList(lexicalPatternStrs, sr.features.lexicalPatternStr);
        inBetweenWords = setList(inBetweenWords, sr.features.wordsInBetween);
        se3Words = setList(se3Words, sr.features.wordsOfSe3);
        if (sr.se3 != null)
            se3Phrases = setList(se3Phrases, sr.se3.text.toLowerCase().replaceAll("\\s+", " "));
        se1Se3Se2DependencyPaths = setList(se1Se3Se2DependencyPaths, sr.features.dependencyPathsSe1Se3Se2);
        se1Se2DependencyPaths = setList(se1Se2DependencyPaths, sr.features.dependencyPathsSe1Se2);
        se3Se2DependencyPaths = setList(se3Se2DependencyPaths, sr.features.dependencyPathsSe3Se2);
        se3WordLinkedWithSe3Se2DependencyPaths = setList(se3WordLinkedWithSe3Se2DependencyPaths, sr.features.se3WordLinkedWithDependencyPathsSe3Se2);        
        allSharedGeneralInquirerClasses = setList(allSharedGeneralInquirerClasses, sr.features.sharedGeneralInquirerClasses);
        allSharedVerbNetClasses = setList(allSharedVerbNetClasses, sr.features.sharedVerbNetClasses);
        ordersOfElements = setList(ordersOfElements, sr.features.orderOfElements);
        distancesBetweenSpatialElements = setList(distancesBetweenSpatialElements, sr.features.distanceSe1Se2);
        distancesBetweenSpatialElements = setList(distancesBetweenSpatialElements, sr.features.distanceSe1Se3);        
        distancesBetweenSpatialElements = setList(distancesBetweenSpatialElements, sr.features.distanceSe2Se3);
        binnedDistances = setList(binnedDistances, sr.features.binnedDistanceSe1Se2Se3);            
        
        setUniFeaturesLists(sr.se1, true);
        setUniFeaturesLists(sr.se2, true);
        setUniFeaturesLists(sr.se3, false);
    }    
     
    /**
     * Sets spatial element specific universal feature lists.
     * 
     * @param se is the spatial element.
     * @param se1OrSe2 flag indicating whether it is the first or second spatial element.
     */
    public static void setUniFeaturesLists(SpatialElement se, boolean se1OrSe2) {
        if (se == null)
            return;
        if (se1OrSe2) {
            allWordNetSynsets = setList(allWordNetSynsets, se.synsets);            
            allWordNetHypernyms = setList(allWordNetHypernyms, se.hypernyms);
        }
        allSRLs = setList(allSRLs, se.srls);            
        elementsTypes = setList(elementsTypes, se.type);            
        elementsRoles = setList(elementsRoles, se.roles);
    }
    
    /**
     * clears all feature lists
     */
    public static void clearFeatureLists() {
        concatenatedLemmaStrs = new ArrayList<>();
        concatenatedWordStrs = new ArrayList<>();
        lexicalPatternStrs = new ArrayList<>();
        inBetweenWords = new ArrayList<>();
        se3Words = new ArrayList<>();
        se3Phrases = new ArrayList<>();
        se1Se3Se2DependencyPaths = new ArrayList<>();
        se1Se2DependencyPaths = new ArrayList<>();
        se3Se2DependencyPaths = new ArrayList<>();
        se3WordLinkedWithSe3Se2DependencyPaths = new ArrayList<>();
        allSharedGeneralInquirerClasses = new ArrayList<>();
        allSharedVerbNetClasses = new ArrayList<>();
        ordersOfElements = new ArrayList<>();
        distancesBetweenSpatialElements = new ArrayList<>();
        distancesBetweenSpatialElements = new ArrayList<>();
        distancesBetweenSpatialElements = new ArrayList<>();
        binnedDistances = new ArrayList<>();
        allWordNetSynsets = new ArrayList<>();
        allWordNetHypernyms = new ArrayList<>();
        allSRLs = new ArrayList<>();
        elementsTypes = new ArrayList<>();
        elementsRoles = new ArrayList<>();
    }        
    
    /**
     * Sets universal features sizes.
     */
    public static void setUniFeaturesSizes() {
        endF1 = concatenatedLemmaStrs.size()+1;    //concat lemma strings
        endF2 = endF1+concatenatedWordStrs.size()+1;    //concat word strings
        endF3 = endF2+lexicalPatternStrs.size()+1;    //lexical pattern
        endF4 = endF3+inBetweenWords.size()+1;    //words in between
        endF5 = endF4+se3Words.size()+1;    //SE3's words
        endF6 = endF5+2+1;    //SE2 seen as SE3 or either are absent
        endF7 = endF6+se1Se3Se2DependencyPaths.size()+1;    //dependency from SE1 to SE3 to SE2
        endF8 = endF7+se1Se2DependencyPaths.size()+1;    //dependency from SE1 to SE2
        endF9 = endF8+se3Se2DependencyPaths.size()+1;    //dependency from SE3 to SE2
        endF10 = endF9+se3WordLinkedWithSe3Se2DependencyPaths.size()+1;   //paths from SE3 to SE2 with SE3's string
        endF11 = endF10+2+1;   //about prep objects or absent
        endF12 = endF11+allWordNetSynsets.size()+1;   //wordnet synsets of SE1
        endF13 = endF12+allWordNetSynsets.size()+1;   //wordnet synsets of SE2
        endF14 = endF13+allWordNetHypernyms.size()+1;   //wordnet hypernyms of SE1
        endF15 = endF14+allWordNetHypernyms.size()+1;   //wordnet hypernyms of SE2
        endF16 = endF15+allSRLs.size()+1;   //srl of SE1
        endF17 = endF16+allSRLs.size()+1;   //srl of SE2
        endF18 = endF17+allSRLs.size()+1;   //srl of SE3
        endF19 = endF18+allSharedGeneralInquirerClasses.size()+1;   //GI categories shared by SE1 and SE2
        endF20 = endF19+allSharedVerbNetClasses.size()+1;   //VerbNet classes shared by SE1 and SE2
        endF21 = endF20+ordersOfElements.size()+1;   //order of participants
        endF22 = endF21+2;   //a specific order: yes or no
        endF23 = endF22+distancesBetweenSpatialElements.size()+1;   //distanceSe1Se2
        endF24 = endF23+distancesBetweenSpatialElements.size()+1;   //distanceSe1Se3
        endF25 = endF24+distancesBetweenSpatialElements.size()+1;   //distanceSe2Se3
        endF26 = endF25+binnedDistances.size()+1;   //binnedDistanceSe1Se2Se3
        endF27 = endF26+elementsTypes.size()+1;   //spatial element type of SE1
        endF28 = endF27+elementsTypes.size()+1;   //spatial element type of SE2
        endF29 = endF28+elementsTypes.size()+1;   //spatial element type of SE3
        endF30 = endF29+elementsRoles.size()+1;   //spatial element role of SE1
        endF31 = endF30+elementsRoles.size()+1;   //spatial element role of SE2
        endF32 = endF31+elementsRoles.size()+1;   //spatial element role of SE3            
    }      
       
    /**
     * Gets an svm-style feature for a single elementFeature.
     * 
     * @param begin index for a feature-type
     * @param features list of values from training data of a specific feature type
     * @param elementFeature 
     * @param end index for a feature-type
     * @return String svm-style feature.
     */
    public static String getFeature(int begin, List<String> features, String elementFeature, int end) {
        return features.contains(elementFeature) ? Integer.toString(begin+features.indexOf(elementFeature)+1)+":1" : Integer.toString(end)+":1";
    }
    
    /**
     * Gets svm-style features for a list of elementFeatures.
     * 
     * @param begin index for a feature-type
     * @param features list of values from training data of a specific feature type
     * @param elementFeatures
     * @param end index for a feature-type
     * @return String svm-style features.
     */
    public static String getFeature(int begin, List<String> features, List<String> elementFeatures, int end) {
        List<Integer> elementFeatureIndexes = new ArrayList<>();
        for (String elementFeature : elementFeatures) {
            if (!features.contains(elementFeature)) {
                if (!elementFeatureIndexes.contains(end))
                    elementFeatureIndexes.add(end);
                continue;
            }
            int featureIndex = begin+features.indexOf(elementFeature)+1;
            if (!elementFeatureIndexes.contains(featureIndex))
                elementFeatureIndexes.add(featureIndex);
        }
        Collections.sort(elementFeatureIndexes);
        String featureString = "";
        for (int elementFeatureIndex : elementFeatureIndexes)
            featureString += elementFeatureIndex+":1 ";
        featureString = featureString.trim();
        return featureString;
    }
    
    /**
     * Gets svm-style feature string for a given relation instance.
     * 
     * @param sr SpatialRelation object to generate features for.
     * @return svm-style feature string.
     */
    public String toString(SpatialRelation sr) {        
        return
                "|BT| "+parseStringFeature+" |ET| "+
                getFeature(0, concatenatedLemmaStrs, concatenatedLemmaStr, endF1) + " " + 
                getFeature(endF1, concatenatedWordStrs, concatenatedWordStr, endF2) + " " +
                getFeature(endF2, lexicalPatternStrs, lexicalPatternStr, endF3) + " " +
                getFeature(endF3, inBetweenWords, wordsInBetween, endF4) + " " +
                getFeature(endF4, se3Words, wordsOfSe3, endF5) + " " +
                (sr.se2 == null ? endF6+":1" : se3Phrases.contains(sr.se2.text.toLowerCase().replaceAll("\\s+", " ")) ? Integer.toString(endF5+1)+":1" : Integer.toString(endF5+2)+":1") + " " +
                getFeature(endF6, se1Se3Se2DependencyPaths, dependencyPathsSe1Se3Se2, endF7) + " " +
                getFeature(endF7, se1Se2DependencyPaths, dependencyPathsSe1Se2, endF8) + " " +
                getFeature(endF8, se3Se2DependencyPaths, dependencyPathsSe3Se2, endF9) + " " + 
                getFeature(endF9, se3WordLinkedWithSe3Se2DependencyPaths, se3WordLinkedWithDependencyPathsSe3Se2, endF10) + " " +
                //F11about prep objects + " " +
                (sr.se1 == null ? endF12+":1" : getFeature(endF11, allWordNetSynsets, sr.se1.synsets, endF12)) + " " + 
                (sr.se2 == null ? endF13+":1" : getFeature(endF12, allWordNetSynsets, sr.se2.synsets, endF13)) + " " +
                (sr.se1 == null ? endF14+":1" : getFeature(endF13, allWordNetHypernyms, sr.se1.hypernyms, endF14)) + " " +
                (sr.se2 == null ? endF15+":1" : getFeature(endF14, allWordNetHypernyms, sr.se2.hypernyms, endF15)) + " " + 
                (sr.se1 == null ? endF16+":1" : getFeature(endF15, allSRLs, sr.se1.srls, endF16)) + " " +
                (sr.se2 == null ? endF17+":1" : getFeature(endF16, allSRLs, sr.se2.srls, endF17)) + " " +
                (sr.se3 == null ? endF18+":1" : getFeature(endF17, allSRLs, sr.se3.srls, endF18)) + " " +
                getFeature(endF18, allSharedGeneralInquirerClasses, sharedGeneralInquirerClasses, endF19) + " " +
                getFeature(endF19, allSharedVerbNetClasses, sharedVerbNetClasses, endF20) + " " +
                getFeature(endF20, ordersOfElements, orderOfElements, endF21) + " " +   
                (orderOfElements.equals("r3-r2-r1") ? Integer.toString(endF21+1)+":1" : endF22+":1") + " " +
                getFeature(endF22, distancesBetweenSpatialElements, distanceSe1Se2, endF23) + " " +
                getFeature(endF23, distancesBetweenSpatialElements, distanceSe1Se3, endF24) + " " +
                getFeature(endF24, distancesBetweenSpatialElements, distanceSe2Se3, endF25) + " " +
                getFeature(endF25, binnedDistances, binnedDistanceSe1Se2Se3, endF26) + " " +
                (sr.se1 == null ? endF27+":1" : getFeature(endF26, elementsTypes, sr.se1.type, endF27)) + " " +
                (sr.se2 == null ? endF28+":1" : getFeature(endF27, elementsTypes, sr.se2.type, endF28)) + " " +
                (sr.se3 == null ? endF29+":1" : getFeature(endF28, elementsTypes, sr.se3.type, endF29)) + " " +
                (sr.se1 == null ? endF30+":1" : getFeature(endF29, elementsRoles, sr.se1.roles, endF30)) + " " +
                (sr.se2 == null ? endF31+":1" : getFeature(endF30, elementsRoles, sr.se2.roles, endF31)) + " " +
                (sr.se3 == null ? endF32+":1" : getFeature(endF31, elementsRoles, sr.se3.roles, endF32)) +" |EV|";
    }      
    
    public static List<String> concatenatedLemmaStrs = new ArrayList<>();
    public static List<String> concatenatedWordStrs = new ArrayList<>();
    public static List<String> lexicalPatternStrs = new ArrayList<>();
    public static List<String> inBetweenWords = new ArrayList<>();
    public static List<String> se3Words = new ArrayList<>();
    public static List<String> se3Phrases = new ArrayList<>();
    public static List<String> se1Se3Se2DependencyPaths = new ArrayList<>();
    public static List<String> se1Se2DependencyPaths = new ArrayList<>();
    public static List<String> se3Se2DependencyPaths = new ArrayList<>();
    public static List<String> se3WordLinkedWithSe3Se2DependencyPaths = new ArrayList<>();    
    public static List<String> allWordNetSynsets = new ArrayList<>();
    public static List<String> allWordNetHypernyms = new ArrayList<>();
    public static List<String> allSRLs = new ArrayList<>();
    public static List<String> allSharedGeneralInquirerClasses = new ArrayList<>();
    public static List<String> allSharedVerbNetClasses = new ArrayList<>();
    public static List<String> ordersOfElements = new ArrayList<>();
    public static List<String> distancesBetweenSpatialElements = new ArrayList<>();
    public static List<String> binnedDistances = new ArrayList<>();
    public static List<String> elementsTypes = new ArrayList<>();
    public static List<String> elementsRoles = new ArrayList<>();        
    
    public static final Map<Integer, Integer> BIN_BOUNDS_MAP;
    static {
        Map<Integer, Integer> aMap = new HashMap<>();
        aMap.put(1, -80);
        aMap.put(2, -70);
        aMap.put(3, -60);
        aMap.put(4, -50);        
        aMap.put(5, -40);
        aMap.put(6, -30);
        aMap.put(7, -20);
        aMap.put(8, -10);
        aMap.put(9, 0);        
        aMap.put(10, 10);
        aMap.put(11, 20);
        aMap.put(12, 30);
        aMap.put(13, 40);        
        aMap.put(14, 50);
        aMap.put(15, 60);
        aMap.put(16, 70);
        aMap.put(17, 80);        
        BIN_BOUNDS_MAP = Collections.unmodifiableMap(aMap);        
    }    
    
    public static int endF1;    //concat lemma strings
    public static int endF2;    //concat word strings
    public static int endF3;    //lexical pattern
    public static int endF4;    //words in between
    public static int endF5;    //SE3's words
    public static int endF6;    //SE2 seen as SE3
    public static int endF7;    //dependency from SE1 to SE3 to SE2
    public static int endF8;    //dependency from SE1 to SE2
    public static int endF9;    //dependency from SE3 to SE2
    public static int endF10;   //paths from SE3 to SE2 with SE3's string
    public static int endF11;   //about prep objects
    public static int endF12;   //wordnet synsets of SE1
    public static int endF13;   //wordnet synsets of SE2
    public static int endF14;   //wordnet hypernyms of SE1
    public static int endF15;   //wordnet hypernyms of SE2
    public static int endF16;   //srl of SE1
    public static int endF17;   //srl of SE2
    public static int endF18;   //srl of SE3
    public static int endF19;   //GI categories shared by SE1 and SE2
    public static int endF20;   //VerbNet classes shared by SE1 and SE2
    public static int endF21;   //order of participants
    public static int endF22;   //a specific order
    public static int endF23;   //distanceSe1Se2
    public static int endF24;   //distanceSe1Se3
    public static int endF25;   //distanceSe2Se3
    public static int endF26;   //binnedDistanceSe1Se2Se3
    public static int endF27;   //spatial element type of SE1
    public static int endF28;   //spatial element type of SE2
    public static int endF29;   //spatial element type of SE3
    public static int endF30;   //spatial element role of SE1
    public static int endF31;   //spatial element role of SE2
    public static int endF32;   //spatial element role of SE3    
    
}
