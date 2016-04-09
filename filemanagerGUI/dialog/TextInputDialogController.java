/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.BaseController;
import filemanagerGUI.ViewManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */





public abstract class TextInputDialogController extends BaseDialog implements TextInputDialogEssentials {
    @FXML public TextField textField;
    @FXML public Button buttonEnter;
    @FXML public Button buttonCancel;
    @FXML public Label description;
    
    protected SimpleBooleanProperty nameIsAvailable = new SimpleBooleanProperty();
    protected String stringToCheck;
    
    @Override
    public void setUp(String title){
        super.setUp(title);
        buttonEnter.disableProperty().bind(nameIsAvailable.not());
    }
}
