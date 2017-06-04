/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.FileManaging.AutoBackupMaker;
import LibraryLB.FileManaging.FileReader;
import LibraryLB.Log;
import LibraryLB.Containers.ParametersMap;
import filemanagerGUI.dialog.CommandWindowController;
import filemanagerLogic.Enums;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import utility.ErrorReport;
import utility.FavouriteLink;
import utility.PathStringCommands;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB extends Application {
    public static final String HOME_DIR  = System.getProperty("user.dir")+File.separator;
    public static final String VIRTUAL_FOLDERS_DIR = HOME_DIR+"VIRTUAL_FOLDERS"+File.separator;
    public static final String ARTIFICIAL_ROOT_DIR = HOME_DIR+"ARTIFICIAL_ROOT";
    public static String USER_DIR = HOME_DIR;
    public static String ROOT_NAME = "ROOT";
    public static int MAX_THREADS_FOR_TASK = 10;
    public static VirtualFolder ArtificialRoot;// = new VirtualFolder(ARTIFICIAL_ROOT_DIR);
    public static VirtualFolder VirtualFolders;// = new VirtualFolder(VIRTUAL_FOLDERS_DIR);
    public static String logPath;
    public static int DEPTH = 1;
    public static SimpleBooleanProperty DEBUG = new SimpleBooleanProperty(false);
    public static int LogBackupCount = 1;
    public static SimpleBooleanProperty useBufferedFileStreams = new SimpleBooleanProperty(true);
    public static ParametersMap parameters;
    public static PathStringCommands customPath = new PathStringCommands(HOME_DIR);
    public static ObservableList<ExtPath> remountUpdateList = FXCollections.observableArrayList();
    
    @Override
    public void start(Stage primaryStage) {
       
//        new Thread( ()->{
            System.err.println("STARTING");
            reInit();
            if(DEBUG.not().get()){
                ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
            }   
//        }).start();
         
    } 
    public static void main(String[] args) {
        launch(args);
    }
    public static void remount(){
        remountUpdateList.clear();
//        remountUpdateList.add(VirtualFolders);
        
        ArtificialRoot.files.put(VirtualFolders.propertyName.get(), VirtualFolders);
        for(ExtPath f:ArtificialRoot.getFilesCollection()){
            if(!Files.isDirectory(f.toPath()) && !f.isVirtual.get()){
                ArtificialRoot.files.remove(f.propertyName.get());
            }else{
                remountUpdateList.add(f);
            }
        }
        
        
        File[] roots = File.listRoots();
        for (File root : roots) {
            mountDevice(root.getAbsolutePath());
        }
        remountUpdateList.setAll(ArtificialRoot.getFilesCollection());
    }
    public static boolean mountDevice(String name){
        boolean result = false;
        name = name.toUpperCase();
        Log.print("Mount: "+name);
        Path path = Paths.get(name);
        if(Files.isDirectory(path)){
            ExtFolder device = new ExtFolder(name);
            int nameCount = path.getNameCount();
            if(nameCount == 0){
                result = true;
                String newName = path.getRoot().toString();
                device.propertyName.set(newName);
                if(!ArtificialRoot.files.containsKey(newName)){
                    ArtificialRoot.files.put(newName, device);
                    if(!remountUpdateList.contains(device)){
                        remountUpdateList.add(device);
                    }
                    
                }else{
                    result = false;
                }
            }
        }
        return result;
    }
    public static boolean folderIsVirtual(ExtPath fileToCheck){
        ExtFolder baseFolder = FileManagerLB.VirtualFolders;
        HashSet<String> set = new HashSet<>();
        for(ExtPath file:baseFolder.files.values()){
            set.add(file.getAbsoluteDirectory());
        }
        return set.contains(fileToCheck.getAbsoluteDirectory());
    }
    public static Set<String> getRootSet(){
        return ArtificialRoot.files.keySet();   
    }
    public static void doOnExit(){
        Log.print("Exit call invoked");
        ViewManager.getInstance().closeAllFramesNoExit();
        try {         
            
//            LibraryLB.FileManaging.FileReader.writeToFile(USER_DIR+"Log.txt", Log.getInstance().list);
            AutoBackupMaker BM = new AutoBackupMaker(LogBackupCount,USER_DIR+"BUP","YYYY-MM-dd HH.mm.ss");
            Log.close();
            Collection<Runnable> makeNewCopy = BM.makeNewCopy(logPath);
            makeNewCopy.forEach(th ->{
                th.run();
            });
            BM.cleanUp().run();
            Files.delete(Paths.get(logPath));
            
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }

    }
    public static void reInit(){
        Log.print("INITIALIZE");
        ViewManager.getInstance().closeAllFramesNoExit();
        MediaPlayerController.VLCfound = false;
        MainController.actionList = new ArrayList<>();
        MainController.dragList = FXCollections.observableArrayList();
        MainController.errorLog = FXCollections.observableArrayList();
        MainController.links = FXCollections.observableArrayList();
        MainController.markedList = FXCollections.observableArrayList();
        MainController.propertyMarkedSize = Bindings.size(MainController.markedList);
        ArtificialRoot = new VirtualFolder(ARTIFICIAL_ROOT_DIR);
        VirtualFolders = new VirtualFolder(VIRTUAL_FOLDERS_DIR);
        ArtificialRoot.setIsAbsoluteRoot(true);
//        if(CommandWindowController.executor!=null){
            CommandWindowController.executor.stopEverything();
            CommandWindowController.executor.setRunnerSize(0);
//        }
        readParameters();
        logPath = USER_DIR + Log.getZonedDateTime("HH-MM-ss")+" Log.txt";
        try{
            Path userdir = Paths.get(USER_DIR);
            if(!Files.isDirectory(userdir)){
                Files.createDirectories(userdir);
            }
            Log.changeStream('f',logPath);
        }catch(Exception e){
            ErrorReport.report(e);
        }
        Log.print("Before start executor");
        CommandWindowController.executor.setRunnerSize(CommandWindowController.maxExecutablesAtOnce);
        Log.print("After start executor");
        ArtificialRoot.propertyName.set(ROOT_NAME);
        MainController.links.add(new FavouriteLink(ROOT_NAME,ArtificialRoot));
        ViewManager.getInstance().newWindow(ArtificialRoot);
        Log.print("After new window");
        //Create directories
        try{
            Files.createDirectories(Paths.get(FileManagerLB.USER_DIR+MediaPlayerController.PLAYLIST_DIR));
        }catch(Exception e){
            ErrorReport.report(e);
        }

        
        
    }
    public static void readParameters(){
        ArrayDeque<String> list = new ArrayDeque<>();         
        try{
            list.addAll(FileReader.readFromFile(HOME_DIR+"Parameters.txt","//","/*","*/"));
        }
        catch(Exception e){
            ErrorReport.report(e);                
        }
        parameters = new ParametersMap(list,"=");
        Log.print("Parameters",parameters);
        
        DEBUG.set((boolean) parameters.defaultGet("debug",false));
        DEPTH = (int) parameters.defaultGet("lookDepth",2);
        LogBackupCount = (int) parameters.defaultGet("logBackupCount", 2);
        ROOT_NAME = (String) parameters.defaultGet("ROOT_NAME", ROOT_NAME);
        MAX_THREADS_FOR_TASK = (int) parameters.defaultGet("maxThreadsForTask", 15);
        USER_DIR = new PathStringCommands((String) parameters.defaultGet("userDir", HOME_DIR)).getPath()+File.separator;
        FileManagerLB.useBufferedFileStreams.setValue((boolean) parameters.defaultGet("bufferedFileStreams", true));
        VirtualFolder.VIRTUAL_FOLDER_PREFIX = (String) parameters.defaultGet("virtualPrefix", "V");
        MediaPlayerController.VLC_SEARCH_PATH = new PathStringCommands((String) parameters.defaultGet("vlcPath", HOME_DIR+"lib")).getPath()+File.separator;
        PathStringCommands.number = (String) parameters.defaultGet("filter.number", "#");
        PathStringCommands.fileName = (String) parameters.defaultGet("filter.name", "<n>");
        PathStringCommands.nameNoExt = (String) parameters.defaultGet("filter.nameNoExtension", "<nne>");
        PathStringCommands.filePath = (String) parameters.defaultGet("filter.path", "<ap>");
        PathStringCommands.extension = (String) parameters.defaultGet("filter.nameExtension", "<ne>");
        PathStringCommands.parent1 = (String) parameters.defaultGet("filter.parent1", "<p1>");
        PathStringCommands.parent2 = (String) parameters.defaultGet("filter.parent2", "<p2>");
        PathStringCommands.custom = (String) parameters.defaultGet("filter.custom", "<c>");
        PathStringCommands.relativeCustom = (String) parameters.defaultGet("filter.relativeCustom", "<rc>");
        CommandWindowController.commandInit = (String) parameters.defaultGet("code.init", "init");
        CommandWindowController.truncateAfter = (Integer) parameters.defaultGet("code.truncateAfter", 100000);
        CommandWindowController.maxExecutablesAtOnce = (Integer) parameters.defaultGet("code.maxExecutables", 2);
        CommandWindowController.commandGenerate = (String) parameters.defaultGet("code.commandGenerate", "generate");
        CommandWindowController.commandApply = (String) parameters.defaultGet("code.commandApply", "apply");
        CommandWindowController.commandClear = (String) parameters.defaultGet("code.clear", "clear");
        CommandWindowController.commandCancel = (String) parameters.defaultGet("code.cancel", "cancel");
        CommandWindowController.commandList = (String) parameters.defaultGet("code.list", "list");
        CommandWindowController.commandListRec = (String) parameters.defaultGet("code.listRec", "listRec");
        CommandWindowController.commandSetCustom = (String) parameters.defaultGet("code.setCustom", "setCustom");
        CommandWindowController.commandHelp = (String) parameters.defaultGet("code.help", "help");
        CommandWindowController.commandListParams = (String) parameters.defaultGet("code.listParameters", "listParams");
        CommandWindowController.maxExecutablesAtOnce = (Integer) parameters.defaultGet("code.maxThreadsForCommand", TaskFactory.PROCESSOR_COUNT);
        CommandWindowController.commandCopyFolderStructure = (String) parameters.defaultGet("code.copyFolderStructure", "copyStructure");


        
    }
    public static void restart(){
        try {
            Log.print("Restart request");
            FileManagerLB.doOnExit();
            System.err.println("Restart request");//Message to parent process
            Thread.sleep(10000);
            System.err.println("Failed to respond");
            System.err.println("Terminating");
        } catch (InterruptedException ex) {
        }
        System.exit(707);
    }
    
}
