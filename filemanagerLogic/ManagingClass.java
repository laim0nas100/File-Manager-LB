/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

//import static filemanagerGUI.FileManagerLB.rootDirectory;
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

/**
 *
 * @author lemmin
 */
public class ManagingClass {
    
    
    private Vector<LocationInRoot> folderCache;
    private int cacheIndex;
    public ExtFolder currentDir;
    public ExtFolder rootDirectory;
    
    public ManagingClass(ExtFolder root){
        rootDirectory = root;
        changeDirTo(rootDirectory);
        folderCache = new Vector<>();
        cacheIndex = 0;
        try {
            LocationInRoot loc = new LocationInRoot(rootDirectory,rootDirectory);
            folderCache.add(loc);
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
    }
    public void changeDirTo(ExtFolder file){
        currentDir = file;
        currentDir.update();
        addCacheNode(file);
        
        
    }
    public void changeToForward(){
        if(cacheIndex+1<folderCache.size()){
            currentDir = (ExtFolder) this.getFileByLocation(rootDirectory, folderCache.get(++cacheIndex));
            currentDir.update();
        }
    }
        public void changeToPrevious(){
        if(cacheIndex-1>=0){
            currentDir = (ExtFolder) this.getFileByLocation(rootDirectory, folderCache.get(--cacheIndex));
            currentDir.update();
        }
    }
    public void changeToParent(){
        if(!currentDir.getAbsolutePath().equals(rootDirectory.getAbsolutePath())){
            
            try {
                LocationInRoot location;
                location = new LocationInRoot(rootDirectory,currentDir).getParentLocation(); System.out.println("< "+location.toString()+" >");
                ExtFolder folder = (ExtFolder) getFileByLocation(rootDirectory, location);
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
        list.addAll(rootDirectory.getListRecursive());
        return list;  
    }
    public void printAllContents(){
        for(ExtFile file:this.rootDirectory.getListRecursive()){
            System.out.println(file.getAbsolutePath());
        }
    }
    
    
    //RENAME
    public boolean renameTo(ExtFile fileToRename, String newName){
        return renameTo(rootDirectory,fileToRename,newName);
    }
    public boolean renameByRegex(ExtFile fileToRename, String regex, String replacement){
       return renameByRegex(rootDirectory,fileToRename,regex,replacement);
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
        try {
            int i =cacheIndex+1;
            while(i<folderCache.size()){
                folderCache.remove(i++);
            }
            folderCache.add(new LocationInRoot(rootDirectory,folder));
            cacheIndex=folderCache.size()-1;        
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
    }
    
    public ExtFile getFileByLocation(ExtFolder root,LocationInRoot location){
        if(location.length()==0){
            return root;
        }
        int i =0;
        return getFolderByLocationRec((ExtFolder) root.files.get(location.coordinates.get(i)),i+1,location);
    }
    private ExtFolder getFolderByLocationRec(ExtFolder root, int i, LocationInRoot location){
        if(i<location.coordinates.size()){
            if(!root.isPopulated()){
                root.update();
            }
            return getFolderByLocationRec((ExtFolder) root.files.get(location.coordinates.get(i)),i+1,location);
        }else{
            return root;
        }
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
        ExtFolder folder = (ExtFolder) root.files.get(location.coordinates.get(i++));
        for(;i<location.length()-1;i++){
            folder = (ExtFolder) folder.files.get(location.at(i));
        }
        folder.files.put(location.at(i),file);
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
            //System.out.println(array1.getAbsolutePath());
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
            //System.out.println(array1.getAbsolutePath());
            array1.setDestination(Paths.get(dest.getAbsolutePath()+File.separatorChar + array1.getRelativePath()));
        }
        return array;
    }
    
    
    
    
    
}
