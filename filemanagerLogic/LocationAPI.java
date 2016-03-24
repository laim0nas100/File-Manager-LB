/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import static filemanagerGUI.FileManagerLB.FolderForDevices;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import utility.Log;

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
    public LocationInRoot getLocation(String path){
        LocationInRoot location = new LocationInRoot(path);
        return location;
    }
    public boolean existByLocation(LocationInRoot location) {
        int i = 0;
        ExtFolder folder = FolderForDevices;
        while (i < location.length()) {
            if (folder.files.containsKey(location.at(i))) {
                folder = (ExtFolder) folder.files.get(location.at(i));
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public void removeByLocation(LocationInRoot location) {
        int i = 0;
        ExtFolder folder = FolderForDevices;
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.files.get(location.at(i++));
        }
        folder.files.remove(location.at(i));
    }

    public void putByLocation(LocationInRoot location, ExtFile file) {
        int i = 0;
        ExtFolder folder = FolderForDevices;
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.files.get(location.at(i++));
        }
        folder.files.put(location.getName(), file);
    }

    public ExtFile getFileByLocation(LocationInRoot location) {
        if(location.length()==0){
            return FolderForDevices;
        }
        try{
            int i = 0;
            ExtFolder folder = FolderForDevices;
            Log.writeln("Request:" + location.toString());
            while (i < location.length()-1) {
                folder = (ExtFolder) folder.files.get(location.at(i++));
                //Log.writeln(folder.propertyName.get());
            }
        
        return folder.files.get(location.getName());
        }catch(Exception x){
            x.printStackTrace();
            return null;
        }
       
    }
    
}
