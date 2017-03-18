/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import LibraryLB.Threads.TimeoutTask;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.fileStructure.ExtFolder;
import java.nio.file.Files;
import java.util.concurrent.Future;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import utility.ErrorReport;
import utility.ExtStringUtils;
import utility.FileNameException;
import utility.PathStringCommands;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class RenameDialogController extends TextInputDialogController {
    
    
    
    @FXML public Label nameAvailable;
   
    
    private ExtPath itemToRename;
    private ExtFolder folder;
    private ObservableList<String> listToCheck = FXCollections.observableArrayList();
    private TimeoutTask folderUpdateTask = new TimeoutTask(1000,100,()->{
        update(); 
        Platform.runLater(() ->{
            if(!listToCheck.contains(textField.getText().trim()) && textField.getText().length() > 0){
                nameAvailable.setText("Available");   
                nameIsAvailable.set(true);
            }
        });
            
        
    });

    @Override
    public void exit() {
        super.exit(); 
    }
    public void afterShow(ExtFolder folder,ExtPath itemToRename){
        this.description.setText("Rename "+itemToRename.propertyName.get());
        this.itemToRename = itemToRename;
        this.textField.setText(itemToRename.propertyName.get());
        nameIsAvailable.set(false);
        this.folder = folder;
        this.textField.textProperty().addListener(listener->{
            checkAvailable();
        });

    }
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
    }
    @Override
    public void checkAvailable(){
        if(!Files.exists(itemToRename.toPath())){
            exit();
        }
        nameIsAvailable.set(false);
        nameAvailable.setText("Taken");
        folderUpdateTask.update();
        
    }
    @Override
    public void apply(){
        if(nameIsAvailable.get()){
            try {
                PathStringCommands fallback = new PathStringCommands(TaskFactory.resolveAvailablePath(folder, itemToRename.propertyName.get()).trim());
                TaskFactory.getInstance().renameTo(itemToRename.getAbsolutePath(),ExtStringUtils.trimEnd(textField.getText()),fallback.getName(true));
                exit();
            }catch(FileNameException ex){
                this.nameAvailable.setText(ex.getMessage());
            } catch (Exception ex) {
                ErrorReport.report(ex);
            } 
        }
    }   

    @Override
    public void update() {
        
        listToCheck.clear();
        folder.update();
        for(ExtPath file:folder.getFilesCollection()){
            listToCheck.add(file.propertyName.get());
        }
    }
}
