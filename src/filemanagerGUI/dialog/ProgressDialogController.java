/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import LibraryLB.ExtTask;
import filemanagerGUI.BaseController;
import filemanagerGUI.ViewManager;
import java.util.ArrayList;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
    @FXML public ProgressBar progressBar;
    @FXML public Label text;
    @FXML public Label taskDescription;
    @FXML public Label timeWasted;
    @FXML public Label labelProgress;
    protected CustomClock clock;
    private ExtTask task;
    private SimpleBooleanProperty paused;
    private String fullText = "";
    
    public void afterShow(ExtTask newTask,boolean pause){
        super.afterShow();
        paused = new SimpleBooleanProperty(pause);
        this.task = newTask;
        task.paused.bind(paused);
        
        progressBar.progressProperty().bind(task.progressProperty());
        this.labelProgress.textProperty().bind(this.progressBar.progressProperty().multiply(100).asString("%1$.2f").concat("%"));
        
        cancelButton.disableProperty().bind(task.runningProperty().not());
        okButton.disableProperty().bind(cancelButton.disableProperty().not());
        pauseButton.disableProperty().bind(cancelButton.disabledProperty());
        text.textProperty().bind(task.messageProperty());
        task.messageProperty().addListener(onChange ->{
            fullText+= text.getText()+"\n";
            
        });
        
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
    public void showFullText(){
        ViewManager.getInstance().newListFrame("Progress so far", Arrays.asList(fullText.split("\n")));
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
    @Override
    public void exit(){
        super.exit();
    }
    
}
