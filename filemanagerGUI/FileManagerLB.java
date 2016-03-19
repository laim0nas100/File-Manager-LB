/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.ExtFolder;
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
    
    public static ExtFolder rootDirectory;
    @Override
    public void start(Stage primaryStage) {
        //rootDirectory = new ExtFolder("/mnt/ExtraSpace/Test1/");
        rootDirectory = new ExtFolder("E:\\");
        rootDirectory.populateFolder();
        ViewManager.getInstance().newWindow();
        
        
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
