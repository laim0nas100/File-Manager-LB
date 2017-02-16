/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

/**
 *
 * @author Laimonas Beniušis
 */

public abstract class BaseController implements Initializable{
    protected String windowID;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {}; 
    
    protected void beforeShow(String title){
    }
    public String getID(){
        return this.windowID;
    }
    protected void afterShow(){
        
    }
    
    public void exit(){
        ViewManager.getInstance().closeFrame(this.windowID);
        ViewManager.getInstance().updateAllFrames();
    }
    public abstract void update();
}
