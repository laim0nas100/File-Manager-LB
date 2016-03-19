/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import filemanagerGUI.dialog.ProgressDialogController;
import filemanagerLogic.ExtFile;
import filemanagerLogic.ExtTask;
import filemanagerLogic.TaskFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import javafx.concurrent.Task;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
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
    public HashMap<String,Stage> messageBoxes;
    public HashMap<String,Stage> progressDialogs;
    public HashMap<String,Stage> windows;
    public static ViewManager getInstance(){
        return INSTANCE;
    }
    private int findSmallestAvailable(HashMap<String,Stage> map){
        int i =1;
        while(true){
            if(map.containsKey(WINDOW_TITLE + i)){
                i++;
            }else{
                return i;
            }
        }
    }
    
    // WINDOW ACTIONS
    public void newWindow(){
        try {
            int index = findSmallestAvailable(windows);
            
            Stage stage = buildNewWindowStage(WINDOW_TITLE,index);
            windows.put(stage.getTitle(),stage);
            windows.get(stage.getTitle()).show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }
    public void closeWindow(String title){
        windows.get(title).close();
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
    private Stage buildNewWindowStage(String title,int id) throws IOException{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainComponents.fxml"));
        Parent root = loader.load();
        
        Stage stage = new Stage();
        stage.setTitle(title+id);
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest((WindowEvent we) -> {
            System.out.println("Closing by request");
            this.closeWindow(stage.getTitle());
        });
        
        MainController controller = loader.<MainController>getController();
        controller.title = stage.getTitle();
        
        return stage;
    }
    
    
    //PROGRESS DIALOG ACTIONS
    public void newProgressDialog(ExtTask task){
        System.out.println(task.getState());
        try {
            int index = findSmallestAvailable(progressDialogs);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog/ProgressDialog.fxml"));
           
            Parent root = loader.load();  
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
            progressDialogs.put(stage.getTitle(),stage);          
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }
    public void closeProgressDialog(String title){
        progressDialogs.get(title).close();
        progressDialogs.remove(title);
    }
    
    //CUSTOM VIEWS
    
    public void setTableView(String title,TableView tableView){
        Scene scene = windows.get(title).getScene();
        AnchorPane left = (AnchorPane) scene.lookup("#left");
        left.getChildren().clear();
        left.getChildren().add(tableView);
    }
    public void setFlowView(String title,ScrollPane flowViewScroll,FlowPane flowView){
        Scene scene = windows.get(title).getScene();
        AnchorPane left = (AnchorPane) scene.lookup("#left");
        left.getChildren().clear();
        left.getChildren().add(flowView);
    }
    
    
}
