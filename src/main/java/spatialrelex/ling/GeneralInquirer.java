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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jenny D'Souza
 */
public class GeneralInquirer {
    
    public static Map<String, List<String>> wordGeneralInquirerCategories = new HashMap<>();    
    
    public static void initializeGeneralInquirer(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));   
        int i = 0;
        while (in.ready()) {            
            String s = in.readLine().trim();
            
            if (i == 0) {
                i++;
                continue;
            }
            
            s = s.split("\\|")[0].trim().toLowerCase();
            String[] tokens = s.split("\\s");
            
            if (tokens[0].contains("#"))
                tokens[0] = tokens[0].split("#")[0]; 
            List<String> categories = wordGeneralInquirerCategories.get(tokens[0]);
            if (categories == null)
                wordGeneralInquirerCategories.put(tokens[0], categories = new ArrayList<>());
            
            categories.addAll(Arrays.asList(tokens));
            categories.remove(tokens[0]);
            Set<String> categoriesSet = new HashSet<>(categories);
            categories.clear();
            categories.addAll(categoriesSet);
        }
    }    
    
    public static List<String> getGeneralInquirerCategories(List<String> categories, String word) {
        if (!wordGeneralInquirerCategories.containsKey(word))
            return categories;
        Set<String> categoriesSet = new HashSet<>(categories);        
        categories.addAll(wordGeneralInquirerCategories.get(word));
        return new ArrayList<>(categoriesSet);
    }    
    
}
