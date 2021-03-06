/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.fileStructure.ExtFolder;
import LibraryLB.Log;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Locale;
import utility.DesktopApi;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class LocationAPI {
    private static class LocationWalker{
        public LocationInRoot location;
        
        private int index;
        public ExtFolder folderBefore;
        public ExtFolder currentFolder;
        public ExtPath currentFile;
        public LocationWalker(LocationInRoot location){
           this.location = location;
           reset();
        }
        public boolean reachedEnd(){
            return index == this.location.length();
        }
        public String nextCoordinate(){
            if(index>= location.length()){
                return null;
            }
            return this.location.at(index);
        }
        public boolean iteration(){
            if(location.isUppercase()){
                if (currentFolder.hasFileIgnoreCase(location.at(index))) {
                    currentFile = currentFolder.getIgnoreCase(location.at(index));
                } else {
                    return false;
                } 
            }else{
                if (currentFolder.files.containsKey(location.at(index))) {
                    currentFile = currentFolder.files.get(location.at(index));
                } else {
                    return false;
                } 
            }
            if(currentFile instanceof ExtFolder){
                folderBefore = currentFolder;
                currentFolder = (ExtFolder) currentFile;
            }
            index++;
            return true;
        }
        public boolean canDoStep(boolean existTest){
            ExtPath path = null;
            if(index>= location.length()){
                return false;
            }
            if(location.isUppercase()){
                if (currentFolder.hasFileIgnoreCase(location.at(index))) {
                    path = currentFolder.getIgnoreCase(location.at(index));
                } else {
                    return false;
                } 
            }else{
                if (currentFolder.files.containsKey(location.at(index))) {
                    path = currentFolder.files.get(location.at(index));
                } else {
                    return false;
                } 
            }
            if(path==null){
                return false;
            }
            if(!existTest ){
               return path instanceof ExtFolder; 
            }
            return true;
            
        }
        public boolean isUppercase(){
            return location.isUppercase();
        }
        public final void reset(){
            index = 0;
            folderBefore = FileManagerLB.ArtificialRoot;
            currentFolder = FileManagerLB.ArtificialRoot;
            currentFile = FileManagerLB.ArtificialRoot;
        }
        
    }
    private static final LocationAPI INSTANCE = new LocationAPI();
    protected LocationAPI() {};
    public static LocationAPI getInstance(){
        return INSTANCE;
    }
    public LocationInRoot getLocationMapping(String path){
        return new LocationInRoot(path);
    }
    public ExtPath getFileAndPopulate(String pathl){
        ExtPath file = FileManagerLB.ArtificialRoot; 
        pathl = pathl.trim();
        Log.print("getFileAndPopulate:"+pathl);
        if(!pathl.isEmpty() && !pathl.equals(FileManagerLB.ROOT_NAME)){
            
            try{
                try{
                    if(DesktopApi.getOs().isWindows()){ //Directory Mounting BS on Windows
                        Path path = Paths.get(pathl).toRealPath();
                        pathl = path.toAbsolutePath().toString();
                        Log.print("realPath:"+path);
                        if(!FileManagerLB.getRootSet().contains(path.getRoot().toString().toUpperCase(Locale.ROOT))){
                            if(FileManagerLB.mountDevice(path.getRoot().toString())){
//                                FileManagerLB.ArtificialRoot.update();
                                Log.print("Mounted",path);
                            }
                        }
                    }
                }catch(Exception e){
//                    ErrorReport.report(new Exception("windows auto pathing exception: " +pathl));
                }
                LocationInRoot loc = new LocationInRoot(pathl);
                Log.print("Location:",loc);
                populateByLocation(loc.getParentLocation());
                
                file = getFileByLocation(loc);
                
            }catch(Exception e){
                ErrorReport.report(e);
            }
        }
        return file;           
            
    }
    public boolean existByLocation(LocationInRoot location) {
        LocationWalker walker = new LocationWalker(location);
        while(walker.canDoStep(true)){
            walker.iteration();
        }
        return walker.reachedEnd();
        
        
    }
    public void removeByLocation(LocationInRoot location) {
        LocationWalker walker = new LocationWalker(location);
        while(walker.canDoStep(true)){
            walker.iteration();
        }
        if(walker.reachedEnd()){
            String key;
            if(location.isUppercase()){
                key = walker.currentFolder.getKey(location.getName());
            }else{
                key = location.getName();
            }
            walker.currentFolder.files.remove(key);
            Log.print("Remove by location success");
        }
    }
    public void putByLocation(LocationInRoot location, ExtPath file) {
        LocationWalker walker = new LocationWalker(location);
        while(walker.canDoStep(true)){
            walker.iteration();
        }
        if(walker.nextCoordinate().equals(location.getName())){
            walker.currentFolder.files.put(file.propertyName.get(), file);
            Log.print("Put by location success");
        }
        
    }
    public void putByLocationRecursive(LocationInRoot location, ExtPath file) {
        LocationWalker walker = new LocationWalker(location);
        while(walker.canDoStep(true)){
            walker.iteration();
            walker.currentFolder.update();
        }
        walker.currentFolder.files.put(file.propertyName.get(), file);
    }
    private void populateByLocation(LocationInRoot location){
        Log.print("Populate by location",location);
        LocationWalker walker = new LocationWalker(location);
        while(walker.canDoStep(true)){
            walker.iteration();
            walker.currentFolder.update();
        }
    }
    public ExtPath getFileByLocation(LocationInRoot location) {
        Log.print("Get file by location",location);
        LocationWalker walker = new LocationWalker(location);
        while(walker.canDoStep(true)){
            walker.iteration();
        }
        return walker.currentFile;
       
    }
    public ExtPath getFileOptimized(String path){      
        LocationInRoot loc = new LocationInRoot(path);
        if(!existByLocation(loc)){
            getFileAndPopulate(path);
        }
        return getFileIfExists(loc);
    }
    public ExtPath getFileIfExists(LocationInRoot location){
        ExtPath fileByLocation = getFileByLocation(location);
        LocationInRoot mapping = fileByLocation.getMapping();
        Log.print(location,mapping);
        if(location.equals(mapping)){
            Log.print("Equals");
            return fileByLocation;
        }else{
            Log.print("Different");
            return null;
        }
    }
    public void addToCollectionSafe(Collection<ExtPath> collection,LocationInRoot location){
        ExtPath file = getFileIfExists(location);
        if(file != null){
            collection.add(file);
        }
    }
}
