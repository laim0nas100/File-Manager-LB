/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import LibraryLB.Log;
import filemanagerGUI.FileManagerLB;
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
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import javafx.scene.control.TextField;
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
    private String nameReplace;
    private String pathReplace;
    private String nameNoExtReplace;
    private int truncateAfter;
    private String commandGenerate;
    private String commandApply;
    private String commandCreateVirtual;
    private String commandListVirtualFolders;
    private String commandListVirtual;
    private String commandList;
    private String commandListRec;
    private String commandClear;
    private String commandAddToVirtual;
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
        command = new Command(textField);
        pathReplace = (String) FileManagerLB.parameters.defaultGet("code.path", "<p>");
        nameReplace = (String) FileManagerLB.parameters.defaultGet("code.name", "<n>");
        nameNoExtReplace = (String) FileManagerLB.parameters.defaultGet("code.nameNoExtension", "<nne>");
        truncateAfter = (Integer) FileManagerLB.parameters.defaultGet("code.truncateAfter", 100000);
        commandGenerate = (String) FileManagerLB.parameters.defaultGet("code.commandGenerate", "generate");
        commandApply = (String) FileManagerLB.parameters.defaultGet("code.commandApply", "apply");
        commandCreateVirtual = (String) FileManagerLB.parameters.defaultGet("code.createVirtualFolder", "virtual");
        commandListVirtualFolders = (String) FileManagerLB.parameters.defaultGet("code.listVirtualFolders", "listVirtualFolders");
        commandListVirtual = (String) FileManagerLB.parameters.defaultGet("code.listVirtual", "listVirtual");
        commandClear = (String) FileManagerLB.parameters.defaultGet("code.clear", "clear");
        commandAddToVirtual = (String) FileManagerLB.parameters.defaultGet("code.addToVirtual", "add");
        commandList = (String) FileManagerLB.parameters.defaultGet("code.list", "list");
        commandListRec = (String) FileManagerLB.parameters.defaultGet("code.listRec", "listRec");
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
        public void apply(String name) throws IOException{
            LinkedList<String> readFromFile = new LinkedList(LibraryLB.FileManaging.FileReader.readFromFile(name));
            this.setTextAfterwards = true;
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
                    if(add.contains(pathReplace)){
                        add = ExtStringUtils.replace(add, pathReplace, absPath);
                    }
                    String fileName=absPath.substring(absPath.lastIndexOf(File.separator)+1);
                    String fileNameNoExt = fileName;
                    if (fileName.contains(".")){
                        fileNameNoExt = fileName.substring(0,fileName.lastIndexOf("."));
                    }
                    if(add.contains(nameReplace)){
                        add = ExtStringUtils.replace(add,nameReplace, fileName );
                    }
                    if(add.contains(nameNoExtReplace)){
                        add = ExtStringUtils.replace(add, nameNoExtReplace, fileNameNoExt);
                    }
                    commands.add(add);
                    Log.writeln(command+"||"+add);
                }
                LibraryLB.FileManaging.FileReader.writeToFile(FileManagerLB.HOME_DIR+name, commands);
            }catch(Exception ex){
                ErrorReport.report(ex);
            }
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
                    if(commands.getFirst().equalsIgnoreCase(commandGenerate)){
                        commands.removeFirst();
                        String newCom = command;
                        newCom = ExtStringUtils.replaceOnce(newCom, commandGenerate+" ", "");
                        String index = commands.removeFirst();
                        newCom = ExtStringUtils.replaceOnce(newCom, index+" ", "");
                        generate(newCom,index);
                        return null;
                    }else if(commands.getFirst().equalsIgnoreCase(commandApply)){
                        commands.removeFirst();
                        apply(commands.removeFirst());
                        return null;
                    }else if(commands.getFirst().equalsIgnoreCase(commandCreateVirtual)){
                        VirtualFolder.createVirtualFolder();
                        return null;
                    }else if(commands.getFirst().equalsIgnoreCase(commandListVirtualFolders)){
                        for(ExtFile f:FileManagerLB.VirtualFolders.files.values()){
                            addToTextArea(textArea,f.getName()+"\n");
                        }
                        
                        return null;
                     }else if(commands.getFirst().equalsIgnoreCase(commandListVirtual)){
                        commands.removeFirst();
                        VirtualFolder VF = (VirtualFolder) FileManagerLB.VirtualFolders.files.get(commands.getFirst());
                        if (VF == null){
                            addToTextArea(textArea,"No such virtual folder:"+commands.getFirst());
                            
                        }else{
                            addToTextArea(textArea,"Listing "+VF.getAbsoluteDirectory()+"\n");
                            for(ExtFile f:VF.getFilesCollection()){
                                addToTextArea(textArea,f.getName()+"\n");
                            }
                        }
                        
                        return null;
                        
                    }else if(commands.getFirst().equalsIgnoreCase(commandAddToVirtual)){
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
                        return null;
                     
                    }else if(commands.getFirst().equalsIgnoreCase(commandListRec)){
                        String newCom = ExtStringUtils.replace(command, commands.getFirst()+" ","");
                        ExtFile file = LocationAPI.getInstance().getFileAndPopulate(newCom);
                        addToTextArea(textArea,"Listing:");
                        for(ExtFile f:file.getListRecursive()){
                                addToTextArea(textArea,f.getName()+"\n");
                            }
                        return null;
                    }else if(commands.getFirst().equalsIgnoreCase(commandList)){
                        String newCom = ExtStringUtils.replace(command, commands.getFirst()+" ","");
                        ExtFile file = LocationAPI.getInstance().getFileAndPopulate(newCom);
                        addToTextArea(textArea,"Listing:"+file.getAbsoluteDirectory()+"\n");
                        if(file.getIdentity().equals(Identity.FOLDER)){
                            ExtFolder folder = (ExtFolder) file;
                            for(ExtFile f:folder.getFilesCollection()){
                                addToTextArea(textArea,f.getName()+"\n");
                            }
                        }
                        
                        return null;
                        
                    }else if(commands.getFirst().equalsIgnoreCase(commandClear)){
                        textArea.clear();
                        return null;
                    }
                    
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
