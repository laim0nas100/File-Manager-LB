/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import java.io.File;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Laimonas Beniušis
 */
public abstract class FileAbs extends File {
    public BooleanProperty isAbsoluteRoot;
    public StringProperty propertyName;
    public StringProperty propertyType;
    public LongProperty propertySize;
    public StringProperty propertyDate;
    public StringProperty propertySizeAuto;
    
    public FileAbs(String string) {
        super(string);
    }

    public LocationInRoot getMapping(){
        return LocationAPI.getInstance().getLocationMapping(this.getAbsolutePath());
    }
}
