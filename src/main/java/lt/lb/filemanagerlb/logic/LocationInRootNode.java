package lt.lb.filemanagerlb.logic;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.Predicates;
import lt.lb.commons.io.autopath.AutoPath;

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

    public Map<String, LocationInRootNode> leafs;
    public String self;
    public int index;

    public LocationInRootNode(String self, int i) {
        this.self = self;
        this.index = i;
        this.leafs = new HashMap<>();
        AutoPath.fs("");
    }

    public void add(LocationInRoot loc, int index) {
        LocationInRootNode currentNode = this;
        int size = loc.length();
        int last = size - 1;
        for (int i = 0; i < size; i++) {
            String key = loc.at(i);
            if (currentNode.leafs.containsKey(key)) {
                currentNode = currentNode.leafs.get(key);
            } else {

                LocationInRootNode newNode = new LocationInRootNode(key, -1);
                if (i == last) {
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
        boolean isFolder = !leafs.isEmpty();
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
        if (index >= 0) { // included
            String part = index + "" + indexEnd + self;
            result.add(part);
        }
        if (isFolder) {
            String part = folderStart + self;
            result.add(part);
        }
        Collections.sort(values);
        for (LocationInRootNode node : values) {
            result.addAll(node.specialString());
        }
        if (isFolder) {
            result.add(folderEnd);
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

    private static class StringWithIndex {

        public final String str;
        public final Integer index;

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
        return Stream.of(array).filter(Predicates.isNotNull()).collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<StringWithIndex> resolvePrivate(String parentPath, boolean includeFolders) {
        ArrayList<StringWithIndex> list = new ArrayList<>();
        String path = parentPath + this.self;
        if (this.leafs.isEmpty() || includeFolders) {
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
