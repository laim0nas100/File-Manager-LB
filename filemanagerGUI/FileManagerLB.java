/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ManagingClass;
import filemanagerLogic.TaskFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import utility.Log;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB extends Application {
    //public static HashMap<Integer,Stage> windows;
    public static final String ARTIFICIAL_ROOT_NAME = "Devices";
    public static ExtFolder FolderForDevices;

    @Override
    public void start(Stage primaryStage) {
        //Log.changeStream('f', new File("E:\\log.txt"));
        //rootDirectory = new ExtFolder("/mnt/ExtraSpace/Test1/");
        FolderForDevices = new ExtFolder(ARTIFICIAL_ROOT_NAME);
        FolderForDevices.setIsRoot(true);
        FolderForDevices.setPopulated(true);
        
        //mountDevice("E:\\");
        //mountDevice("C:\\");
       
        mountDevice("/");
        FolderForDevices.name.set("DEVICES");
        ViewManager.getInstance().newWindow(FolderForDevices, FolderForDevices);
        
        
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    public static boolean mountDevice(String name){
        boolean result = false;
        if(Files.isDirectory(Paths.get(name))){
            ExtFolder device = new ExtFolder(name);
            int nameCount = device.toPath().getNameCount();
            Log.write("Is direcory");
            if(nameCount == 0){
                result = true;
                String newName = name;
                //System is Windows
                if(!name.equals("/")){
                    newName = name.substring(0, name.lastIndexOf(File.separator));
                }
                
                Log.writeln("newName="+newName);
                device.name.set(newName);
                FolderForDevices.files.put(newName, device);
                device.populateFolder();
                Log.writeln("Mounted successfully");
                //Log.write(FolderForDevices.files.keySet());
            }
        }
        return result;
    }
    
    
    
}
