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
    public String path1;
    public String path2;
    public ExtEntry(Entry entry){
        super(entry);
        action = new SimpleStringProperty("(0) No Action");
        actionCompleted = new SimpleBooleanProperty(false);
        actionType = new SimpleIntegerProperty(0);
    }
    public static String getActionDescription(int act){
       //Action Types
            //0 - no Action
            //1 - copy to source
            //2 - copy to compared
            //3 - delete from source
            //4 - delete from compared
        switch(act){
            case(1):{
                return ("(1) Copy from compared to source");
            }
            case(2):{
                return ("(2) Copy from source to compared");
            }
            case(3):{
                return ("(3) Delete from source");
            }
            case(4):{
                return ("(4) Delete from compared");
            }
            default:{
                return ("(0) No Action");
            }
        } 
    }
    public void setAction(int act){
        if(act<0 || act>4){
            act = 0;
        }
        this.actionType.set(act);
        this.action.set(getActionDescription(act));
        
    }
    @Override
    public String toString(){
        return super.toString() + " "+getActionDescription(actionType.get());
    }
}
