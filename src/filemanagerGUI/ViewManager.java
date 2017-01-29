/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import LibraryLB.Log;
import filemanagerGUI.dialog.AdvancedRenameController;
import filemanagerGUI.dialog.CommandWindowController;
import filemanagerGUI.dialog.DirSyncController;
import filemanagerGUI.dialog.DuplicateFinderController;
import filemanagerGUI.dialog.ListController;
import filemanagerGUI.dialog.ProgressDialogController;
import filemanagerGUI.dialog.RenameDialogController;
import filemanagerGUI.dialog.WebDialogController;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.FrameTitle;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ExtTask;
import filemanagerLogic.fileStructure.ExtPath;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * @author Laimonas Beniušis
 */
public class ViewManager {
    public SimpleBooleanProperty autoCloseProgressDialogs;
    private static final ViewManager INSTANCE = new ViewManager();
    protected ViewManager(){
        this.autoCloseProgressDialogs = new SimpleBooleanProperty(false);
        this.frames = new ConcurrentHashMap<>();
        this.windows = new HashSet<>();

    };
    private final ConcurrentHashMap<String,Frame> frames;
    private final HashSet<String> windows;

    public static ViewManager getInstance(){
        return INSTANCE;
    }
    private int findSmallestAvailable(Map<String,Frame> map,String title){
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
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.WINDOW);
                    MainController controller = (MainController) frame.getController();
                    windows.add(frame.getTitle());
                    controller.beforeShow(frame.getTitle(),rootFolder,currentFolder);
                    frame.getStage().show();
                } catch (IOException ex) {
                    ErrorReport.report(ex);
                }            
                return null;
            }
        };
        Platform.runLater(et);
        
    }
    public void updateAllWindows(){
        Platform.runLater(()->{
            for(String s:windows){
                MainController controller = (MainController) frames.get(s).getController();
                controller.update();
            }
        }); 
    }
    public void updateAllFrames(){
        Platform.runLater(()->{
            frames.values().forEach( frame ->{
                frame.getController().update();
            });
        });
    }
    public Frame getFrame(String windowID){
        return frames.get(windowID);
    }
    
//DIALOG ACTIONS
    public void newProgressDialog(ExtTask task){
        
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
            try {

                Frame frame = newFrame(FrameTitle.PROGRESS_DIALOG);
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
            return null;
            }
        };
        Platform.runLater(et);
    }
    public void newRenameDialog(ExtFolder folder,ExtPath itemToRename){
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.TEXT_INPUT_DIALOG);
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
                return null;
            }
        };
        Platform.runLater(et);
    }
    public void newAdvancedRenameDialog(Collection<String> list){
       
       
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
            try {
                Frame frame = newFrame(FrameTitle.ADVANCED_RENAME_DIALOG);
                AdvancedRenameController controller = (AdvancedRenameController) frame.getController();
                controller.beforeShow(frame.getStage().getTitle(),list);
                frame.getStage().show();
                controller.afterShow();
                frame.getStage().toFront();      
            } catch (Exception ex) {
                ErrorReport.report(ex);
            } 
            return null;
            }
        };
        Platform.runLater(et);
    }
    public void newDirSyncDialog(){
        
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
            try {
                Frame frame = newFrame(FrameTitle.DIR_SYNC_DIALOG);
                DirSyncController controller = (DirSyncController) frame.getController();
                controller.beforeShow(frame.getStage().getTitle());
                frame.getStage().show();
                controller.afterShow();
                frame.getStage().toFront();

            } catch (Exception ex) {
                ErrorReport.report(ex);
            }    
            return null;
            }
        };
        Platform.runLater(et);
        
  
    }
    public void newDuplicateFinderDialog(ExtFolder root){
      
      
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
               try {
                    Frame frame = newFrame(FrameTitle.DUPLICATE_FINDER_DIALOG);
                    DuplicateFinderController controller = (DuplicateFinderController) frame.getController();
                    controller.beforeShow(frame.getStage().getTitle(),root);
                    frame.getStage().show();
                    controller.afterShow();
                    frame.getStage().toFront();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }  
                return null;
            }
        };
        Platform.runLater(et);
   }
    public void newWebDialog(Enums.WebDialog info){     

        ExtTask et = new ExtTask() {
        @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.WEB_DIALOG);
                    WebDialogController controller = (WebDialogController) frame.getController();
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.getStage().show();
                    controller.afterShow(info);
                    frame.getStage().toFront();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
            return null;
            }
        };
        Platform.runLater(et);
    } 
    public void newCommandDialog(){
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
            try {
                Frame frame = newFrame(FrameTitle.COMMAND_DIALOG);
                CommandWindowController controller = (CommandWindowController) frame.getController();
                controller.beforeShow(frame.getStage().getTitle());
                frame.getStage().show();
                controller.afterShow();
                frame.getStage().toFront();

            } catch (Exception ex) {
                ErrorReport.report(ex);
            }    
            return null;
            }
        };
        Platform.runLater(et);
    }
    private Frame newFrame(FrameTitle info,Object...params) throws IOException, Exception{
        Boolean frameIsSingleton = false;
        if(params.length>0){
            frameIsSingleton = (Boolean) params[0];
        }
        String title = info.getTitle();
        if(!frameIsSingleton){
            int index = findSmallestAvailable(frames,info.getTitle());
            title+=index;
        }
        if(frames.containsKey(title)){
            throw new Exception("Frame:"+title+"Allready exists");
        }
        URL url = getClass().getResource("/filemanagerGUI/"+info.recourse);
        Log.write("URL=",url.toString());
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        BaseController controller = loader.getController();
        stage.setOnCloseRequest((WindowEvent we) -> {
            controller.exit();
        });
        controller.windowID = title;
        Frame frame = new Frame(stage,controller);
        this.frames.put(frame.getTitle(),frame);
        
        return frame;
       
    }
    public void closeFrame(String windowID){
        frames.get(windowID).getStage().close();
        frames.remove(windowID);
        windows.remove(windowID);
        if(windows.isEmpty()){
            try{
                FileManagerLB.doOnExit();
            }catch(Exception e){
                ErrorReport.report(e);
            }
            System.exit(0);
        }
    }
    
    public void newListFrame(String description, Collection<String> list){
        ExtTask et = new ExtTask() {
            @Override
            protected Void call() throws Exception {
            try {
                Frame frame = newFrame(FrameTitle.LIST_FRAME);
                ListController controller = (ListController) frame.getController();
                controller.beforeShow(frame.getStage().getTitle(),description);
                
                frame.getStage().show();
                controller.afterShow(list);
                frame.getStage().toFront();

            } catch (Exception ex) {
                ErrorReport.report(ex);
            }    
            return null;
            }
        };
        Platform.runLater(et);
    }
    
    
}
