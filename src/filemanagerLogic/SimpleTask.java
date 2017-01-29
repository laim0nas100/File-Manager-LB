/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import LibraryLB.ExtTask;
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
    
}
