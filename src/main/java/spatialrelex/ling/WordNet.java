/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.ling;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import main.java.spatialrelex.markup.SpatialElement;

/**
 *
 * @author Jenny D'Souza
 */
public class WordNet {
    
    public static WordNetDatabase database;    
    
    public static void initializeWordNet() {
        database = WordNetDatabase.getFileInstance();    
    }
    
    public static SpatialElement setWordNetSynsetsAndHypernyms(SpatialElement se, String word, String pos) {        
        if (pos.equals("NN")) {
            Synset[] synsets = database.getSynsets(word, SynsetType.NOUN);
            for (int i = 0; i < synsets.length; i++) { 
                se.synsets.addAll(Arrays.asList(synsets[i].getWordForms()));                    
                NounSynset nounSynset = (NounSynset)(synsets[i]);
                NounSynset[] hypernyms = nounSynset.getHypernyms();
                for (int j = 0; j < hypernyms.length; j++) {
                    String[] wordforms = hypernyms[j].getWordForms();
                    se.hypernyms.addAll(Arrays.asList(wordforms));
                }
            }
        }        
        else if (pos.equals("VB")) {
            Synset[] synsets = database.getSynsets(word, SynsetType.VERB);
            for (int i = 0; i < synsets.length; i++) { 
                se.synsets.addAll(Arrays.asList(synsets[i].getWordForms()));
                VerbSynset verbSynset = (VerbSynset)(synsets[i]);
                VerbSynset[] hypernyms = verbSynset.getHypernyms();
                for (int j = 0; j < hypernyms.length; j++) {
                    String[] wordforms = hypernyms[j].getWordForms();
                    se.hypernyms.addAll(Arrays.asList(wordforms));
                }
            }  
        }        
        
        Set<String> set = new HashSet<>(se.synsets);
        se.synsets.clear();
        se.synsets.addAll(set);
        set = new HashSet<>(se.hypernyms);
        se.hypernyms.clear();
        se.hypernyms.addAll(set);
        
        return se;
    }
    
}
