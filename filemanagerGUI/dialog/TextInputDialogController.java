/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.BaseController;
import filemanagerGUI.ViewManager;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class TextInputDialogController extends BaseController {
    
    
    @FXML public Label description;
    @FXML public Label nameAvailable;
    @FXML public TextField textField;
    @FXML public Button buttonEnter;
    @FXML public Button buttonCancel;
    
    private boolean nameIsAvailable;
    private ExtFile itemToRename;
    private ObservableList<String> listToCheck = FXCollections.observableArrayList();
    private String possibleName;
    
    public void setUp(String title,ObservableList<ExtFile> currentList,ExtFile itemToRename){
        this.description.setText("Rename "+itemToRename.propertyName.get());
        this.title = title;
        this.itemToRename = itemToRename;
        this.textField.clear();
        nameIsAvailable = false;
        for(ExtFile file:currentList){
            listToCheck.add(file.propertyName.get());
        }
    }
    @Override
    public void exit(){
        ViewManager.getInstance().closeTextInputDialog(title);
    }
    public void checkNameAvailable(){
        possibleName = textField.getText();
        if(listToCheck.contains(possibleName)){
            nameIsAvailable = false;
            nameAvailable.setText("Taken");
        }else{
            nameAvailable.setText("Available");
            nameIsAvailable = true;
        }
    }
    public void apply(){
        if(nameIsAvailable){
            try {
                TaskFactory.getInstance().renameTo(itemToRename.getAbsolutePath(),possibleName );
                ViewManager.getInstance().updateAllWindows();
                exit();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    
    
    
}
