/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.java.spatialrelex.markup.Doc;
import main.java.spatialrelex.markup.SpatialRelation;
import main.java.spatialrelex.util.ExternalCommand;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Jenny D'Souza
 */
public class Evaluator {
    
    public static final List<String> C_VALUES = Arrays.asList("0.01", "0.1", "1.0", "10.0", "50.0", "100.0", "500.0", "1000.0", "5000.0", "10000.0");    
    public static final List<String> COST_VALUES = Arrays.asList("0.01", "0.1", "1.0", "2.0", "4.0", "6.0", "8.0", "10.0", "20.0", "40.0");    
    public static final List<String> T_VALUES = Arrays.asList(/*"0.0",*/ "0.2", "0.4", "0.6", "0.8", "1.0", "1.2", "1.4", "1.6", "1.8", "2.0");
    
    public static Map<String, String> relationBestC;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("LINK", "0.1");
        aMap.put("MOVELINK", "0.01");
        aMap.put("SOURCE", "0.01");
        aMap.put("MIDPOINT", "1.0");
        aMap.put("GOAL", "0.1");
        aMap.put("LANDMARK_MOVELINK", "0.01");
        aMap.put("PATH", "1.0");
        aMap.put("MOTION_SIGNAL", "0.1");
        relationBestC = aMap;
    }     
    public static Map<String, String> relationBestCost = new HashMap<>();
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("LINK", "6.0");
        aMap.put("MOVELINK", "1.0");
        aMap.put("SOURCE", "6.0");
        aMap.put("MIDPOINT", "0.1");
        aMap.put("GOAL", "1.0");
        aMap.put("LANDMARK_MOVELINK", "20.0");
        aMap.put("PATH", "1.0");
        aMap.put("MOTION_SIGNAL", "1.0");
        relationBestCost = aMap;
    }        
    public static Map<String, String> relationBestT = new HashMap<>();
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("LINK", "0.0");
        aMap.put("MOVELINK", "0.0");
        aMap.put("SOURCE", "0.2");
        aMap.put("MIDPOINT", "0.2");
        aMap.put("GOAL", "0.0");
        aMap.put("LANDMARK_MOVELINK", "0.0");
        aMap.put("PATH", "0.0");
        aMap.put("MOTION_SIGNAL", "0.2");
        relationBestT = aMap;
    }  
        
    /**
     * gets f-score by standard formula.
     * 
     * @param recall
     * @param precision
     * @return 
     */
    public static double getFscore(double recall, double precision) {
        return (precision == 0 || recall == 0) ? 0.0 : (2*precision*recall)/(precision+recall);
    }
    
    /**
     * gets precision by standard formula.
     * 
     * @param tp
     * @param fp
     * @return 
     */
    public static double getPrecision(double tp, double fp) {
        return tp == 0.0 ? 0.0 : tp/(tp+fp);
    }
    
    /**
     * gets recall by standard formula.
     * 
     * @param tp
     * @param total
     * @return 
     */
    public static double getRecall(double tp, double total) {
        return tp == 0.0 ? 0.0 : tp/total;
    }    
    
    public static double[] getTpFp(String[] test, String[] result, List<String> movelinks) {
        double[] tpFp = new double[2];
        
        for (int i = 0; i < test.length; i++) {
            double r = Double.parseDouble(result[i].trim());
            String[] testTokens = test[i].trim().split("\\s");
            double t = Double.parseDouble(testTokens[0]);
            
            String element = testTokens[testTokens.length-1].substring(0, testTokens[testTokens.length-1].lastIndexOf("-"));

            if (movelinks.contains(element)) {
                if (t == 1 && r >= 0.0)
                    tpFp[0]++;
                else if (r >= 0.0)
                    tpFp[1]++;
            }
        }
        
        return tpFp;
    }
    
    /**
     * 
     * @param test
     * @param result
     * @return 
     */
    public static List<String> getExtractedElements(String[] test, String[] result) {
        List<String> trueElements = new ArrayList<>();        
        for (int i = 0; i < test.length; i++) {
            double r = Double.parseDouble(result[i].trim());
            String[] testTokens = test[i].trim().split("\\s");
            String element = testTokens[testTokens.length-1].replace("-null", "");
            
            if (r >= 0.0 && !trueElements.contains(element))
                trueElements.add(element);
        }        
        return trueElements;
    }    
    
    /**
     * 
     * @param test
     * @param result
     * @return 
     */
    public static double[] getTpFp(String[] test, String[] result) {
        double[] tpFp = new double[2];
        
        for (int i = 0; i < test.length; i++) {
            double r = Double.parseDouble(result[i].trim());
            double t = Double.parseDouble(test[i].trim().split("\\s")[0]);
            if (t == 1 && r >= 0.0) 
                tpFp[0]++;
            else if (r >= 0.0)
                tpFp[1]++;            
        }
        
        return tpFp;
    }
    
    /**
     * gets the total number of relations in data.
     * 
     * @param data
     * @return 
     */
    public static double getTotal(String[] data) {
        double total = 0.0;
        for (String line : data) {
            line = line.trim();
            String[] lineTokens = line.split("\\s+");
            if (lineTokens[0].equals("1"))
                total++;
        }
        return total;        
    }        
    
    /**
     * Develops the optimal model based on user-provided training and development data.
     * Tries all combinations of C_VALUES and N-best to find the combination that maximizes recall.
     * 
     * @param relation
     * @param train
     * @param model
     * @param test
     * @param result
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void develop(String relation, String train, String model, String test, String result) throws FileNotFoundException, IOException {
        double bestFscore = 0.0;    
        double r = 0.0;
        double p = 0.0;
                
        //stores the total entities annotated in development data
        String[] data = FileUtils.readFileToString(new File(test)).split("\\n");
        double total = getTotal(data);
        
        //Optional roles for MOVELINK are extracted only for trigger-mover pairs that were already extracted
        List<String> movelinks = new ArrayList<>();
        if (!relation.equals("LINK") && !relation.equals("MOVELINK")) {
            String[] testArr = FileUtils.readFileToString(new File("main\\data\\testMOVELINK.txt")).split("\\n");
            String[] resultArr = FileUtils.readFileToString(new File("main\\data\\resultMOVELINK.txt")).split("\\n");
            movelinks = getExtractedElements(testArr, resultArr);
        }
                
        for (String c_value : C_VALUES) {            
            for (String cost_value : COST_VALUES) {
                for (String t_value : T_VALUES) {                       

                    //train a model
                    String command = Main.SVM_DIR+"\\svm_learn.exe -c "+c_value+" -j "+cost_value+" -C + -T "+t_value+" "+train+" "+model;
                    ExternalCommand.run(command); 
                    //test the trained model
                    command = Main.SVM_DIR+"\\svm_classify.exe "+test+" "+model+" "+result;
                    ExternalCommand.run(command);                 

                    //compute tp, fp, recall, precision and f-score of result from test
                    double[] tpFp = relation.equals("LINK") || relation.equals("MOVELINK") ? 
                            getTpFp(data, FileUtils.readFileToString(new File(result)).split("\\n")) :
                            getTpFp(data, FileUtils.readFileToString(new File(result)).split("\\n"), movelinks);

                    double tempRecall = getRecall(tpFp[0], total);
                    double tempPrecision = getPrecision(tpFp[0], tpFp[1]);
                    double tempFscore = getFscore(tempRecall, tempPrecision);

                    //if new score is better than previous best score, then
                    //update previous best score to new score and 
                    //set c and cost to current model parameter combination. 
                    if (tempFscore > bestFscore ||  //if new fscore is better than previous fscore
                            (tempFscore == bestFscore && tempRecall > r)) { //if new fscore is equal to previous fscore and new recall is better than previous recall
                        bestFscore = tempFscore;
                        r = tempRecall; p = tempPrecision;

                        relationBestC.put(relation, c_value);
                        relationBestCost.put(relation, cost_value);
                        relationBestT.put(relation, t_value);
                    }
                }
            }
        }       
        
        String bestC = relationBestC.get(relation);
        String bestCost = relationBestCost.get(relation);
        String bestT = relationBestT.get(relation);
                
        //get relation extraction results on best parameter combination
        //train a model
        String command = Main.SVM_DIR+"\\svm_learn.exe -c "+bestC+" -j "+bestCost+" -C + -T "+bestT+" "+train+" "+model;
        ExternalCommand.run(command); 
        //test the trained model
        command = Main.SVM_DIR+"\\svm_classify.exe "+test+" "+model+" "+result;
        ExternalCommand.run(command);                             
        
        Main.log.write(("for "+relation+" on DEV data\n").getBytes());
        Main.log.write(("best c-value: "+bestC+"; best cost: "+bestCost+"; best t-value: "+bestT+"\n").getBytes());
        Main.log.write(("r: "+r+"; p: "+p+"; f: "+bestFscore+"\n\n").getBytes());
    }     
    
    public static void train(String c_value, String cost_value, String t_value, String train, String model) {
        String command = Main.SVM_DIR+"\\svm_learn.exe -c "+c_value+" -j "+cost_value+" "+train+" "+model;
        ExternalCommand.run(command);         
    }
    
    public static void classify(String test, String model, String result) {
        String command = Main.SVM_DIR+"\\svm_classify.exe "+test+" "+model+" "+result;
        ExternalCommand.run(command);                         
    }
    
    public static void applyBestModel(String relation, String train, String model, String test, String result) {
        train(relationBestC.get(relation), relationBestCost.get(relation), relationBestT.get(relation), train, model);
        classify(test, model, result);
    }
    
    /**
     * 
     * @param test
     * @param result
     * @param fileTriggerMoverRoleOtherElement
     * @param roleName
     * @return 
     */
    public static Map<String, Map<String, List<String>>> getMovelinkSubpart(String[] test, String[] result, 
            Map<String, Map<String, List<String>>> fileTriggerMoverRoleOtherElement, String roleName) {
        
        for (int i = 0; i < test.length; i++) {
            double r = Double.parseDouble(result[i].trim());
            String[] testTokens = test[i].trim().split("\\s");
            
            String key = testTokens[testTokens.length-1];
            String element = key.substring(0, key.lastIndexOf("-"));
            String roleElement = key.substring(key.lastIndexOf("-")+1);
                        
            if (r >= 0.0 && fileTriggerMoverRoleOtherElement.containsKey(element)) {
                Map<String, List<String>> roleOtherElement = fileTriggerMoverRoleOtherElement.get(element);
                
                if (roleOtherElement == null)
                    fileTriggerMoverRoleOtherElement.put(element, roleOtherElement = new HashMap<>());
                
                List<String> elements = roleOtherElement.get(roleName);
                if (elements == null)
                    roleOtherElement.put(roleName, elements = new ArrayList<>());
                if (!elements.contains(roleElement))
                    elements.add(roleElement);                
            }                        
        }
        
        return fileTriggerMoverRoleOtherElement;        
    }
    
    /**
     * 
     * @return 
     * @throws java.io.IOException
     */
    public static Map<String, Map<String, List<String>>> combineMovelinkSubparts() throws IOException {
        Map<String, Map<String, List<String>>> fileTriggerMoverRoleOtherElement = new HashMap<>(SpatialRelation.fileTriggerMoverRoleOtherElement);
        
        for (String relation : SpatialRelation.RELATION_ROLENAME.keySet()) {            
            if (SpatialRelation.ORDERED_SIEVES.contains(relation))
                continue;
            
            String[] testArr = FileUtils.readFileToString(new File("main\\data\\test"+relation+".txt")).split("\\n");
            String[] resultArr = FileUtils.readFileToString(new File("main\\data\\result"+relation+".txt")).split("\\n");
            
            fileTriggerMoverRoleOtherElement = getMovelinkSubpart(testArr, resultArr, fileTriggerMoverRoleOtherElement, relation);
        }
        
        return fileTriggerMoverRoleOtherElement;
    }
    
    /**
     * 
     * @param fileDocumentObject 
     * @throws java.io.IOException 
     */    
    public static void setSievesOrder(Map<String, Doc> fileDocumentObject) throws IOException {
        
        Map<String, Map<String, List<String>>> extractedMovelinks = combineMovelinkSubparts();
        
        String bestSieve = "";
        double bestSievePrecision = 0.0;
        
        //repeat for all remaining optional elements        
        for (String relation : SpatialRelation.RELATION_ROLENAME.keySet()) {
            
            if (SpatialRelation.ORDERED_SIEVES.contains(relation))
                continue;
            
            double tp = 0.0;
            double fp = 0.0;

            for (String file : fileDocumentObject.keySet()) {
                Doc document = fileDocumentObject.get(file);

                Map<String, Map<String, List<String>>> goldMovelinks = document.triggerMoverRoleOtherElements;

                for (String triggerMover : goldMovelinks.keySet()) {

                    Map<String, List<String>> extractedRoleOtherElements = extractedMovelinks.get(triggerMover);
                    if (extractedRoleOtherElements == null)
                        continue;

                    Map<String, List<String>> roleOtherElements = goldMovelinks.get(triggerMover);                    
                    if (roleOtherElements != null) {
                        for (String role : roleOtherElements.keySet()) {
                            List<String> otherElements = roleOtherElements.get(role);

                            if (!extractedRoleOtherElements.containsKey(role))
                                continue;

                            List<String> extractedOtherElements = extractedRoleOtherElements.get(role);

                            List<String> tempList = new ArrayList<>(otherElements);
                            tempList.retainAll(extractedOtherElements);                        
                            tp += tempList.size();

                            tempList = new ArrayList<>(extractedOtherElements);
                            tempList.removeAll(otherElements);
                            fp += tempList.size();
                        }
                        continue;
                    }

                    for (String role : extractedRoleOtherElements.keySet()) {
                        List<String> extractedOtherElements = extractedRoleOtherElements.get(role);
                        fp += extractedOtherElements.size();
                    }
                }            
            }
            
            double precision = getPrecision(tp, fp);
            if (precision > bestSievePrecision) {
                bestSieve = relation;
                bestSievePrecision = precision;
            }
        }
        
        SpatialRelation.ORDERED_SIEVES.add(bestSieve);
        addChosenSieveResults(extractedMovelinks, bestSieve);
    }
    
    public static void addChosenSieveResults(Map<String, Map<String, List<String>>> extractedMovelinks, String sieve) {        
        for (String fileTriggerMover : SpatialRelation.fileTriggerMoverRoleOtherElement.keySet()) {
            
            Map<String, List<String>> roleOtherElement = SpatialRelation.fileTriggerMoverRoleOtherElement.get(fileTriggerMover);
            if (roleOtherElement == null)
                SpatialRelation.fileTriggerMoverRoleOtherElement.put(fileTriggerMover, roleOtherElement = new HashMap<>());
            
            Map<String, List<String>> extractedRoleOtherElement = extractedMovelinks.get(fileTriggerMover);
            if (extractedRoleOtherElement != null && extractedRoleOtherElement.containsKey(sieve)) {
                roleOtherElement.put(sieve, extractedRoleOtherElement.get(sieve));
            }            
        }
        
    }
    
}
