/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import utility.Log;
import static filemanagerGUI.FileManagerLB.ArtificialRoot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class LocationAPI {
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
        
        if(!pathl.isEmpty()){
            try{
                pathl = pathl.toUpperCase();
                Path path = Paths.get(pathl).toRealPath();
                ExtFile tempFile = new ExtFile(path.toString());
                if(!tempFile.isRoot()){
                    FileManagerLB.mountDevice(path.getRoot().toString());
                }
                if(Files.exists(path)){
                    LocationInRoot fileLocation = new LocationInRoot(path.toString());
                    if(!existByLocation(fileLocation)){
                        ExtFolder folder = new ExtFolder(new File(path.toString()).getParent());
                        putByLocationRecursive(folder.getMapping(), folder);
                        Log.writeln(folder.getMapping());
                        folder.update(); 
                    }
                    file = getFileByLocation(fileLocation);
                }else{
                    file = null;
                }
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
            if (folder.files.containsKey(location.at(i))) {
                folder = (ExtFolder) folder.files.get(location.at(i));
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
            folder = (ExtFolder) folder.files.get(location.at(i++));
        }
        folder.files.remove(location.at(i));
    }

    public void putByLocation(LocationInRoot location, ExtFile file) {
        int i = 0;
        ExtFolder folder = ArtificialRoot;
        //Log.writeln("Put by location:"+location.toString());
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.files.get(location.at(i++));

        }
        folder.files.put(location.getName(), file);
    }
    public void putByLocationRecursive(LocationInRoot location, ExtFile file) {
        int i = 0;
        ExtFolder folder = ArtificialRoot;
        
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.files.get(location.at(i++));
            Log.writeln(i+" "+location.length()+" Current:"+folder.getAbsolutePath());
            folder.update();
        }
        folder.files.put(location.getName(), file);
    }

    public ExtFile getFileByLocation(LocationInRoot location) {
        if(location.length()==0){
            return ArtificialRoot;
        }
        try{
            int i = 0;
            ExtFolder folder = ArtificialRoot;
            //Log.writeln("Request:" + location.toString());
            while (i < location.length()-1) {
                folder = (ExtFolder) folder.files.get(location.at(i++));
                //Log.writeln(folder.propertyName.get());
            }
        
        return folder.files.get(location.getName());
        }catch(Exception x){
            ErrorReport.report(x);
            return null;
        }
       
    }
    public boolean exists(String file){
        try{
            Path get = Paths.get(file).toRealPath();
            return Files.exists(get);
        }catch(Exception e){
            return false;
        }
    }
    public boolean isDirectory(String file){
        try{
            Path get = Paths.get(file).toRealPath();
            return Files.isDirectory(get);
        }catch(Exception e){
            return false;
        }
    
    }
    public String getRealPath(String path) throws IOException{
            Path get = Paths.get(path).toRealPath();
            return get.toString();
    }
    
}
