/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.customUI.AbstractCommandField;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import javafx.scene.control.TextField;
import javafx.scene.text.TextFlow;

/**
 * FXML Controller class
 *
 * @author Lemmin
 */
public class CommandWindowController extends BaseDialog {
    @FXML TextField textField;
    @FXML TextArea textArea;
    private Command command;
    
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
        command = new Command(textField);
        
    }
    public class Command extends AbstractCommandField{

        public Command(TextField tf) {
            super(tf);
        }

        @Override
        public void submit(String command) {
            Task<Void> task = new Task<Void>(){
                @Override
                protected Void call() throws Exception {
                    Process process;
//                    process = Runtime.getRuntime().exec(command);
                    LinkedList<String> commands = new LinkedList<>();
                    for(String s:command.split(" ")){
                        commands.add(s);
                    }
                    ProcessBuilder builder = new ProcessBuilder(commands.toArray(new String[0]));
                    builder.redirectErrorStream(true);
                    process = builder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));  
                    String line = reader.readLine();

                    while(line!=null){
                        final String l = line;
                        Platform.runLater(()->{
                            if(this.isCancelled()){
                                process.destroy();
                            }
                            textArea.setText(textArea.getText()+l+"\n");
                            textArea.positionCaret(textArea.getLength());
                        });
                        line = reader.readLine();
                    }
                    
                    final int errorCode = process.exitValue();
                    Platform.runLater(()->{                          
                            textArea.setText(textArea.getText()+"Error Code:"+errorCode+"\n");
                            textArea.positionCaret(textArea.getLength());
                        });
                    return null;
                }                
            };
            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        }
        
    }
    
    @Override
    public void update() {
    }
    public void submit(){
        command.submit(this.textField.getText());
    }

 
    
}
