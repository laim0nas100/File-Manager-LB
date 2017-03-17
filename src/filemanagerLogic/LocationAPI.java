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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class LocationAPI {
//    private static class LocationInRootWalker{
//        public LocationInRootWalker(LocationInRoot location,UnaryOperator<ExtFolder> midway){
//            int i = 0;
//            ExtFolder folder = FileManagerLB.ArtificialRoot;
//            //Log.writeln(location.toString());
//            while (i< location.length()-1) {
//                if(location.isUppercase()){
//                    if (folder.hasFileIgnoreCase(location.at(i))) {
//                        folder = (ExtFolder) folder.getIgnoreCase(location.at(i));
//                    } else {
//                        return false;
//                    } 
//                }else{
//                    if (folder.files.containsKey(location.at(i))) {
//                        folder = (ExtFolder) folder.files.get(location.at(i));
//                    } else {
//                        return false;
//                    } 
//                }
//                i++;
//            }
//            
//        }
//    }
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
        //pathl = ExtStringUtils.upperCase(pathl);
        
        pathl = pathl.trim();
        Log.write("getFileAndPopulate:"+pathl);
        if(!pathl.isEmpty() && !pathl.equals(FileManagerLB.ROOT_NAME)){
            
            try{
                try{
                    if(File.separator.equals("\\")){ //Directory Mounting BS on Windows
                        Path path = Paths.get(pathl).toRealPath();
                        pathl = path.toAbsolutePath().toString();
                        Log.write("realPath:"+path);
                        if(!FileManagerLB.getRootSet().contains(path.getRoot().toString().toUpperCase(Locale.ROOT))){
                            if(FileManagerLB.mountDevice(path.getRoot().toString())){
                                FileManagerLB.ArtificialRoot.update();
                            }
                        }
                    }
                }catch(Exception e){
//                    ErrorReport.report(new Exception("windows auto pathing exception: " +pathl));
                }
                LocationInRoot loc = new LocationInRoot(pathl);
                Log.write("Location:",loc);
                this.populateByLocation(loc.getParentLocation());
                
                file = getClosestFileByLocation(loc);
                
            }catch(Exception e){
                ErrorReport.report(e);
            }
        }
        return file;           
            
    }
    public boolean existByLocation(LocationInRoot location) {
        int i = 0;
        ExtFolder folder = FileManagerLB.ArtificialRoot;
        //Log.writeln(location.toString());
        while (i< location.length()-1) {
            if(location.isUppercase()){
                if (folder.hasFileIgnoreCase(location.at(i))) {
                    folder = (ExtFolder) folder.getIgnoreCase(location.at(i));
                } else {
                    return false;
                } 
            }else{
                if (folder.files.containsKey(location.at(i))) {
                    folder = (ExtFolder) folder.files.get(location.at(i));
                } else {
                    return false;
                } 
            }
            i++;
        }
        return folder.hasFileIgnoreCase(location.getName());
    }
    
    public void removeByLocation(LocationInRoot location) {
        int i = 0;
        ExtFolder folder = FileManagerLB.ArtificialRoot;
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.getIgnoreCase(location.at(i++));
        }
        String key = folder.getKey(location.at(i));
        folder.files.remove(key);
    }

    public void putByLocation(LocationInRoot location, ExtPath file) {
        int i = 0;
        ExtFolder folder = FileManagerLB.ArtificialRoot;
        //Log.writeln("Put by location:"+location.toString());
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.getIgnoreCase(location.at(i++));

        }
        folder.files.put(file.propertyName.get(), file);
    }
    public void putByLocationRecursive(LocationInRoot location, ExtPath file) {
        int i = 0;
        ExtFolder folder = FileManagerLB.ArtificialRoot;
        
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.getIgnoreCase(location.at(i++));
            //Log.writeln(i+" "+location.length()+" Current:"+folder.getAbsolutePath());
            folder.update();
        }
        folder.files.put(file.propertyName.get(), file);
    }
    private void populateByLocation(LocationInRoot location){
//        int i = 0;
        ExtFolder folder = FileManagerLB.ArtificialRoot;
        Log.writeln("Populate by location",location);
        //folder.update();
        for(String s:location.coordinates) {
            
            if(folder.hasFileIgnoreCase(s)){
                folder = (ExtFolder) folder.getIgnoreCase(s);
            }else{
                return;
            }
            folder.update();
            
            //Log.writeln(i+" "+location.length()+" Current:"+folder.getAbsolutePath());
            
        }
    }
    public ExtPath getFileByLocation(LocationInRoot location) {
        if(location.length()==0){
            return FileManagerLB.ArtificialRoot;
        }
        try{
            ExtFolder folder = FileManagerLB.ArtificialRoot;
            ExtPath file = FileManagerLB.ArtificialRoot;
            //Log.writeln("Request:" + location.toString());
            for (String s:location.coordinates) {
                if(folder.hasFileIgnoreCase(s)){
                    file = folder.getIgnoreCase(s);
                    if(file.getIdentity().equals(Enums.Identity.FOLDER)){
                        folder = (ExtFolder) file; 
                    }else{
                        return file;
                    }
                }else{
                    return folder;
                }
                //Log.writeln(folder.propertyName.get());
            }
        
        return file;
        }catch(Exception x){
            ErrorReport.report(x);
            return null;
        }
       
    }
    public ExtPath getClosestFileByLocation(LocationInRoot location){
        if(location.length()==0){
            return FileManagerLB.ArtificialRoot;
        }
        ExtFolder folder = FileManagerLB.ArtificialRoot;
        ExtPath file = FileManagerLB.ArtificialRoot;
        try{
            for (String s:location.coordinates) {
                if(folder.hasFileIgnoreCase(s)){
                    file = folder.getIgnoreCase(s);
                    if(file.getIdentity().equals(Enums.Identity.FOLDER)){
                        folder = (ExtFolder) folder.getIgnoreCase(s);        
                    }else{
                        return file;
                    }
                }else{
                    return folder;
                }
            }
        }catch(Exception x){
            ErrorReport.report(x);
        }
        return file;
    }
    public ExtPath getFileOptimized(String path){
        LocationInRoot loc = new LocationInRoot(path);
        if(!this.existByLocation(loc)){
            return getFileAndPopulate(path);
        }else{
            return getFileByLocation(loc);
        }
    }
}
