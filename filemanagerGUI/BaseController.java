/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.stage.Stage;

/**
 *
 * @author Laimonas Beniušis
 */

public class BaseController implements Initializable{
    protected String title;
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }
    public void setUp(String title){
       this.title = title;
    }
    public void exit(){
        
    }
}
