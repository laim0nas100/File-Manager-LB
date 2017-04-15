/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import LibraryLB.FX.FXTask;
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
import LibraryLB.Parsing.StringOperations;
import LibraryLB.FX.FXTaskPooler;
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
import LibraryLB.Threads.ExtTask;
import LibraryLB.Threads.TaskPooler;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import utility.ErrorReport;
import utility.ExtInputStream;
import utility.FileNameException;
import utility.PathStringCommands;

/**
 *  
 * @author Laimonas Beniušis
 * Produces Tasks 
 */

//
public class TaskFactory {
    
    public static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
    private static final HashSet<Character> illegalCharacters = new HashSet<>();
    private static final TaskFactory INSTANCE = new TaskFactory();
    public static final ExecutorService mainExecutor = Executors.newFixedThreadPool(PROCESSOR_COUNT);
    public static String dragInitWindowID ="";
    public static TaskFactory getInstance(){
        
        return INSTANCE;
    }
    protected TaskFactory(){
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
    public String renameTo(String fileToRename, String newName,String fallbackName) throws IOException, FileNameException{
        for(Character c:newName.toCharArray()){
            if(illegalCharacters.contains(c)){
                throw new FileNameException(newName+" contains illegal character "+c);
            }
        }
        LocationInRoot location  = new LocationInRoot(fileToRename);
        
        ExtPath file = LocationAPI.getInstance().getFileIfExists(location);
        if(file==null){
            throw new FileNameException(fileToRename+" was not found");
        }
        ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileIfExists(location.getParentLocation());
        String path1 = file.getAbsolutePath();
        String path2 = parent.getAbsoluteDirectory()+newName;
        String fPath = parent.getAbsoluteDirectory()+fallbackName;
        LocationAPI.getInstance().removeByLocation(location);
        Log.print("Rename:",path1,"New Name:"+newName,"Fallback:"+fallbackName);
        if(path1.equalsIgnoreCase(path2)){
            Files.move(Paths.get(path1),Paths.get(fPath));
            Files.move(Paths.get(fPath), Paths.get(path2));
        }else{
            Files.move(Paths.get(path1), Paths.get(path2));
        }
        return path2;
    }
    
//PREPARE FOR TASKS

    public void addToMarked(ExtPath file){
        Platform.runLater(()->{
            if(file!=null&&!MainController.markedList.contains(file)){
                MainController.markedList.add(file);
            } 
        });
        
        
    }

    public Collection<String> populateStringFileList(Collection<ExtPath> filelist){
        Collection<String> collection = FXCollections.observableArrayList();
        filelist.forEach(item ->{
            collection.add(item.getAbsoluteDirectory());
        });
        return collection;
    }
    
    private ArrayList<ActionFile> prepareForCopy(Collection<ExtPath> fileList, ExtPath dest){
        Log.print("List recieved in task");
        
        for(ExtPath file:fileList){
            Log.print(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtPath file:fileList){
            Collection<ExtPath> listRecursive = new ArrayList<>();
            listRecursive.addAll(file.getListRecursive(true));
            ExtPath parentFile = LocationAPI.getInstance().getFileIfExists(file.getMapping().getParentLocation());
            for(ExtPath f:listRecursive){
                String relativePath = parentFile.relativeTo(f.getAbsoluteDirectory());
                list.add(new ActionFile(f.getAbsoluteDirectory(),dest.getAbsoluteDirectory()+relativePath));
            }

            
           
        }
        list.sort(cmpDesc);
        Log.print("List after computing");
        for (ActionFile array1 : list) {
            Log.print(array1.paths[0]+" -> "+array1.paths[1]);
        }
        return list;
        
    }
    private ArrayList<ActionFile> prepareForCopy(Collection<ExtPath> fileList, ExtPath dest,ExtPath root){
        Log.print("List recieved in task test");
        
        for(ExtPath file:fileList){
            Log.print(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtPath file:fileList){
//            Log.write("Root",root);
            String relativePath = file.relativeFrom(root.getAbsoluteDirectory());
//            Log.write("RelativePath:",relativePath);
            ActionFile af = new ActionFile(file.getAbsoluteDirectory(),dest.getAbsoluteDirectory()+relativePath);
            list.add(af);
//            Log.write("Add",af);
        }
        list.sort(cmpDesc);
        Log.print("List after computing");

        
        for (ActionFile array1 : list) {
            Log.print(array1.paths[0]+" -> "+array1.paths[1]);
        }
        return list;
        
    }
    private ArrayList<ActionFile> prepareForDelete(Collection<ExtPath> fileList){
        Log.print("List recieved in task");
        for(ExtPath file:fileList){
            Log.print(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtPath file:fileList){
            Collection<ExtPath> listRecursive = file.getListRecursive(true);
            for(ExtPath f:listRecursive){
//                f.path = null;
                list.add(new ActionFile(f.getAbsoluteDirectory()));
            }
        }
        list.sort(cmpAsc);
        Log.print("List after computing");
        for(ActionFile file:list){
            Log.print(file.toString());
        }
        return list;
        
    } 
    private ArrayList<ActionFile> prepareForMove(Collection<ExtPath> fileList,ExtPath dest){
        Log.print("List recieved in task");
        
        for(ExtPath file:fileList){
            Log.print(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for(ExtPath file:fileList){
            Collection<ExtPath> listRecursive = file.getListRecursive(true);
            ExtPath parentFile = LocationAPI.getInstance().getFileIfExists(file.getMapping().getParentLocation());
            for(ExtPath f:listRecursive){
                try{
                String relativePath = f.relativeFrom(parentFile.getAbsolutePath());
                //Log.write("RelativePath: ",relativePath);
                ActionFile AF = new ActionFile(f.getAbsoluteDirectory(),dest.getAbsoluteDirectory()+relativePath);
                //Log.write("ActionFile: ",AF);
                list.add(AF);
                }catch(Exception e){
                    ErrorReport.report(e);
                }
            }
           
        }
        list.sort(cmpDesc);
        Log.print("List after computing");
        for (ActionFile array1 : list) {
            Log.print(array1.paths[0]+" -> "+array1.paths[1]);
        }
        return list;
    }
    
//TASKS    
    public FXTask copyFiles(Collection<ExtPath> fileList, ExtPath dest,ExtPath root){  
        return new FXTask(){
            @Override
            protected Void call() throws Exception {
                String str;
                updateMessage("Populating list for copy");
                List<ActionFile> list;
                if(root == null){
                   list = prepareForCopy(fileList,dest);
                }else{
                    Log.print("Test copy");
                    list = prepareForCopy(fileList,dest,root);
                }
                Log.print("In a task now");
                Log.print(list);
                 
                for(int i=0; i<list.size(); i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        return null;
                    }
                    
                    str = "Source: \t\t"+list.get(i).paths[0]+"\n";
                    str +="Destination: \t"+list.get(i).paths[1];
                    updateMessage(str);
                    updateProgress(i, list.size());
                    try{
                        final int currentIndex = i;
                        ExtInputStream stream = new ExtInputStream(list.get(i).paths[0]);
                        stream.progress.addListener(listener ->{
                            updateProgress(currentIndex+stream.progress.get(),list.size());
                        });
                        Files.copy(stream, list.get(i).paths[1]);
//                        list.get(i).copy();
                        Log.print("OK:"+list.get(i));
                        
                    }catch(Exception e){
                        ErrorReport.report(e); 
                    }
                    updateProgress(i+1, list.size());                    
                }
                updateProgress(1,1);
                updateMessage("FINISHED");
                return null;
            }
        };
    }
    public FXTask moveFiles(Collection<ExtPath> fileList, ExtPath dest){
        return new FXTask(){
            @Override protected Void call() throws Exception {
                ArrayList<ActionFile> leftFolders = new ArrayList<>();
                String str;
                updateMessage("Populating list for move");
                List<ActionFile> list = prepareForMove(fileList,dest);
                updateMessage("Begin");
                int index1 = 0;
                for(; index1<list.size(); index1++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        return null;
                    }
                    
                    str = "Source: \t\t"+list.get(index1).paths[0]+"\n";
                    str +="Destination: \t"+list.get(index1).paths[1];
                    updateMessage(str);
                    updateProgress(index1+0.5, list.size()+leftFolders.size());
                    try{
                        if(Files.isDirectory(list.get(index1).paths[0])){
                            leftFolders.add(list.get(index1));
                            Files.createDirectory(list.get(index1).paths[1]);
                            Log.print("Added to folders:"+list.get(index1).paths[1]);
                        }else{
                            list.get(index1).move();
//                            Files.move(list.get(index1).paths[0],list.get(index1).paths[1]);
                            
                            Log.print("OK:"+list.get(index1));
                        }
                    }catch(Exception e){
                        ErrorReport.report(e); 
                        
                    }
                    updateProgress(index1+1, list.size()+leftFolders.size());
                }
                updateMessage("Deleting leftover folders");
                int i=0;
                Log.print("Folders size: "+leftFolders.size());
                leftFolders.sort(cmpDesc);
                for(ActionFile f:leftFolders){
                    try{
                        Log.print("Deleting "+f.paths[0]);
//                        Files.delete(f.paths[0]);
                        f.delete();
                        i++;
                    }catch(Exception x){
                        ErrorReport.report(x);
                    }
                    updateProgress(index1+i+2, list.size()+leftFolders.size());
                }
                updateProgress(1,1);
                updateMessage("FINISHED");
                return null;
            }
            
        };
    }
    public FXTask deleteFiles(Collection<ExtPath> fileList){
        return new FXTask(){
            @Override protected Void call() throws Exception {
                String str;
                updateMessage("Populating list for deletion");
                ArrayList<ActionFile> list = prepareForDelete(fileList);
                for(int i=0; i<list.size(); i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        return null;
                    }
                    
                    str = "Deleting: \t"+list.get(i).paths[0];
                    updateMessage(str);
                    updateProgress(i+0.5, list.size());
                    try{
                        final ActionFile f = list.get(i);
                        f.delete();
                        
                    }catch(Exception e){
                        ErrorReport.report(e);
                    }
                    updateProgress(i+1, list.size());

                }
                updateProgress(1,1);
                updateMessage("FINISHED");
                return null;
            }
        };
    } 
    private void populateRecursiveParallelInner(ExtFolder folder,int depth, ExecutorService exe){
        if(0<depth){
            Callable task = new Callable() {
                @Override
                public Void call() throws Exception {
                    Log.print("Folder Iteration "+depth+"::"+folder.getAbsoluteDirectory());
                    folder.update();
                    for(ExtFolder fold:folder.getFoldersFromFiles()){
                        populateRecursiveParallelInner(fold, depth-1,exe);
                    }
                    return null;
                }
            };
            exe.submit(task);   
        }
    }

    public void populateRecursiveParallelContained(ExtFolder folder, int depth){
        populateRecursiveParallelInner(folder,depth,mainExecutor); 
    }
    public Runnable populateRecursiveParallel(ExtFolder folder, int depth){
        TaskPooler pooler = new TaskPooler(PROCESSOR_COUNT); 
        populateRecursiveParallelInner(folder,depth,pooler); 
        return pooler;
    }

    
 //MISC
    public static String resolveAvailablePath(ExtFolder folder,String name){
        String path = folder.getAbsoluteDirectory();
        String newName = name;
        while(folder.hasFileIgnoreCase(newName)){
            newName = "New "+newName;
        }
        return path+newName;
    }
    
    public FXTask markFiles(Collection<String> list){
        return new FXTask(){
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
        return new ExtTask(){
            @Override
            protected Void call() throws Exception {

                    TaskFactory.getInstance().populateRecursiveParallelContained(folder, 50);
//                    Thread thread = new Thread(populateRecursiveParallel);
//                    thread.setDaemon(true);
//                    thread.start();
//                    thread.join();

                    
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
    public FXTask snapshotLoadTask(String windowID,ExtFolder folder,File nextSnap){
        return new FXTask(){
            @Override
            protected Void call() throws Exception {
                    
                    MainController frame = (MainController) ViewManager.getInstance().getFrame(windowID).getController();

                    Platform.runLater(()->{
                        frame.snapshotView.getItems().clear();
                        frame.snapshotView.getItems().add("Snapshot Loading");
                    });
//                    TaskFactory.getInstance().populateRecursiveParallelNew(folder, 50);
                    Thread thread = new Thread(TaskFactory.getInstance().populateRecursiveParallel(folder, 50));
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
    
    public FXTask syncronizeTask(String folder1, String folder2, Collection<ExtEntry> listFirst){
         return new FXTask(){
            @Override
            protected Void call() throws InterruptedException{
                
                int i=0;
                int size = listFirst.size();
                Log.print("List");
                for(ExtEntry e:listFirst){
                    Log.print(e.relativePath,"  ",e.action.get());
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
        Log.print(action);
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
    
    
    public FXTask duplicateFinderTask(ArrayList<PathStringCommands> array,double ratio,List list,Map map){
        FXTaskPooler executor = new FXTaskPooler(FileManagerLB.MAX_THREADS_FOR_TASK,0);
            for(int i=0; i<array.size();i++){
                ExtTask<Long> task;
                if(map==null){
                    task = duplicateCompareTask(i,array,ratio,list);
                }else{
                    task = duplicateCompareTaskLookUp(i,array,ratio,list,map);
                }
                executor.submit(task);
            }
        return executor;
                    
        
    }
    public ExtTask<Long> duplicateCompareTaskLookUp(int index,ArrayList<PathStringCommands>array,double ratio,List list,Map map){
        return new ExtTask<Long>(){
            @Override
            public Long call() throws Exception{
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
    public ExtTask<Long> duplicateCompareTask(int index,ArrayList<PathStringCommands>array,double ratio,List list){
        return new ExtTask<Long>(){
            @Override
            public Long call() throws Exception{
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
