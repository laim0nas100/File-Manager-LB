/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import javafx.stage.Stage;

/**
 *
 * @author Laimonas Beniušis
 */
public class Frame {
    private Stage stage;
    private BaseController controller;

    public Frame(Stage stage, BaseController controller) {
        this.stage = stage;
        this.controller = controller;
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
    public void setStage(Stage stage){
        this.stage = stage;
    }
    public String getTitle(){
        return this.stage.getTitle();
    } 
}
