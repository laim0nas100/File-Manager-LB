/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.ViewManager;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class RenameDialog extends TextInputDialogController {
    
    
    
    @FXML public Label nameAvailable;
   
    
    private ExtFile itemToRename;
    private ObservableList<String> listToCheck = FXCollections.observableArrayList();
    
    
    public void setUp(String title,ObservableList<ExtFile> currentList,ExtFile itemToRename){
        super.setUp(title);
        this.description.setText("Rename "+itemToRename.propertyName.get());
        
        this.itemToRename = itemToRename;
        this.textField.clear();
        nameIsAvailable.set(false);
        
        for(ExtFile file:currentList){
            listToCheck.add(file.propertyName.get());
        }
    }
    @Override
    public void checkAvailable(){
        stringToCheck = textField.getText();
        
        if(listToCheck.contains(stringToCheck) ||stringToCheck.length()<2){
            nameIsAvailable.set(false);
            nameAvailable.setText("Taken");
        }else{
            nameAvailable.setText("Available");
            nameIsAvailable.set(true);
        }
    }
    @Override
    public void apply(){
        if(nameIsAvailable.get()){
            try {
                TaskFactory.getInstance().renameTo(itemToRename.getAbsolutePath(),stringToCheck);
                ViewManager.getInstance().updateAllWindows();
                exit();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }   
}
