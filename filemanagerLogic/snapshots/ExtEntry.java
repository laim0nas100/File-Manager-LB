/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.snapshots;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Laimonas Beniušis
 */
public class ExtEntry extends Entry{
    public SimpleStringProperty action;
    public SimpleBooleanProperty actionCompleted;
    public SimpleIntegerProperty actionType;
    public ExtEntry(Entry entry){
        super(entry);
        action = new SimpleStringProperty("(0) No Action");
        actionCompleted = new SimpleBooleanProperty(false);
        actionType = new SimpleIntegerProperty(0);
    }
    
    public void setAction(int act){
        this.actionType.set(act);
        //Action Types
            //0 - no Action
            //1 - copy to source
            //2 - copy from source
            //3 - delete from source
            //4 - delete from compared
        switch(act){
            case(1):{
                this.action.set("(1) Copy from compared to source");
                break;
            }
            case(2):{
                this.action.set("(2) Copy from source to compared");
                break;
            }
            case(3):{
                this.action.set("(3) Delete from source");
                break;
            }
            case(4):{
                this.action.set("(4) Delete from compared");
                break;
            }
            default:{
                this.action.set("(0) No Action");
                this.actionType.set(0);
                break;
            }
        }
    }
    
}
