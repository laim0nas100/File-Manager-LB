/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import static filemanagerGUI.FileManagerLB.ArtificialRoot;
import filemanagerGUI.dialog.AdvancedRenameController;
import filemanagerGUI.dialog.DirSyncController;
import filemanagerGUI.dialog.ProgressDialogController;
import filemanagerGUI.dialog.RenameDialogController;
import filemanagerGUI.dialog.WebRegexHelpController;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ExtTask;
import filemanagerLogic.fileStructure.ExtFile;
import java.io.IOException;
import java.nio.file.Files;
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
    private static class Titles{
        private static final String WINDOW = "LB File Manager ";
        private static final String PROGRESS_DIALOG ="Progress Dialog ";
        private static final String TEXT_INPUT_DIALOG="Text Input Dialog ";
        private static final String MESSAGE_DIALOG="Message Dialog ";
        private static final String ADVANCED_RENAME_DIALOG="Advanced Rename ";
        private static final String DIR_SYNC_DIALOG="Directory Syncronization ";
        private static final String REGEX_HELP_DIALOG="Regex Help ";
    }
    
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
            int index = findSmallestAvailable(windows,Titles.WINDOW);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/Main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.<MainController>getController();
            Stage stage = new Stage();
            stage.setTitle(Titles.WINDOW+index);
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
            try {
                Files.deleteIfExists(ArtificialRoot.toPath());
            } catch (IOException ex) {
            }
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
    
//DIALOG ACTIONS
    public void newProgressDialog(ExtTask task){
        System.out.println(task.getState());
        try {
            int index = findSmallestAvailable(dialogs,Titles.PROGRESS_DIALOG);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/ProgressDialog.fxml"));
           
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(Titles.PROGRESS_DIALOG+index);
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
    public void newRenameDialog(ExtFolder folder,ExtFile itemToRename){
        try {
            int index = findSmallestAvailable(dialogs,Titles.TEXT_INPUT_DIALOG);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/RenameDialog.fxml"));
           
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(Titles.TEXT_INPUT_DIALOG+index);
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
            int index = findSmallestAvailable(dialogs,Titles.ADVANCED_RENAME_DIALOG);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/AdvancedRename.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(Titles.ADVANCED_RENAME_DIALOG+index);
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
           int index = findSmallestAvailable(dialogs,Titles.DIR_SYNC_DIALOG);
           FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/DirSync.fxml"));
           Parent root = loader.load();
           Stage stage = new Stage();
           stage.setTitle(Titles.DIR_SYNC_DIALOG+index);
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
    public void newRegexHelpDialog(){
    try {
           int index = findSmallestAvailable(dialogs,Titles.REGEX_HELP_DIALOG);
           FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/WebRegexHelp.fxml"));
           Parent root = loader.load();
           Stage stage = new Stage();
           stage.setTitle(Titles.REGEX_HELP_DIALOG+index);
           stage.setScene(new Scene(root));
           stage.show();
           stage.toFront();
           WebRegexHelpController controller = loader.<WebRegexHelpController>getController();
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
