/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import static filemanagerGUI.FileManagerLB.ArtificialRoot;
import filemanagerGUI.dialog.AdvancedRenameController;
import filemanagerGUI.dialog.DirSyncController;
import filemanagerGUI.dialog.DuplicateFinderController;
import filemanagerGUI.dialog.ProgressDialogController;
import filemanagerGUI.dialog.RenameDialogController;
import filemanagerGUI.dialog.WebDialogController;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.FrameTitle;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ExtTask;
import filemanagerLogic.fileStructure.ExtFile;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;

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
        private static final String WINDOW = "File Manager LB ";
        private static final String PROGRESS_DIALOG ="Progress Dialog ";
        private static final String TEXT_INPUT_DIALOG="Text Input Dialog ";
        private static final String MESSAGE_DIALOG="Message Dialog";
        private static final String ADVANCED_RENAME_DIALOG="Advanced Rename ";
        private static final String DIR_SYNC_DIALOG="Directory Synchronization ";
        private static final String REGEX_HELP_DIALOG="Web Dialog ";
        private static final String DUPLICATE_FINDER_DIALOG="Duplicate Finder ";
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
            if(map.containsKey(title+i)){
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
            controller.beforeShow(stage.getTitle(),rootFolder,currentFolder);
            stage.show();
        } catch (IOException ex) {
            ErrorReport.report(ex);
        }
        
    }
    public void closeWindow(String title){
        
        if(windows.size()==1){
            ArrayList<String> titles = new ArrayList<>();
            dialogs.values().forEach(dialog ->{
                titles.add(dialog.getTitle());
            });
            
            titles.forEach(t ->{
                closeDialog(t);
            });
            FileManagerLB.doOnExit();
            windows.get(title).getStage().close();
            dialogs.clear();
            windows.clear();
            System.exit(0);
        }else{
            windows.get(title).getStage().close();
            windows.remove(title); 
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

            Frame frame = newDialogFrame(FrameTitle.PROGRESS_DIALOG);
            ProgressDialogController controller = (ProgressDialogController) frame.getController();
            controller.beforeShow(frame.getStage().getTitle());
            frame.getStage().setMaxHeight(300);
            frame.getStage().setMinHeight(250);
            frame.getStage().setMinWidth(400);
            frame.getStage().show();
            controller.afterShow(task);
            frame.getStage().requestFocus();
            frame.getStage().toFront();         
        } catch (Exception ex) {
           ErrorReport.report(ex);
        }
        
    }
    public void newRenameDialog(ExtFolder folder,ExtFile itemToRename){
        try {
            
            Frame frame = newDialogFrame(FrameTitle.TEXT_INPUT_DIALOG);
            RenameDialogController controller = (RenameDialogController) frame.getController();
            controller.beforeShow(frame.getStage().getTitle());
            frame.getStage().setMaxHeight(200);
            frame.getStage().setMinHeight(200);
            frame.getStage().setMinWidth(500);
            frame.getStage().show();
            frame.getStage().setAlwaysOnTop(true);
            
            controller.afterShow(folder,itemToRename);
            frame.getStage().toFront();           
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
        
    }
    public void newAdvancedRenameDialog(ArrayList<String> list){
       try {
            Frame frame = newDialogFrame(FrameTitle.ADVANCED_RENAME_DIALOG);
            AdvancedRenameController controller = (AdvancedRenameController) frame.getController();
            controller.beforeShow(frame.getStage().getTitle(),list);
            frame.getStage().show();
            controller.afterShow();
            frame.getStage().toFront();      
        } catch (Exception ex) {
            ErrorReport.report(ex);
        } 
    }
    public void newDirSyncDialog(){
      try {
           Frame frame = newDialogFrame(FrameTitle.DIR_SYNC_DIALOG);
           DirSyncController controller = (DirSyncController) frame.getController();
           controller.beforeShow(frame.getStage().getTitle());
           frame.getStage().show();
           controller.afterShow();
           frame.getStage().toFront();
              
       } catch (Exception ex) {
           ErrorReport.report(ex);
       } 
    }
    public void newDuplicateFinderDialog(ExtFolder root){
      try {
           Frame frame = newDialogFrame(FrameTitle.DUPLICATE_FINDER_DIALOG);
           DuplicateFinderController controller = (DuplicateFinderController) frame.getController();
           controller.beforeShow(frame.getStage().getTitle(),root);
           frame.getStage().show();
           controller.afterShow();
           frame.getStage().toFront();
              
       } catch (Exception ex) {
           ErrorReport.report(ex);
       } 
   }
    public void newWebDialog(Enums.WebDialog info){     
    try {
           Frame frame = newDialogFrame(FrameTitle.WEB_DIALOG);
           WebDialogController controller = (WebDialogController) frame.getController();
           controller.beforeShow(frame.getStage().getTitle());
           frame.getStage().show();
           controller.afterShow(info);
           frame.getStage().toFront();
              
       } catch (Exception ex) {
           ErrorReport.report(ex);
       }
    }
    private Frame newDialogFrame(FrameTitle info) throws IOException{
        int index = findSmallestAvailable(dialogs,info.title);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(info.recourse));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(info.title+" "+index);
        stage.setScene(new Scene(root));
        BaseController controller = loader.getController();
        stage.setOnCloseRequest((WindowEvent we) -> {
            controller.exit();
        });
        Frame frame = new Frame(stage,controller);
        dialogs.put(frame.getTitle(),frame); 
        return frame;
       
    }
    public void closeDialog(String title){
        dialogs.get(title).getStage().close();
        dialogs.remove(title);
    }
    
    
}
