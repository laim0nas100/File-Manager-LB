/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui;

import java.util.HashMap;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Laimonas Beniušis
 */
public class Frame {
    public static class Pos{
        public SimpleDoubleProperty x,y;
        public Pos(double X, double Y){
            this.x = new SimpleDoubleProperty(X);
            this.y = new SimpleDoubleProperty(Y);
        }
        
        @Override
        public String toString(){
            return "["+x.get()+":"+y.get()+"]";
        }
    }
    public ChangeListener listenerX,listenerY;
    public static HashMap<String,Pos> positionMemoryMap = new HashMap<>();
    private Stage stage;
    private BaseController controller;
    private String frameType;
    
    public Frame(Stage stage, BaseController controller, String frameType) {
        this.stage = stage;
        this.controller = controller;
        this.frameType = frameType;
    }
    
    public BaseController getController(){
        return this.controller;
    }
    public void setController(BaseController controller){
        this.controller = controller;
    }
    public Stage getStage(){
        return this.stage;
    }
    public Scene getScene(){
        return this.stage.getScene();
    }
    public void setStage(Stage stage){
        this.stage = stage;
    }
    public String getTitle(){
        return this.stage.getTitle();
    }
    public String getID(){
        return this.controller.getID();
    }
    public String getFrameTitle(){
        return this.frameType;
    }
}
