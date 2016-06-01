/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import LibraryLB.LoopingList;

/**
 *
 * @author Laimonas Beniušis
 */
public abstract class AbstractCommandField {
    public TextField field;
    public LoopingList<String> commandHistory;
    public AbstractCommandField(TextField tf){
        commandHistory = new LoopingList<>();
        field = tf;
        field.setOnKeyReleased(eh ->{
            KeyCode code = eh.getCode();
            if(code.equals(KeyCode.UP)){
                field.setText(commandHistory.prev());
            }
            if(code.equals(KeyCode.DOWN)){
                field.setText(commandHistory.next());
            }
        });
        field.setOnAction(eh ->{
            String command = field.getText();
            commandHistory.add(command);
            field.clear();
            submit(command);
        });
    }
    public abstract void submit(String command);
}
