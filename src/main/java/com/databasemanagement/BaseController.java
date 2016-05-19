/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.databasemanagement;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

/**
 *
 * @author Laimonas Beniušis
 */

public abstract class BaseController implements Initializable{
    protected String title;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {}; 
    
    protected void setUp(String title){
       this.title = title;
    }
    public abstract void exit();
}
