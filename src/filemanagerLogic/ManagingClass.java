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
        rebind();
    }
    private void setCurrentDir(ExtPath path){
        if(path instanceof ExtFolder){
            currentDir = (ExtFolder) path;
            rebind();
        }
        
    }
    public void changeDirTo(ExtFolder file){
        setCurrentDir(file);
        addCacheNode(currentDir);
        
    }
    private void rebind(){
        isVirtual.bind(currentDir.isVirtual);
        isAbsoluteRoot.bind(currentDir.isAbsoluteRoot);
    }
    public void changeToForward(){
       
        if(cacheIndex+1 < folderCache.size()){
            cacheIndex++;
            setCurrentDir(folderCache.get(cacheIndex));
        } 
        Log.print(cacheIndex+" : "+folderCache);
    }
    public void changeToPrevious(){
        
        if(cacheIndex > 0){
            cacheIndex--;
            setCurrentDir(folderCache.get(cacheIndex));
        }
        Log.print(cacheIndex+" : "+folderCache);
    }
    public void changeToParent(){
        if(hasParent()){
            try {
                if(currentDir.isRoot()||(currentDir.equals(FileManagerLB.VirtualFolders))){
                    changeDirTo(FileManagerLB.ArtificialRoot);
                }else if(currentDir instanceof VirtualFolder){
                    changeDirTo(FileManagerLB.VirtualFolders);
                }else{
                    LocationInRoot location = new LocationInRoot(currentDir.getAbsoluteDirectory());
                    ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileIfExists(location.getParentLocation());
                    changeDirTo(folder);
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
    
    public void getCurrentContents(ObservableList<ExtPath> list){
        currentDir.update(list);
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
    
    public ExtPath createNewFolder() throws IOException{
        String newName = "New Folder";
        newName = TaskFactory.resolveAvailablePath(currentDir, newName);
        Files.createDirectory(Paths.get(newName));
        ExtFolder folder = new ExtFolder(newName);
        LocationInRoot location = new LocationInRoot(newName);
        LocationAPI.getInstance().putByLocation(location, folder);
        return folder;
    }
    public ExtPath createNewFile() throws IOException{
        String newName = "New File";
        newName = TaskFactory.resolveAvailablePath(currentDir, newName);
        Files.createFile(Paths.get(newName));
        ExtPath file = new ExtPath(newName);
        LocationInRoot location = new LocationInRoot(newName);
        LocationAPI.getInstance().putByLocation(location, file);
        return file;
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
