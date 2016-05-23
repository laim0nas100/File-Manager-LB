/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import utility.Log;
import static filemanagerGUI.FileManagerLB.ArtificialRoot;
import static filemanagerGUI.FileManagerLB.reportError;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;

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
    public ExtFile getFileAndPopulate(String path){
        ExtFile file;
        
        if(path.isEmpty()){
                file = ArtificialRoot;
        }else {
            if(Files.exists(Paths.get(path))){
                LocationInRoot fileLocation = new LocationInRoot(path);
                if(!existByLocation(fileLocation)){
                    ExtFolder folder = new ExtFolder(new File(path).getParent());
                    putByLocationRecursive(folder.getMapping(), folder);
                    Log.writeln(folder.getMapping());
                    folder.update(); 
                }
                file = getFileByLocation(fileLocation);
            }else{
                file = null;
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
            reportError(x);
            return null;
        }
       
    }
    
}
