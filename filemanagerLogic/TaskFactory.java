/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import static filemanagerGUI.FileManagerLB.FolderForDevices;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utility.Log;

/**
 *  
 * @author Laimonas Beniušis
 * Produces Tasks (ready to use threads)
 */

//
public class TaskFactory {
    public static ExtFile itemToRename;
    public static ObservableList<ExtFile> dragList = FXCollections.observableArrayList();
    public static ObservableList<ExtFile> markedList = FXCollections.observableArrayList();
    private static final TaskFactory instance = new TaskFactory();
    public static TaskFactory getInstance(){
        
        return instance;
    }
    protected TaskFactory(){}
        // File actions
    private static final Comparator<ExtFile> cmpDesc = (ExtFile f1, ExtFile f2) -> {
        return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
    };
    private static final Comparator<ExtFile> cmpAsc = (ExtFile f1, ExtFile f2) -> {
        return f2.getAbsolutePath().compareTo(f1.getAbsolutePath());
    };  
    
//RENAME
    public void renameTo(String fileToRename, String newName) throws IOException{
        LocationInRoot location  = new LocationInRoot(fileToRename);
        ExtFile file = LocationAPI.getInstance().getFileByLocation(location);
        Path newPath = Paths.get(file.getParent()+File.separator+newName);
        Files.move(file.toPath(), newPath);
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
    
//PREPARE FOR TASKS
    public ExtFile[] prepareForCopy(Collection<ExtFile> fileList, ExtFile dest){
        Log.writeln("List recieved in task");
        
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ExtFile> list = new ArrayList<>();
        for(ExtFile file:fileList){
            
            LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
            ExtFile newFile = LocationAPI.getInstance().getFileByLocation(location);
            if(newFile.getIdentity().equals("folder")){
                ExtFolder folder = (ExtFolder) newFile;
                ArrayList<ExtFile> subList = new ArrayList<>();
                subList.addAll(folder.getListRecursive());
                for(ExtFile subFile:subList){
                    subFile.setRelativePath(folder);
                }
                folder.setRelativePath(folder.getName());
                list.addAll(subList);
                list.add(folder);
            }else{
                newFile.setRelativePath(newFile.getName());
                list.add(newFile);  
            }
            
        }
        list.sort(cmpDesc);
        Log.writeln("List after computing");
        for(ExtFile file:list){
            Log.writeln(file.getAbsolutePath());
        }
        ExtFile[] array = new ExtFile[list.size()];
        array = list.toArray(array);
        for (ExtFile array1 : array) {
            array1.setDestination(Paths.get(dest.getAbsolutePath()+File.separatorChar + array1.getRelativePath()));
            
        }
        return array;
        
    }   
    public ExtFile[] prepareForDelete(Collection<ExtFile> fileList){
    Log.writeln("List recieved in task");
        
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ExtFile> list = new ArrayList<>();
        for(ExtFile file:fileList){
            
            LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
            ExtFile newFile = LocationAPI.getInstance().getFileByLocation(location);
            if(newFile.getIdentity().equals("folder")){
                ExtFolder folder = (ExtFolder) newFile;
                ArrayList<ExtFile> subList = new ArrayList<>();
                subList.addAll(folder.getListRecursive());
                list.addAll(subList);
                list.add(folder);
            }else{
                list.add(newFile);  
            }
            
        }
        list.sort(cmpAsc);
        Log.writeln("List after computing");
        for(ExtFile file:list){
            Log.writeln(file.getAbsolutePath());
        }
        ExtFile[] array = new ExtFile[list.size()];
        array = list.toArray(array);
        return array;
        
    } 
    public ExtFile[] prepareForMove(Collection<ExtFile> fileList,ExtFile dest){
       Log.writeln("List recieved in task");
        
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ExtFile> list = new ArrayList<>();
        for(ExtFile file:fileList){
            
            LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
            ExtFile newFile = LocationAPI.getInstance().getFileByLocation(location);
            if(newFile.getIdentity().equals("folder")){
                ExtFolder folder = (ExtFolder) newFile;
                ArrayList<ExtFile> subList = new ArrayList<>();
                subList.addAll(folder.getListRecursive());
                for(ExtFile subFile:subList){
                    subFile.setRelativePath(folder);
                }
                folder.setRelativePath(folder.getName());
                list.addAll(subList);
                list.add(folder);
            }else{
                newFile.setRelativePath(newFile.getName());
                list.add(newFile);  
            }
            
        }
        list.sort(cmpDesc);
        Log.writeln("List after computing");
        for(ExtFile file:list){
            Log.writeln(file.getAbsolutePath());
        }
        ExtFile[] array = new ExtFile[list.size()];
        array = list.toArray(array);
        for (ExtFile array1 : array) {
            array1.setDestination(Paths.get(dest.getAbsolutePath()+File.separatorChar + array1.getRelativePath()));
            
        }
        return array;
        
    
    }
    
//TASKS    
    public ExtTask copyFiles(Collection<ExtFile> fileList, ExtFile dest){  
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                String str = "";
                updateMessage("Populating list for copy");
                ExtFile[] list = prepareForCopy(fileList,dest);
                for(int i=0; i<list.length; i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        break;
                    }
                    
                    str = "Source: \t\t"+list[i].getAbsolutePath()+"\n";
                    str +="Destination: \t"+list[i].getDestination();
                    updateMessage(str);
                    updateProgress(i+0.5, list.length);
                    try{
                        Files.copy(list[i].toPath(),list[i].getDestination());
                        list[i].setOperationSuccessfull(true);
                        System.out.println("OK:"+list[i].getAbsolutePath());
                        
                    }catch(Exception e){
                        list[i].setOperationSuccessfull(false);
                        System.out.println("Error:"+list[i].getAbsolutePath()+" "+e.getLocalizedMessage());
                    }
                    updateProgress(i+1, list.length);
                    //updateMessage(str);
                    
                }
                updateMessage("FINISHED");
                return null;
            }
        };
    }
    public ExtTask moveFiles(Collection<ExtFile> fileList, ExtFile dest){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                ArrayList<ExtFile> leftFolders = new ArrayList<>();
                String str = "";
                updateMessage("Populating list for move");
                ExtFile[] list = prepareForMove(fileList,dest);
                int index1 = 0;
                for(; index1<list.length; index1++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        break;
                    }
                    
                    str = "Source: \t\t"+list[index1].getAbsolutePath()+"\n";
                    str +="Destination: \t"+list[index1].getDestination();
                    updateMessage(str);
                    updateProgress(index1+0.5, list.length+leftFolders.size());
                    try{
                        if(list[index1].getIdentity().equals("folder")){
                            leftFolders.add(list[index1]);
                            Files.createDirectory(list[index1].getDestination());
                            Log.writeln("Added to flders:"+list[index1].getAbsolutePath());
                        }else{
                            Files.move(list[index1].toPath(),list[index1].getDestination());
                            list[index1].setOperationSuccessfull(true);
                            System.out.println("OK:"+list[index1].getAbsolutePath());
                        }
                    }catch(Exception e){
                        list[index1].setOperationSuccessfull(false);
                        System.out.println("Error:"+list[index1].getAbsolutePath()+" "+e.getLocalizedMessage());
                    }
                    updateProgress(index1+1, list.length+leftFolders.size());
                    //updateMessage(str);
                }
                updateMessage("Deleting leftover folders");
                int i=0;
                Log.writeln("Folders size: "+leftFolders.size());
                leftFolders.sort(cmpDesc);
                for(;i<leftFolders.size();i++){
                    try{
                        Log.writeln("Deleting "+leftFolders.get(i));
                        Files.delete(leftFolders.get(i).toPath());
                    }catch(Exception x){
                        
                    }
                    updateProgress(index1+i+2, list.length+leftFolders.size());
                }
                updateProgress(index1+i+2, list.length+leftFolders.size());
                updateMessage("FINISHED");
                return null;
            }
            
        };
    }
    public ExtTask deleteFiles(Collection<ExtFile> fileList){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                String str = "";
                updateMessage("Populating list for deletion");
                ExtFile[] list = prepareForDelete(fileList);
                for(int i=0; i<list.length; i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        break;
                    }

                    str = "Deleting: \t"+list[i].getAbsolutePath();
                    updateMessage(str);
                    updateProgress(i+0.5, list.length);
                    try{
                        list[i].setOperationSuccessfull(Files.deleteIfExists(list[i].toPath()));
                     
                    }catch(Exception e){
                        list[i].setOperationSuccessfull(false);
                        System.out.println("Error:"+list[i].getAbsolutePath()+" "+e.getLocalizedMessage());
                    }
                    updateProgress(i+1, list.length);
                    //updateMessage(str);
                }
                updateMessage("FINISHED");
                return null;
            }
        };
    }
    
    
    
    
    private void populateRecursiveParallelInner(ExtFolder folder, int level, final int MAX_DEPTH){
        if(level<MAX_DEPTH){
            LocationInRoot loc = LocationAPI.getInstance().getLocationMapping(folder.getAbsolutePath());
            if(!LocationAPI.getInstance().existByLocation(loc)){
                LocationAPI.getInstance().putByLocation(loc, folder);
            }
            folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(loc);
            if(!folder.isPopulated()){
                folder.populateFolder();
                Log.writeln("Folder Iteration "+level+"::"+folder.getAbsolutePath());
            }
            
            for(ExtFolder fold:folder.getFoldersFromFiles()){
                LocationInRoot location = LocationAPI.getInstance().getLocationMapping(fold.getAbsolutePath());
                LocationAPI.getInstance().removeByLocation(location);
                populateRecursiveParallelInner(fold, level+1,MAX_DEPTH);

            }
        }
    }
    public ExtTask populateRecursiveParallel(ExtFolder folder, final int MAX_DEPTH){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                int level = 0;
                populateRecursiveParallelInner(folder,level,MAX_DEPTH);
            return null;
            }
        };
    }
}
