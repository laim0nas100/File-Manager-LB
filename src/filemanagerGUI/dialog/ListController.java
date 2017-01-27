/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author lemmin
 */
public class ListController extends BaseDialog {

    @FXML public Label descriptionLabel;
    @FXML public ListView listView;
    @FXML public TextField pathToSave;
    @FXML public TextField size;
    @Override
    public void update() {
    }
    
    public void beforeShow(String title,String desc){
        super.beforeShow(title);
        this.descriptionLabel.setText(desc);
        
    }
    public void afterShow(Collection<String> list){
        Platform.runLater(()->{
            listView.getItems().setAll(list);
            size.setText(list.size()+"");
        });
    }
    public void save() throws FileNotFoundException, UnsupportedEncodingException{
        String text = this.pathToSave.getText();
        LibraryLB.FileManaging.FileReader.writeToFile(text, this.listView.getItems());
    }
            
    
}
