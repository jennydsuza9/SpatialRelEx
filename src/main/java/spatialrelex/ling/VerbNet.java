/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.ling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import main.java.spatialrelex.markup.SpatialElement;

/**
 *
 * @author Jenny D'Souza
 */
public class VerbNet {
    
    public static Map<String, List<String>> wordVerbNetClass = new HashMap<>();
    public static Map<String, List<String>> wordVerbNetSubClass = new HashMap<>();    
    
    public static void initializeVerbNet(File file) throws IOException {
        List<String> words = new ArrayList<>();
        String classV = "";
        String subClassV = "";
        
        BufferedReader in = new BufferedReader(new FileReader(file));           
        while (in.ready()) {            
            String s = in.readLine().trim();
            
            if (s.matches("CLASS:.*")) {
                if (!words.isEmpty()) {
                    if (!subClassV.equals("")) 
                        setVerbNetSubClasses(words, subClassV);
                    setVerbNetClasses(words, classV);
                }
                words = new ArrayList<>();
                subClassV = "";
                classV = s.split(":")[1].trim();
            }
            else if (s.matches("SUBCLASS:.*")) {
                if (!words.isEmpty() && !subClassV.equals("")) {
                    setVerbNetSubClasses(words, subClassV);
                    setVerbNetClasses(words, classV);
                }
                words = new ArrayList<>();
                subClassV = s.split(":")[1].trim();
            }
            else if (s.matches("MEMBER:.*")) 
                words.add(s.split(":")[1]);
        }
        
        if (!words.isEmpty()) {
            if (!subClassV.equals(""))
                setVerbNetSubClasses(words, subClassV);
            setVerbNetClasses(words, classV);
        }
        
    }
    
    public static void setVerbNetSubClasses(List<String> words, String subClassV) {
        for (String word : words) {
            List<String> subClass = wordVerbNetSubClass.get(word);
            if (subClass == null)
                wordVerbNetSubClass.put(word, subClass = new ArrayList<>());
            subClass.add(subClassV);
        }
    }    
    
    public static void setVerbNetClasses(List<String> words, String classV) {
        for (String word : words) {
            List<String> classes = wordVerbNetClass.get(word);
            if (classes == null)
                wordVerbNetClass.put(word, classes = new ArrayList<>());
            classes.add(classV);
        }
    }    
    
    public static List<String> getClasses(List<String> classes, String word) {
        if (!wordVerbNetClass.containsKey(word))
            return classes;
        Set<String> classesSet = new HashSet<>(classes);        
        classesSet.addAll(wordVerbNetClass.get(word));
        return new ArrayList<>(classesSet);
    }    
    
    public static List<String> getSubClasses(List<String> classes, String word) {
        if (!wordVerbNetSubClass.containsKey(word))
            return classes;
        Set<String> subClassesSet = new HashSet<>(classes);        
        subClassesSet.addAll(wordVerbNetSubClass.get(word));
        return new ArrayList<>(subClassesSet);
    }    
    
    public static List<String> getVerbNetClasses(List<String> verbNetClasses, String word) {
        verbNetClasses = getClasses(verbNetClasses, word.toLowerCase());
        verbNetClasses = getSubClasses(verbNetClasses, word.toLowerCase());
        
        return verbNetClasses;
    }
    
}
