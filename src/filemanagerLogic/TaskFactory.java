/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import LibraryLB.Threads.ExtTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import filemanagerLogic.fileStructure.ExtPath;
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
import LibraryLB.Threads.TaskExecutor;
import filemanagerGUI.FileManagerLB;
import filemanagerGUI.MainController;
import filemanagerGUI.ViewManager;
import filemanagerGUI.dialog.DuplicateFinderController;
import filemanagerLogic.fileStructure.ActionFile;
import filemanagerLogic.snapshots.ExtEntry;
import filemanagerLogic.snapshots.Snapshot;
import filemanagerLogic.snapshots.SnapshotAPI;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import utility.ErrorReport;
import utility.FileNameException;
import utility.PathStringCommands;

/**
 *  
 * @author Laimonas Beniušis
 * Produces Tasks 
 */

//
public class TaskFactory {
    
    
    private final HashSet<Character> illegalCharacters;
    private static final TaskFactory INSTANCE = new TaskFactory();
    public static String dragInitWindowID ="";
    public static TaskFactory getInstance(){
        
        return INSTANCE;
    }
    protected TaskFactory(){
        illegalCharacters = new HashSet<>();
        Character[] arrayWindows = new Character[] {
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
        if(File.separator.equals('/')){
            illegalCharacters.add('/');
        }else{
            illegalCharacters.addAll(Arrays.asList(arrayWindows));
        }
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
        ExtPath file = LocationAPI.getInstance().getFileByLocation(location);
        ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location.getParentLocation());
        String path1 = file.getAbsolutePath();
        String path2 = parent.getAbsoluteDirectory()+newName;
        String fPath = parent.getAbsoluteDirectory()+fallbackName;
        
        Log.write("Rename:",path1,"New Name:"+newName,"Fallback:"+fallbackName);
        if(path1.equalsIgnoreCase(path2)){
            Files.move(Paths.get(path1),Paths.get(fPath));
            parent.update();
            Files.move(Paths.get(fPath), Paths.get(path2));
        }else{
            Files.move(Paths.get(path1), Paths.get(path2));
        }
    }
    
//PREPARE FOR TASKS

    public void addToMarked(ExtPath file){
        Platform.runLater(()->{
            if(file!=null&&!MainController.markedList.contains(file)){
                MainController.markedList.add(file);
            } 
        });
        
        
    }
    public Collection<ExtPath> populateExtPathList(Collection<String> filelist){
        Collection<ExtPath> collection = FXCollections.observableArrayList();
        filelist.forEach(item ->{
            collection.add(LocationAPI.getInstance().getFileAndPopulate(item));
        });
        return collection;
    }
    public Collection<String> populateStringFileList(Collection<ExtPath> filelist){
        Collection<String> collection = FXCollections.observableArrayList();
        filelist.forEach(item ->{
            collection.add(item.getAbsoluteDirectory());
        });
        return collection;
    }
    
    private ActionFile[] prepareForCopy(Collection<ExtPath> fileList, ExtPath dest){
        Log.writeln("List recieved in task");
        
        for(ExtPath file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtPath file:fileList){
            Collection<ExtPath> listRecursive = file.getListRecursive();
            ExtPath parentFile = LocationAPI.getInstance().getFileByLocation(file.getMapping().getParentLocation());
            for(ExtPath f:listRecursive){
                String relativePath = f.relativeFrom(parentFile.getAbsolutePath());
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
    private ActionFile[] prepareForDelete(Collection<ExtPath> fileList){
        Log.writeln("List recieved in task");
        for(ExtPath file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtPath file:fileList){
            Collection<ExtPath> listRecursive = file.getListRecursive();
            for(ExtPath f:listRecursive){
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
    private ActionFile[] prepareForMove(Collection<ExtPath> fileList,ExtPath dest){
        Log.writeln("List recieved in task");
        
        for(ExtPath file:fileList){
            Log.writeln(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtPath file:fileList){
            Collection<ExtPath> listRecursive = file.getListRecursive();
            ExtPath parentFile = LocationAPI.getInstance().getFileByLocation(file.getMapping().getParentLocation());
            for(ExtPath f:listRecursive){
                try{
                String relativePath = f.relativeFrom(parentFile.getAbsolutePath());
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
    public ExtTask copyFiles(Collection<String> fileList, ExtPath dest){  
        return new SimpleTask(){
            protected Void call() throws Exception {
                String str;
                updateMessage("Populating list for copy");
                ActionFile[] list = prepareForCopy(populateExtPathList(fileList),dest);
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
    public ExtTask moveFiles(Collection<String> fileList, ExtPath dest){
        return new SimpleTask(){
            @Override protected Void call() throws Exception {
                ArrayList<ActionFile> leftFolders = new ArrayList<>();
                String str;
                updateMessage("Populating list for move");
                ActionFile[] list = prepareForMove(populateExtPathList(fileList),dest);
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
                            Log.writeln("Added to folders:"+list[index1].paths[1]);
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
        return new SimpleTask(){
            @Override protected Void call() throws Exception {
                String str;
                updateMessage("Populating list for deletion");
                ActionFile[] list = prepareForDelete(populateExtPathList(fileList));
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
                    str = "Deleting: \t"+list[i].paths[0];
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
    private void populateRecursiveParallelInner(ExtFolder folder,int depth){
        
        if(0<depth){
            Log.writeln("Folder Iteration "+depth+"::"+folder.getAbsoluteDirectory());
            folder.updateTask.cancel();
            if(folder.isVirtual.get()){
                folder.update();
            }else{
                folder.startUpdateTask();
            }
            
            folder.getFoldersFromFiles().stream().forEach((fold) -> {
                populateRecursiveParallelInner(fold, depth-1);
            });
        }
    }
    public ExtTask populateRecursiveParallel(ExtFolder folder, int depth){
        return new SimpleTask(){
            @Override protected Void call() throws Exception {
                populateRecursiveParallelInner(folder,depth);
            return null;
            }
        };
    }
    
 //MISC
    public static String resolveAvailablePath(ExtFolder folder,String name){
        String path = folder.getAbsoluteDirectory();
        String newName = name;
        int i=0;
        while(folder.files.containsKey(newName)){
            newName = "New "+newName;
        }
        return path+newName;
    }
    
    public ExtTask markFiles(List<String> list){
        return new SimpleTask(){
            @Override
            protected Void call(){
                list.forEach(file ->{
                    addToMarked(LocationAPI.getInstance().getFileOptimized(file));
                });
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
        return new SimpleTask(){
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
        return new SimpleTask(){
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
    
    public ExtTask syncronizeTask(String folder1, String folder2, Collection<ExtEntry> listFirst){
         return new SimpleTask(){
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
                    Files.copy(action.paths[1], action.paths[0],StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
                }catch(Exception e){
                    ErrorReport.report(e);
                    if(Files.isDirectory(action.paths[0])){
//                        Files.setLastModifiedTime(action.paths[0], Files.getLastModifiedTime(action.paths[1]));
                        entry.isModified = false;
                    }
                     
                }
                break;
            }case(2):{
                try{
                    Files.copy(action.paths[0], action.paths[1], StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);  
                }catch(Exception e){
                    ErrorReport.report(e);
                    if(Files.isDirectory(action.paths[1])){
//                        Files.setLastModifiedTime(action.paths[1], Files.getLastModifiedTime(action.paths[0]));
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
    
    
    public ExtTask duplicateFinderTask(ArrayList<PathStringCommands> array,double ratio,List list,Map map){
        TaskExecutor executor = new TaskExecutor(FileManagerLB.MAX_THREADS_FOR_TASK,1);
            for(int i=0; i<array.size();i++){
                
                Task<Long> task;
                if(map==null){
                    task = duplicateCompareTask(i,array,ratio,list);
                }else{
                    task = duplicateCompareTaskLookUp(i,array,ratio,list,map);

                }
                executor.addTask(task);
            }
        return executor;
                    
        
    }
    public Task<Long> duplicateCompareTaskLookUp(int index,ArrayList<PathStringCommands>array,double ratio,List list,Map map){
        return new Task<Long>(){
            @Override
            protected Long call() throws Exception{
                long progress = index;
                PathStringCommands file = array.get(index);
                String name = file.getName(true);
                for(int j=index+1; j<array.size();j++){
                    progress++;
                    if(this.isCancelled()){
                        return progress;
                    }

                    PathStringCommands file1 = array.get(j);
                    String otherName = file1.getName(true);
                    String key = name+"/$/"+otherName;
                    DuplicateFinderController.SimpleTableItem item;
                    Double rat;
                    if(map.containsKey(key)){
                        rat = (Double) map.get(key);
                        
                    }else{
                        rat = StringOperations.correlationRatio(name,otherName);                
                        map.put(key,rat);
                    }
                    if(rat>=ratio){
                        item = new DuplicateFinderController.SimpleTableItem(file,file1,rat);
                        list.add(item);
                    }
                }
                return progress;
            };
        };
    }
    public Task<Long> duplicateCompareTask(int index,ArrayList<PathStringCommands>array,double ratio,List list){
        return new Task<Long>(){
            @Override
            protected Long call() throws Exception{
                long progress = index;
                PathStringCommands file = array.get(index);
                String name = file.getName(true);
                for(int j=index+1; j<array.size();j++){
                    progress++;
                    if(this.isCancelled()){
                        return progress;
                    }

                    PathStringCommands file1 = array.get(j);
                    double rat = StringOperations.correlationRatio(name,file1.getName(true));
                    DuplicateFinderController.SimpleTableItem item = new DuplicateFinderController.SimpleTableItem(file,file1,rat);
                    if(rat>=ratio){
                        list.add(item); 
                    }
                    
                }
                return progress;
            };
        };
    }
}
