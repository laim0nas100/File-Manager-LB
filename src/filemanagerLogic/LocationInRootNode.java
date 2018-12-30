/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import lt.lb.commons.F;
import lt.lb.commons.Predicates;

/**
 * create file tree
 *
 * start with an empty root node and add files to it
 *
 * @author Lemmin
 */
public class LocationInRootNode implements Comparable {

    public final static String folderStart = "\\";
    public final static String folderEnd = "/";
    public final static Character indexEnd = ':';

    public HashMap<String, LocationInRootNode> leafs;
    public String self;
    public int index;

    public LocationInRootNode(String self, int i) {
        this.self = self;
        this.index = i;
        this.leafs = new HashMap<>();
    }

    public void add(LocationInRoot loc, int index) {
        LocationInRootNode currentNode = this;
        while (!loc.coordinates.isEmpty()) {
//            Log.write(loc.coordinates);
            String key = loc.coordinates.pollFirst();
            if (currentNode.leafs.containsKey(key)) {
//                Log.write("Contains");
                currentNode = currentNode.leafs.get(key);
            } else {

                LocationInRootNode newNode = new LocationInRootNode(key, -1);
                if (loc.coordinates.isEmpty()) {
                    newNode.index = index;
                }
                currentNode.leafs.put(key, newNode);
                currentNode = newNode;
            }
        }

    }

    @Override
    public String toString() {
        String res = "";
        boolean isFolder = !leafs.values().isEmpty();

        if (index >= 0) {
            res += index + "" + indexEnd + self + "\n";
        }
        if (isFolder) {
            res += folderStart + self + "\n";
        }
        for (LocationInRootNode node : leafs.values()) {
            res += node.toString();
        }
        if (isFolder) {
            res += folderEnd + "\n";
        }

        return res;
    }

    public ArrayList<String> specialString() {
        boolean isFolder = !leafs.values().isEmpty();
        ArrayList<String> result = new ArrayList<>();
        ArrayList<LocationInRootNode> values = new ArrayList<>();
        if (isFolder) {
            ArrayList<LocationInRootNode> folders = new ArrayList<>();
            ArrayList<LocationInRootNode> files = new ArrayList<>();
            for (LocationInRootNode node : leafs.values()) {
                if (node.leafs.isEmpty()) {
                    files.add(node);
                } else {
                    folders.add(node);
                }
            }
            values.addAll(folders);
            values.addAll(files);
        }
        if (index >= 0) {
            String part = index + "" + indexEnd + self;
//            res+= part+ "\n";
            result.add(part);
        }
        if (isFolder) {
            String part = folderStart + self;
            result.add(part);
//            res+=part + "\n";
        }
        Collections.sort(values);
        for (LocationInRootNode node : values) {
            result.addAll(node.specialString());
//            res+=node.specialString();

        }
        if (isFolder) {
            result.add(folderEnd);
//            res+=folderEnd + "\n";
        }
        return result;
    }

    @Override
    public int compareTo(Object o) {
        LocationInRootNode other = (LocationInRootNode) o;
        boolean isFolder = !leafs.values().isEmpty();
        if (isFolder) {
            return 1;
        }
        return this.index - other.index;
    }

    private class StringWithIndex {

        public String str;
        public Integer index;

        public StringWithIndex(String str, int index) {
            this.str = str;
            this.index = index;
        }

        @Override
        public String toString() {
            return index + indexEnd + str;
        }
    }

    public ArrayList<String> resolve(boolean includeFolders) {
        ArrayList<StringWithIndex> resolvePrivate = resolvePrivate("", includeFolders);
        String[] array = new String[resolvePrivate.size() + 1];
        for (StringWithIndex p : resolvePrivate) {
            if (p.index >= 0) {
                array[p.index] = p.str;
            }
        }
        return F.fillCollection(Stream.of(array).filter(Predicates.isNotNull()), new ArrayList<>());
    }

    private ArrayList<StringWithIndex> resolvePrivate(String parentPath, boolean includeFolders) {
        ArrayList<StringWithIndex> list = new ArrayList<>();
        String path = parentPath + this.self;
        if (this.leafs.isEmpty()) {
            list.add(new StringWithIndex(path, this.index));
        } else if (includeFolders) {
            list.add(new StringWithIndex(path, this.index));
        }
        if (!path.endsWith(File.separator) && path.length() > 0) {
            path += File.separator;
        }
        for (LocationInRootNode node : this.leafs.values()) {
            list.addAll(node.resolvePrivate(path, includeFolders));
        }
        return list;
    }

    public static LocationInRootNode nodeFromFile(Collection<String> lines) {
        ArrayDeque<LocationInRootNode> folderStack = new ArrayDeque<>();
        LocationInRootNode root = new LocationInRootNode("", -1);
        folderStack.add(root);
        for (String line : lines) {
            if (line.startsWith(folderStart)) {
                line = line.substring(1);
                LocationInRootNode node = new LocationInRootNode(line, -1);
                folderStack.getLast().leafs.put(node.self, node);
                folderStack.addLast(node);
            } else if (line.startsWith(folderEnd)) {
                folderStack.pollLast();
            } else if (line.length() > 2 && line.contains("" + indexEnd)) {
                //Add simple path ####:path
                String index = "";
                for (Character c : line.toCharArray()) {
                    if (c.equals(indexEnd)) {
                        break;
                    }
                    if (Character.isDigit(c)) {
                        index += c;
                    }
                }
//                Log.write(index,line);

                int i = Integer.parseInt(index);
                line = line.substring(index.length() + 1);
                LocationInRootNode node = new LocationInRootNode(line, i);
                folderStack.getLast().leafs.put(node.self, node);
            }
        }
        return root;
    }

}
