/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.BaseController;
import filemanagerGUI.ViewManager;
import filemanagerLogic.ExtTask;
import filemanagerLogic.TaskFactory;
import java.time.Clock;
import java.time.Instant;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import utility.CustomClock;

/**
 * FXML Controller class
 *
 * @author lemmin
 */
public class ProgressDialogController extends BaseController {

    @FXML public VBox base;
    
    @FXML public Button okButton;
    @FXML public Button cancelButton;
    @FXML public Button pauseButton;
    @FXML public ProgressBar bar;
    @FXML public ProgressIndicator indicator;
    @FXML public Label text;
    @FXML public Label taskDescription;
    @FXML public Label timeWasted;
    
    protected CustomClock clock;

    
    private ExtTask task;
    
    
    public void setUp(String title,ExtTask newTask){
        this.title = title;
        this.task = newTask;
        bar.progressProperty().bind(task.progressProperty());
        indicator.progressProperty().bind(bar.progressProperty());
        cancelButton.disableProperty().bind(task.runningProperty().not());
        okButton.disableProperty().bind(cancelButton.disableProperty().not());
        pauseButton.disableProperty().bind(cancelButton.disabledProperty());
        text.textProperty().bind(task.messageProperty());
        taskDescription.setText(task.getTaskDescription());
        
        Thread t = new Thread(this.task);
        clock = new CustomClock();
        t.setDaemon(true);
        timeWasted.textProperty().bind(clock.timeProperty);
        t.start();
        task.setOnSucceeded((e)->{
            clock.stopTimer();
            if(ViewManager.getInstance().autoCloseProgressDialogs.get()){
                this.exit();
            }
        });
        
        
    }
    public void cancelTask(){
        this.task.cancel();
        exit();
    }
    public void exit(){
        
        ViewManager.getInstance().closeProgressDialog(this.title);
        ViewManager.getInstance().updateAllWindows();
    }
    public void pauseTask(){
        if(task.isPaused()&&task.isRunning()){
            pauseButton.setText("PAUSE");
            task.setPaused(false);
        }else if(!task.isPaused()&&task.isRunning()){
            pauseButton.setText("CONTINUE");
            task.setPaused(true);
        }
    }
    
}
