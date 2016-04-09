/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import java.io.File;
import java.nio.file.Path;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Laimonas Beniušis
 */
public abstract class FileAbs extends File {
    
    protected boolean operationSuccessfull;
    public StringProperty propertyName;
    public StringProperty propertyType;
    public LongProperty propertySize;
    
    public FileAbs(String string) {
        super(string);
    }
    public boolean isOperationSuccessfull() {
        return operationSuccessfull;
    }

    public void setOperationSuccessfull(boolean operationSuccessfull) {
        this.operationSuccessfull = operationSuccessfull;
    }
    public LocationInRoot getMapping(){
        return LocationAPI.getInstance().getLocationMapping(this.getAbsolutePath());
    }
}
