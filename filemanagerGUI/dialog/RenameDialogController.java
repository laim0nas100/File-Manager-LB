/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import static filemanagerGUI.FileManagerLB.reportError;
import filemanagerGUI.ViewManager;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import java.io.IOException;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import utility.FileNameException;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class RenameDialogController extends TextInputDialogController {
    
    
    
    @FXML public Label nameAvailable;
   
    
    private ExtFile itemToRename;
    private ObservableList<String> listToCheck = FXCollections.observableArrayList();
    
    
    public void setUp(String title,ObservableList<ExtFile> currentList,ExtFile itemToRename){
        super.setUp(title);
        this.description.setText("Rename "+itemToRename.propertyName.get());
        this.itemToRename = itemToRename;
        this.textField.setText(itemToRename.propertyName.get());
        nameIsAvailable.set(false);
        
        for(ExtFile file:currentList){
            listToCheck.add(file.propertyName.get().toUpperCase(Locale.ROOT));
        }
    }
    @Override
    public void checkAvailable(){
        stringToCheck = textField.getText();
        if(listToCheck.contains(stringToCheck.toUpperCase(Locale.ROOT)) ||stringToCheck.length()<1){
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
                TaskFactory.getInstance().renameTo(itemToRename.getAbsolutePath(),stringToCheck.trim());
                exit();
            } catch (IOException ex) {
                reportError(ex);
            } catch(FileNameException ex){
                //reportError(ex);
                this.nameAvailable.setText(ex.getMessage());
            }
        }
    }   
}
