/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import LibraryLB.Log;
import static filemanagerGUI.FileManagerLB.ArtificialRoot;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class LocationAPI {
    private static class LocationException extends Exception{
        
    }
    private static final LocationAPI INSTANCE = new LocationAPI();
    protected LocationAPI() {};
    public static LocationAPI getInstance(){
        return INSTANCE;
    }
    public LocationInRoot getLocationMapping(String path){
        return new LocationInRoot(path);
    }
    public ExtFile getFileAndPopulate(String pathl){
        ExtFile file = ArtificialRoot;
        //pathl = ExtStringUtils.upperCase(pathl);
        
        if(!pathl.isEmpty()){
            try{
                ExtFile tempFile = new ExtFile(pathl);
                try{
                    if(File.separator.equals("\\")){ //Directory Mounting BS on Windows
                        Path path = Paths.get(pathl).toRealPath();
                        tempFile = new ExtFile(path.toAbsolutePath().toString());
                        if(!tempFile.isRoot()){
                            if(FileManagerLB.mountDevice(path.getRoot().toString())){
                                ArtificialRoot.update();
                            }
                        }
                    }
                }catch(Exception e){
                    //ErrorReport.report(new Exception("windows auto pathing exception"));
                }
                //loc = tempFile.getMapping();
                if(tempFile.isDirectory()){
                    tempFile = new ExtFolder(tempFile.getAbsoluteDirectory());
                }
                LocationInRoot loc = tempFile.getMapping();
                Log.write("Location:",loc);
                Log.write("Path:",tempFile.getAbsoluteDirectory());
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
        ExtFolder folder = ArtificialRoot;
        //Log.writeln(location.toString());
        while (i< location.length()-1) {
            if (folder.hasFileIgnoreCase(location.at(i))) {
                folder = (ExtFolder) folder.getIgnoreCase(location.at(i));
                i++;
            } else {
                return false;
            }
        }
        return folder.files.containsKey(location.getName());
    }
    
    public void removeByLocation(LocationInRoot location) {
        int i = 0;
        ExtFolder folder = ArtificialRoot;
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.getIgnoreCase(location.at(i++));
        }
        String key = folder.getKey(location.at(i));
        folder.files.remove(key);
    }

    public void putByLocation(LocationInRoot location, ExtFile file) {
        int i = 0;
        ExtFolder folder = ArtificialRoot;
        //Log.writeln("Put by location:"+location.toString());
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.getIgnoreCase(location.at(i++));

        }
        folder.files.put(file.propertyName.get(), file);
    }
    public void putByLocationRecursive(LocationInRoot location, ExtFile file) {
        int i = 0;
        ExtFolder folder = ArtificialRoot;
        
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.getIgnoreCase(location.at(i++));
            //Log.writeln(i+" "+location.length()+" Current:"+folder.getAbsolutePath());
            folder.update();
        }
        folder.files.put(file.propertyName.get(), file);
    }
    private void populateByLocation(LocationInRoot location){
        int i = 0;
        ExtFolder folder = ArtificialRoot;
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
    public ExtFile getFileByLocation(LocationInRoot location) {
        if(location.length()==0){
            return ArtificialRoot;
        }
        try{
            ExtFolder folder = ArtificialRoot;
            ExtFile file = ArtificialRoot;
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
    public ExtFile getClosestFileByLocation(LocationInRoot location){
        if(location.length()==0){
            return ArtificialRoot;
        }
        ExtFolder folder = ArtificialRoot;
        ExtFile file = ArtificialRoot;
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
    
}