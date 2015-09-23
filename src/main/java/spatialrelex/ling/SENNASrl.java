/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.ling;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import main.java.spatialrelex.Main;

/**
 *
 * @author Jenny D'Souza
 */
public class SENNASrl {
    
    public static List<String> getSRLRoles(String s) {      
        Set<String> srlRoles = new HashSet<>();
        String[] tokens = s.split("\\s+");
        for (int i = 2; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
            if (tokens[i].equals("O") || tokens[i].split("\\-").length < 2)
                continue;            
            srlRoles.add(tokens[i].split("\\-")[1]);
        }
        if (srlRoles.isEmpty())
            return null;
        return new ArrayList<>(srlRoles);
    }
    
    public static Map<Integer, List<String>> getTokenSRLRoles(String s) throws IOException {
        String[] lines = s.split("\n");
        Map<Integer, List<String>> tokenSRLRoles = new HashMap<>();
        int token = 1;
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
            if (lines[i].equals(""))
                continue;
            lines[i] = lines[i].replaceAll("[\uFEFF-\uFFFF]", ""); 
            tokenSRLRoles.put(token, getSRLRoles(lines[i]));
            token++;
        }
        return tokenSRLRoles;
    }
    
    public static Map<Integer, List<String>> getReadOut(String pathToSenna, String[] params) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        String s = "cmd /C senna-win32.exe -iobtags -usrtokens -srl < log.txt";
        Process p;
        p = rt.exec(s, params, new File(pathToSenna));

        BufferedReader processOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        ReadThread r = new ReadThread(processOutput);
        Thread th = new Thread(r);
        th.start();
        p.waitFor();
        r.stop();
        s = r.res;
        
        Map<Integer, List<String>> tokenSRLRoles = getTokenSRLRoles(s);
        
        p.destroy();
        th.join();
        return tokenSRLRoles;
    } 
    
    public static Map<Integer, List<String>> getSRLRoles(Map<Integer, List<String>> startOffsetSRLRoles) throws IOException, InterruptedException {        
        
        String[] params = new String[1];
        params[0] = "";
        
        Map<Integer, List<String>> tokenSRLRoles = getReadOut("main\\resources\\senna\\", params);
        int token = 1;
        for (int startOffset : startOffsetSRLRoles.keySet()) {
            startOffsetSRLRoles.put(startOffset, tokenSRLRoles.get(token));
            token++;
        }
        
        return startOffsetSRLRoles;
    }    
    
    public static class ReadThread implements Runnable{

        BufferedReader reader;
        char[] buf = new char[100000];
        String res = "";
        boolean stop;
        public ReadThread(BufferedReader reader) {
            this.reader = reader;
            stop = false;
        }

        @Override
        public void run() {
        res = "";

            while (!stop) {
                try {
                    reader.read(buf);
                    res += new String(buf);

                } catch (IOException ex) {
                    System.err.print(ex);
                }
            }
        }

        public void stop() {
            stop = true;
        }
    }    
        
    
}
