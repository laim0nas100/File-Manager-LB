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
import filemanagerGUI.ViewManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utility.Log;

/**
 *
 * @author lemmin
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
                //currentDir.update();
            }else if(file.isRoot()){
                LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
                currentDir = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location);
                currentDir.update();
            }else{
                if(file.isPopulated()){
                    file.update();
                }else{
                    file.populateFolder();
                }
                LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
               
                if(!LocationAPI.getInstance().existByLocation(location)){
                    //Log.writeln(root.files.keySet());
                    Log.writeln("Put "+file.getAbsolutePath()+" to:"+location.toString());
                    LocationAPI.getInstance().putByLocation(location, file);
                }else{
                    Log.writeln("Location "+location.toString()+" Exists");
                }
                currentDir = file;
            }

        Log.writeln(cacheIndex+" : "+folderCache);
        addCacheNode(currentDir);
        
        
    }
    public void changeToForward(){
       
        if(cacheIndex+1<folderCache.size()){
            currentDir = (ExtFolder) LocationAPI.getInstance().getFileByLocation(folderCache.get(++cacheIndex));
            currentDir.update();
        } 
        Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToPrevious(){
        
        if(cacheIndex-1>=0){
            currentDir = (ExtFolder) LocationAPI.getInstance().getFileByLocation(folderCache.get(--cacheIndex));
            currentDir.update();
        }
        Log.writeln(cacheIndex+" : "+folderCache);
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
    
    
    //RENAME
    public boolean renameTo(ExtFile fileToRename, String newName){
        return renameTo(FolderForDevices,fileToRename,newName);
    }
    public boolean renameByRegex(ExtFile fileToRename, String regex, String replacement){
       return renameByRegex(FolderForDevices,fileToRename,regex,replacement);
    }
    public boolean renameTo(ExtFolder root,ExtFile fileToRename,String newName){
        ExtFile newPath = new ExtFile(fileToRename.getParentFile().getPath() + File.separator + newName);
        try{
            LocationInRoot oldLoc = new LocationInRoot(root,fileToRename);
            LocationInRoot newLoc = new LocationInRoot(oldLoc);
            newLoc.setName(newName);
            Files.move(fileToRename.toPath(), newPath.toPath());
            fileToRename.propertyName.set(newName);
            this.renameRootKeys(root, newLoc, oldLoc);
        }catch(Exception e){
                return false;
            }
        return true;
    }
    
    public boolean renameByRegex(ExtFolder root,ExtFile fileToRename, String regex,String replacement){
        String name = fileToRename.getName();
        name = name.replaceAll(regex, replacement);
        return renameTo(root,fileToRename,name);
    }
    
    
    //LocationInRoot Specifics
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
    
    public void renameRootKeys(ExtFolder root,LocationInRoot newLoc,LocationInRoot oldLoc ){
        
        ExtFile file = LocationAPI.getInstance().getFileByLocation(oldLoc);
        if(file.getIdentity().equals("file")){
            ExtFile newFile = new ExtFile(file.getParentFile().getAbsolutePath()+File.separatorChar+newLoc.getName());
            LocationAPI.getInstance().removeByLocation(oldLoc);
            LocationAPI.getInstance().putByLocation(newLoc, newFile);
        }else if(file.getIdentity().equals("folder")){
            ExtFolder newFile = new ExtFolder(file.getParentFile().getAbsolutePath()+File.separatorChar+newLoc.getName());
            newFile.populateRecursive();
            LocationAPI.getInstance().removeByLocation(oldLoc);
            LocationAPI.getInstance().putByLocation(newLoc, newFile);
        }
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
    
    
    

    
    
    
    
    
    
    
    
    
}
