/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tooltip;

/**
 *
 * @author Laimonas Beniušis
 */
public class FavouriteLink {
    public SimpleStringProperty propertyName;
    private String location;
    public FavouriteLink(String name,String dir){
        propertyName = new SimpleStringProperty(name);
        if(dir.isEmpty()){
          location = "";  
        } else if(new File(dir).exists()){
            location = Paths.get(dir).toString();
        }
        
    }

    public String getDirectory(){
        return location;
    }
    public Tooltip getToolTip(){
        Tooltip tltp = new Tooltip();
        tltp.setText(this.getDirectory());
        return tltp;
    }
    
}
