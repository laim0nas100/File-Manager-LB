/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import java.io.File;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Laimonas Beniušis
 */
public abstract class FileAbs extends File {
    
    protected boolean operationSuccessfull;
    public StringProperty name;
    
    
    public FileAbs(String string) {
        super(string);
    }
    public boolean isOperationSuccessfull() {
        return operationSuccessfull;
    }

    public void setOperationSuccessfull(boolean operationSuccessfull) {
        this.operationSuccessfull = operationSuccessfull;
    }
    public StringProperty getPropertyName(){
        return name;
    }
}
