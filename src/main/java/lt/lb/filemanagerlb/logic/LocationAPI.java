/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.logic;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Locale;
import lt.lb.commons.F;
import lt.lb.commons.containers.collections.CollectionOp;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.FileManagerLB;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.utility.DesktopApi;
import lt.lb.filemanagerlb.utility.ErrorReport;
import org.tinylog.Logger;

/**
 *
 * @author Laimonas Beniušis
 */
public class LocationAPI {

    private static class LocationWalker {

        public LocationInRoot location;

        private int index;
        public ExtFolder folderBefore;
        public ExtFolder currentFolder;
        public ExtPath currentFile;

        public LocationWalker(LocationInRoot location) {
            this.location = location;
            reset();
        }

        public boolean reachedEnd() {
            return index == this.location.length();
        }

        public String nextCoordinate() {
            if (index >= location.length()) {
                return null;
            }
            return this.location.at(index);
        }

        public boolean iteration() {
            if (location.isUppercase()) {
                if (currentFolder.hasFileIgnoreCase(location.at(index))) {
                    currentFile = currentFolder.getIgnoreCase(location.at(index));
                } else {
                    return false;
                }
            } else {
                if (currentFolder.files.containsKey(location.at(index))) {
                    currentFile = currentFolder.files.get(location.at(index));
                } else {
                    return false;
                }
            }
            if (currentFile instanceof ExtFolder) {
                folderBefore = currentFolder;
                currentFolder = (ExtFolder) currentFile;
            }
            index++;
            return true;
        }

        public boolean canDoStep(boolean existTest) {
            ExtPath path = null;
            if (index >= location.length()) {
                return false;
            }
            if (location.isUppercase()) {
                if (currentFolder.hasFileIgnoreCase(location.at(index))) {
                    path = currentFolder.getIgnoreCase(location.at(index));
                } else {
                    return false;
                }
            } else {
                if (currentFolder.files.containsKey(location.at(index))) {
                    path = currentFolder.files.get(location.at(index));
                } else {
                    return false;
                }
            }
            if (path == null) {
                return false;
            }
            if (!existTest) {
                return path instanceof ExtFolder;
            }
            return true;

        }

        public boolean isUppercase() {
            return location.isUppercase();
        }

        public final void reset() {
            index = 0;
            folderBefore = FileManagerLB.ArtificialRoot;
            currentFolder = FileManagerLB.ArtificialRoot;
            currentFile = FileManagerLB.ArtificialRoot;
        }

    }
    private static final LocationAPI INSTANCE = new LocationAPI();

    protected LocationAPI() {
    }

    ;
    public static LocationAPI getInstance() {
        return INSTANCE;
    }

    public LocationInRoot getLocationMapping(String path) {
        return new LocationInRoot(path);
    }

    private Path recursiveRootResolve(Path start, int limmit) {
        Path parent = start.getParent();
        if (parent == null || limmit <= 0) {
            return start;
        } else {
            return recursiveRootResolve(parent, limmit - 1);
        }
    }

    public ExtPath getFileAndPopulate(String pathl) {
        ExtPath file = FileManagerLB.ArtificialRoot;
        pathl = pathl.trim();
        Logger.info("getFileAndPopulate:" + pathl);
        if (!pathl.isEmpty() && !pathl.equals(D.ROOT_NAME)) {

            try {
                try {
                    if (DesktopApi.getOs().isWindows()) { //Directory Mounting BS on Windows
                        Path path = Paths.get(pathl).toRealPath();
                        pathl = path.toAbsolutePath().toString();
                        Logger.info("realPath:" + path);
                        Path potentialRoot = path.getRoot();
                        if (potentialRoot == null) {
                            potentialRoot = recursiveRootResolve(path, 100);
                        }
                        String rootStr = potentialRoot.toString().toUpperCase(Locale.ROOT);
                        if (!FileManagerLB.getRootSet().contains(rootStr)) {
                            if (FileManagerLB.mountDevice(rootStr)) {
//                                FileManagerLB.ArtificialRoot.update();
                                Logger.info("Mounted "+ path);
                            }
                        }
                    }
                } catch (Exception e) {
                    ErrorReport.report(e);
//                    ErrorReport.report(new Exception("windows auto pathing exception: " +pathl));
                }
                LocationInRoot loc = new LocationInRoot(pathl);
                Logger.info("Location: "+ loc);
                populateByLocation(loc.getParentLocation());

                file = getFileByLocation(loc);

            } catch (Exception e) {
                ErrorReport.report(e);
            }
        }
        return file;

    }

    public boolean existByLocation(LocationInRoot location) {
        LocationWalker walker = new LocationWalker(location);
        while (walker.canDoStep(true)) {
            walker.iteration();
        }
        return walker.reachedEnd();

    }

    public void removeByLocation(LocationInRoot location) {
        LocationWalker walker = new LocationWalker(location);
        while (walker.canDoStep(true)) {
            walker.iteration();
        }
        if (walker.reachedEnd()) {
            String key;
            if (location.isUppercase()) {
                key = walker.currentFolder.getKey(location.getName());
            } else {
                key = location.getName();
            }
            walker.currentFolder.files.remove(key);
            Logger.info("Remove by location success");
        }
    }

    public void putByLocation(LocationInRoot location, ExtPath file) {
        LocationWalker walker = new LocationWalker(location);
        while (walker.canDoStep(true)) {
            walker.iteration();
        }
        if (walker.nextCoordinate().equals(location.getName())) {
            walker.currentFolder.files.put(file.propertyName.get(), file);
            Logger.info("Put by location success");
        }

    }

    public void putByLocationRecursive(LocationInRoot location, ExtPath file) {
        LocationWalker walker = new LocationWalker(location);
        while (walker.canDoStep(true)) {
            walker.iteration();
            walker.currentFolder.update();
        }
        walker.currentFolder.files.put(file.propertyName.get(), file);
    }

    private void populateByLocation(LocationInRoot location) {
        Logger.info("Populate by location "+ location);
        LocationWalker walker = new LocationWalker(location);
        while (walker.canDoStep(true)) {
            walker.iteration();
            walker.currentFolder.update();
        }
    }

    public ExtPath getFileByLocation(LocationInRoot location) {
        Logger.info("Get file by location "+ location);
        LocationWalker walker = new LocationWalker(location);
        while (walker.canDoStep(true)) {
            walker.iteration();
        }
        return walker.currentFile;

    }

    public ExtPath getFileOptimized(String path) {
        LocationInRoot loc = new LocationInRoot(path);
        if (!existByLocation(loc)) {
            return getFileAndPopulate(path);
        }
        return getFileIfExists(loc);
    }

    public ExtPath getFileIfExists(LocationInRoot location) {
        ExtPath fileByLocation = getFileByLocation(location);
        LocationInRoot mapping = fileByLocation.getMapping();
        Logger.info(location + " " + mapping);
        if (location.equals(mapping)) {
            Logger.info("Equals");
            return fileByLocation;
        } else {
            Logger.info("Different");
            return null;
        }
    }

    public void addToCollectionSafe(Collection<ExtPath> collection, LocationInRoot location) {
        ExtPath file = getFileIfExists(location);
        if (file != null) {
            collection.add(file);
        }
    }

    public void filterIfExists(Collection<ExtPath> collection) {
        CollectionOp.filterParallel(collection, ExtPath.EXISTS, D.exe);
    }
}
