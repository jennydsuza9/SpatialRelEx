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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import main.java.spatialrelex.ling.Features;
import main.java.spatialrelex.markup.Doc;
import main.java.spatialrelex.markup.SpatialRelation;
import org.xml.sax.SAXException;

/**
 *
 * @author Jenny D'Souza
 */
public class Trainer {
    
    private final Collection<File> files;
    public Map<String, Doc> fileDocumentObject = new HashMap<>();
    
    public Trainer(Collection<File> files) {
        this.files = files;
    }    
    
    public void generateTriplets() throws SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException {
        for (File file : files) {
            
            String fileStr = file.toString();
            System.out.println("generating instances: "+fileStr);
            
            //parses the spatial entity and relation annotations from input xml files
            Doc document = new Doc();
            document.parseXMLDataFile(fileStr);
            
            //generates negative LINK training instances
            document.generateNonLinks();    
            //generates negative MOVELINK training instances
            document.generateNonMovelinks();    
            
            //datastructure for storing the file's relation instances
            fileDocumentObject.put(fileStr, document);
        }
    }
    
    /**
     * 
     * @param relation
     * @throws IOException 
     */
    public void setFeatures(String relation) throws IOException {
        for (String file : fileDocumentObject.keySet()) {
            
            Doc document = fileDocumentObject.get(file);
            
            Map<String, SpatialRelation> tripletsSpatialRelations = document.relationTripletsSpatialRelations.get(relation);
            if (tripletsSpatialRelations == null)
                continue;
            
            for (String triplet : tripletsSpatialRelations.keySet()) {
                SpatialRelation sr = tripletsSpatialRelations.get(triplet);
                
                sr.features = new Features();
                sr.features.setUniFV(document, sr);
                
                if (relation.equals("LINK")) {
                    if ((sr.se1 != null && !sr.features.parseStringFeature.contains("trajector")) ||
                            (sr.se2 != null && !sr.features.parseStringFeature.contains("landmark_link")) ||
                            (sr.se3 != null && !sr.features.parseStringFeature.contains("trigger_link"))) {
                        System.out.println(triplet);
                        System.out.println(sr.features.parseStringFeature);
                        System.exit(1);
                    }
                }
                
                if (Main.train)
                    Features.setUniFeatureLists(sr);
            }            
        }
    }
    
    /**
     * 
     * 
     * @param output
     * @param relation
     * @throws IOException 
     */
    public void writeLabelledSVMData(FileOutputStream output, String relation) throws IOException {
        for (String file : fileDocumentObject.keySet()) {
            
            Doc document = fileDocumentObject.get(file);
            
            Map<String, SpatialRelation> tripletsSpatialRelations = document.relationTripletsSpatialRelations.get(relation);
            if (tripletsSpatialRelations == null)
                continue;
            
            System.out.println("writing labelled data for: "+file);
            
            for (String triplet : tripletsSpatialRelations.keySet()) {
                SpatialRelation sr = tripletsSpatialRelations.get(triplet);
                
                String featureStr = sr.features.toString(sr).replaceAll("\\s+", " ").trim();
                
                output.write((sr.classValue+" "+featureStr+" # "+file+"-"+triplet+"\n").getBytes());   
                
            }  
            
            //document.relationTripletsSpatialRelations.remove(relation);
        }
    }
        
    
}
