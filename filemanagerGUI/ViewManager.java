/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import filemanagerGUI.dialog.ProgressDialogController;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ExtTask;
import java.io.IOException;
import java.util.HashMap;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author lemmin
 */
public class ViewManager {
    public static final String WINDOW_TITLE = "LB File Manager ";
    public static final String PROGRESS_TITLE ="Progress Dialog ";
    private static final ViewManager INSTANCE = new ViewManager();
    
    protected ViewManager(){
        this.messageBoxes = new HashMap<>();
        this.progressDialogs = new HashMap<>();
        this.windows = new HashMap<>();
    };
    public HashMap<String,Frame> textInputDialogs;
    public HashMap<String,Frame> messageBoxes;
    public HashMap<String,Frame> progressDialogs;
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
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("mainComponents.fxml"));
            Parent root = loader.load();
            MainController controller = loader.<MainController>getController();
            Frame frame = new Frame();
            Stage stage = new Stage();
            stage.setTitle(WINDOW_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.closeWindow();
            });
            frame.setController(controller);
            frame.setStage(stage);
            windows.put(frame.getTitle(),frame);
            stage.show();
            controller.setUp(stage.getTitle(),rootFolder,currentFolder);
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }
    public void closeWindow(String title){
        windows.get(title).getStage().close();
        windows.remove(title);
        if(windows.size()==0){
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
        String[] keys = windows.keySet().toArray(new String[0]);
        for(String s:keys){
            MainController controller = (MainController) windows.get(s).getController();
            controller.updateCurrentView();
        }
    }
    //PROGRESS DIALOG ACTIONS
    public void newProgressDialog(ExtTask task){
        System.out.println(task.getState());
        try {
            int index = findSmallestAvailable(progressDialogs,PROGRESS_TITLE);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog/ProgressDialog.fxml"));
           
            Parent root = loader.load();
            Frame frame = new Frame();
            Stage stage = new Stage();
            stage.setTitle(PROGRESS_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setMaxHeight(300);
            stage.setMinHeight(250);
            stage.setMinWidth(400);
            stage.show();
            stage.toFront();
            //stage.setResizable(false);
            ProgressDialogController controller = loader.<ProgressDialogController>getController();
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });  
            controller.setUp(stage.getTitle(), task);
            frame.setController(controller);
            frame.setStage(stage);
            progressDialogs.put(frame.getTitle(),frame);          
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }
    public void closeProgressDialog(String title){
        progressDialogs.get(title).getStage().close();
        progressDialogs.remove(title);
    }
    //Text Input Dialogs
    
    
    //CUSTOM VIEWS
    
    public void setTableView(String title,TableView tableView){
        Scene scene = windows.get(title).getStage().getScene();
        AnchorPane left = (AnchorPane) scene.lookup("#left");
        left.getChildren().clear();
        left.getChildren().add(tableView);
    }
    public void setFlowView(String title,ScrollPane flowViewScroll,FlowPane flowView){
        Scene scene = windows.get(title).getStage().getScene();
        AnchorPane left = (AnchorPane) scene.lookup("#left");
        left.getChildren().clear();
        left.getChildren().add(flowView);
    }
    
    
}
