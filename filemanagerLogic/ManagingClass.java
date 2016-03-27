/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

//import static filemanagerGUI.FileManagerLB.rootDirectory;
import static filemanagerGUI.FileManagerLB.FolderForDevices;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utility.Log;

/**
 *
 * @author Laimonas Beniušis
 */
public class ManagingClass {
    
    
    private final ArrayList<LocationInRoot> folderCache;
    private ArrayList<ExtFile> currentContents;
    private int cacheIndex;
    public ExtFolder currentDir;
    public ManagingClass(ExtFolder root){
        folderCache = new ArrayList<>();
        cacheIndex = 0;
    }
    public void changeDirTo(ExtFolder file){
        Log.writeln("Change dir to: "+file.getAbsolutePath());
            if(file.isAbsoluteRoot()){
                currentDir = FolderForDevices;
               
            }else if(file.isRoot()){
                currentDir = (ExtFolder) LocationAPI.getInstance().getFileByLocation(
                        new LocationInRoot(file.getAbsolutePath()));
            }else{
                LocationInRoot location = new LocationInRoot(file.getAbsolutePath());      
                if(!LocationAPI.getInstance().existByLocation(location)){
                    //Log.writeln("Put "+file.getAbsolutePath()+" to:"+location.toString());
                    LocationAPI.getInstance().putByLocation(location, file);
                }
                currentDir = file;
            }
        
        //Log.writeln(cacheIndex+" : "+folderCache);
        addCacheNode(currentDir);
        
    }
    public void changeToForward(){
       
        if(cacheIndex+1<folderCache.size()){
            currentDir = (ExtFolder) LocationAPI.getInstance()
                    .getFileByLocation(folderCache.get(++cacheIndex));
            currentDir.update();
        } 
        Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToPrevious(){
        
        if(cacheIndex-1>=0){
            currentDir = (ExtFolder) LocationAPI.getInstance()
                    .getFileByLocation(folderCache.get(--cacheIndex));
            currentDir.update();
        }
        //Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToParent(){
        if(!currentDir.isAbsoluteRoot()){
            try {
                LocationInRoot location = new LocationInRoot(currentDir.getAbsolutePath());
//                Log.writeln("Absolute Path:"+currentDir.getAbsolutePath());
//                Log.writeln("CurrentLocation:"+location.toString());
                location = location.getParentLocation();
//                Log.writeln("ParentLocation:"+location.toString()+" >");
                ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location);
//                Log.writeln("Parent path:"+folder.getAbsolutePath());
                this.changeDirTo(folder);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
            
        }
    }
    public ObservableList<ExtFile> getCurrentContents(){
        ObservableList<ExtFile> list = FXCollections.observableArrayList();
        list.addAll(currentDir.getFilesCollection());
        return list;       
    }
    public ObservableList<ExtFile> getAllContents(){
        ObservableList<ExtFile> list = FXCollections.observableArrayList();
        list.addAll(FolderForDevices.getListRecursive());
        return list;  
    }
    public void printAllContents(){
        for(ExtFile file:FolderForDevices.getListRecursive()){
            Log.writeln(file.getAbsolutePath());
        }
    }
      
    private void addCacheNode(ExtFolder folder){
        if(!folderCache.isEmpty()){
            ArrayList<LocationInRoot> saveList = new ArrayList<>();
            for(int i=0; i<cacheIndex+1; i++){
                saveList.add(folderCache.get(i));
            }
            folderCache.clear();
            folderCache.addAll(saveList);
        }
        if(folder.isAbsoluteRoot()){
            folderCache.add(new LocationInRoot(""));
        }else{            
            folderCache.add(new LocationInRoot(folder.getAbsolutePath()));
        }
        cacheIndex = folderCache.size()-1;
    }
    
    public void createNewFolder() throws IOException{
        String path = this.currentDir.getAbsolutePath();
        String newName = "New Folder";
        while(currentDir.files.containsKey(newName)){
            newName= "New "+newName; 
        }
        path +=File.separator+newName;
        Files.createDirectory(Paths.get(path));
        ExtFolder folder = new ExtFolder(path);
        LocationInRoot location = new LocationInRoot(path);
        LocationAPI.getInstance().putByLocation(location, folder);
    }
    public void createNewFile() throws IOException{
        String path = this.currentDir.getAbsolutePath();
        String newName = "New File";
        while(currentDir.files.containsKey(newName)){
            newName= "New "+newName; 
        }
        path +=File.separator+newName;
        Files.createFile(Paths.get(path));
        ExtFile file = new ExtFile(path);
        LocationInRoot location = new LocationInRoot(path);
        LocationAPI.getInstance().putByLocation(location, file);
    }
}
