/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.databasemanagement;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import org.dalesbred.result.ResultTable;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class TableWindowController extends BaseController {
    @FXML public TableView table;
    public void setUp(String title,ResultTable table){
        this.setUp(title);
        ViewManager.getInstance().wrapResultTable(table, this.table);
    }
    @Override
    public void exit() {
        ViewManager.getInstance().closeWindow(title);
    }

    
}
