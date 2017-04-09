/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import LibraryLB.Threads.FXTask;
import LibraryLB.Log;
import filemanagerGUI.dialog.AdvancedRenameController;
import filemanagerGUI.dialog.CommandWindowController;
import filemanagerGUI.dialog.DuplicateFinderController;
import filemanagerGUI.dialog.ListController;
import filemanagerGUI.dialog.ProgressDialogController;
import filemanagerGUI.dialog.RenameDialogController;
import filemanagerGUI.dialog.WebDialogController;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.FrameTitle;
import filemanagerLogic.SimpleTask;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.fileStructure.ExtPath;
import java.io.IOException;
import java.lang.ref.Reference;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Task;

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
    public SimpleBooleanProperty autoStartProgressDialogs;
    public SimpleBooleanProperty pinProgressDialogs;
    public SimpleBooleanProperty pinTextInputDialogs;
    private static final ViewManager INSTANCE = new ViewManager();
    protected ViewManager(){
        this.autoCloseProgressDialogs = new SimpleBooleanProperty(false);
        this.autoStartProgressDialogs = new SimpleBooleanProperty(false);
        this.pinProgressDialogs = new SimpleBooleanProperty(false);
        this.pinTextInputDialogs = new SimpleBooleanProperty(false);
        this.frames = new HashMap<>();
        this.windows = new HashSet<>();

    };
    public final HashMap<String,Frame> frames;
    public final HashSet<String> windows;
    private boolean initStart = false;

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
    public void newWindow(ExtFolder currentFolder){
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.WINDOW);
                    MainController controller = (MainController) frame.getController();
                    windows.add(frame.getID());
                    controller.beforeShow(frame.getTitle(),currentFolder);
                    frame.getStage().show();
                    controller.afterShow();
                } catch (IOException ex) {
                    ErrorReport.report(ex);
                }            
                return null;
            }
        };
        Platform.runLater(et);
        
    }
    public void updateAllWindows(){
//        Platform.runLater(()->{
            for(String s:windows){
                MainController controller = (MainController) frames.get(s).getController();
                controller.update();
            }
//        }); 
    }
    public void updateAllFrames(){
        frames.values().forEach( frame ->{
            frame.getController().update();
        });
    }
    public Frame getFrame(String windowID){
        return frames.get(windowID);
    }
    public boolean frameIsVisible(String windowID){
        boolean res = frames.containsKey(windowID);
//        Log.write(windowID +" isVisible call:"+res );
        return res;
    }
    
//DIALOG ACTIONS
    public void newProgressDialog(FXTask task){
        
        FXTask et = new FXTask() {
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
                frame.getStage().setAlwaysOnTop(ViewManager.getInstance().pinProgressDialogs.get());
                controller.afterShow(task);
                frame.getStage().requestFocus();
                frame.getStage().toFront();         
            } catch (Exception ex) {
               ErrorReport.report(ex);
            }    
            return null;
            }
        };
        et.runOnPlatform();
    }
    public void newRenameDialog(ExtFolder folder,ExtPath itemToRename){
        FXTask et = new FXTask() {
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
                    frame.getStage().setAlwaysOnTop(ViewManager.getInstance().pinTextInputDialogs.get());
                    controller.afterShow(folder,itemToRename);
                    frame.getStage().requestFocus();
                    frame.getStage().toFront();            
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();
    }
    public void newAdvancedRenameDialog(ExtFolder folder){
       
       
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
            try {
                Frame frame = newFrame(FrameTitle.ADVANCED_RENAME_DIALOG);
                AdvancedRenameController controller = (AdvancedRenameController) frame.getController();
                controller.beforeShow(frame.getStage().getTitle(),folder);
                frame.getStage().show();
                controller.afterShow();
                frame.getStage().toFront();      
            } catch (Exception ex) {
                ErrorReport.report(ex);
            } 
            return null;
            }
        };
        et.runOnPlatform();
    }
    public void newDirSyncDialog(){
        
        FXTask et = new FXTask() {
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
        et.runOnPlatform();
        
  
    }
    public void newDuplicateFinderDialog(ExtFolder root){

        FXTask et = new FXTask() {
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
        et.runOnPlatform();
   }
    public void newWebDialog(Enums.WebDialog info){     

        FXTask et = new FXTask() {
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
        et.runOnPlatform();
    } 
    public void newCommandDialog(){
        FXTask et = new FXTask() {
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
        et.runOnPlatform();
    }
    public void newListFrame(String description, Collection<String> list){
        FXTask et = new FXTask() {
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
        et.runOnPlatform();
        
    } 
    public void newMediaPlayer(){
        
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.MEDIA_PLAYER);
                    MediaPlayerController controller = (MediaPlayerController) frame.getController();
                    SimpleBooleanProperty property = new SimpleBooleanProperty(true);
                    SimpleTask init = new SimpleTask() {
                        @Override
                        protected Void call() throws Exception {
                            try{
                                controller.discover();
                            }catch(Exception e){
                                ErrorReport.report(e);
                                property.set(false);
                            }
                            return null;
                        }
                    };
                    init.setOnSucceeded(event ->{
                        new SimpleTask() {
                            @Override
                            protected Void call() throws Exception {
                                if(property.get()){
                                    controller.afterShow();
                                }else{
                                    closeFrame(controller.windowID);
                                }
                                return null;
                            }
                        }.runOnPlatform();
                        
                    });
                    frame.getStage().show();
                    frame.getStage().toFront();
                    new Thread(init).start();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }

            return null;
            }
        };
        et.runOnPlatform();
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
        Log.print("URL=",url.toString());
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
        Frame frame = new Frame(stage,controller,info.title);       
        this.frames.put(frame.getTitle(),frame);
        
        
        final Frame.Pos[] pos = new Frame.Pos[1];
        if(!Frame.positionMemoryMap.containsKey(info.title)){
            pos[0] = new Frame.Pos(stage.getX(), stage.getY());          
            Frame.positionMemoryMap.put(info.title,pos[0]);
        }
        pos[0] = Frame.positionMemoryMap.get(info.title);
        ChangeListener listenerY = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            pos[0].y.set((double) newValue);
        };
        ChangeListener listenerX = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            pos[0].x.set((double) newValue);
        };
        
        stage.setX(pos[0].x.get());
        stage.setY(pos[0].y.get());
        frame.listenerX = listenerX;
        frame.listenerY = listenerY;
        stage.xProperty().addListener(listenerX);
        stage.yProperty().addListener(listenerY);
        
        
        return frame;
       
    }
    public void closeFrame(String windowID){
        if(this.initStart){
            return;
        }
        
        Frame frame = frames.get(windowID);
        Stage stage = frame.getStage();
        stage.xProperty().removeListener(frame.listenerX);
        stage.yProperty().removeListener(frame.listenerY);
        stage.close();
        
        frames.remove(windowID);
        windows.remove(windowID);
        if(windows.isEmpty()){
            closeAllFramesNoExit();
            try{
                FileManagerLB.doOnExit();
            }catch(Exception e){
                ErrorReport.report(e);
            }
            System.exit(0);
        }
    }
    public void closeAllFramesNoExit(){
        this.initStart = true;
        frames.keySet().forEach(key->{
            Frame frame = frames.get(key);
            frame.getController().exit(); 
            Log.print("Close",frame.getID());
            frame.getStage().close();
        });
        frames.clear();
        windows.clear();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ViewManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.initStart = false;        
    }
    
    
    
}
