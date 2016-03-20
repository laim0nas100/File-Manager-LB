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
        //rootDirectory = new ExtFolder("/mnt/ExtraSpace/Test1/");
        FolderForDevices = new ExtFolder(ARTIFICIAL_ROOT_NAME);
        FolderForDevices.setIsRoot(true);
        FolderForDevices.setPopulated(true);
        ExtFolder folder1 = new ExtFolder("E:\\");
        folder1.name.set("E:\\");
        ExtFolder folder2 = new ExtFolder("C:\\");
        folder2.name.set("C:\\");
        FolderForDevices.files.put("C:", folder2);
        FolderForDevices.files.put("E:",folder1);
        FolderForDevices.name.set("DEVICES");
        ViewManager.getInstance().newWindow(FolderForDevices, folder1);
        
        
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
