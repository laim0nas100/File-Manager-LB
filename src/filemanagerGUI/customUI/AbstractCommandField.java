/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import LibraryLB.Containers.LoopingList;
import java.util.HashMap;

/**
 *
 * @author Laimonas Beniušis
 */
public abstract class AbstractCommandField {
    public HashMap<Object,Command> commands = new HashMap<>();
    public TextField field;
    public LoopingList<String> commandHistory = new LoopingList<>();
    public AbstractCommandField(TextField tf){
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
    public void addCommand(String commandInit,Command command){
        this.commands.put(commandInit, command);
    }
    public abstract void submit(String command);
    public boolean runCommand(Object commandInit,Object...params) throws Exception{
        if(this.commands.containsKey(commandInit)){
            this.commands.get(commandInit).run(params);
            return true;
        }
        return false;
    }
    public interface Command{
        public void run(Object...params) throws Exception;
    }
}
