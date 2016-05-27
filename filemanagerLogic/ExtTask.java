/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import static filemanagerGUI.FileManagerLB.reportError;
import javafx.concurrent.Task;

/**
 * Custom Task
 * @author Laimonas Beniušis
 */
public class ExtTask extends Task<Void> {
    
    @Override
    protected Void call() throws Exception {
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    private String taskDescription;
    private long refreshDuration = 500;
    private boolean paused;
    
    public String getTaskDescription() {
        return taskDescription;
    }
    
    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    public void setRefreshDuration(long numb){
        refreshDuration = numb;
    }
    public long getRefreshDuration(){
        return this.refreshDuration;
    }
    public void report(Exception e){
        reportError(e);
    }


    
}
