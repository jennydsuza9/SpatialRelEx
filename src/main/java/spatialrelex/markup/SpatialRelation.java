/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.markup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import main.java.spatialrelex.ling.Features;
import org.w3c.dom.Element;

/**
 *
 * @author Jenny D'Souza
 */
public class SpatialRelation {
    
    public final static List<String> RELATIONS = Arrays.asList("LINK", "MOVELINK", 
            "SOURCE", "MIDPOINT", "GOAL", "LANDMARK_MOVELINK", "PATH", "MOTION_SIGNAL");
        
    public static final Map<String, String> RELATION_ROLENAME;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("SOURCE", "source");
        aMap.put("MIDPOINT", "midPoint");
        aMap.put("GOAL", "goal");
        aMap.put("LANDMARK_MOVELINK", "landmark");
        aMap.put("PATH", "pathID");
        aMap.put("MOTION_SIGNAL", "motion_signalID");
        RELATION_ROLENAME = Collections.unmodifiableMap(aMap);
    }    
    
    public static List<String> ORDERED_SIEVES = new LinkedList<>();    
    public static Map<String, Map<String, List<String>>> fileTriggerMoverRoleOtherElement = new HashMap<>();
    
    public final static List<String> TYPES = Arrays.asList("QSLINK", "OLINK", "MOVELINK");
    
    public final static String TRIGGER_SE_TYPE = "SPATIAL_SIGNAL";
    public final static List<String> TRAJ_LAND_SE_TYPES = Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT", "MOTION");
    
    public final static String TRIGGER_MOVELINK_SE_TYPE = "MOTION";
    public final static List<String> MVR_SRC_GOAL_LNDMRK_SE_TYPES = Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT");
    public final static List<String> MIDPT_SE_TYPES = Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY");
    public final static String PATH_SE_TYPE = "PATH";
    public final static String MS_SE_TYPE = "MOTION_SIGNAL";
    
    public static final Map<String, String> RELATION_PREFIX_MAP;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("QSLINK", "qsl");
        aMap.put("OLINK", "ol");
        aMap.put("MOVELINK", "mvl");
        RELATION_PREFIX_MAP = Collections.unmodifiableMap(aMap);
    }
       
    public String type;    
    public String classValue;  
    public SpatialElement se1;
    public SpatialElement se2;
    public SpatialElement se3;
    public String idString;
    public String rolesString;
    public Features features;
    public String featureStr = "";
    
    public SpatialRelation(String type, String classValue) {
        this.type = type;
        this.classValue = classValue;
    }
    
    public static void setOrderedSievesList() {
        SpatialRelation.ORDERED_SIEVES.add("PATH");
        SpatialRelation.ORDERED_SIEVES.add("GOAL");
        SpatialRelation.ORDERED_SIEVES.add("LANDMARK_MOVELINK");
        SpatialRelation.ORDERED_SIEVES.add("SOURCE");
        SpatialRelation.ORDERED_SIEVES.add("MOTION_SIGNAL");
        SpatialRelation.ORDERED_SIEVES.add("MIDPOINT");
    }
    
    /**
     * Processes relation from gold annotations.
     * 
     * @param document with the relation
     * @param spatialRelationEl is the xml object of the relation.
     * @throws IOException 
     */
    public void processRelation(Doc document, Element spatialRelationEl) throws IOException {
        if (type.equals("LINK")) 
            storeLink(document, spatialRelationEl);
        else if (type.equals("MOVELINK"))
            storeMovelink(document, spatialRelationEl);        
    }
    
    /**
     * Stores a Link (QSLINK\OLINK) from gold annotations.
     * 
     * @param document with the Link
     * @param spatialRelationEl is the XML object of the Link with its various attributes
     * @throws IOException 
     */
    public void storeLink(Doc document, Element spatialRelationEl) throws IOException {
        String trigger = spatialRelationEl.getAttribute("trigger");
        String landmark = spatialRelationEl.getAttribute("landmark");
        String trajector = spatialRelationEl.getAttribute("trajector");
                        
        SpatialElement triggerSE = !document.idSpatialElement.containsKey(trigger) ? null : document.idSpatialElement.get(trigger);
        SpatialElement landmarkSE = !document.idSpatialElement.containsKey(landmark) ? null : document.idSpatialElement.get(landmark);
        SpatialElement trajectorSE = !document.idSpatialElement.containsKey(trajector) ? null : document.idSpatialElement.get(trajector);
        
        //these roles are obtained from the annotations
        //initializes the list of roles that the element is in or is predicted to be in
        if (trajectorSE != null) 
            trajectorSE = SpatialElement.setRoles(trajectorSE, "trajector");
        if (landmarkSE != null) 
            landmarkSE = SpatialElement.setRoles(landmarkSE, "landmark_link");
        if (triggerSE != null) 
            triggerSE = SpatialElement.setRoles(triggerSE, "trigger_link");
        
        //a check that all spatial elements are in the same sentence
        if (!sameSentence(document, trajectorSE, landmarkSE, triggerSE))
            return;
            
        //stores the relation
        storeRelation(document, this, trajectorSE, "trajector", landmarkSE, "landmark_link", triggerSE, "trigger_link");
    }    
     
    /**
     * Called from storeMovelink to get the list of midpoint, path, or 
     * motion-signal ids from the XML annotations.
     * 
     * @param attributeStr containing the midpoint, path, or motion-signal ids.
     * @return List of ids.
     */
    public List<String> getList(String attributeStr) {
        List<String> attributeValuesList = new ArrayList<>();
        if (!attributeStr.equals("")) {
            String[] attributeValues = attributeStr.replaceAll(", ", ",").split(",");
            for (String attributeValue : attributeValues)
                attributeValuesList.add(attributeValue);
        }
        return attributeValuesList;
    }    
    
    /**
     * Called form storeMovelink to get the list of midpoint, path, or
     * motion-signal ids from the XML annotations.
     * 
     * @param document with the spatial elements.
     * @param SEIds with the IDs of the spatial elements.
     * @return List of spatial elements.
     */
    public List<SpatialElement> getSpatialElements(Doc document, List<String> SEIds) {
        List<SpatialElement> SEs = new ArrayList<>();
        for (String SEId : SEIds) {
            if (document.idSpatialElement.containsKey(SEId))
                SEs.add(document.idSpatialElement.get(SEId));
        }
        return SEs;
    }
    
    /**
     * Stores a MOVELINK from gold annotations.
     * 
     * @param document with the MOVELINK
     * @param spatialRelationEl is the XML object of the MOVELINK with its various attributes.
     * @throws java.io.IOException
     */
    public void storeMovelink(Doc document, Element spatialRelationEl) throws IOException {
        String trigger = spatialRelationEl.getAttribute("trigger");
        String mover = spatialRelationEl.getAttribute("mover");
        String source = spatialRelationEl.getAttribute("source");
        List<String> midPoint = getList(spatialRelationEl.getAttribute("midPoint"));
        String goal = spatialRelationEl.getAttribute("goal");
        String landmark = spatialRelationEl.getAttribute("landmark");
        List<String> path = getList(spatialRelationEl.getAttribute("pathID"));
        List<String> motion_signal = getList(spatialRelationEl.getAttribute("motion_signalID"));
        
        SpatialElement triggerSE = !document.idSpatialElement.containsKey(trigger) ? null : document.idSpatialElement.get(trigger);
        SpatialElement moverSE = !document.idSpatialElement.containsKey(mover) ? null : document.idSpatialElement.get(mover);
        SpatialElement sourceSE = !document.idSpatialElement.containsKey(source) ? null : document.idSpatialElement.get(source);
        List<SpatialElement> midPointSEs = getSpatialElements(document, midPoint);        
        SpatialElement goalSE = !document.idSpatialElement.containsKey(goal) ? null : document.idSpatialElement.get(goal);        
        SpatialElement landmarkSE = !document.idSpatialElement.containsKey(landmark) ? null : document.idSpatialElement.get(landmark);
        List<SpatialElement> pathSEs = getSpatialElements(document, path);
        List<SpatialElement> motion_signalSEs = getSpatialElements(document, motion_signal);
        
        //these roles are obtained from the annotations
        //initializes the list of roles that the element is in or is predicted to be in
        if (triggerSE != null) 
            triggerSE = SpatialElement.setRoles(triggerSE, "trigger_movelink");
        if (moverSE != null) 
            moverSE = SpatialElement.setRoles(moverSE, "mover");
        if (sourceSE != null) 
            sourceSE = SpatialElement.setRoles(sourceSE, "source");
        if (!midPointSEs.isEmpty()) {
            for (SpatialElement midPointSE : midPointSEs) 
                midPointSE = SpatialElement.setRoles(midPointSE, "midPoint");
        }
        if (goalSE != null) 
            goalSE = SpatialElement.setRoles(goalSE, "goal");
        if (landmarkSE != null) 
            landmarkSE = SpatialElement.setRoles(landmarkSE, "landmark_movelink");
        if (!pathSEs.isEmpty()) {
            for (SpatialElement pathSE : pathSEs) 
                pathSE = SpatialElement.setRoles(pathSE, "path");
        }
        if (!motion_signalSEs.isEmpty()) {
            for (SpatialElement motion_signalSE : motion_signalSEs) {
                motion_signalSE = SpatialElement.setRoles(motion_signalSE, "motion_signal");
            }
        }
        
        //a check that trigger and mover are in the same sentence
        if (!sameSentence(document, triggerSE, moverSE, null))
            return;
            
        //stores the relation
        SpatialRelation sr = new SpatialRelation("MOVELINK", classValue);
        sr.storeRelation(document, sr, triggerSE, "trigger_movelink", moverSE, "mover", null, "");
        
        //stores core MOVELINK with remaining roles
        sr = new SpatialRelation("SOURCE", classValue);
        sr.storeMovelink(document, sr, triggerSE, moverSE, sourceSE, "source");
        for (SpatialElement midPointSE : midPointSEs) {
            sr = new SpatialRelation("MIDPOINT", classValue);
            sr.storeMovelink(document, sr, triggerSE, moverSE, midPointSE, "midPoint");
        }
        sr = new SpatialRelation("GOAL", classValue);
        sr.storeMovelink(document, sr, triggerSE, moverSE, goalSE, "goal");
        sr = new SpatialRelation("LANDMARK_MOVELINK", classValue);
        sr.storeMovelink(document, sr, triggerSE, moverSE, landmarkSE, "landmark_movelink");
        for (SpatialElement pathSE : pathSEs) {
            sr = new SpatialRelation("PATH", classValue);
            sr.storeMovelink(document, sr, triggerSE, moverSE, pathSE, "path");
        }
        for (SpatialElement motion_signalSE : motion_signalSEs) {
            sr = new SpatialRelation("MOTION_SIGNAL", classValue);
            sr.storeMovelink(document, sr, triggerSE, moverSE, motion_signalSE, "motion_signal");
        }
    }     
    
    /**
     * Stores MOVELINKs combining (trigger,mover) with optional participants to form triplets.
     * 
     * @param document with the MOVELINK.
     * @param sr
     * @param triggerSE is the trigger participant in the MOVELINK.
     * @param moverSE is the mover participant in the MOVELINK.
     * @param otherSE is the optional participant in the MOVELINK.
     * @param otherSERole is the role of the optional participant in the MOVELINK.
     * @throws IOException 
     */
    public void storeMovelink(Doc document, SpatialRelation sr, SpatialElement triggerSE, SpatialElement moverSE, SpatialElement otherSE, String otherSERole) throws IOException {
        if (otherSE == null)
            return;
        
        //a check that trigger, mover and other element are in the same sentence
        if (!sameSentence(document, triggerSE, moverSE, otherSE))
            return;
        
        //stores the relation
        storeRelation(document, sr, triggerSE, "trigger_movelink", moverSE, "mover", otherSE, otherSERole);
    }
    
    /**
     * Checks if the spatial elements are in the same sentence.
     * 
     * @param document with the spatial elements.
     * @param se1 is the first spatial element.
     * @param se2 is the second spatial element.
     * @param se3 is the third spatial element.
     * @return <true\> if spatial elements are in the same sentence, <false\> otherwise. 
     */
    public boolean sameSentence(Doc document, SpatialElement se1, SpatialElement se2, SpatialElement se3) {
        if ((se1 == null && se2 == null) || (se1 == null && se3 == null) || (se2 == null && se3 == null))
            return false;
        int s1 = se1 == null ? -1 : document.startOffsetIndexedWord.get(se1.start).sentIndex();
        int s2 = se2 == null ? -1 : document.startOffsetIndexedWord.get(se2.start).sentIndex();
        int s3 = se3 == null ? -1 : document.startOffsetIndexedWord.get(se3.start).sentIndex();
        if (s1 != -1 && s2 != -1 && s3 != -1 && (s1 != s2 || s2 != s3 || s1 != s3))
            return false;
        if (s2 != -1 && s3 != -1 && s2 != s3)
            return false;
        if (s1 != -1 && s2 != -1 && s1 != s2)
            return false;
        if (s1 != -1 && s3 != -1 && s1 != s3)
            return false;
        return true;
    }
    
    /**
     * Gets the ID string of the spatial elements in the relation.
     * 
     * @param se1 is the first relation participant.
     * @param se1Role is se1's role.
     * @param se2 is the second relation participant.
     * @param se2Role is se2's role.
     * @param se3 is the third relation participant.
     * @param se3Role is se3's role.
     * @return 
     */
    public String getRelationIdString(SpatialElement se1, String se1Role, SpatialElement se2, String se2Role, SpatialElement se3, String se3Role) {
        String idString = "";
        if (se1 != null && se2 != null && se3 != null) {
            se1.role = se1Role;
            se2.role = se2Role;
            se3.role = se3Role;
            idString = se1.id+"-"+se2.id+"-"+se3.id;
        }
        else if (se1 != null && se2 != null) {
            se1.role = se1Role;
            se2.role = se2Role;
            idString = se1.id+"-"+se2.id+"-null";
        }
        else if (se1 != null && se3 != null) {
            se1.role = se1Role;
            se3.role = se3Role;
            idString = se1.id+"-null-"+se3.id;
        }
        else if (se2 != null && se3 != null) {
            se2.role = se2Role;
            se3.role = se3Role;
            idString = "null-"+se2.id+"-"+se3.id;
        }
        return idString;
    }
    
    /**
     * Stores the given relation for processing.
     * 
     * @param document with the relation.
     * @param sr
     * @param se1 is the first relation participant.
     * @param se1Role is se1's role.
     * @param se2 is the second relation participant.
     * @param se2Role is se2's role.
     * @param se3 is the third relation participant.
     * @param se3Role is se3's role.
     * @throws IOException 
     */    
    public void storeRelation(Doc document, SpatialRelation sr, SpatialElement se1, String se1Role, SpatialElement se2, String se2Role, SpatialElement se3, String se3Role) throws IOException {        
        //gets the relation triplet id string
        //also sets each element with their posited role in this relation        
        sr.idString = getRelationIdString(se1, se1Role, se2, se2Role, se3, se3Role);
        //the idString is empty when two or more of the three spatial elements are null
        if (sr.idString.equals(""))
            return;
        
        if (sr.classValue.equals("1") && !sr.type.equals("LINK")) {
            Map<String, List<String>> roleOtherElements = document.triggerMoverRoleOtherElements.get(document.filename+"-"+se1.id+"-"+se2.id);
            if (roleOtherElements == null)
                document.triggerMoverRoleOtherElements.put(document.filename+"-"+se1.id+"-"+se2.id, roleOtherElements = new HashMap<>());
            if (se3 != null) {                
                String role = se3Role.toUpperCase();
                List<String> otherElements = roleOtherElements.get(role);
                if (otherElements == null)
                    roleOtherElements.put(role, otherElements = new ArrayList<>());
                if (!otherElements.contains(se3.id))
                    otherElements.add(se3.id);                
            }
        }
        
        Map<String, SpatialRelation> tripletsSpatialRelations = document.relationTripletsSpatialRelations.get(sr.type);
        if (tripletsSpatialRelations == null)
            document.relationTripletsSpatialRelations.put(sr.type, tripletsSpatialRelations = new HashMap<>());
        
        //for non (or unannotated) relations, a check is made to see if it
        //was added as a positive relation earlier, 
        //in which case this new relation is not stored.
        if (!sr.classValue.equals("1") && tripletsSpatialRelations.containsKey(this.idString))
            return;
        
        sr.se1 = se1;
        if (se1 != null)
            sr.rolesString = se1Role;
        sr.se2 = se2;
        if (se2 != null)
            sr.rolesString = sr.rolesString == null ? se2Role : sr.rolesString+"-"+se2Role;
        sr.se3 = se3;
        if (se3 != null)
            sr.rolesString = sr.rolesString == null ? se3Role : sr.rolesString+"-"+se3Role;        
        
        tripletsSpatialRelations.put(sr.idString, sr);
    }    
       
    public static List<String> getPrecedingSieves(String currentSieve) {
        if (!SpatialRelation.ORDERED_SIEVES.contains(currentSieve))
            return SpatialRelation.ORDERED_SIEVES;
        List<String> precedingSieves = new ArrayList<>();
        for (String ordered_sieve : SpatialRelation.ORDERED_SIEVES) {
            if (currentSieve.equals(ordered_sieve))
                return precedingSieves;
            precedingSieves.add(ordered_sieve);
        }
        return precedingSieves;
    }
    
}
