/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.markup;

import edu.stanford.nlp.ling.IndexedWord;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import main.java.spatialrelex.ling.GeneralInquirer;
import main.java.spatialrelex.ling.VerbNet;
import main.java.spatialrelex.ling.WordNet;
import org.w3c.dom.Element;

/**
 *
 * @author Jenny D'Souza
 */
public class SpatialElement {
    
    public final static List<String> TYPES = Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", 
            "NONMOTION_EVENT", "MOTION", "SPATIAL_SIGNAL", "MOTION_SIGNAL");    
    
    public List<String> type = new ArrayList<>();   
    public String semantic_type;    
    
    public String id;
    public int start;
    public int end;
    public int startToken;
    public int endToken;
    public String text; 
    public String role;
    
    public List<String> roles = new ArrayList<>();    
    
    //features from external sources
    public String lemmaText;    
    public List<String> hypernyms = new ArrayList<>();
    public List<String> synsets = new ArrayList<>();
    public List<String> srls = new ArrayList<>();
    public List<String> generalInquirerCategories = new ArrayList<>();
    public List<String> verbNetClasses = new ArrayList<>();
        
    /**
     * Spatial element constructor.
     * 
     * @param type is the ISO-Space type of the spatial element.
     * @param spatialEntityEl is the spatial element xml annotation object.
     * @throws UnsupportedEncodingException 
     */
    public SpatialElement(String type, Element spatialEntityEl) throws UnsupportedEncodingException {
        this.start = Integer.parseInt(spatialEntityEl.getAttribute("start"));
        this.end = Integer.parseInt(spatialEntityEl.getAttribute("end"));
        this.id = spatialEntityEl.getAttribute("id");        
        this.text = new String(spatialEntityEl.getAttribute("text").getBytes("US-ASCII"), "US-ASCII");
        if (!this.type.contains(type))
            this.type.add(type);        
        this.semantic_type = spatialEntityEl.getAttribute("semantic_type");
    }    
    
    public SpatialElement(String role, int start, int end, int startToken, int endToken) {
        this.role = role;
        this.start = start;
        this.end = end;
        this.startToken = startToken;
        this.endToken = endToken;
    }
    
    public static SpatialElement modifyOffsets(SpatialElement se, Doc document) {        
        if (document.startOffsetIndexedWord.containsKey(se.start))
            return se;
        
        int tempParserOffset = se.start-1;
        while (tempParserOffset > 0 && tempParserOffset > se.start-5) {
            if (document.startOffsetIndexedWord.containsKey(tempParserOffset)) {
                
                String parserWord = document.startOffsetIndexedWord.get(tempParserOffset).value();
                String seWord = se.text.split("\\s+")[0];
                
                if ((parserWord.length() > 1 && seWord.matches("[^a-zA-Z0-9]{0,2}"+parserWord+"[^a-zA-Z0-9]{0,2}")) || 
                        parserWord.matches("[^a-zA-Z0-9]{0,2}"+seWord+"[^a-zA-Z0-9]{0,2}") || parserWord.equals(seWord)) {
                    int difference = tempParserOffset - se.start;
                    se.start = tempParserOffset;
                    se.end = se.end + difference;
                    return se;                
                }
            }
            tempParserOffset--;
        }
                
        tempParserOffset = se.start+1;
        while (tempParserOffset < se.start+5) {
            if (document.startOffsetIndexedWord.containsKey(tempParserOffset)) {

                String parserWord = document.startOffsetIndexedWord.get(tempParserOffset).value();
                String seWord = se.text.split("\\s+")[0];
                
                if ((parserWord.length() > 1 && seWord.matches("[^a-zA-Z0-9]{0,2}"+parserWord+"[^a-zA-Z0-9]{0,2}")) || 
                        parserWord.matches("[^a-zA-Z0-9]{0,2}"+seWord+"[^a-zA-Z0-9]{0,2}") || parserWord.equals(seWord)) {   
                    int difference = tempParserOffset - se.start;
                    se.start = tempParserOffset;
                    se.end = se.end + difference;                    
                    return se;
                }
                
            }
            tempParserOffset++;
        }      
        
        return null;
    }
        
    public static SpatialElement setSpatialElementFeatures(Doc document, SpatialElement se) {
        IndexedWord iw = document.startOffsetIndexedWord.get(se.start);
        se.lemmaText = iw.lemma();        
        se.startToken = iw.index();
        se.endToken = iw.index();
        int i = se.start+1;
        while (i < se.end) {
            if (!document.startOffsetIndexedWord.containsKey(i)) {
                i++;
                continue;
            }
            
            iw = document.startOffsetIndexedWord.get(i);
            se.endToken = iw.index();
            se.lemmaText += " "+iw.lemma();
            if (iw.tag().contains("NN")) {
                se.generalInquirerCategories = GeneralInquirer.getGeneralInquirerCategories(se.generalInquirerCategories, iw.value().toLowerCase());
                se = WordNet.setWordNetSynsetsAndHypernyms(se, iw.tag(), "NN");
            }
            else if (iw.tag().contains("VB")) {
                se.verbNetClasses = VerbNet.getVerbNetClasses(se.verbNetClasses, iw.value().toLowerCase());
                se = WordNet.setWordNetSynsetsAndHypernyms(se, iw.tag(), "VB");
            }
            List<String> tokenSRLs = document.startOffsetSRLRoles.get(i);
            i++;
            
            if (tokenSRLs == null)
                continue;
            for (String tokenSRL : tokenSRLs) {
                if (se.srls.contains(tokenSRL))
                    continue;
                se.srls.add(tokenSRL);
            }
        }
                
        return se;
    }
    
    public static SpatialElement setRoles(SpatialElement se, String role) {
        if (!se.roles.contains(role))
            se.roles.add(role);
        return se;
    }    
    
}
