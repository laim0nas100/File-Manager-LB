package lt.lb.filemanagerlb.utility;

import java.io.File;
import java.util.List;
import java.util.Objects;
import lt.lb.commons.reflect.unified.ReflFields;
import org.apache.commons.lang3.Strings;

/**
 *
 * @author laim0nas100
 */
public class PathStringCommands {

    public static String fileName, nameNoExt, filePath, parent1, parent2, number, custom, relativeCustom, extension;
    private String absolutePath;

    @Override
    public boolean equals(Object e) {
        boolean eq = false;

        if ((e != null) && (e instanceof PathStringCommands)) {
            PathStringCommands ob = (PathStringCommands) e;
            eq = ob.absolutePath.equals(this.absolutePath);
        }
        return eq;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.absolutePath);
        return hash;
    }

    public PathStringCommands(String path) {
        absolutePath = path;
        if (absolutePath.endsWith(File.separator)) {
            absolutePath = absolutePath.substring(0, absolutePath.length() - 1);
        }
    }

    public String getName(boolean extension) {
        String name = PathStringCommands.getName(absolutePath);

        if (!extension && name.contains(".")) {//remove extension
            int index = Strings.CS.lastIndexOf(name, ".");
            name = name.substring(0, index);
        }
        return name;
    }

    public static String getName(String path) {
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }
        int index = Strings.CS.lastIndexOf(path, File.separator) + 1;
        path = path.substring(index);
        return path;
    }

    public String getExtension() {
        String name = this.getName(true);
        if (name.contains(".")) {
            int index = Strings.CS.lastIndexOf(name, ".") + 1;
            if (index < name.length()) {
                name = name.substring(index);
            } else {
                return "";
            }
        } else {
            return "";
        }
        return name;
    }

    public String getParent(int timesToGoUp) {
        String current = this.absolutePath;
        while (timesToGoUp > 0) {
            current = PathStringCommands.goUp(current);
            timesToGoUp--;
        }
        return current;
    }

    public static String goUp(String current) {
        int index = Math.max(Strings.CS.lastIndexOf(current, PathStringCommands.getName(current)) - 1, 0);
        current = current.substring(0, index);
        if (!Strings.CS.contains(current, File.separator)) {
            current += File.separator;
        }
        return current;

    }

    public String relativePathFrom(String possibleParent) {
        if (!possibleParent.endsWith(File.separator)) {
            possibleParent += File.separator;
        }
        String path = absolutePath;
        if (!path.contains(possibleParent) || path.equalsIgnoreCase(possibleParent)) {
            return absolutePath;
        } else {
            return Strings.CS.replaceOnce(path, possibleParent, "");
        }
    }

    public String relativePathTo(String possibleChild) {

        String path = absolutePath + File.separator;
        if (!possibleChild.contains(path) || possibleChild.equalsIgnoreCase(path)) {
            return absolutePath;
        } else {
            return Strings.CI.replaceOnce(possibleChild, path, "");
        }
    }

    public String getPath() {
        return absolutePath;
    }

    public void setPath(String path) {
        this.absolutePath = path;
    }

    public static List<String> returnDefinedKeys() {
        return ReflFields.getStaticFields(PathStringCommands.class, String.class).mapSafeOpt(m -> m.safeGet()).toList();
    }

}
