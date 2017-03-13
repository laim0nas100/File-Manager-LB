/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import LibraryLB.Log;
import filemanagerGUI.FileManagerLB;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.util.Collection;
import javafx.beans.property.SimpleBooleanProperty;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class ManagingClass {
    
    public SimpleBooleanProperty isVirtual = new SimpleBooleanProperty(false);
    public SimpleBooleanProperty isAbsoluteRoot = new SimpleBooleanProperty(false);
    private final ArrayList<ExtPath> folderCache;
    private int cacheIndex;
    public ExtFolder currentDir;
    public ManagingClass(ExtFolder dir){
        folderCache = new ArrayList<>();
        cacheIndex = 0;
        currentDir = dir;
        isVirtual.bind(currentDir.isVirtual);
        isAbsoluteRoot.bind(currentDir.isAbsoluteRoot);
        
    }
    public void changeDirTo(ExtFolder file){
        currentDir = file;
        isVirtual.bind(currentDir.isVirtual);
        isAbsoluteRoot.bind(currentDir.isAbsoluteRoot);
        addCacheNode(currentDir);
        
    }
    public void changeToForward(){
       
        if(cacheIndex+1 < folderCache.size()){
            currentDir = (ExtFolder) folderCache.get(++cacheIndex);
        } 
        Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToPrevious(){
        
        if(cacheIndex > 0){
            currentDir = (ExtFolder) folderCache.get(--cacheIndex);
        }
        Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToParent(){
        if(!currentDir.isAbsoluteRoot.get()){
            try {
                if(currentDir.isRoot()||(currentDir.equals(FileManagerLB.VirtualFolders))){
                    this.changeDirTo(FileManagerLB.ArtificialRoot);
                }else if(currentDir instanceof VirtualFolder){
                    this.changeDirTo(FileManagerLB.VirtualFolders);
                }else{
                    LocationInRoot location = new LocationInRoot(currentDir.getAbsoluteDirectory());
                    ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location.getParentLocation());
                    this.changeDirTo(folder);
                }
            } catch (Exception ex) {
                ErrorReport.report(ex);
            } 
        }
    }
    public Collection<ExtPath> getCurrentContents(){
        currentDir.update();
        return currentDir.getFilesCollection();       
    }
    public ObservableList<ExtPath> getAllContents(){
        ObservableList<ExtPath> list = FXCollections.observableArrayList();
        list.addAll(FileManagerLB.ArtificialRoot.getListRecursive(false));
        return list;  
    }
    private void addCacheNode(ExtFolder folder){
        if(!folderCache.isEmpty()){
            ArrayList<ExtPath> saveList = new ArrayList<>();
            for(int i=0; i<cacheIndex+1; i++){
                saveList.add(folderCache.get(i));
            }
            folderCache.clear();
            folderCache.addAll(saveList);
        }
        if(folder.isAbsoluteRoot.get()){
            folderCache.add(FileManagerLB.ArtificialRoot);
        }else{            
            folderCache.add(folder);
        }
        cacheIndex = folderCache.size()-1;
    }
    
    public void createNewFolder() throws IOException{
        String newName = "New Folder";
        newName = TaskFactory.resolveAvailablePath(currentDir, newName);
        Files.createDirectory(Paths.get(newName));
        ExtFolder folder = new ExtFolder(newName);
        LocationInRoot location = new LocationInRoot(newName);
        LocationAPI.getInstance().putByLocation(location, folder);
    }
    public void createNewFile() throws IOException{
        String newName = "New File";
        newName = TaskFactory.resolveAvailablePath(currentDir, newName);
        Files.createFile(Paths.get(newName));
        ExtPath file = new ExtPath(newName);
        LocationInRoot location = new LocationInRoot(newName);
        LocationAPI.getInstance().putByLocation(location, file);
    }
    public boolean hasPrev(){
        return this.cacheIndex!=0;
    }
    public boolean hasForward(){
        return this.folderCache.size() > this.cacheIndex+1;
    }
    public boolean hasParent(){
        if(currentDir==null){
            return false;
        }
        return !(currentDir.isAbsoluteRoot.get());
    }
}
