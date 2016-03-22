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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Vector;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import utility.Log;

/**
 *  Produces background tasks (threads)
 * @author Laimonas Beniušis
 */

//
public class TaskFactory {
    public static ObservableList<ExtFile> selectedList;

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
    
    public ExtFile[] prepareForCopy(Collection<ExtFile> fileList, ExtFile dest){
        Log.writeln("List recieved in task");
        
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        Vector<ExtFile> list = new Vector<>();
        for(ExtFile file:fileList){
            
            LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
            ExtFile newFile = LocationAPI.getInstance().getFileByLocation(FolderForDevices, location);
            if(newFile.getIdentity().equals("folder")){
                ExtFolder folder = (ExtFolder) newFile;
                Vector<ExtFile> subList = new Vector<>();
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
        Vector<ExtFile> list = new Vector<>();
        for(ExtFile file:fileList){
            
            LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
            ExtFile newFile = LocationAPI.getInstance().getFileByLocation(FolderForDevices, location);
            if(newFile.getIdentity().equals("folder")){
                ExtFolder folder = (ExtFolder) newFile;
                Vector<ExtFile> subList = new Vector<>();
                subList.addAll(folder.getListRecursive());
                list.addAll(subList);
                list.add(folder);
            }else{
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
        return array;
        
    }
    
    public ExtFile[] prepareForMove(Collection<ExtFile> fileList,ExtFile dest){
       Log.writeln("List recieved in task");
        
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        Vector<ExtFile> list = new Vector<>();
        for(ExtFile file:fileList){
            
            LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
            ExtFile newFile = LocationAPI.getInstance().getFileByLocation(FolderForDevices, location);
            if(newFile.getIdentity().equals("folder")){
                ExtFolder folder = (ExtFolder) newFile;
                Vector<ExtFile> subList = new Vector<>();
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
        list.sort(cmpAsc);
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
                return null;
            }
            
        };
    }
    public ExtTask moveFiles(Collection<ExtFile> fileList, ExtFile dest){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                Vector<ExtFile> leftFolders = new Vector<>();
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
                        }
                        Files.move(list[index1].toPath(),list[index1].getDestination());
                        list[index1].setOperationSuccessfull(true);
                        System.out.println("OK:"+list[index1].getAbsolutePath());
                    }catch(Exception e){
                        list[index1].setOperationSuccessfull(false);
                        System.out.println("Error:"+list[index1].getAbsolutePath()+" "+e.getLocalizedMessage());
                    }
                    updateProgress(index1+1, list.length+leftFolders.size());
                    //updateMessage(str);
                }
                updateMessage("Deleting leftover folders");
                int i=0;
                for(;i<leftFolders.size()-1;i++){
                    try{
                    Files.delete(leftFolders.get(i).toPath());
                    }catch(Exception x){
                        
                    }
                    updateProgress(index1+i+2, list.length+leftFolders.size());
                }
                updateProgress(index1+i+2, list.length+leftFolders.size());
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
                return null;
            }
        };
    }

}
