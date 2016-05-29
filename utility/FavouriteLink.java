/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tooltip;

/**
 *
 * @author Laimonas Beniušis
 */
public class FavouriteLink {
    private final SimpleStringProperty propertyName;
    private final String location;
    public FavouriteLink(String name,String dir){
        propertyName = new SimpleStringProperty(name);
        if(dir.isEmpty()){
            location = "ROOT";  
        } else {
            location = dir;
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
    public SimpleStringProperty getPropertyName(){
        return this.propertyName;
    }
    
}
