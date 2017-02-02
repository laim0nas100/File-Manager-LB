/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.BaseController;
import filemanagerGUI.ViewManager;
import LibraryLB.ExtTask;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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
 * @author Laimonas Beniušis
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
    private SimpleBooleanProperty paused;
    
    public void afterShow(ExtTask newTask,boolean pause){
        super.afterShow();
        paused = new SimpleBooleanProperty(pause);
        this.task = newTask;
        task.paused.bind(paused);
        bar.progressProperty().bind(task.progressProperty());
        indicator.progressProperty().bind(bar.progressProperty());
        cancelButton.disableProperty().bind(task.runningProperty().not());
        okButton.disableProperty().bind(cancelButton.disableProperty().not());
        pauseButton.disableProperty().bind(cancelButton.disabledProperty());
        
        text.textProperty().bind(task.messageProperty());
        taskDescription.setText(task.getTaskDescription());
        
        Thread t = new Thread(task);
        clock = new CustomClock();
        t.setDaemon(true);
        timeWasted.textProperty().bind(clock.timeProperty);
        clock.paused.bind(paused);
        
        task.setOnSucceeded((e)->{
            clock.stopTimer();
            if(task.childTask!=null){
                task.run();
            }
            if(ViewManager.getInstance().autoCloseProgressDialogs.get()){
                this.exit();
            }
        });
        
        if(pause){
            pauseButton.setText("CONTINUE");
            paused.set(true);
        }
        Platform.runLater(()->{
            t.start();
            
        });
        //t.start();
        
        
    }
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
    }
    public void cancelTask(){
        this.task.cancel();
        exit();
    }
    public void pauseTask(){
        if(task.isPaused()&&task.isRunning()){
            pauseButton.setText("PAUSE");
            paused.set(false);
        }else if(!task.isPaused()&&task.isRunning()){
            pauseButton.setText("CONTINUE");
            paused.set(true);
        }
    }

    @Override
    public void update() {
    }
    
}
