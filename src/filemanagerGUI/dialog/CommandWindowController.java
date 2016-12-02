/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import LibraryLB.Log;
import filemanagerGUI.FileManagerLB;
import filemanagerGUI.customUI.AbstractCommandField;
import filemanagerLogic.TaskFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import javafx.scene.control.TextField;
import org.apache.commons.lang3.StringUtils;
import utility.ErrorReport;
import utility.ExtStringUtils;

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
        public void handleStream(BufferedReader reader,TextArea textArea) throws IOException{
            String line = reader.readLine();
            while(line!=null){
                final String l = line;
                Platform.runLater(()->{
                    textArea.setText(textArea.getText()+l+"\n");
                    textArea.positionCaret(textArea.getLength());
                });
                line = reader.readLine();
            }
        }
        public void apply(String name) throws IOException{
            ArrayList<String> readFromFile = LibraryLB.FileManaging.FileReader.readFromFile(name);
            for(String command:readFromFile){
                Log.writeln(command);
                submit(command);
            }
        }
        public void generate(String command, String name){
            try{
                System.out.println(TaskFactory.getInstance().markedList);
                LinkedList<String> l = new LinkedList<>();
                l.addAll(0, TaskFactory.getInstance().markedList);
                LinkedList<String> commands = new LinkedList<>();
                for(String absPath:l){
                    if(absPath.endsWith(File.separator)){
                        absPath = absPath.substring(0, absPath.length()-1);
                    }
                    String add = command;
                    if(add.contains("<p>")){
                        add = ExtStringUtils.replaceOnce(add, "<p>", absPath);
                    }
                    if(add.contains("<n>")){
                        add = ExtStringUtils.replaceOnce(add,"<n>", absPath.substring(absPath.lastIndexOf(File.separator)+1));
                    }
                    commands.add(add);
                    Log.writeln(command+"||"+add);
                }
                LibraryLB.FileManaging.FileReader.writeToFile(FileManagerLB.DIR+name, commands);
            }catch(Exception ex){
                ErrorReport.report(ex);
            }
        }
        @Override
        public void submit(String command) {
            Task<Void> task = new Task<Void>(){
                @Override
                protected Void call() throws Exception {
                    Process process;
                    LinkedList<String> commands = new LinkedList<>();
                    for(String s:command.split(" ")){
                        commands.add(s);
                    }
                    if(commands.getFirst().equalsIgnoreCase("generate:")){
                        commands.removeFirst();
                        String newCom = command;
                        newCom = ExtStringUtils.replaceOnce(newCom, "generate: ", "");
                        String index = commands.removeFirst();
                        newCom = ExtStringUtils.replaceOnce(newCom, index+" ", "");
                        generate(newCom,index);
                        return null;
                    }else if(commands.getFirst().equalsIgnoreCase("do:")){
                        commands.removeFirst();
                        apply(commands.removeFirst());
                        return null;
                    }
                    ProcessBuilder builder = new ProcessBuilder(commands.toArray(new String[0]));
                    builder.redirectErrorStream(true);
                    process = builder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));  
                    handleStream(reader,textArea);
                    
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
