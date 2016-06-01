/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import filemanagerGUI.dialog.AdvancedRenameController;
import filemanagerGUI.dialog.DirSyncController;
import filemanagerGUI.dialog.ProgressDialogController;
import filemanagerGUI.dialog.RenameDialogController;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ExtTask;
import filemanagerLogic.fileStructure.ExtFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import utility.ErrorReport;

/**
 *
 * @author lemmin
 */
public class ViewManager {
    public static final String WINDOW_TITLE = "LB File Manager ";
    public static final String PROGRESS_DIALOG_TITLE ="Progress Dialog ";
    public static final String TEXT_INPUT_DIALOG_TITLE="Text Input Dialog ";
    public static final String MESSAGE_DIALOG_TITLE="Message Dialog ";
    public static final String ADVANCED_RENAME_DIALOG_TITLE="Advanced Rename ";
    public static final String DIR_SYNC_DIALOG_TITLE="Directory Syncronization ";
    public SimpleBooleanProperty autoCloseProgressDialogs;
    private static final ViewManager INSTANCE = new ViewManager();
    
    protected ViewManager(){
        this.autoCloseProgressDialogs = new SimpleBooleanProperty(false);
        this.windows = new HashMap<>();
        this.dialogs = new HashMap<>();

    };
    public HashMap<String,Frame> dialogs;
    public HashMap<String,Frame> windows;

    public static ViewManager getInstance(){
        return INSTANCE;
    }
    private int findSmallestAvailable(HashMap<String,Frame> map,String title){
        int i =1;
        while(true){
            if(map.containsKey(title + i)){
                i++;
            }else{
                return i;
            }
        }
    }
    
// WINDOW ACTIONS
    public void newWindow(ExtFolder rootFolder,ExtFolder currentFolder){
        try {
            int index = findSmallestAvailable(windows,WINDOW_TITLE);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/Main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.<MainController>getController();
            Stage stage = new Stage();
            stage.setTitle(WINDOW_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });
            Frame frame = new Frame(stage,controller);
            windows.put(frame.getTitle(),frame);
            controller.setUp(stage.getTitle(),rootFolder,currentFolder);
            stage.show();

            
            
        } catch (IOException ex) {
            ErrorReport.report(ex);
        }
        
    }
    public void closeWindow(String title){
        windows.get(title).getStage().close();
        windows.remove(title);
        if(windows.isEmpty()){
            System.exit(0);
        }
    }
    public void closeAllWindows(){
        String[] keys = windows.keySet().toArray(new String[0]);
        for(String s:keys){
            closeWindow(s);
        }
    }
    public void updateAllWindows(){
        Platform.runLater(()->{
            for(String s:windows.keySet()){
                MainController controller = (MainController) windows.get(s).getController();
                controller.updateCurrentView();
            }
        });
        
    }
    public BaseController getController(String windowID){
        return windows.get(windowID).getController();
    }
//PROGRESS DIALOG ACTIONS
    public void newProgressDialog(ExtTask task){
        System.out.println(task.getState());
        try {
            int index = findSmallestAvailable(dialogs,PROGRESS_DIALOG_TITLE);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/ProgressDialog.fxml"));
           
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(PROGRESS_DIALOG_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setMaxHeight(300);
            stage.setMinHeight(250);
            stage.setMinWidth(400);
            stage.show();
            stage.toFront();
            stage.requestFocus();
            
            //stage.setResizable(false);
            ProgressDialogController controller = loader.<ProgressDialogController>getController();
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });  
            controller.setUp(stage.getTitle(), task);
            Frame frame = new Frame(stage,controller);
            dialogs.put(frame.getTitle(),frame);          
        } catch (Exception ex) {
           ErrorReport.report(ex);
        }
        
    }
    
//TEXT INPUT DIALOG ACTIONS
   
    public void newRenameDialog(ExtFolder folder,ExtFile itemToRename){
        try {
            int index = findSmallestAvailable(dialogs,TEXT_INPUT_DIALOG_TITLE);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/RenameDialog.fxml"));
           
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(TEXT_INPUT_DIALOG_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setMaxHeight(200);
            stage.setMinHeight(200);
            stage.setMinWidth(500);
            stage.show();
            stage.toFront();
            stage.setAlwaysOnTop(true);
            RenameDialogController controller = loader.<RenameDialogController>getController();
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });
            controller.setUp(stage.getTitle(),folder,itemToRename);
            Frame frame = new Frame(stage,controller);
            dialogs.put(frame.getTitle(),frame);          
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
        
    }
    public void newAdvancedRenameDialog(ArrayList<String> list){
       try {
            int index = findSmallestAvailable(dialogs,ADVANCED_RENAME_DIALOG_TITLE);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/AdvancedRename.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(ADVANCED_RENAME_DIALOG_TITLE+index);
            stage.setScene(new Scene(root));
            stage.show();
            stage.toFront();
            AdvancedRenameController controller = loader.<AdvancedRenameController>getController();
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });
            controller.setUp(stage.getTitle(),list);
            Frame frame = new Frame(stage,controller);
            dialogs.put(frame.getTitle(),frame);          
        } catch (Exception ex) {
            ErrorReport.report(ex);
        } 
    }
    public void newDirSyncDialog(){
      try {
           int index = findSmallestAvailable(dialogs,DIR_SYNC_DIALOG_TITLE);
           FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/DirSync.fxml"));
           Parent root = loader.load();
           Stage stage = new Stage();
           stage.setTitle(DIR_SYNC_DIALOG_TITLE+index);
           stage.setScene(new Scene(root));
           stage.show();
           stage.toFront();
           DirSyncController controller = loader.<DirSyncController>getController();
           stage.setOnCloseRequest((WindowEvent we) -> {
               controller.exit();
           });
           controller.setUp(stage.getTitle());
           Frame frame = new Frame(stage,controller);
           dialogs.put(frame.getTitle(),frame);          
       } catch (Exception ex) {
           ErrorReport.report(ex);
       } 
   }
    public void closeDialog(String title){
        dialogs.get(title).getStage().close();
        dialogs.remove(title);
    }
    
}
