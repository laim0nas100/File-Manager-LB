/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

//import static filemanagerGUI.FileManagerLB.rootDirectory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerGUI.ViewManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utility.Log;

/**
 *
 * @author lemmin
 */
public class ManagingClass {
    
    
    private Vector<LocationInRoot> folderCache;
    private int cacheIndex;
    public ExtFolder currentDir;
    public static ExtFolder root;
    public ManagingClass(ExtFolder root){
        this.root = root;
        folderCache = new Vector<>();
        cacheIndex = 0;
    }
    public void changeDirTo(ExtFolder file){
            if(file.isRoot()){
                currentDir = root;
                //currentDir.update();
            }else{
                file.update();
                LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
               
                if(!existByLocation(root,location)){
                    //Log.writeln(root.files.keySet());
                    Log.writeln("Put "+file.getAbsolutePath()+" to:"+location.toString());
                    putByLocation(root, location, file);
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
            currentDir = (ExtFolder) this.getFileByLocation(root, folderCache.get(++cacheIndex));
            currentDir.update();
        } 
        Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToPrevious(){
        
        if(cacheIndex-1>=0){
            currentDir = (ExtFolder) this.getFileByLocation(root, folderCache.get(--cacheIndex));
            currentDir.update();
        }
        Log.writeln(cacheIndex+" : "+folderCache);
    }
    public void changeToParent(){
        if(!currentDir.isRoot()){
            try {
                LocationInRoot location = new LocationInRoot(currentDir.getAbsolutePath());
//                Log.writeln("Absolute Path:"+currentDir.getAbsolutePath());
//                Log.writeln("CurrentLocation:"+location.toString());
                location = location.getParentLocation();
//                Log.writeln("ParentLocation:"+location.toString()+" >");
                ExtFolder folder = (ExtFolder) getFileByLocation(root, location);
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
        list.addAll(root.getListRecursive());
        return list;  
    }
    public void printAllContents(){
        for(ExtFile file:root.getListRecursive()){
            Log.writeln(file.getAbsolutePath());
        }
    }
    
    
    //RENAME
    public boolean renameTo(ExtFile fileToRename, String newName){
        return renameTo(root,fileToRename,newName);
    }
    public boolean renameByRegex(ExtFile fileToRename, String regex, String replacement){
       return renameByRegex(root,fileToRename,regex,replacement);
    }
    public boolean renameTo(ExtFolder root,ExtFile fileToRename,String newName){
        ExtFile newPath = new ExtFile(fileToRename.getParentFile().getPath() + File.separator + newName);
        try{
            LocationInRoot oldLoc = new LocationInRoot(root,fileToRename);
            LocationInRoot newLoc = new LocationInRoot(oldLoc);
            newLoc.setName(newName);
            Files.move(fileToRename.toPath(), newPath.toPath());
            fileToRename.name.set(newName);
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
            Vector<LocationInRoot> saveList = new Vector<>();
            for(int i=0; i<cacheIndex+1; i++){
                saveList.add(folderCache.get(i));
            }
            folderCache.clear();
            folderCache.addAll(saveList);
        }
        if(folder.isRoot()){
            folderCache.add(new LocationInRoot(folder.name.get()).getParentLocation());
        }else{            
            folderCache.add(new LocationInRoot(folder.getAbsolutePath()));
        }
        cacheIndex = folderCache.size()-1;
    }
    
    public ExtFile getFileByLocation(ExtFolder root,LocationInRoot location){

        int i=0;
        ExtFolder folder = root;
        Log.writeln("Request:"+location.toString());
        while(i<location.length()){
           folder = (ExtFolder) folder.files.get(location.at(i++));
           //System.out.print(folder.getAbsolutePath());
        }
        return folder;
    }
    public void renameRootKeys(ExtFolder root,LocationInRoot newLoc,LocationInRoot oldLoc ){
        
        ExtFile file = getFileByLocation(root,oldLoc);
        if(file.getIdentity().equals("file")){
            ExtFile newFile = new ExtFile(file.getParentFile().getAbsolutePath()+File.separatorChar+newLoc.getName());
            this.removeByLocation(root, oldLoc);
            this.putByLocation(root, newLoc, newFile);
        }else if(file.getIdentity().equals("folder")){
            ExtFolder newFile = new ExtFolder(file.getParentFile().getAbsolutePath()+File.separatorChar+newLoc.getName());
            newFile.populateRecursive();
            this.removeByLocation(root, oldLoc);
            this.putByLocation(root, newLoc, newFile);
        }
    }
    public void removeByLocation(ExtFolder root,LocationInRoot location){
        int i =0;
        ExtFolder folder = (ExtFolder) root.files.get(location.coordinates.get(i++));
        for(;i<location.length()-1;i++){
            folder = (ExtFolder) folder.files.get(location.at(i));
        }
        folder.files.remove(location.at(i));
    }
    
    public void putByLocation(ExtFolder root,LocationInRoot location, ExtFile file){
        int i =0;
        ExtFolder folder = root;
        while(i<location.length()-1){
            folder = (ExtFolder) folder.files.get(location.at(i++));
        }
        folder.files.put(location.getName(),file);
    }
    public boolean existByLocation(ExtFolder root,LocationInRoot location){
        int i =0;
        ExtFolder folder = root;
        while(i<location.length()){
            if(folder.files.containsKey(location.at(i))){
                folder = (ExtFolder) folder.files.get(location.at(i++));
            }else{
                return false;
            }
        }
        return true;
    }
    
    
    
    // File actions
    private static final Comparator<ExtFile> cmpDesc = (ExtFile f1, ExtFile f2) -> {
        return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
    };
    private static final Comparator<ExtFile> cmpAsc = (ExtFile f1, ExtFile f2) -> {
        return f2.getAbsolutePath().compareTo(f1.getAbsolutePath());
    };  
    
    
    
    public ExtFile[] prepareForCopy(Collection<ExtFile> fileList, ExtFile dest){ 
        Vector<ExtFile> list = new Vector<>();
        list.addAll(fileList);
        list.sort(cmpDesc);
        ExtFile[] array = new ExtFile[list.size()];
        array = list.toArray(array);
        for (ExtFile array1 : array) {
            //Log.writeln(array1.getAbsolutePath());
            array1.setDestination(Paths.get(dest.getAbsolutePath()+File.separatorChar + array1.getRelativePath()));
        }
        return array;
        
    }
    
    public ExtFile[] prepareForDelete(Collection<ExtFile> fileList){
       Vector<ExtFile> list = new Vector<>();
       list.addAll(fileList);
       list.sort(cmpAsc);
       ExtFile[] array = new ExtFile[0];
       array = list.toArray(array);
       return array; 
    }
    public ExtFile[] prepareForMove(Collection<ExtFile> fileList,ExtFile dest){
       Vector<ExtFile> list = new Vector<>();
        list.addAll(fileList);
        list.sort(cmpAsc);
        ExtFile[] array = new ExtFile[list.size()];
        array = list.toArray(array);
        for (ExtFile array1 : array) {
            //Log.writeln(array1.getAbsolutePath());
            array1.setDestination(Paths.get(dest.getAbsolutePath()+File.separatorChar + array1.getRelativePath()));
        }
        return array;
    }
    
    
    
    
    
}
