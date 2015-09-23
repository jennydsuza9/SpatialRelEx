/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.ling;

import edu.stanford.nlp.ling.IndexedWord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import main.java.spatialrelex.markup.SpatialElement;
import main.java.spatialrelex.util.Tree;

/**
 *
 * @author Jenny D'Souza
 */
public class ParseTree {
    
    String parseString;
    Map<Integer, String> intIndexParseNode;
    Map<Integer, List<Integer>> parseNodeSuccessors;
    Tree<Integer> parseTree;
    
    public ParseTree(String parseString) {
        this.parseString = parseString;
        this.intIndexParseNode = new HashMap<>();
        this.parseNodeSuccessors = new HashMap<>();
    }
    
    public Tree<Integer> getParseTree(int root, Map<Integer, List<Integer>> parseNodeSuccessors) {
        Tree<Integer> tree = new Tree<>(root);        
        List<Integer> unvisited_list = new LinkedList<>();
        List<Integer> visited_list = new ArrayList<>();
                
        unvisited_list.add(tree.getHead());
        while (!unvisited_list.isEmpty()) {
            
            Integer head = unvisited_list.remove(0);
            if (!visited_list.contains(head))
                visited_list.add(head);
            
            if (!parseNodeSuccessors.containsKey(head))
                continue;
            
            List<Integer> successors = parseNodeSuccessors.get(head);
            for (Integer n : successors)
                if (!visited_list.contains(n))
                    unvisited_list.add(n);
            
            Tree<Integer> t = head == root ? tree : new Tree<>(head);
            
            for (Integer n : successors) {
                if (visited_list.contains(n))
                    continue;
                                
                if (t.getHead() == root) 
                    t.addLeaf(n);
                else 
                    tree.addLeaf(head, n);
            }
        }
        return tree;
    }    
    
    public void convertParseTree() {
        int index = 1000;
        int tokenIndex = 1;
        String[] tokens = parseString.split("\\s");

        List<Integer> nodeIndexes = new LinkedList<>();
            
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].matches("\\(.*") && i+1 < tokens.length && tokens[i+1].matches(".*?\\)+")) {
                i++;
                int j = 0;
                while (tokens[i].charAt(j) != ')') {
                    j++;
                }
                
                int nodeIndex = nodeIndexes.get(nodeIndexes.size()-1);
                List<Integer> successors = parseNodeSuccessors.get(nodeIndex);
                if (successors == null)
                    parseNodeSuccessors.put(nodeIndex, successors = new LinkedList<>());
                successors.add(tokenIndex);
                tokenIndex++;
                j++;
                while (j < tokens[i].length() && tokens[i].charAt(j) == ')') {
                    nodeIndexes.remove(nodeIndexes.size()-1);
                    j++;
                }
                continue;
            }

            String parseNode = tokens[i].replaceAll("\\(", "");
            intIndexParseNode.put(index, parseNode);

            if (nodeIndexes.size() >= 1) {
                int nodeID = nodeIndexes.get(nodeIndexes.size()-1);
                List<Integer> successors = parseNodeSuccessors.get(nodeID);
                if (successors == null)
                    parseNodeSuccessors.put(nodeID, successors = new LinkedList<>());
                successors.add(index);
            }

            nodeIndexes.add(index);
            index++;
        }            
            
        this.parseTree = getParseTree(1000, parseNodeSuccessors);
    }    
    
    public int DFS(Tree<Integer> node) {
        List<Integer> unvisited = new LinkedList<>();
        unvisited.add(node.getHead());
        int last_node = -1;
        while (!unvisited.isEmpty()) {
            Tree<Integer> new_node = node.getTree(unvisited.remove(0));
            last_node = new_node.getHead();
            List<Integer> successors = new ArrayList<>(new_node.getSuccessorsNames(new_node.getHead()));
            unvisited.addAll(0, successors);
        }
        return last_node;
    }    
    
    public Map<Integer, List<Integer>> getLeftmostChildAncestors(List<Integer> nodes, Tree<Integer> tree) {
        Map<Integer, List<Integer>> leftmostChildAncestors = new HashMap<>();
        
        for (int node : nodes) {
            int leftmostChild = DFS(tree.getTree(node));
            List<Integer> ancestors = leftmostChildAncestors.get(leftmostChild);
            if (ancestors == null)
                leftmostChildAncestors.put(leftmostChild, ancestors = new ArrayList<>());
            if (!ancestors.contains(node))
                ancestors.add(node);
        }
        
        return leftmostChildAncestors;
    }    
    
    public Map<Integer, List<Integer>> getNodesOnPathToRoot(int token) {
        Map<Integer, List<Integer>> nodesOnPath = new HashMap<>();
        
        Tree<Integer> node = parseTree.getTree(token).getParent();
        while (node != null) {
            nodesOnPath.put(node.getHead(), new ArrayList<>(node.getSuccessorsNames(node.getHead())));
            node = node.getParent();
        }
        
        return nodesOnPath;
    }        
    
    public String getParseTreeFeature(Map<Integer, SpatialElement> startOffsetSpatialElement, 
            Map<Integer, Integer> tokenNumStartOffset, Map<Integer, IndexedWord> startOffsetIndexedWord) {        
        String parse_string = "";
        Map<Integer, List<Integer>> nodesToRoot = new HashMap<>();
        List<Integer> nodesInCommon = new ArrayList<>();
        
        Map<Integer, String> endTokenIndexRole = new HashMap<>();
        
        for (int startOffset : startOffsetSpatialElement.keySet()) {
            SpatialElement se = startOffsetSpatialElement.get(startOffset);
            endTokenIndexRole.put(se.endToken, se.role);
            
            Map<Integer, List<Integer>> nodesOnPath = new HashMap<>(getNodesOnPathToRoot(se.endToken));
            List<Integer> nodes = new ArrayList<>(nodesOnPath.keySet());
            if (nodesInCommon.isEmpty())
                nodesInCommon.addAll(nodes);
            else 
                nodesInCommon.retainAll(nodes);
            nodesToRoot.putAll(nodesOnPath);
        }
        
        Collections.sort(nodesInCommon);
        int preserve_index = nodesInCommon.size()-1;
        int root_node = nodesInCommon.get(preserve_index);        
        nodesInCommon.remove(preserve_index);
        nodesToRoot.keySet().removeAll(nodesInCommon);
        
        Tree<Integer> new_tree = getParseTree(root_node, nodesToRoot);
        Map<Integer, List<Integer>> leftmost_child_ancestors_map = getLeftmostChildAncestors(new ArrayList<>(nodesToRoot.keySet()), new_tree);
        
        List<Integer> unvisited = new LinkedList<>();
        unvisited.add(new_tree.getHead());
        while (!unvisited.isEmpty()) {
            Tree<Integer> node = new_tree.getTree(unvisited.remove(0));
            int head = node.getHead();
            //node is a leaf
            if (node.getSuccessors(head).isEmpty()) {
                //is the node
                if (endTokenIndexRole.containsKey(head)) {
                    String role = endTokenIndexRole.get(head);
                    parse_string += " ("+role+" "+role+")";
                }
                else if (tokenNumStartOffset.containsKey(head)) {
                    String pos = startOffsetIndexedWord.get(tokenNumStartOffset.get(head)).tag();
                    parse_string += " ("+pos+" "+pos+")";
                }
                else {
                    String phrase_tag = intIndexParseNode.get(head);
                    parse_string += " ("+phrase_tag+" "+phrase_tag+")";
                }
                
                if (leftmost_child_ancestors_map.containsKey(head)) {
                    int ancestors = leftmost_child_ancestors_map.get(head).size();
                    while (ancestors != 0) {
                        parse_string += ")";
                        ancestors--;
                    }
                }
            }
            else {
                String phrase_tag = intIndexParseNode.get(head);
                parse_string += " ("+phrase_tag;
            }
            unvisited.addAll(0, new ArrayList<>(node.getSuccessorsNames(head)));
        }
        parse_string = parse_string.trim();
        
        return parse_string;
    }
        
    
}
