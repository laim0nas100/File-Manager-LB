/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.ExtTask;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import javafx.application.Application;
import javafx.application.Platform;
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
        Mb (1024*1024/8,"Kb"),
        MB (1024*1024,"MB"),
        Gb (1024*1024*1024/8,"Gb"),
        GB (1024*1024*1024,"GB");
        public final long size;
        public final String sizename;
        DATA_SIZE(long size,String s){
            this.size = size;
            this.sizename = s;
        }
    }
    
    public static final String ARTIFICIAL_ROOT_NAME = "Devices";
    public static ExtFolder ArtificialRoot;
    public static HashSet<String> rootSet;
    public static DATA_SIZE DataSize;
    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static final int DEPTH = 2;
    @Override
    public void start(Stage primaryStage) {
        links = FXCollections.observableArrayList();
        errorLog = FXCollections.observableArrayList();
        DataSize = DATA_SIZE.KB;
        ArtificialRoot = new ExtFolder(ARTIFICIAL_ROOT_NAME);
        ArtificialRoot.setPopulated(true);
        ArtificialRoot.setIsAbsoluteRoot(true);
        rootSet = new HashSet<>();
        remount();
        ArtificialRoot.propertyName.set("ROOT");
        links.add(new FavouriteLink("ROOT",""));
        
        ViewManager.getInstance().newWindow(ArtificialRoot, ArtificialRoot);
        
    } 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    public static void remount(){
        
        File[] roots = File.listRoots();
        for(int i = 0; i < roots.length ; i++){
            System.out.println("Root["+i+"]:" + roots[i].getAbsolutePath());
            mountDevice(roots[i].getAbsolutePath());
        }
    }
    public static boolean mountDevice(String name){
        boolean result = false;
        if(!new File(name).exists()){
            return false;
        }
        if(Files.isDirectory(Paths.get(name))){
            ExtFolder device = new ExtFolder(name);
            int nameCount = device.toPath().getNameCount();
            //Log.write("Is direcory");
            if(nameCount == 0){
                result = true;
                String newName = device.toPath().getRoot().toString();
                //Log.writeln("newName= "+newName);
                device.propertyName.set(newName);
                ArtificialRoot.files.putIfAbsent(newName, device);
                rootSet.add(newName);
                //new Thread(TaskFactory.getInstance().populateRecursiveParallel(device, DEPTH)).start();
                //Log.writeln("Mounted successfully");
            }
        }
        return result;
    }
    public static void reportError(Exception ex){
        ErrorReport error = new ErrorReport(ex);
        errorLog.add(0, error);
    }

}
