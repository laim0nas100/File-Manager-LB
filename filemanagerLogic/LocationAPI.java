/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

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

    public boolean existByLocation(ExtFolder root, LocationInRoot location) {
        int i = 0;
        ExtFolder folder = root;
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

    public void removeByLocation(ExtFolder root, LocationInRoot location) {
        int i = 0;
        ExtFolder folder = (ExtFolder) root.files.get(location.coordinates.get(i++));
        for (; i < location.length() - 1; i++) {
            folder = (ExtFolder) folder.files.get(location.at(i));
        }
        folder.files.remove(location.at(i));
    }

    public void putByLocation(ExtFolder root, LocationInRoot location, ExtFile file) {
        int i = 0;
        ExtFolder folder = root;
        while (i < location.length() - 1) {
            folder = (ExtFolder) folder.files.get(location.at(i++));
        }
        folder.files.put(location.getName(), file);
    }

    public ExtFile getFileByLocation(ExtFolder root, LocationInRoot location) {
        if(location.length()==0){
            return root;
        }
        try{
            int i = 0;
            ExtFolder folder = root;
            Log.writeln("Request:" + location.toString());
            while (i < location.length()-1) {
                folder = (ExtFolder) folder.files.get(location.at(i++));
                Log.writeln(folder.getAbsolutePath());
            }
        
        return folder.files.get(location.at(i));
        }catch(Exception x){
            x.printStackTrace();
            return null;
        }
       
    }
    
}
