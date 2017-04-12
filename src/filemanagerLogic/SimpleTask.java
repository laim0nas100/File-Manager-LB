/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import LibraryLB.Threads.ExtTask;
import LibraryLB.FX.FXTask;
import javafx.application.Platform;
import utility.ErrorReport;

/**
 *
 * @author Lemmin
 */
public abstract class SimpleTask extends ExtTask{
    
    @Override
    protected abstract Void call() throws Exception;

    public void report(Exception e) {
        ErrorReport.report(e);
    }
    public SimpleTask(){};
    public static SimpleTask temp(){
        return new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                return null;
            };
        };
    }
    public void runOnPlatform(){
        new Thread( ()->{
            Platform.runLater(this);
        }).start();
        
    }
}
