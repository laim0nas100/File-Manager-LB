/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerLogic.LocationAPI;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.apache.commons.lang3.StringUtils;
import utility.ErrorReport;
import utility.ExtStringUtils;
import utility.FileNameException;
import utility.Log;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class RenameDialogController extends TextInputDialogController {
    
    
    
    @FXML public Label nameAvailable;
   
    
    private ExtFile itemToRename;
    private ExtFolder folder;
    private ObservableList<String> listToCheck = FXCollections.observableArrayList();
    
    
    public void setUp(String title,ExtFolder folder,ExtFile itemToRename){
        super.setUp(title);
        this.description.setText("Rename "+itemToRename.propertyName.get());
        this.itemToRename = itemToRename;
        this.textField.setText(itemToRename.propertyName.get());
        nameIsAvailable.set(false);
        this.folder = folder;
        
        
    }
    @Override
    public void checkAvailable(){
        listToCheck.clear();
        folder.update();
        for(ExtFile file:folder.getFilesCollection()){
            listToCheck.add(file.propertyName.get());
        }
        stringToCheck = textField.getText();
        if(listToCheck.contains(stringToCheck) ||stringToCheck.length()<1){
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
                String fallback = TaskFactory.resolveAvailableName(folder, itemToRename.propertyName.get()).trim();
                fallback = ExtStringUtils.replaceOnce(fallback, folder.getAbsoluteDirectory(), "");
                TaskFactory.getInstance().renameTo(itemToRename.getAbsolutePath(),stringToCheck.trim(),fallback);
                exit();
            }catch(FileNameException ex){
                this.nameAvailable.setText(ex.getMessage());
            } catch (Exception ex) {
                ErrorReport.report(ex);
            } 
        }
    }   
}
