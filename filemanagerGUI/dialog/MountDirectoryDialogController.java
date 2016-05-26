/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.FileManagerLB;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class MountDirectoryDialogController extends TextInputDialogController {
    
    
    @Override
    public void setUp(String title){
        super.setUp(title);
        this.description.setText("Mount directory");
    }
    @Override
    public void apply(){
        nameIsAvailable.set(FileManagerLB.mountDevice(stringToCheck));
        if(nameIsAvailable.get()){
            this.exit();
        }
    }

    @Override
    public void checkAvailable() {
        stringToCheck = this.textField.getText();
        nameIsAvailable.set(false);
        if(new File(stringToCheck).exists()){
            Path path = Paths.get(stringToCheck);
            stringToCheck = path.toString();
            if(!FileManagerLB.getRootSet().contains(stringToCheck)){
                if((path.getNameCount() ==0)&&(Files.isDirectory(path))){
                    nameIsAvailable.set(true);
                }
            }
        }
    }
    
}
