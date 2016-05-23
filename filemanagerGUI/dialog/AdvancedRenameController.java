/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;
import static filemanagerGUI.FileManagerLB.reportError;
import filemanagerGUI.dialog.BaseDialog;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import utility.AdvancedRename;
import utility.Log;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class AdvancedRenameController extends BaseDialog {

    
@FXML public Tab tbSpecificRename;
@FXML public Tab tbNumerize;
    
@FXML public TextField tfStrReg;
@FXML public TextField tfReplaceWith;
@FXML public TextField tfFilter;
@FXML public TextField tfStartingNumber;

@FXML public ListView lwCurrentFolder;
@FXML public ListView lwFutureFolder;

@FXML public CheckBox useRegex;

@FXML public Button buttonApply;

private ArrayList<String> nameCollection1;
private ArrayList<String> nameCollection2;
private long startingNumber;
private LocationInRoot location;
public void setUp(String title,LocationInRoot folderLocation){
    
    super.setUp(title);
    this.location = folderLocation;
    this.setNumber();
    nameCollection1 = new ArrayList<>();
    nameCollection2 = new ArrayList<>();
    
    lwCurrentFolder.getItems().setAll(nameCollection1);
    lwFutureFolder.getItems().setAll(nameCollection2);
    updateLists();
}
public void updateLists(){
    ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location);
    folder.update();
    nameCollection1.clear();
    nameCollection2.clear();
    for(ExtFile f:folder.getFilesCollection()){
        String name = f.propertyName.get();
        nameCollection1.add(name);
        nameCollection2.add(name);
    }
   
    nameCollection1.sort(String.CASE_INSENSITIVE_ORDER);
    nameCollection2.sort(String.CASE_INSENSITIVE_ORDER);
    lwCurrentFolder.getItems().clear();
    lwCurrentFolder.getItems().addAll(nameCollection1);
    lwFutureFolder.getItems().clear();
    lwFutureFolder.getItems().addAll(nameCollection2);
    
    buttonApply.setDisable(true);
}
public void update(){
    updateLists();
    if(this.tbNumerize.isSelected()){
        String filter = this.tfFilter.getText();
        setNumber();
        nameCollection2.clear();
        long number = startingNumber;
        for(String s:nameCollection1){
            try { 
                nameCollection2.add(AdvancedRename.parseFilter(s, filter, number++));
            } catch (AdvancedRename.FilterException ex) {
                reportError(ex);
            }
        }
        lwFutureFolder.getItems().clear();
        lwFutureFolder.getItems().addAll(nameCollection2);
    }else{
        String strRegex = this.tfStrReg.getText();
        String replacement =""+ this.tfReplaceWith.getText();
        nameCollection2.clear();
        if(useRegex.isSelected()){
            for(String s:nameCollection1){
                nameCollection2.add(AdvancedRename.parseRegex(s, strRegex, replacement)); 
            }
        }else{
           for(String s:nameCollection1){
                nameCollection2.add(AdvancedRename.parseSimple(s, strRegex, replacement)); 
           } 
        }
        lwFutureFolder.getItems().clear();
        lwFutureFolder.getItems().addAll(nameCollection2);
    }
    buttonApply.setDisable(false);
}
public void setNumber(){
    try{
        startingNumber = Integer.parseInt(this.tfStartingNumber.getText());
    }catch(Exception ex){
        startingNumber = 0;
        this.tfStartingNumber.setText(startingNumber+"");
        //reportError(ex);
    }
}
public void apply(){
    ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location);
    for(int i=0;i<nameCollection1.size();i++){
        String absolutePath = folder.files.get(nameCollection1.get(i)).getAbsolutePath();
        try {
            TaskFactory.getInstance().renameTo(absolutePath, nameCollection2.get(i));
        } catch (Exception ex) {
            reportError(ex);
        }
    }
}
    
}
