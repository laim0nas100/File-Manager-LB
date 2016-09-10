/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.customUI.CosmeticsFX.ExtTableView;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolderDialogController extends BaseDialog {
    @FXML public TableView table;
    @FXML public Text folderPath;
    
    public VirtualFolder folder;
    private ExtTableView extTableView;
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
        extTableView = new ExtTableView(table);
        extTableView.table.setOnDragDetected(eh->{
            
        });
    }
    @Override
    public void update(){
        folder.update();
        extTableView.updateContentsAndSort(folder.getList());
        
    }
    
}
