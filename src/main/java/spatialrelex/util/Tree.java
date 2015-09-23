/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spatialrelex.util;

/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ycoppel@google.com (Yohann Coppel)
 * 
 * @param <T>
 *          Object's type in the tree.
 */
public class Tree<T>{

    private T head;

    private List<Tree<T>> leaves = new ArrayList<>();

    private Tree<T> parent = null;

    private Map<T, Tree<T>> locate = new HashMap<>();

    public Tree(T head) {
        this.head = head;
        locate.put(head, this);
    }


    public void addLeaf(T root, T leaf) {
        if (locate.containsKey(root)) {
            locate.get(root).addLeaf(leaf);
        } else {
            addLeaf(root).addLeaf(leaf);
        }
    }

    public Tree<T> addLeaf(T leaf) {
        Tree<T> t = new Tree<>(leaf);
        leaves.add(t);
        t.parent = this;
        t.locate = this.locate;
        locate.put(leaf, t);
        return t;
    }

    public T getHead() {
        return head;
    }

    public Tree<T> getTree(T element) {
        return locate.get(element);
    }

    public Tree<T> getParent() {
        return parent;
    }

    public Collection<T> getSuccessorsNames(T root) {
        Collection<T> successors = new ArrayList<>();
        Tree<T> tree = getTree(root);
        if (null != tree) {
            for (Tree<T> leaf : tree.leaves) {
                successors.add(leaf.head);
            }
        }
        return successors;
    }
    
    public Collection<Tree<T>> getSuccessors(T root) {
        Collection<Tree<T>> successors = new ArrayList<>();
        Tree<T> tree = getTree(root);
        if (null != tree) {
            for (Tree<T> leaf : tree.leaves) {
                successors.add(leaf);
            }
        }
        return successors;
    }

    public Collection<T> getAncestorsNames(T node) {
        Collection<T> ancestors = new ArrayList<>();
        Tree<T> tree = getTree(node);
        if (null != tree) {
            while (null != tree.getParent()) {
                ancestors.add(tree.getParent().head);
                tree = tree.getParent();
            }
        }
        return ancestors;
    }    
    
    public Collection<Tree<T>> getAncestors(T node) {
        Collection<Tree<T>> ancestors = new ArrayList<>();
        Tree<T> tree = getTree(node);
        if (null != tree) {
            while (null != tree.getParent()) {
                ancestors.add(tree.getParent());
                tree = tree.getParent();
            }
        }
        return ancestors;
    }
    
    public boolean DFS(T key) {
        boolean found = false;

        for(int i = 0; i < leaves.size() && !found; i++) {
            Tree<T> child = leaves.get(i);
            
            String val = child.head.toString();
            if(val.equals(key.toString()))
                found = true;
            else
                found = child.DFS(key);
            
        }        
        
        return found;
    }     
    
    
    public void DFS () {
        for (Tree<T> child : leaves) {
            child.DFS();
            System.out.println(child.head);
        }
    }
    
    
    
    @Override
    public String toString() {
        return printTree(0);
    }
    
    private static final int indent = 2;

    private String printTree(int increment) {
        String s = "";
        String inc = "";
        for (int i = 0; i < increment; ++i) {
            inc = inc + " ";
        }
        s = inc + head;
        for (Tree<T> child : leaves) {
            s += "\n" + child.printTree(increment + indent);
        }
        return s;
    }
      
    
}