/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import LibraryLB.Containers.ParametersMap;
import LibraryLB.Log;
import LibraryLB.Parsing.Lexer;
import LibraryLB.Parsing.Literal;
import LibraryLB.Parsing.Token;
import filemanagerGUI.BaseController;
import filemanagerGUI.FileManagerLB;
import filemanagerGUI.MainController;
import filemanagerGUI.ViewManager;
import filemanagerGUI.customUI.AbstractCommandField;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import javafx.scene.control.TextField;
import utility.ErrorReport;
import utility.ExtStringUtils;
import utility.PathStringCommands;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class CommandWindowController extends BaseController {
    @FXML TextField textField;
    @FXML TextArea textArea;
    private Commander command;
    private int executeQueue = 0;
    
    public static int maxExecutablesAtOnce = 10;
    public static int truncateAfter;
    public static String  commandGenerate,
                    commandApply,
                    commandList,
                    commandListRec,
                    commandClear,
                    commandHelp,
                    commandListParams,
                    commandSetCustom;
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
        
        command = new Commander(textField);
        command.addCommand(commandGenerate, (Object... params) -> {
                String newCom = (String) params[0];
                newCom = ExtStringUtils.replaceOnce(newCom, commandGenerate+" ", "");
                command.generate(newCom);
        });
        
        command.addCommand(commandApply, (Object... params)->{
                String newCom = (String) params[0];
                newCom = ExtStringUtils.replaceOnce(newCom, commandApply+" ", "");
                command.apply(newCom);
        });
        command.addCommand(commandListRec, (Object... params)->{
                ArrayDeque<String> deque = new ArrayDeque<>();
                String newCom = (String) params[0];
                newCom = ExtStringUtils.replaceOnce(newCom, commandListRec+" ", "");
                ExtPath file = LocationAPI.getInstance().getFileAndPopulate(newCom);
                
                for(ExtPath f:file.getListRecursive()){    
                    deque.add(f.getAbsoluteDirectory());
                }
                String desc = "Listing recursive:"+deque.removeFirst();
                ViewManager.getInstance().newListFrame(desc, deque);
        });
        command.addCommand(commandList, (Object... params)->{
                ArrayDeque<String> deque = new ArrayDeque<>();
                String newCom = (String) params[0];
                newCom = ExtStringUtils.replaceOnce(newCom, commandList+" ", "");
                ExtPath file = LocationAPI.getInstance().getFileAndPopulate(newCom);
                if(file.getIdentity().equals(Identity.FOLDER)){
                    String desc = "Listing:"+file.getAbsoluteDirectory();

                    ExtFolder folder = (ExtFolder) file;
                    for(ExtPath f:folder.getFilesCollection()){
                        deque.add(f.getAbsoluteDirectory());
                    }
                    ViewManager.getInstance().newListFrame(desc, deque);
                }
        });
        command.addCommand(commandSetCustom, (Object... params)->{
                String newCom = (String) params[0];
                newCom = ExtStringUtils.replaceOnce(newCom, commandSetCustom+" ", "");
                FileManagerLB.customPath.setPath(newCom);
        });
        command.addCommand(commandClear, (Object... params)->{
                textArea.clear();
        });
        command.addCommand(commandHelp, (Object... params)->{
                command.addToTextArea(textArea,"Read Parameters.txt file for info\n");
        });
        command.addCommand(commandListParams, (Object... params)->{
            ArrayList<String> list = new ArrayList<>(FileManagerLB.parameters.map.keySet());
            Collections.sort(list);
            list.forEach(key->{
                ParametersMap.ParameterObject parameter = FileManagerLB.parameters.getParameter(key);
                command.addToTextArea(textArea, parameter.toString()+"\n");
            });
        });
        
        
    }
    
    public class Commander extends AbstractCommandField{
        private boolean setTextAfterwards = false;
        public Commander(TextField tf) {
            super(tf);
        }
        public void handleStream(Process process,TextArea textArea,boolean setTextAfterwards,String command) throws IOException{
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            LinkedList<String> lines = new LinkedList<>();
            lines.add("$:"+command);
            if(!setTextAfterwards){
                addToTextArea(textArea,"$:"+command);
            }
            while(line!=null){
                if(setTextAfterwards){
                    lines.add(line+"\n");
                }else{ 
                    addToTextArea(textArea,line+"\n");
                }
                line = reader.readLine();
            }
            final int errorCode = process.exitValue();
            if(setTextAfterwards){
                    lines.add("Error Code:"+errorCode+"\n\n");
                }else{
                    addToTextArea(textArea,"Error Code:"+errorCode+"\n\n");
            }
            if(setTextAfterwards){
                Platform.runLater(()->{
                    String main = textArea.getText();
                    for(String ln:lines){
                        main+=ln.trim()+"\n";
                    }
                    addToTextArea(textArea,main);
                });
            }
            executeQueue--;
            
        }
        public void addToTextArea(TextArea textA,String text){
            Platform.runLater(()->{
                String newString = textA.getText()+text;
                textA.setText(newString.substring(0,Math.min(truncateAfter,newString.length())));
                textA.positionCaret(textA.getLength());
            });
            
        }
//        public void truncateTextArea(TextArea textA,int length){
//            Platform.runLater(()->{
//                if(textA.getLength()>length){
//                   textA.setText(textA.getText().substring(textA.getLength()-length));
//                   textA.positionCaret(textA.getLength());
//                }
//            });
//            
//        }
        public void apply(String name) throws IOException, InterruptedException{
            LinkedList<String> readFromFile = new LinkedList(LibraryLB.FileManaging.FileReader.readFromFile(name));
            this.setTextAfterwards = true;
            for(String command:readFromFile){
                do{
                        Thread.sleep(500);
                    }while(executeQueue>maxExecutablesAtOnce);
                Log.writeln(command);
                submit(command);
            }
        }
        public void generate(String command){
            try{
            
                System.out.println(MainController.markedList);
                LinkedList<String> l = new LinkedList<>();
                l.addAll(MainController.markedList);
                LinkedList<String> allCommands = new LinkedList<>();
                Lexer lexer = new Lexer(command);
                lexer.skipWhitespace = false;
                lexer.addToken(PathStringCommands.returnDefinedKeys());
                for(String absPath:l){
                    PathStringCommands pathInfo = new PathStringCommands(absPath);
                    lexer.reset();
                    String commandToAdd = "";
                    while(true){
                        Token token = lexer.getNextToken();
                        if(token==null){
                            break;
                        }
                        if(token.id.equals(PathStringCommands.fileName)){
                            commandToAdd+=pathInfo.getName(true);
                        }else if(token.id.equals(PathStringCommands.nameNoExt)){
                            commandToAdd+=pathInfo.getName(false);
                        }else if(token.id.equals(PathStringCommands.filePath)){
                            commandToAdd+=pathInfo.getPath();
                        }else if(token.id.equals(PathStringCommands.extension)){
                            commandToAdd+=pathInfo.getExtension();
                        }else if(token.id.equals(PathStringCommands.parent1)){
                            commandToAdd+=pathInfo.getParent(1);
                        }else if(token.id.equals(PathStringCommands.parent2)){
                            commandToAdd+=pathInfo.getParent(2);
                        }else if(token.id.equals(PathStringCommands.custom)){
                            commandToAdd+=FileManagerLB.customPath.getPath();
                        }else if(token.id.equals(PathStringCommands.relativeCustom)){
                            commandToAdd+=FileManagerLB.customPath.relativeTo(pathInfo.getPath());
                        }else{
                            Literal lit = (Literal)token;
                            commandToAdd+=lit.value;
                        }
                        
                    }
                    allCommands.add(commandToAdd);
                    Log.writeln(command+" => "+commandToAdd);
                }
                ViewManager.getInstance().newListFrame("Script generation", allCommands);
//                LibraryLB.FileManaging.FileReader.writeToFile(FileManagerLB.HOME_DIR+name, commands);
            }catch(Exception ex){
                ErrorReport.report(ex);
            }
        }
        public boolean customCommand(String firstWord,String fullCommand,LinkedList<String> commands) throws IOException, InterruptedException{
//            ArrayList<String> deque = new ArrayList<>();
//            if(firstWord.equalsIgnoreCase(commandGenerate)){
//                commands.removeFirst();
//                String newCom = fullCommand;
//                newCom = ExtStringUtils.replaceOnce(newCom, commandGenerate+" ", "");
//                generate(newCom);
//                return true;
//            }else 
//            if(firstWord.equalsIgnoreCase(commandApply)){
//                commands.removeFirst();
//                apply(commands.removeFirst());
//                return true;
//            }else
//            if(firstWord.equalsIgnoreCase(commandListRec)){
//                String newCom = ExtStringUtils.replace(fullCommand, commands.getFirst()+" ","");
//                ExtPath file = LocationAPI.getInstance().getFileAndPopulate(newCom);
//                for(ExtPath f:file.getListRecursive()){    
//                    deque.add(f.getAbsoluteDirectory());
//                }
//                String desc = "Listing recursive:"+deque.remove(0);
//                ViewManager.getInstance().newListFrame(desc, deque);
//                return true;
//            }else 
//            if(firstWord.equalsIgnoreCase(commandList)){
//                String newCom = ExtStringUtils.replace(fullCommand, commands.getFirst()+" ","");
//                ExtPath file = LocationAPI.getInstance().getFileAndPopulate(newCom);
//                if(file.getIdentity().equals(Identity.FOLDER)){
//                    String desc = "Listing:"+file.getAbsoluteDirectory();
//
//                    ExtFolder folder = (ExtFolder) file;
//                    for(ExtPath f:folder.getFilesCollection()){
//                        deque.add(f.getAbsoluteDirectory());
//                    }
//                    System.out.println(deque);
//                    ViewManager.getInstance().newListFrame(desc, deque);
//                }
//                
//                return true;
//            }else
//            if(firstWord.equalsIgnoreCase(commandSetCustom)){
//                if(MainController.markedList.isEmpty()){
//                    return true;
//                }
//                String get = MainController.markedList.get(0);
//                FileManagerLB.customPath.setPath(get);
//                return true;
//            
//            }else 
//            if(firstWord.equalsIgnoreCase(commandHelp)){
//                addToTextArea(textArea,"Read Parameters.txt file for info\n");
//                return true;
//            }else if(firstWord.equalsIgnoreCase(commandClear)){
//                textArea.clear();
//                return true;
//            }
            return false;
        }
        @Override
        public void submit(String command) {
            Task<Void> task = new Task<Void>(){
                @Override
                protected Void call() throws Exception {
                    
                    LinkedList<String> commands = new LinkedList<>();
                    for(String s:command.split(" ")){
                        if(s.length()>0) commands.add(s);
                    }
                    if(runCommand(commands.getFirst(),command)){
                        return null;
                    }
                    executeQueue++;
                    Log.write("Queue:"+executeQueue+" "+command+"\n");
                    ProcessBuilder builder = new ProcessBuilder(commands.toArray(new String[0]));
                    builder.redirectErrorStream(true);
                    Process process = builder.start();
                    handleStream(process,textArea,setTextAfterwards,command);
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
        command.setTextAfterwards = false;
        command.submit(this.textField.getText());
    }

 
    
}
