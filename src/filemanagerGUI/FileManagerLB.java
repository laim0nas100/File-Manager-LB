/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.FileManaging.AutoBackupMaker;
import LibraryLB.FileManaging.FileReader;
import LibraryLB.Log;
import LibraryLB.ParametersMap;
import filemanagerLogic.Enums;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Application;
import javafx.application.Platform;
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
    public static final String ARTIFICIAL_ROOT_NAME = System.getProperty("user.dir")+File.separator+"root.Don't use this.root";
    public static ExtFolder ArtificialRoot = new ExtFolder(ARTIFICIAL_ROOT_NAME);
    public static String DIR  = System.getProperty("user.dir");
    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static int DEPTH = 2;
    public static SimpleBooleanProperty DEBUG = new SimpleBooleanProperty(true);
    public static int LogBackupCount;
    public static ParametersMap parameters;
    @Override
    public void start(Stage primaryStage) {
        ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
        Platform.runLater(()->{
            
            links = FXCollections.observableArrayList();
            errorLog = FXCollections.observableArrayList();
            ArtificialRoot.setPopulated(true);
            ArtificialRoot.setIsAbsoluteRoot(true);
            if(!DIR.endsWith(File.separator)){
                DIR+=File.separator;
            }

            try{
                Log.changeStream('f', new File(DIR+"Log.txt"));
                ArrayList<String> list = FileReader.readFromFile(DIR+"Parameters.txt");
                parameters = new ParametersMap(list);
                DEBUG = new SimpleBooleanProperty((boolean) parameters.defaultGet("debug",false));
                DEPTH = (int) parameters.defaultGet("lookDepth",2);
                LogBackupCount = (int) parameters.defaultGet("logBackupCount", 2);

                Files.deleteIfExists(ArtificialRoot.toPath());
                Files.createFile(ArtificialRoot.toPath());
                Log.writeln("Parameters",parameters);

            }catch(Exception e){
                ErrorReport.report(e);
            }
            ArtificialRoot.propertyName.set("ROOT");
            links.add(new FavouriteLink("ROOT",""));
            ViewManager.getInstance().newWindow(ArtificialRoot, ArtificialRoot);
            ViewManager.getInstance().updateAllWindows();
        });
        
    } 
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
    public static void doOnExit(){
        try {
            Files.deleteIfExists(ArtificialRoot.toPath());
            AutoBackupMaker BM = new AutoBackupMaker(LogBackupCount,DIR+"BUP","YYYY-MM-dd HH.mm.ss");
            Collection<Runnable> makeNewCopy = BM.makeNewCopy(DIR+"Log.txt");
            makeNewCopy.forEach(th ->{
                th.run();
            });
            BM.cleanUp().run();
        } catch (IOException ex) {
            ErrorReport.report(ex);
        }
        System.exit(0);
    }
    
}
