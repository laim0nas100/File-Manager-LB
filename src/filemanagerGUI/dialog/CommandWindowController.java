/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import LibraryLB.Log;
import LibraryLB.Parsing.Lexer;
import LibraryLB.Parsing.Literal;
import LibraryLB.Parsing.Token;
import filemanagerGUI.FileManagerLB;
import filemanagerGUI.ViewManager;
import filemanagerGUI.customUI.AbstractCommandField;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
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
 * @author Lemmin
 */
public class CommandWindowController extends BaseDialog {
    @FXML TextField textField;
    @FXML TextArea textArea;
    private Command command;
    private int executeQueue = 0;
    private int maxExecutablesAtOnce = 10;
    
    private int truncateAfter;
    private String  commandGenerate,
                    commandApply,
                    commandCreateVirtual,
                    commandListVirtualFolders,
                    commandListVirtual,
                    commandList,
                    commandListRec,
                    commandClear,
                    commandHelp,
                    commandAddToVirtual,
                    commandSetCustom;
    private HashSet<String> commandSet;
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
        this.commandSet = new HashSet<>();
       
        command = new Command(textField);
        truncateAfter = (Integer) FileManagerLB.parameters.defaultGet("code.truncateAfter", 100000);
        maxExecutablesAtOnce = (Integer) FileManagerLB.parameters.defaultGet("code.maxExecutables", 2);
        commandGenerate = (String) FileManagerLB.parameters.defaultGet("code.commandGenerate", "generate");
        commandApply = (String) FileManagerLB.parameters.defaultGet("code.commandApply", "apply");
        commandCreateVirtual = (String) FileManagerLB.parameters.defaultGet("code.createVirtualFolder", "virtual");
        commandListVirtualFolders = (String) FileManagerLB.parameters.defaultGet("code.listVirtualFolders", "listVirtualFolders");
        commandListVirtual = (String) FileManagerLB.parameters.defaultGet("code.listVirtual", "listVirtual");
        commandClear = (String) FileManagerLB.parameters.defaultGet("code.clear", "clear");
        commandAddToVirtual = (String) FileManagerLB.parameters.defaultGet("code.addToVirtual", "add");
        commandList = (String) FileManagerLB.parameters.defaultGet("code.list", "list");
        commandListRec = (String) FileManagerLB.parameters.defaultGet("code.listRec", "listRec");
        commandSetCustom = (String) FileManagerLB.parameters.defaultGet("code.setCustom", "setCustom");
        commandHelp = (String) FileManagerLB.parameters.defaultGet("code.help", "help");

        String[] coms = new String[]{
                    commandGenerate,
                    commandApply,
                    commandCreateVirtual,
                    commandListVirtualFolders,
                    commandListVirtual,
                    commandList,
                    commandListRec,
                    commandClear,
                    commandHelp,
                    commandAddToVirtual,
                    commandSetCustom
        };
        commandSet.addAll(Arrays.asList(coms));
    }
    
    public class Command extends AbstractCommandField{
        private boolean setTextAfterwards = false;
        public Command(TextField tf) {
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
            truncateTextArea(textArea,truncateAfter);
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
                    truncateTextArea(textArea,truncateAfter);
                });
            }
            executeQueue--;
            
        }
        public void addToTextArea(TextArea textA,String text){
            Platform.runLater(()->{
            textA.setText(textA.getText()+text);
            textA.positionCaret(textA.getLength());   
            });
            
        }
        public void truncateTextArea(TextArea textA,int length){
            Platform.runLater(()->{
            if(textA.getLength()>length){
               textA.setText(textA.getText().substring(textA.getLength()-length));
               textA.positionCaret(textA.getLength());
            }
            });
            
        }
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
        public void generate(String command, String name){
            try{
            
                System.out.println(TaskFactory.getInstance().markedList);
                LinkedList<String> l = new LinkedList<>();
                l.addAll(TaskFactory.getInstance().markedList);
                LinkedList<String> commands = new LinkedList<>();
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
                    commands.add(commandToAdd);
                    Log.writeln(command+" => "+commandToAdd);
                }
                LibraryLB.FileManaging.FileReader.writeToFile(FileManagerLB.HOME_DIR+name, commands);
            }catch(Exception ex){
                ErrorReport.report(ex);
            }
        }
        public String reconstruct(Collection<String> list){
            String original = "";
            for(String s:list){
                original+=" "+s;
            }
            if(original.length()>0){
                original = original.substring(1);
            }
            return original;
        }
        public boolean customCommand(String firstWord,String fullCommand,LinkedList<String> commands) throws IOException, InterruptedException{
            ArrayList<String> deque = new ArrayList<>();
            if(firstWord.equalsIgnoreCase(commandGenerate)){
                commands.removeFirst();
                String newCom = fullCommand;
                newCom = ExtStringUtils.replaceOnce(newCom, commandGenerate+" ", "");
                String index = commands.removeFirst();
                newCom = ExtStringUtils.replaceOnce(newCom, index+" ", "");
                generate(newCom,index);
                return true;
            }else if(firstWord.equalsIgnoreCase(commandApply)){
                commands.removeFirst();
                apply(commands.removeFirst());
                return true;
            }else if(firstWord.equalsIgnoreCase(commandCreateVirtual)){
                VirtualFolder.createVirtualFolder();
                return true;
            }else if(firstWord.equalsIgnoreCase(commandListVirtualFolders)){
                
                for(ExtFile f:FileManagerLB.VirtualFolders.files.values()){
                    deque.add(f.getAbsoluteDirectory());
                }
                ViewManager.getInstance().newListFrame("Virtual Folders", deque);
                return true;
             }else if(firstWord.equalsIgnoreCase(commandListVirtual)){
                commands.removeFirst();
                VirtualFolder VF = (VirtualFolder) FileManagerLB.VirtualFolders.files.get(commands.getFirst());
                if (VF == null){
                    addToTextArea(textArea,"No such virtual folder:"+commands.getFirst());
                }else{
                    String desc = "Listing "+VF.getAbsoluteDirectory();
                    for(ExtFile f:VF.getFilesCollection()){
                        deque.add(f.getName());
                    }
                    ViewManager.getInstance().newListFrame(desc, deque);
                }
                return true;

            }else if(firstWord.equalsIgnoreCase(commandAddToVirtual)){
                commands.removeFirst();
                VirtualFolder VF = (VirtualFolder) FileManagerLB.VirtualFolders.files.get(commands.getFirst());
                if (VF == null){
                    addToTextArea(textArea,"No such virtual folder:"+commands.getFirst());

                }else{
                    for(String file:TaskFactory.getInstance().markedList){
                        ExtFile f = LocationAPI.getInstance().getFileAndPopulate(file);
                        VF.files.put(f.propertyName.get(), f);
                        FileManagerLB.VirtualFolders.files.put(VF.getName(), VF);
                    }
                }
                return true;

            }else if(firstWord.equalsIgnoreCase(commandListRec)){
                String newCom = ExtStringUtils.replace(fullCommand, commands.getFirst()+" ","");
                ExtFile file = LocationAPI.getInstance().getFileAndPopulate(newCom);
                for(ExtFile f:file.getListRecursive()){    
                    deque.add(f.getAbsoluteDirectory());
                }
                String desc = "Listing recursive:"+deque.remove(0);
                ViewManager.getInstance().newListFrame(desc, deque);
                return true;
            }else if(firstWord.equalsIgnoreCase(commandList)){
                String newCom = ExtStringUtils.replace(fullCommand, commands.getFirst()+" ","");
                ExtFile file = LocationAPI.getInstance().getFileAndPopulate(newCom);
                if(file.getIdentity().equals(Identity.FOLDER)){
                    String desc = "Listing:"+file.getAbsoluteDirectory();

                    ExtFolder folder = (ExtFolder) file;
                    for(ExtFile f:folder.getFilesCollection()){
                        deque.add(f.getAbsoluteDirectory());
                    }
                    System.out.println(deque);
                    ViewManager.getInstance().newListFrame(desc, deque);
                }
                
                return true;
            }else if(firstWord.equalsIgnoreCase(commandSetCustom)){
                if(TaskFactory.getInstance().markedList.isEmpty()){
                    return true;
                }
                String get = TaskFactory.getInstance().markedList.get(0);
                FileManagerLB.customPath.setPath(get);
                return true;
            
            }else if(firstWord.equalsIgnoreCase(commandHelp)){
                addToTextArea(textArea,"Read Parameters.txt file for info\n");
                return true;
            }else if(firstWord.equalsIgnoreCase(commandClear)){
                textArea.clear();
                return true;
            }
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
                    if(commandSet.contains(commands.getFirst())){
                        customCommand(commands.getFirst(),command,commands);
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
