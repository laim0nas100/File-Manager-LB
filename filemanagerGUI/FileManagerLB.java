/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.LocationAPI;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import utility.ErrorReport;
import utility.FavouriteLink;
import utility.Log;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB extends Application {
    public static enum DATA_SIZE{
        B  (1,"B"),
        KB (1024,"KB"),
        MB (1024*1024,"MB"),
        GB (1024*1024*1024,"GB");
        public long size;
        public String sizename;
        DATA_SIZE(long size,String s){
            this.size = size;
            this.sizename = s;
        }
        public void set(DATA_SIZE e,String sizename){
            e.size = DATA_SIZE.valueOf(sizename).size;
            e.sizename = DATA_SIZE.valueOf(sizename).sizename;
        }
    }
    
    public static String ARTIFICIAL_ROOT_NAME = "./ROOT";
    public static ExtFolder ArtificialRoot;
    public static DATA_SIZE DataSize;
    
    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static final int DEPTH = 2;
    @Override
    public void start(Stage primaryStage) {
        links = FXCollections.observableArrayList();
        errorLog = FXCollections.observableArrayList();
        DataSize = DATA_SIZE.KB;
        ARTIFICIAL_ROOT_NAME = TaskFactory.resolveAvailableName(new ExtFolder("/"), ARTIFICIAL_ROOT_NAME);
        ArtificialRoot = new ExtFolder(ARTIFICIAL_ROOT_NAME);
        ArtificialRoot.setPopulated(true);
        ArtificialRoot.setIsAbsoluteRoot(true);
        Log.write(ARTIFICIAL_ROOT_NAME);
        remount();
        ArtificialRoot.propertyName.set("ROOT");
        links.add(new FavouriteLink("ROOT",""));
//        ArtificialRoot.getFoldersFromFiles().stream().forEach((device) -> {
//            //new Thread(TaskFactory.getInstance().populateRecursiveParallel(device, 1)).start();
//        });
        ViewManager.getInstance().newWindow(ArtificialRoot, ArtificialRoot);
        //ViewManager.getInstance().newMountDirectoryDialog();
        ViewManager.getInstance().updateAllWindows();
    } 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    public static void remount(){
        
        File[] roots = File.listRoots();
        for(ExtFile f:ArtificialRoot.getFilesCollection()){
            if(!Files.isDirectory(f.toPath())){
                ArtificialRoot.files.remove(f.propertyName.get());
            }
        }
        for(int i = 0; i < roots.length ; i++){
            Log.writeln("Root["+i+"]:" + roots[i].getAbsolutePath());
            mountDevice(roots[i].getAbsolutePath());
        }
    }
    public static boolean mountDevice(String name){
        boolean result = false;
        if(!new File(name).exists()){
            return false;
        }
        if(Files.isDirectory(new File(name).toPath())){
            ExtFolder device = new ExtFolder(name);
            int nameCount = device.toPath().getNameCount();
            //Log.write("Is direcory");
            if(nameCount == 0){
                result = true;
                String newName = device.toPath().getRoot().toString();
                //Log.writeln("newName= "+newName);
                device.propertyName.set(newName);
                ArtificialRoot.files.putIfAbsent(newName, device);
                //Log.writeln("Mounted successfully");
            }
        }
        return result;
    }
    public static void reportError(Exception ex){
        ErrorReport error = new ErrorReport(ex);
        System.err.println(ex.getMessage());
        errorLog.add(0, error);
    }
    public static Set<String> getRootSet(){
        HashSet<String> set = new HashSet<>();
        for(ExtFile file:ArtificialRoot.files.values()){
            set.add(file.propertyName.get());
        }
        return set;
        
    }

}
