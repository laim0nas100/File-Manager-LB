/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import LibraryLB.Log;
import static filemanagerGUI.FileManagerLB.ArtificialRoot;
import java.util.Collection;
import javafx.application.Platform;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class ManagingClass {
    
    
    private final ArrayList<LocationInRoot> folderCache;
    private int cacheIndex;
    public ExtFolder currentDir;
    public ManagingClass(ExtFolder root){
        folderCache = new ArrayList<>();
        cacheIndex = 0;
    }
    public void changeDirTo(ExtFolder file){
        Log.writeln("Change dir to: "+file.getAbsoluteDirectory());
            if(file.isAbsoluteRoot()){
                currentDir = ArtificialRoot;
               
            }else if(file.isRoot()){
                currentDir = (ExtFolder) ArtificialRoot.files.get(file.getAbsoluteDirectory());
            }else{
                LocationInRoot location = new LocationInRoot(file.getAbsolutePath());      
                if(!LocationAPI.getInstance().existByLocation(location)){
                    Log.writeln("Put "+file.getAbsolutePath()+" to:"+location.toString());
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
            //currentDir.update();
        } 
        Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToPrevious(){
        
        if(cacheIndex-1>=0){
            currentDir = (ExtFolder) LocationAPI.getInstance()
                    .getFileByLocation(folderCache.get(--cacheIndex));
            //currentDir.update();
        }
        //Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToParent(){
        if(!currentDir.isAbsoluteRoot()){
            try {
                if(currentDir.isRoot()){
                    this.changeDirTo(ArtificialRoot);
                }else{
                LocationInRoot location = new LocationInRoot(currentDir.getAbsolutePath());
//                Log.writeln("Absolute Path:"+currentDir.getAbsolutePath());
//                Log.writeln("CurrentLocation:"+location.toString());
                location = location.getParentLocation();
//                Log.writeln("ParentLocation:"+location.toString()+" >");
                ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location);
//                Log.writeln("Parent path:"+folder.getAbsolutePath());
                this.changeDirTo(folder);
                }
            } catch (Exception ex) {
                ErrorReport.report(ex);
            } 
        }
    }
    public Collection<ExtFile> getCurrentContents(){
        return currentDir.getFilesCollection();       
    }
    public ObservableList<ExtFile> getAllContents(){
        ObservableList<ExtFile> list = FXCollections.observableArrayList();
        list.addAll(ArtificialRoot.getListRecursive());
        return list;  
    }
    public void printAllContents(){
        for(ExtFile file:ArtificialRoot.getListRecursive()){
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
        String newName = "New Folder";
        newName = TaskFactory.resolveAvailableName(currentDir, newName);
        Files.createDirectory(Paths.get(newName));
        ExtFolder folder = new ExtFolder(newName);
        LocationInRoot location = new LocationInRoot(newName);
        LocationAPI.getInstance().putByLocation(location, folder);
    }
    public void createNewFile() throws IOException{
        String newName = "New File";
        newName = TaskFactory.resolveAvailableName(currentDir, newName);
        Files.createFile(Paths.get(newName));
        ExtFile file = new ExtFile(newName);
        LocationInRoot location = new LocationInRoot(newName);
        LocationAPI.getInstance().putByLocation(location, file);
    }
    public boolean hasPrev(){
        return this.cacheIndex!=0;
    }
    public boolean hasForward(){
        return (this.folderCache.size() - (this.cacheIndex+1)>0);
    }
}
