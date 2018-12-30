/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerGUI.FileManagerLB;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import lt.lb.commons.parsing.StringOp;

/**
 * Location Mapping Class Use in LocationAPI
 *
 * @author Laimonas Beniušis
 */
public class LocationInRoot {

    private boolean upperCase;
    public LinkedList<String> coordinates = new LinkedList<>();

    private LocationInRoot() {

    }

    private LinkedList<String> resolveFromString(LinkedList<String> co, String filePath, boolean doUpperCase) {
        String rootLoc = "";
        if (!filePath.isEmpty()) {
            for (String s : FileManagerLB.getRootSet()) {
                if (StringOp.containsIgnoreCase(filePath, s)) {
                    rootLoc = s;
                    break;
                }

            }
            rootLoc = rootLoc.toUpperCase();
            co.add(rootLoc);
            if (!filePath.equalsIgnoreCase(rootLoc)) {
                if (doUpperCase) {
                    filePath = StringOp.upperCase(filePath);
                }
                filePath = StringOp.replaceOnce(filePath, rootLoc, "");
                String[] fileArray = StringOp.split(filePath, File.separatorChar);
                List<String> asList = Arrays.asList(fileArray);
                ArrayList<String> list = new ArrayList<>();
                list.addAll(asList);

                Iterator<String> iterator = list.iterator();
                while (iterator.hasNext()) {
                    String next = iterator.next();
                    if (next.isEmpty()) {
                        iterator.remove();
                    }
                }
                co.addAll(list);
            }
            //Log.writeln(coordinates);
        }
        return co;
    }

    public LocationInRoot(String path) {
        this.upperCase = true;
        this.coordinates = resolveFromString(this.coordinates, path, true);
    }

    public LocationInRoot(String path, boolean doUppercase) {
        this.upperCase = doUppercase;
        this.coordinates = resolveFromString(this.coordinates, path, doUppercase);
    }

    public LocationInRoot(LocationInRoot loc) {
        this.upperCase = loc.upperCase;
        this.coordinates.addAll(loc.coordinates);
    }

    private LocationInRoot(List<String> coord, boolean upperCase) {
        coordinates = new LinkedList<>();
        coordinates.addAll(coord);
        this.upperCase = upperCase;

    }

    public String getName() {
        if (this.coordinates.size() > 0) {
            return this.coordinates.get(this.coordinates.size() - 1);
        } else {
            return "";
        }
    }

    public void setName(String name) {
        this.coordinates.pollLast();
        this.coordinates.addLast(name);
    }

    public LocationInRoot getRoot() {
        LinkedList<String> list = new LinkedList<>();
        list.add(this.coordinates.getFirst());
        list.add(this.coordinates.get(1));
        return new LocationInRoot(list, this.upperCase);
    }

    public int length() {
        return this.coordinates.size();
    }

    public String at(int i) {
        return this.coordinates.get(i);
    }

    public LocationInRoot getParentLocation() {
        LinkedList<String> list = new LinkedList<>();
        list.addAll(this.coordinates);
        list.removeLast();
        return new LocationInRoot(list, this.upperCase);
    }

    public boolean isUppercase() {
        return this.upperCase;
    }

    @Override
    public String toString() {
        String str = "";
        for (int i = 0; i < length() - 1; i++) {
            str += this.coordinates.get(i) + ",";
        }
        str += getName();
        return "<" + str + ">";
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LocationInRoot) {
            LocationInRoot otherLoc = (LocationInRoot) o;
            boolean same = otherLoc.length() == this.length();
            if (!same) {
                return false;
            }
            if (this.upperCase || ((LocationInRoot) o).upperCase) {
                for (int i = 0; i < this.length(); i++) {
                    if (!this.at(i).equalsIgnoreCase(otherLoc.at(i))) {
                        same = false;
                    }
                }
            } else {
                for (int i = 0; i < this.length(); i++) {
                    if (!this.at(i).equals(otherLoc.at(i))) {
                        same = false;
                    }
                }
            }
            return same;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (this.upperCase ? 1 : 0);
        hash = 11 * hash + Objects.hashCode(this.coordinates);
        return hash;
    }

}
