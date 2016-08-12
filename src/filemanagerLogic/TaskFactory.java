/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import com.fasterxml.jackson.databind.ObjectMapper;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import LibraryLB.Log;
import LibraryLB.StringOperations;
import filemanagerGUI.MainController;
import filemanagerGUI.ViewManager;
import filemanagerGUI.dialog.DuplicateFinderController;
import filemanagerLogic.fileStructure.ActionFile;
import filemanagerLogic.snapshots.ExtEntry;
import filemanagerLogic.snapshots.Snapshot;
import filemanagerLogic.snapshots.SnapshotAPI;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import utility.ErrorReport;
import utility.FileNameException;

/**
 *  
 * @author Laimonas Beniušis
 * Produces Tasks (ready to use threads)
 */

//
public class TaskFactory {
    
    public  ObservableList<ExtFile> dragList;
    public  ObservableList<String> markedList;
    public  ArrayList<ExtFile> actionList;
    private final HashSet<Character> illegalCharacters;
    private static final TaskFactory instance = new TaskFactory();
    private static final int TRY_LIMIT = 5;
    public static String dragInitWindowID ="";
    public static TaskFactory getInstance(){
        
        return instance;
    }
    protected TaskFactory(){
        illegalCharacters = new HashSet<>();
        Character[] array = new Character[] {
                '\\',
                '/',
                '<',
                '*',
                '>',
                '|',
                '?',
                ':',
                '\"'
            };
            illegalCharacters.addAll(Arrays.asList(array));
        dragList = FXCollections.observableArrayList();
        markedList = FXCollections.observableArrayList();
        actionList = new ArrayList<>();
    }
        
    private static final Comparator<ActionFile> cmpDesc = (ActionFile f1, ActionFile f2) -> {
        return f1.paths[0].compareTo(f2.paths[0]);
    };
    private static final Comparator<ActionFile> cmpAsc = (ActionFile f1, ActionFile f2) -> {
        return f2.paths[0].compareTo(f1.paths[0]);
    };
    
//RENAME
    public void renameTo(String fileToRename, String newName,String fallbackName) throws IOException, FileNameException{
        for(Character c:newName.toCharArray()){
            if(illegalCharacters.contains(c)){
                throw new FileNameException(newName+" contains illegal character "+c);
            }
        }
        LocationInRoot location  = new LocationInRoot(fileToRename);
        ExtFile file = LocationAPI.getInstance().getFileByLocation(location);
        ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location.getParentLocation());
        
        String path1 = file.getAbsolutePath();
        String path2 = parent.getAbsoluteDirectory()+newName;
        String fPath = parent.getAbsoluteDirectory()+fallbackName;
        
        Log.writeln("Rename:",path1,"New Name:"+newName,"Fallback:"+fallbackName);
        if(path1.equalsIgnoreCase(path2)){
            Files.move(new File(path1).toPath(),new File(fPath).toPath());
            parent.update();
            Files.move(new File(fPath).toPath(), new File(path2).toPath());
        }else{
            Files.move(new File(path1).toPath(), new File(path2).toPath());
        }
    }
    public void renameRootKeys(ExtFolder root,LocationInRoot newLoc,LocationInRoot oldLoc ){
        
        ExtFile file = LocationAPI.getInstance().getFileByLocation(oldLoc);
        if(file.getIdentity().equals(Enums.Identity.FILE)){
            ExtFile newFile = new ExtFile(file.getParentFile().getAbsolutePath()+File.separatorChar+newLoc.getName());
            LocationAPI.getInstance().removeByLocation(oldLoc);
            LocationAPI.getInstance().putByLocation(newLoc, newFile);
        }else if(file.getIdentity().equals(Enums.Identity.FOLDER)){
            ExtFolder newFile = new ExtFolder(file.getParentFile().getAbsolutePath()+File.separatorChar+newLoc.getName());
            newFile.populateRecursive();
            LocationAPI.getInstance().removeByLocation(oldLoc);
            LocationAPI.getInstance().putByLocation(newLoc, newFile);
        }
    }
    
//PREPARE FOR TASKS

    public void addToMarked(String file){
        Platform.runLater(()->{
            if(file!=null&&!markedList.contains(file)){
                markedList.add(file);
            } 
        });
        
        
    }
    public Collection<ExtFile> populateExtFileList(Collection<String> filelist){
        Collection<ExtFile> collection = FXCollections.observableArrayList();
        filelist.forEach(item ->{
            collection.add(LocationAPI.getInstance().getFileAndPopulate(item));
        });
        return collection;
    }
    public Collection<String> populateStringFileList(Collection<ExtFile> filelist){
        Collection<String> collection = FXCollections.observableArrayList();
        filelist.forEach(item ->{
            collection.add(item.getAbsoluteDirectory());
        });
        return collection;
    }
    
    private ActionFile[] prepareForCopy(Collection<ExtFile> fileList, ExtFile dest){
        Log.writeln("List recieved in task");
        
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtFile file:fileList){
            Collection<ExtFile> listRecursive = file.getListRecursive();
            ExtFile parentFile = LocationAPI.getInstance().getFileByLocation(file.getMapping().getParentLocation());
            for(ExtFile f:listRecursive){
                String relativePath = resolveRelativePath(f, (ExtFolder) parentFile);
                list.add(new ActionFile(f.getAbsolutePath(),dest.getAbsoluteDirectory()+relativePath));
            }
           
        }
        list.sort(cmpDesc);
        Log.writeln("List after computing");

        ActionFile[] array = new ActionFile[list.size()];
        array = list.toArray(array);
        for (ActionFile array1 : array) {
            Log.writeln(array1.paths[0]+" -> "+array1.paths[1]);
        }
        return array;
        
    }   
    private ActionFile[] prepareForDelete(Collection<ExtFile> fileList){
        Log.writeln("List recieved in task");
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtFile file:fileList){
            Collection<ExtFile> listRecursive = file.getListRecursive();
            for(ExtFile f:listRecursive){
                list.add(new ActionFile(f.getAbsolutePath()));
            }
        }
        list.sort(cmpAsc);
        Log.writeln("List after computing");
        for(ActionFile file:list){
            Log.writeln(file);
        }
        ActionFile[] array = new ActionFile[list.size()];
        array = list.toArray(array);
        return array;
        
    } 
    private ActionFile[] prepareForMove(Collection<ExtFile> fileList,ExtFile dest){
        Log.writeln("List recieved in task");
        
        for(ExtFile file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtFile file:fileList){
            Collection<ExtFile> listRecursive = file.getListRecursive();
            ExtFile parentFile = LocationAPI.getInstance().getFileByLocation(file.getMapping().getParentLocation());
            for(ExtFile f:listRecursive){
                try{
                String relativePath = resolveRelativePath(f, (ExtFolder) parentFile);
                //Log.write("RelativePath: ",relativePath);
                ActionFile AF = new ActionFile(f.getAbsolutePath(),dest.getAbsoluteDirectory()+relativePath);
                //Log.write("ActionFile: ",AF);
                list.add(AF);
                }catch(Exception e){
                    ErrorReport.report(e);
                }
            }
           
        }
        list.sort(cmpDesc);
        Log.writeln("List after computing");

        ActionFile[] array = new ActionFile[list.size()];
        array = list.toArray(array);
        for (ActionFile array1 : array) {
            Log.writeln(array1.paths[0]+" -> "+array1.paths[1]);
        }
        return array;
    }
    
//TASKS    
    public ExtTask copyFiles(Collection<String> fileList, ExtFile dest){  
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                String str;
                updateMessage("Populating list for copy");
                ActionFile[] list = prepareForCopy(populateExtFileList(fileList),dest);
                for(int i=0; i<list.length; i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        return null;
                    }
                    
                    str = "Source: \t\t"+list[i].paths[0]+"\n";
                    str +="Destination: \t"+list[i].paths[1];
                    updateMessage(str);
                    updateProgress(i+0.5, list.length);
                    try{
                        Files.copy(list[i].paths[0],list[i].paths[1]);
                        Log.writeln("OK:"+list[i]);
                        
                    }catch(Exception e){
                        ErrorReport.report(e); 
                    }
                    updateProgress(i+1, list.length);                    
                }
                updateProgress(1,1);
                updateMessage("FINISHED");
                return null;
            }
        };
    }
    public ExtTask moveFiles(Collection<String> fileList, ExtFile dest){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                ArrayList<ActionFile> leftFolders = new ArrayList<>();
                String str;
                updateMessage("Populating list for move");
                ActionFile[] list = prepareForMove(populateExtFileList(fileList),dest);
                updateMessage("Begin");
                int index1 = 0;
                for(; index1<list.length; index1++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        return null;
                    }
                    
                    str = "Source: \t\t"+list[index1].paths[0]+"\n";
                    str +="Destination: \t"+list[index1].paths[1];
                    updateMessage(str);
                    updateProgress(index1+0.5, list.length+leftFolders.size());
                    try{
                        if(Files.isDirectory(list[index1].paths[0])){
                            leftFolders.add(list[index1]);
                            Files.createDirectory(list[index1].paths[1]);
                            Log.writeln("Added to flders:"+list[index1].paths[1]);
                        }else{
                            Files.move(list[index1].paths[0],list[index1].paths[1]);
                            Log.writeln("OK:"+list[index1]);
                        }
                    }catch(Exception e){
                        ErrorReport.report(e); 
                        
                    }
                    updateProgress(index1+1, list.length+leftFolders.size());
                }
                updateMessage("Deleting leftover folders");
                int i=0;
                Log.writeln("Folders size: "+leftFolders.size());
                leftFolders.sort(cmpDesc);
                for(ActionFile f:leftFolders){
                    try{
                        Log.writeln("Deleting "+f.paths[0]);
                        Files.delete(f.paths[0]);
                        i++;
                    }catch(Exception x){
                        report(x);
                    }
                    updateProgress(index1+i+2, list.length+leftFolders.size());
                }
                updateProgress(1,1);
                updateMessage("FINISHED");
                return null;
            }
            
        };
    }
    public ExtTask deleteFiles(Collection<String> fileList){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                String str;
                updateMessage("Populating list for deletion");
                ActionFile[] list = prepareForDelete(populateExtFileList(fileList));
                for(int i=0; i<list.length; i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        return null;
                    }
                    str = "Deleting: \t"+list[i];
                    updateMessage(str);
                    updateProgress(i+0.5, list.length);
                    try{
                        
                        Files.deleteIfExists(list[i].paths[0]);
                    }catch(Exception e){
                        ErrorReport.report(e);
                    }
                    updateProgress(i+1, list.length);

                }
                updateProgress(1,1);
                updateMessage("FINISHED");
                return null;
            }
        };
    } 
    private void populateRecursiveParallelInner(ExtFolder folder, int level, final int MAX_DEPTH){
        
        if(level<MAX_DEPTH){
            Log.writeln("Folder Iteration "+level+"::"+folder.getAbsoluteDirectory());
            folder.update();
            
            for(ExtFolder fold:folder.getFoldersFromFiles()){
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
    
 //MISC
    public static String resolveRelativePath(ExtFile f1, ExtFolder f2){

        String parentFolder = f2.getAbsoluteDirectory();
        String path1 = f1.getAbsolutePath();
        String result = org.apache.commons.lang3.StringUtils.replaceOnce(path1, parentFolder, "");
        return result;
    }
    public static String resolveAvailableName(ExtFolder folder,String name){
        String path = folder.getAbsoluteDirectory();
        String newName = name;
        int i=0;
        while(folder.files.containsKey(newName)){
            newName = ++i +name;
        }
        return path+newName;
    }
    
    public ExtTask markFiles(List<String> list){
        return new ExtTask(){
            @Override
            protected Void call(){
                list.forEach(file ->{
                    addToMarked(file);
                });
                return null;
            }
        };
    }
    
    public ExtTask searchTask(String search, boolean useRegex){
        return new ExtTask(){
            @Override
            protected Void call(){
               
                return null;
            }
        };
    }
    
    public Task<Snapshot> snapshotCreateTask(String folder){
        return new Task(){
            @Override
            protected Snapshot call() throws Exception {
                return new Snapshot((ExtFolder) LocationAPI.getInstance().getFileAndPopulate(folder));
            }
  
        };
    }
    public ExtTask snapshotCreateWriteTask(String windowID,ExtFolder folder,File file){
        return new ExtTask(){
            @Override
            protected Void call() throws Exception {

                    ExtTask populateRecursiveParallel = TaskFactory.getInstance().populateRecursiveParallel(folder, 50);
                    Thread thread = new Thread(populateRecursiveParallel);
                    thread.setDaemon(true);
                    thread.start();
                    thread.join();

                    
                    ObjectMapper mapper = new ObjectMapper();
                    Snapshot currentSnapshot =  SnapshotAPI.createSnapshot(folder);

                    Platform.runLater(()->{
                            MainController controller = (MainController) ViewManager.getInstance().getFrame(windowID).getController();
                            controller.snapshotView.getItems().clear();
                        try {
                            mapper.writeValue(file,currentSnapshot);
                            controller.snapshotView.getItems().add("Snapshot:"+file+" created");
                        } catch (IOException ex) {
                            ErrorReport.report(ex);
                            controller.snapshotView.getItems().add("Snapshot:"+file+" failed");
                        }
                        ViewManager.getInstance().updateAllWindows(); 
                    });
                    
                
                return null;
            }
  
        };
    }
    public ExtTask snapshotLoadTask(String windowID,ExtFolder folder,File nextSnap){
        return new ExtTask(){
            @Override
            protected Void call() throws Exception {
                    
                    MainController frame = (MainController) ViewManager.getInstance().getFrame(windowID).getController();

                    Platform.runLater(()->{
                        frame.snapshotView.getItems().clear();
                        frame.snapshotView.getItems().add("Snapshot Loading");
                    });
                    ExtTask populateRecursiveParallel = TaskFactory.getInstance().populateRecursiveParallel(folder, 50);
                    Thread thread = new Thread(populateRecursiveParallel);
                    thread.setDaemon(true);
                    thread.start();
                    thread.join();

                    
                    ObjectMapper mapper = new ObjectMapper();
                    Snapshot currentSnapshot =  SnapshotAPI.createSnapshot(folder);
                    Snapshot sn = SnapshotAPI.getEmptySnapshot();
                    sn = mapper.readValue(nextSnap, sn.getClass());
                    
                    Snapshot result = SnapshotAPI.getOnlyDifferences(SnapshotAPI.compareSnapshots(currentSnapshot, sn));
                    ObservableList list = FXCollections.observableArrayList();
                    list.addAll(result.map.values());
                    
                    frame.snapshotTextDate.setText(sn.dateCreated);
                    frame.snapshotTextFolder.setText(sn.folderCreatedFrom);
                    frame.snapshotTextDate.setVisible(true);
                    frame.snapshotTextFolder.setVisible(true);
                    Platform.runLater(()->{
                        frame.snapshotView.getItems().clear();
                        if(list.size()>0){
                            frame.snapshotView.getItems().addAll(list);
                        }else{
                            frame.snapshotView.getItems().add("No Differences Detected");
                        }
                    });
                         
                return null;
            }
  
        };
    }
    
    public ExtTask syncronizeTask(String folder1,String folder2,Collection<ExtEntry> listFirst){
         return new ExtTask(){
            @Override
            protected Void call() throws InterruptedException{
                
                int i=0;
                int size = listFirst.size();
                Log.write("List");
                for(ExtEntry e:listFirst){
                    Log.write(e.relativePath,"  ",e.action.get());
                }
                //Log.writeln("Size "+size);
                for(ExtEntry entry:listFirst){
                    while(this.isPaused()){
                        Thread.sleep(this.getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    ActionFile actionFile = new ActionFile(folder1+entry.relativePath,folder2+entry.relativePath);
                    try{
                        action(actionFile,entry);
                    }catch(Exception e){
                        ErrorReport.report(e);
                    }
                    this.updateProgress(++i, size);
                    this.updateMessage(entry.action.get()+"\n"+entry.relativePath);
                }
                return null;
            
            }
        };
    }
    private void action(ActionFile action,ExtEntry entry) throws Exception{
        Log.write(action);
        switch(entry.actionType.get()){
            
            case(1):{
                try{
                    Files.copy(action.paths[1], action.paths[0],StandardCopyOption.REPLACE_EXISTING);
                }catch(Exception e){
                    ErrorReport.report(e);
                    if(Files.isDirectory(action.paths[0])){
                        Files.setLastModifiedTime(action.paths[0], Files.getLastModifiedTime(action.paths[1]));
                        entry.isModified = false;
                    }
                     
                }
                break;
            }case(2):{
                try{
                    Files.copy(action.paths[0], action.paths[1], StandardCopyOption.REPLACE_EXISTING);  
                }catch(Exception e){
                    ErrorReport.report(e);
                    if(Files.isDirectory(action.paths[1])){
                        Files.setLastModifiedTime(action.paths[1], Files.getLastModifiedTime(action.paths[0]));
                        entry.isModified = false;
                    }
                }
                break;
            }case(3):{
                Files.delete(action.paths[0]); 
                break;
            }case(4):{
                Files.delete(action.paths[1]);
                break;
            }default:{
                break;
            }
        }
        entry.actionCompleted.set(true);
    }
    public ExtTask duplicateFinderTask(ExtFolder folder,double ratio,ObservableList list){
        return new ExtTask(){
            @Override
            protected Void call() throws Exception{
                ExtFile[] array = new ExtFile[0];
                Log.write(ratio);
                array = folder.getListRecursive().toArray(array);
                    for(int i=0; i<array.length;i++){
                        for(int j=i+1; j<array.length;j++){
                            ExtFile file = array[i];
                            ExtFile file1 = array[j];
                            
                            double rat = StringOperations.correlationRatio(file.propertyName.get(),file1.propertyName.get());
                            
                            if(rat>=ratio){
                                Log.write("Found:",file.getAbsoluteDirectory()+" | ",file1.getAbsoluteDirectory()+" "+rat);
                                Platform.runLater(()->{
                                    list.add(new DuplicateFinderController.SimpleTableItem(file,file1));
                                });
                            }
                            
                        }
                    }   
                return null;
            };
        };
        
    }
    
    
    public static void serializeObject(String whereToSave, Object whatToSave){
        boolean success = true;
        ObjectMapper mapper = new ObjectMapper();
        try{
           
            
            mapper.writeValue(new File(whereToSave), whatToSave);
        }catch(Exception ex){
            ErrorReport.report(ex);
            success = false;
        }
        
        
        if(success){
            Log.write("Saved at: ",whereToSave);
        }else{
            Log.writeln("Failed to save at: ",whereToSave);
        }
        
    }
    public static <TP> boolean readSerializedObject(String whereToRead,TP object) {
        File file = new File(whereToRead);
        if(!file.exists()){
            return false;
        }

        
        boolean success = true;
        try {
           ObjectMapper mapper = new ObjectMapper();
           object = mapper.readValue(file, (Class<TP>) object.getClass());
           
        } catch (Exception e) {
            success = false;
        }
        if(success){
            Log.write("Read: ", object, " at",whereToRead);
        }else{
            Log.write("Failed to read ",whereToRead);
        }
        
    
        return success;
    }
    
}
