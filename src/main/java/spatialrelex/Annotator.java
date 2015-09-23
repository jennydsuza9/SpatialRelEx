/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import main.java.spatialrelex.markup.Doc;
import main.java.spatialrelex.markup.SpatialRelation;
import org.xml.sax.SAXException;

/**
 *
 * @author Jenny D'Souza
 */
public class Annotator {
    
    private final Map<String, Doc> filesDocumentObjects;   
    
    public Annotator(Map<String, Doc> filesDocumentObjects) {
        this.filesDocumentObjects = filesDocumentObjects;
    }       
    
    /**
     * Marks up spatial relations in testFiles.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException 
     */
    public void markUpSpatialRelations() throws IOException, SAXException, ParserConfigurationException, UnsupportedEncodingException, InterruptedException {
        //all annotated files will be written to output directory
        File outputDir = new File("output");
        if (!outputDir.exists())
            outputDir.mkdirs();
        
        for (String fileStr : filesDocumentObjects.keySet()) {
            
            System.out.println("annotating: "+fileStr);            
            
            //parses the spatial entity and relation annotations from input xml files
            Doc document = filesDocumentObjects.get(fileStr);

            for (String relation : SpatialRelation.RELATIONS) {                
                Map<String, SpatialRelation> tripletsSpatialRelations = document.relationTripletsSpatialRelations.get(relation);
                if (tripletsSpatialRelations == null)
                    continue;                
                
                System.out.println("relation: "+relation);
                
                String test = "main\\data\\test"+relation+".txt";                
                writeUnlabelledSVMData(fileStr, tripletsSpatialRelations, new FileOutputStream(test));
                                
                String model = Main.trainDir == null ? Main.RESOURCES_DIR+"\\models\\model"+relation+".txt" : "main\\data\\model"+relation+".txt";
                String result = "main\\data\\result"+relation+".txt";
                Evaluator.classify(test, model, result);
            }
            
            //writes the annotations to output            
            String outputFileName = outputDir.toString()+"\\"+new File(fileStr).getName();
            document.writeRelationAnnotations(outputFileName);
        }
    }
    
    public void writeUnlabelledSVMData(String file, Map<String, SpatialRelation> tripletsSpatialRelations, FileOutputStream output) throws IOException {
        
        System.out.println("file: "+file);
        
        for (String triplet : tripletsSpatialRelations.keySet()) {
            SpatialRelation sr = tripletsSpatialRelations.get(triplet);
            output.write((sr.classValue+" "+sr.featureStr+" # "+file+"-"+triplet+"\n").getBytes());
        }
        
        //System.exit(1);
    }
    
    public static void storeUnLabelledSVMData(Map<String, Doc> fileDocumentObject, String relation) {
        for (String file : fileDocumentObject.keySet()) {
            
            Doc document = fileDocumentObject.get(file);
            
            Map<String, SpatialRelation> tripletsSpatialRelations = document.relationTripletsSpatialRelations.get(relation);
            if (tripletsSpatialRelations == null)
                continue;
            
            System.out.println("storing unlabelled data for: "+file);
            
            for (String triplet : tripletsSpatialRelations.keySet()) {
                SpatialRelation sr = tripletsSpatialRelations.get(triplet);
                
                sr.featureStr = sr.features.toString(sr).replaceAll("\\s+", " ").trim();
            }  
        }
    }    
    
}
