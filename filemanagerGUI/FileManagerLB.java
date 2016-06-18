/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import utility.ErrorReport;
import utility.FavouriteLink;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB extends Application {
    public static final String ARTIFICIAL_ROOT_NAME = System.getProperty("user.dir")+"/root.Don't use this.root";
    public static ExtFolder ArtificialRoot = new ExtFolder(ARTIFICIAL_ROOT_NAME);
    public static DATA_SIZE DataSize;
    
    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static final int DEPTH = 2;
    public static final SimpleBooleanProperty DEBUG = new SimpleBooleanProperty(1==0);
    
    @Override
    public void start(Stage primaryStage) {
        links = FXCollections.observableArrayList();
        errorLog = FXCollections.observableArrayList();
        //DataSize = DATA_SIZE.KB;
        ArtificialRoot.setPopulated(true);
        ArtificialRoot.setIsAbsoluteRoot(true);
        try{
            Files.createFile(ArtificialRoot.toPath());
        }catch(Exception e){
            
        }
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
        for (File root : roots) {
            //Log.writeln("Root["+i+"]:" + roots[i].getAbsolutePath());
            mountDevice(root.getAbsolutePath());
        }
    }
    public static boolean mountDevice(String name){
        boolean result = false;
        name = name.toUpperCase();
        //Log.write("Mount:",name);
        Path path = Paths.get(name);
        if(Files.isDirectory(path)){
            ExtFolder device = new ExtFolder(name);
            int nameCount = path.getNameCount();
            //Log.write("Is direcory");
            if(nameCount == 0){
                result = true;
                String newName = path.getRoot().toString();
                //Log.writeln("newName= "+newName);
                device.propertyName.set(newName);
                if(!ArtificialRoot.files.containsKey(newName)){
                    ArtificialRoot.files.put(newName, device);
                    device.update();
                    
                }else{
                    result = false;
                }
            }
        }
        return result;
    }
    public static Set<String> getRootSet(){
        HashSet<String> set = new HashSet<>();
        for(ExtFile file:ArtificialRoot.files.values()){
            set.add(file.propertyName.get());
        }
        return set;
        
    }
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
}
