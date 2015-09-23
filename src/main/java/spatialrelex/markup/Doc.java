/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.markup;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import main.java.parsers.DomParser;
import main.java.spatialrelex.ling.ParseTree;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 *
 * @author Jenny D'Souza
 */
public class Doc {
    
    public Map<Integer, IndexedWord> startOffsetIndexedWord;
    public Map<Integer, Map<Integer, Integer>> sentenceTokenNumStartOffset;
    public Map<Integer, List<String>> startOffsetSRLRoles;
    public Map<Integer, Map<String, Set<String>>> sentenceTypeIds;
    public Map<String, SpatialElement> idSpatialElement;    
    public Map<Integer, SemanticGraph> sentenceDependencyGraph;
    public Map<Integer, ParseTree> sentenceParseTree;
    
    public Map<String, Map<String, SpatialRelation>> relationTripletsSpatialRelations = new HashMap<>();
    
    public Map<String, Map<String, List<String>>> triggerMoverRoleOtherElements = new HashMap<>();
    
    public String text;    
    
    public String filename;
    
    /**
     * Document constructor which initializes all relevant data-structure maps.
     */
    public Doc() {
        startOffsetIndexedWord = new TreeMap<>();
        sentenceTokenNumStartOffset = new HashMap<>();
        startOffsetSRLRoles = new HashMap<>();
        sentenceTypeIds = new HashMap<>();
        idSpatialElement = new HashMap<>();
        sentenceDependencyGraph = new HashMap<>();
        sentenceParseTree = new HashMap<>();

        relationTripletsSpatialRelations = new HashMap<>();
        
        triggerMoverRoleOtherElements = new HashMap<>();
    }
    
    /**
     * Adds information of spatial element in document into two different maps.
     * The first map sentenceTypeIds links the sentence to the type to the ID of the spatial element.
     * The second map idSpatialElement links the id to the element.
     * 
     * @param se is the spatial element.
     */
    public void addDocSpatialElement(SpatialElement se) {
        se = SpatialElement.modifyOffsets(se, this);
        if (se == null)
            return;
        
        se = SpatialElement.setSpatialElementFeatures(this, se);        
        int sentence = startOffsetIndexedWord.get(se.start).sentIndex();
        Map<String, Set<String>> typeIds = sentenceTypeIds.get(sentence);
        if (typeIds == null)
            sentenceTypeIds.put(sentence, typeIds = new HashMap<>());
        for (String type : se.type) {
            Set<String> ids = typeIds.get(type);
            if (ids == null)
                typeIds.put(type, ids = new HashSet<>());
            ids.add(se.id);
            idSpatialElement.put(se.id, se);
        }
    }
    
    /**
     * Parses input xml file for its text and spatial information annotations.
     * 
     * @param filename is the input xml file.
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     * @throws java.io.UnsupportedEncodingException 
     * @throws java.lang.InterruptedException 
     */
    public void parseXMLDataFile(String filename) throws SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException {
        this.filename = filename;
        DomParser.parseXmlAnnotationsFile(filename, this);
    }
        
    /**
     * Sets the syntactic parse tree of the sentence.
     * 
     * @param sentence index of the parse tree's sentence.
     * @param tree is the parse tree of the sentence.
     */
    public void setSentenceParseTree(int sentence, ParseTree tree) {
        sentenceParseTree.put(sentence, tree);
    }
    
    /**
     * Sets the dependency graph of the sentence in the sentenceDependencyGraph map.
     * 
     * @param sentence is the sentence number of the sentence for which the dependency graph was generated
     * and used as key to store the graph
     * @param dependencyGraph is the dependency graph of the sentence obtained from the Stanford parser.
     */
    public void setSentenceDependencyGraph(int sentence, SemanticGraph dependencyGraph) {
        sentenceDependencyGraph.put(sentence, dependencyGraph);
    }
        
    /**
     * Gets the dependency graph of the sentence.
     * 
     * @param sentence is the number of the sentence for which to return the dependency graph.
     * @return SemanticGraph the Stanford dependency graph for the sentence.
     */
    public SemanticGraph getDependencyGraph(int sentence) {
        return sentenceDependencyGraph.get(sentence);
    }
       
    /**
     * Iterates over all spatial elements in the document and generates non-Links (unannotated Links) from them.
     * 
     * @throws java.io.IOException
     */
    public void generateNonLinks() throws IOException {
        for (int sentence : sentenceTypeIds.keySet()) {
            
            Map<String, Set<String>> typeIds = sentenceTypeIds.get(sentence);
            
            Set<String> triggers = typeIds.get(SpatialRelation.TRIGGER_SE_TYPE);
            if (triggers == null)
                continue;
            
            Set<String> trajectors_landmarks = new HashSet<>();
            for (String trajector_landmark_type : SpatialRelation.TRAJ_LAND_SE_TYPES) {
                if (!typeIds.containsKey(trajector_landmark_type))
                    continue;
                trajectors_landmarks.addAll(typeIds.get(trajector_landmark_type));
            }
            
            for (String triggerId : triggers) {
                SpatialElement triggerSE = idSpatialElement.get(triggerId);
                        
                for (String trajectorId : trajectors_landmarks) {
                    SpatialElement trajector_landmarkSE = idSpatialElement.get(trajectorId);
                    
                    //trajector-null-trigger
                    SpatialRelation sr = new SpatialRelation("LINK", "-1");
                    sr.storeRelation(this, sr, trajector_landmarkSE, "trajector", null, "", triggerSE, "trigger_link");
                    //null-landmark-trigger
                    sr = new SpatialRelation("LINK", "-1");
                    sr.storeRelation(this, sr, null, "", trajector_landmarkSE, "landmark_link", triggerSE, "trigger_link");
                    
                    for (String landmarkId : trajectors_landmarks) {
                        if (trajectorId.equals(landmarkId))
                            continue;
                        
                        SpatialElement landmarkSE = idSpatialElement.get(landmarkId);
                        
                        //trajector-landmark-null
                        sr = new SpatialRelation("LINK", "-1");
                        sr.storeRelation(this, sr, trajector_landmarkSE, "trajector", landmarkSE, "landmark_link", null, "");
                        //trajector-landmark-trigger
                        sr = new SpatialRelation("LINK", "-1");
                        sr.storeRelation(this, sr, trajector_landmarkSE, "trajector", landmarkSE, "landmark_link", triggerSE, "trigger_link");
                    }
                }
            }
        }
    }       
    
    public Set<String> getSEIdSet(List<String> seTypes, Map<String, Set<String>> typeIds) {
        Set<String> seIds = new HashSet<>();
        for (String type : seTypes) {
            if (!typeIds.containsKey(type))
                continue;
            seIds.addAll(typeIds.get(type));
        }           
        return seIds;
    }
    
    /**
     * Iterates over all spatial elements in the document and generates non-Movelinks (unannotated Movelinks) from them.
     * 
     * @throws java.io.IOException
     */
    public void generateNonMovelinks() throws IOException {
        for (int sentence : sentenceTypeIds.keySet()) {
            
            Map<String, Set<String>> typeIds = sentenceTypeIds.get(sentence);
            
            Set<String> triggers = typeIds.get(SpatialRelation.TRIGGER_MOVELINK_SE_TYPE);
            if (triggers == null)
                continue;
            
            Set<String> mvrs_srcs_goals_lndmrks = getSEIdSet(SpatialRelation.MVR_SRC_GOAL_LNDMRK_SE_TYPES, typeIds);
            if (mvrs_srcs_goals_lndmrks.isEmpty())
                continue;
            Set<String> midPts = getSEIdSet(SpatialRelation.MIDPT_SE_TYPES, typeIds);
            Set<String> paths = typeIds.get(SpatialRelation.PATH_SE_TYPE);
            Set<String> motion_signals = typeIds.get(SpatialRelation.MS_SE_TYPE);
            
            for (String triggerId : triggers) {
                SpatialElement triggerSE = idSpatialElement.get(triggerId);
                
                for (String moverId : mvrs_srcs_goals_lndmrks) {
                    SpatialElement moverSE = idSpatialElement.get(moverId);
                    
                    //trigger-mover-null
                    SpatialRelation sr = new SpatialRelation("MOVELINK", "-1");
                    sr.storeRelation(this, sr, triggerSE, "trigger_movelink", moverSE, "mover", null, "");
                    //trigger-mover-source
                    generateNonMovelinks(sr, triggerSE, moverSE, mvrs_srcs_goals_lndmrks, "source");
                    //trigger-mover-midPoint
                    generateNonMovelinks(sr, triggerSE, moverSE, midPts, "midPoint");
                    //trigger-mover-goal
                    generateNonMovelinks(sr, triggerSE, moverSE, mvrs_srcs_goals_lndmrks, "goal");
                    //trigger-mover-landmark
                    generateNonMovelinks(sr, triggerSE, moverSE, mvrs_srcs_goals_lndmrks, "landmark_movelink");
                    //trigger-mover-path
                    generateNonMovelinks(sr, triggerSE, moverSE, paths, "path");
                    //trigger-mover-motion_signal
                    generateNonMovelinks(sr, triggerSE, moverSE, motion_signals, "motion_signal");
                }
            }
        }
    }
    
    /**
     * Complementary to the function above in that it generates non (or unannotated) MOVELINKs
     * for all the optional role participants.
     * 
     * @param sr is the MOVELINK spatial relation object.
     * @param triggerSE is the MOVELINK trigger participant.
     * @param moverSE is the MOVELINK mover participant.
     * @param otherSEIds are the spatial element IDs of the optional participant.
     * @param otherSERole is the role of the optional participant.
     * @throws IOException 
     */
    public void generateNonMovelinks(SpatialRelation sr, SpatialElement triggerSE, SpatialElement moverSE, Set<String> otherSEIds, String otherSERole) throws IOException {
        if (otherSEIds == null)
            return;
        
        for (String otherSEId : otherSEIds) {
            
            if (otherSEId.equals(moverSE.id))
                continue;
            
            SpatialElement otherSE = idSpatialElement.get(otherSEId);     
            sr = new SpatialRelation(otherSERole.toUpperCase(), "-1");
            sr.storeRelation(this, sr, triggerSE, "trigger_movelink", moverSE, "mover", otherSE, otherSERole);
        }
    }
     
    /**
     * Writes all predicted relation annotations to an output xml file.
     * 
     * @param outputFileName is the name of the output file to write to.
     * @throws IOException 
     */
    public void writeRelationAnnotations(String outputFileName) throws IOException {
        String outputStr = "<SpaceEvalTaskv1.2>\n";
        outputStr += "<TEXT>";
        outputStr += text;
        outputStr += "</TEXT>\n<TAGS>\n";    
        for (String id : idSpatialElement.keySet()) {
            SpatialElement se = idSpatialElement.get(id);
            for (String type : se.type) {
                String semanticType = !se.semantic_type.equals("") ? "semantic_type=\""+se.semantic_type+"\" " : "";
                outputStr += "<"+type+" id=\""+se.id+"\" text=\""+se.text+"\" "
                        + "start=\""+se.start+"\" end=\""+se.end+"\" "+semanticType+"/>\n";       
            }
        }
        
        String[] test = FileUtils.readFileToString(new File("main\\data\\testLINK.txt")).split("\\n");
        String[] result = FileUtils.readFileToString(new File("main\\data\\resultLINK.txt")).split("\\n");
        outputStr += getPredictedLinksString(test, result);
        outputStr += getPredictedMovelinksString();

        outputStr += "</TAGS>\n";
        outputStr += "</SpaceEvalTaskv1.2>";
        
        FileOutputStream output = new FileOutputStream(outputFileName);    
        output.write(DomParser.prettyFormat(outputStr).getBytes());
    }    
    
    /**
     * Iterates through the test file and result file pertaining to (trigger,mover) pair data,
     * and gets their predictions.
     * 
     * @param test file with (trigger,mover) pair instances for classification.
     * @param result file with classifications for (trigger,mover) pair instances in the test file.
     * @return Map of predicted (trigger,mover) pairs.
     */
    public Map<String, Map<Double, Map<String, List<String>>>> getPredictedTriggerMovers(String[] test, String[] result) {
        Map<String, Map<Double, Map<String, List<String>>>> triggerMovers = new HashMap<>();
                
        for (int i = 0; i < result.length; i++) {
            if (result[i].trim().equals(""))
                continue;
            double r = Double.parseDouble(result[i].trim());
            if (r < 0.0)
                continue;
            
            String[] tripletIDs = test[i].split("#")[1].trim().split("\\-");
            
            String trigger = tripletIDs[tripletIDs.length-3];
            String mover = tripletIDs[tripletIDs.length-2];
            String lastElement = tripletIDs[tripletIDs.length-1];
            
            //------------------just a check------------------
            if (triggerMovers.containsKey(trigger+"-"+mover)) {
                System.out.println("trigger mover pair already present: "+trigger+"-"+mover);
                System.out.println("line number: "+i);
                System.exit(1);
            }
            //-------------------------------------------------
            
            triggerMovers.put(trigger+"-"+mover, null);
        }
        
        return triggerMovers;
    }    
    
    /**
     * Iterates through the test file and result file pertaining to (trigger,mover,optionalElement) triplet data,
     * and gets their predictions.
     * 
     * @param test file with (trigger,mover,optionalElement) triplet instances for classification.
     * @param result file with classifications for (trigger,mover,optionalElement) triplet instances in the test file.
     * @param relation is the role label of the optional participant.
     * @param triggerMoversOtherElements is a map linking (trigger,mover) pairs with their optional participants.
     * @return Map linking (trigger,mover,optionalElement) triplets.
     */
    public Map<String, Map<Double, Map<String, List<String>>>> getPredictedOptionalRoles(String[] test, String[] result, 
            String relation, Map<String, Map<Double, Map<String, List<String>>>> triggerMoversOtherElements) {
        
        for (int i = 0; i < result.length; i++) {
            if (result[i].trim().equals(""))
                continue;
            double r = Double.parseDouble(result[i].trim());
            if (r < 0.0)
                continue;
            
            String[] tripletIDs = test[i].split("#")[1].trim().split("\\-");
            
            String trigger = tripletIDs[tripletIDs.length-3];
            String mover = tripletIDs[tripletIDs.length-2];
            String lastElement = tripletIDs[tripletIDs.length-1];            
            
            Map<Double, Map<String, List<String>>> confidenceOtherRoles = triggerMoversOtherElements.get(trigger+"-"+mover);
            if (confidenceOtherRoles == null)
                triggerMoversOtherElements.put(trigger+"-"+mover, confidenceOtherRoles = new HashMap<>());
            
            Map<String, List<String>> optionalRoleElements = confidenceOtherRoles.get(r);
            if (optionalRoleElements == null)
                confidenceOtherRoles.put(r, optionalRoleElements = new HashMap<>());
            List<String> elements = optionalRoleElements.get(relation);
            if (elements == null)
                optionalRoleElements.put(relation, elements = new ArrayList<>());
            if (!elements.contains(lastElement))
                elements.add(lastElement);
        }
        
        return triggerMoversOtherElements;
    }
      
    /**
     * Gets the final MOVELINK map by assigning the element or elements predicted with highest confidence for each role.
     * 
     * @param predictedTriggerMoversOptionalRoles contains all optional predictions for a given (trigger,mover) pair.
     * @return Map with final MOVELINKs.
     */
    public Map<String, Map<String, List<String>>> getMovelinksMap(Map<String, Map<Double, Map<String, List<String>>>> predictedTriggerMoversOptionalRoles) {
        Map<String, Map<String, List<String>>> movelinksMap = new HashMap<>();
        
        for (String triggerMover : predictedTriggerMoversOptionalRoles.keySet()) {            
            Map<Double, Map<String, List<String>>> confidenceOptionalRoles = predictedTriggerMoversOptionalRoles.get(triggerMover);
            
            if (confidenceOptionalRoles == null) {
                movelinksMap.put(triggerMover, null);
                continue;
            }
            
            Set<String> selectedElements = new HashSet<>();
            List<Double> confidences = new ArrayList<>(confidenceOptionalRoles.keySet());
            Collections.sort(confidences);
            Collections.reverse(confidences);
            
            Map<String, List<String>> optionalRoleSelectedElements = new HashMap<>();
            for (double confidence : confidences) {
                Map<String, List<String>> optionalRoleElements = confidenceOptionalRoles.get(confidence);
                
                for (String optionalRole : optionalRoleElements.keySet()) {
                    
                    List<String> predictedElements = optionalRoleElements.get(optionalRole);
                    
                    if (optionalRole.equals("MIDPOINT") || optionalRole.equals("PATH") || optionalRole.equals("MOTION_SIGNAL")) {                        
                        for (String element : predictedElements) {
                            if (selectedElements.contains(element))
                                continue;
                            selectedElements.add(element);
                            
                            List<String> elements = optionalRoleSelectedElements.get(optionalRole);
                            if (elements == null)
                                optionalRoleSelectedElements.put(optionalRole, elements = new ArrayList<>());
                            elements.add(element);
                        }
                    }
                    
                    if (optionalRoleSelectedElements.containsKey(optionalRole))
                        continue;
                    
                    for (String element : predictedElements) {
                        if (selectedElements.contains(element))
                            continue;
                        
                        selectedElements.add(element);
                        List<String> elements = new ArrayList<>();
                        elements.add(element);
                        optionalRoleSelectedElements.put(optionalRole, elements);
                    }
                }
            }
            
            movelinksMap.put(triggerMover, optionalRoleSelectedElements.isEmpty() ? null : optionalRoleSelectedElements);
        }
        
        return movelinksMap;
    }
    
    public String getMovelinksString(Map<String, Map<String, List<String>>> movelinks) {
        String outputStr = "";
        int index = 0;
        for (String triggerMover : movelinks.keySet()) {
            String[] triggerMoverIds = triggerMover.split("\\-");
            
            SpatialElement triggerSE = idSpatialElement.get(triggerMoverIds[0]);
            SpatialElement moverSE = idSpatialElement.get(triggerMoverIds[1]);
            
            Map<String, List<String>> optionalRoleElements = movelinks.get(triggerMover);
            
            outputStr += getMovelinkString(index, triggerSE, moverSE, optionalRoleElements)+"\n";
            index++;
        }
        return outputStr;
    }  
    
    public String getMovelinkString(int index, SpatialElement triggerSE, SpatialElement moverSE, Map<String, List<String>> optionalRoleElements) {
        String relationIdentifier = "mvl"+index;
        
        String fromPart = "fromID=\""+triggerSE.id+"\" fromText=\""+triggerSE.text+"\"";
        String toPart = "toID=\""+moverSE.id+"\" toText=\""+moverSE.text+"\"";
        String trigger_mover = "trigger=\""+triggerSE.id+"\" mover=\""+moverSE.id+"\"";
        
        if (optionalRoleElements == null)
            return "<MOVELINK id=\""+relationIdentifier+"\" "+fromPart+" "+toPart+" "+trigger_mover+" />";
        
        String optionalRolesPart = "";
        for (String optionalRole : optionalRoleElements.keySet()) {
            
            List<String> elements = optionalRoleElements.get(optionalRole);
            String elementsStr = "";
            for (String element : elements)
                elementsStr += elementsStr.equals("") ? element : ", "+element;
            
            String role = SpatialRelation.RELATION_ROLENAME.get(optionalRole);
            optionalRolesPart += optionalRolesPart.equals("") ? role+"=\""+elementsStr+"\"" :
                    " "+role+"=\""+elementsStr+"\"";
        }
        
        return "<MOVELINK id=\""+relationIdentifier+"\" "+fromPart+" "+toPart+" "+trigger_mover+" "+optionalRolesPart+" />";
    }
    
    /**
     * Iterates through the predictions by combining information from the various MOVELINK test and result files,
     * and gets a string of all the predicted MOVELINKs.
     * 
     * @return String of all predicted MOVELINKs in XML format.
     * @throws IOException 
     */
    public String getPredictedMovelinksString() throws IOException {        
        String[] test = FileUtils.readFileToString(new File("main\\data\\testMOVELINK.txt")).split("\\n");
        String[] result = FileUtils.readFileToString(new File("main\\data\\resultMOVELINK.txt")).split("\\n");
        
        Map<String, Map<Double, Map<String, List<String>>>> predictedTriggerMoversOptionalRoles = getPredictedTriggerMovers(test, result);
        
        for (String relation : SpatialRelation.RELATIONS) {
            
            if (relation.equals("LINK") || relation.equals("MOVELINK"))
                continue;
            
            test = FileUtils.readFileToString(new File("main\\data\\test"+relation+".txt")).split("\\n");
            result = FileUtils.readFileToString(new File("main\\data\\result"+relation+".txt")).split("\\n");
            
            predictedTriggerMoversOptionalRoles = getPredictedOptionalRoles(test, result, relation, predictedTriggerMoversOptionalRoles);
        }
        
        Map<String, Map<String, List<String>>> movelinks = getMovelinksMap(predictedTriggerMoversOptionalRoles);
        
        return getMovelinksString(movelinks);
    }
    
    /**
     * Iterates through the predictions by combining information from the test and the result files, 
     * and gets a string of all the predicted LINKs.
     * 
     * @param test is the test data for classification.
     * @param result is the result from classification of test data.
     * @return String of all predicted LINKs in XML format.
     */
    public String getPredictedLinksString(String[] test, String[] result) {
        String outputStr = "";
        Map<String, Integer> typeMaxIntID = new HashMap<>();
        for (int i = 0; i < result.length; i++) {
            if (result[i].trim().equals(""))
                continue;
            double r = Double.parseDouble(result[i].trim());
            if (r < 0.0)
                continue;
            String[] tripletIDs = test[i].split("#")[1].trim().split("\\-");
            
            String trajector = tripletIDs[tripletIDs.length-3];            
            String landmark = tripletIDs[tripletIDs.length-2];
            String trigger = tripletIDs[tripletIDs.length-1];            
            
            SpatialElement trajectorSE = trajector.equals("null") ? null : idSpatialElement.get(trajector);
            SpatialElement landmarkSE = landmark.equals("null") ? null : idSpatialElement.get(landmark);
            SpatialElement triggerSE = trigger.equals("null") ? null : idSpatialElement.get(trigger);
            
            String relationType = (triggerSE == null || triggerSE.semantic_type.equals("") || triggerSE.semantic_type.equals("TOPOLOGICAL")) ? "QSLINK" : "OLINK";
            int index = typeMaxIntID.containsKey(relationType) ? typeMaxIntID.get(relationType) : 0;

            outputStr += getLinkString(relationType, index, trajectorSE, landmarkSE, triggerSE)+"\n";
            index++;
            typeMaxIntID.put(relationType, index);
            
            if (triggerSE != null && triggerSE.semantic_type.equals("DIR_TOP")) {
                relationType = "QSLINK";
                index = typeMaxIntID.containsKey(relationType) ? typeMaxIntID.get(relationType) : 0;
                outputStr += getLinkString(relationType, index, trajectorSE, landmarkSE, triggerSE)+"\n";
                index++;
                typeMaxIntID.put(relationType, index);                
            }
        } 
        return outputStr;
    }
    
    /**
     * Gets a single predicted LINK string.
     * 
     * @param relationType whether it is a QSLINK or OLINK.
     * @param index for the relation's unique identifier.
     * @param trajectorSE is the trajector participant in LINK.
     * @param landmarkSE is the landmark participant in LINK.
     * @param triggerSE is the trigger participant in LINK.
     * @return String of the predicted LINK in XML format.
     */
    public String getLinkString(String relationType, int index, SpatialElement trajectorSE, SpatialElement landmarkSE, SpatialElement triggerSE) {
        String relationPrefix = SpatialRelation.RELATION_PREFIX_MAP.get(relationType);

        String fromPart = trajectorSE == null ? "fromID=\"\" fromText=\"\"" : "fromID=\""+trajectorSE.id+"\" fromText=\""+trajectorSE.text+"\"";
        String toPart = landmarkSE == null ? "toID=\"\" toText=\"\"" : "toID=\""+landmarkSE.id+"\" toText=\""+landmarkSE.text+"\"";
        String trajector = trajectorSE == null ? "trajector=\"\"" : "trajector=\""+trajectorSE.id+"\"";
        String landmark = landmarkSE == null ? "landmark=\"\"" : "landmark=\""+landmarkSE.id+"\"";
        String trigger = triggerSE == null ? "trigger=\"\"" : "trigger=\""+triggerSE.id+"\"";        
        
        return "<"+relationType+" id=\""+relationPrefix+index+"\" "+fromPart+" "+toPart+" "+trajector+" "+landmark+" "+trigger+" />";
    }
    
}
