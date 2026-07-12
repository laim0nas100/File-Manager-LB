package lt.lb.filemanagerlb.logic;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import lt.lb.filemanagerlb.gui.FileManagerLB;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/**
 * Location Mapping Class Use in LocationAPI
 *
 * @author laim0nas100
 */
public class LocationInRoot {

    private boolean upperCase;
    private final List<String> coordinates;

    private void resolveFromString(List<String> co, String filePath, boolean doUpperCase) {
        String rootLoc = "";
        if (!filePath.isEmpty()) {
            for (String s : FileManagerLB.getRootSet()) {
                if (Strings.CI.startsWith(filePath, s)) {
                    rootLoc = s;
                    break;
                }

            }
            rootLoc = rootLoc.toUpperCase();
            co.add(rootLoc);
            if (!filePath.equalsIgnoreCase(rootLoc)) {//more than root
                if (doUpperCase) {
                    filePath = StringUtils.upperCase(filePath);
                }
                filePath = Strings.CI.removeStart(filePath, rootLoc);
                String[] fileArray = StringUtils.split(filePath, File.separatorChar);
                for(String next:fileArray){
                    if (!StringUtils.isBlank(next)) {
                        co.add(next);
                    }
                }
            }
            //Log.writeln(coordinates);
        }
    }

    public LocationInRoot(String path) {
        this.upperCase = true;
        coordinates = new ArrayList<>();
        resolveFromString(coordinates, path, true);
    }

    public LocationInRoot(String path, boolean doUppercase) {
        this.upperCase = doUppercase;
        coordinates = new ArrayList<>();
        resolveFromString(coordinates, path, doUppercase);
    }

    public LocationInRoot(LocationInRoot loc) {
        this.upperCase = loc.upperCase;
        coordinates = new ArrayList<>();
        this.coordinates.addAll(loc.coordinates);
    }

    private LocationInRoot(List<String> coord, boolean upperCase) {
        coordinates = new ArrayList<>(coord);
        this.upperCase = upperCase;

    }

    public String getName() {
        if (!this.coordinates.isEmpty()) {
            return this.coordinates.get(this.coordinates.size() - 1);
        } else {
            return "";
        }
    }

    public void setName(String name) {
        coordinates.set(coordinates.size()-1, name);
    }

    public LocationInRoot getRoot() {
        List<String> list = new ArrayList<>();
        list.add(this.coordinates.get(0));
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
        List<String> list = new ArrayList<>();
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
            if (otherLoc.length() != this.length()) {
                return false;
            }
            boolean ignore = this.upperCase || otherLoc.upperCase;
            Iterator<String> mine = this.coordinates.iterator();
            Iterator<String> other = otherLoc.coordinates.iterator();

            while (mine.hasNext() && other.hasNext()) {
                String s1 = mine.next();
                String s2 = other.next();
                if (ignore) {
                    if (!Strings.CI.equals(s1, s2)) {
                        return false;
                    }
                } else {
                    if (!Strings.CS.equals(s1, s2)) {
                        return false;
                    }
                }
            }
            return true;
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
