/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.stage.Stage;
import utility.Log;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB extends Application {
    public static final String ARTIFICIAL_ROOT_NAME = "Devices";
    public static ExtFolder FolderForDevices;

    @Override
    public void start(Stage primaryStage) {
        FolderForDevices = new ExtFolder(ARTIFICIAL_ROOT_NAME);
        FolderForDevices.setIsRoot(true);
        FolderForDevices.setPopulated(true);
        FolderForDevices.setIsAbsoluteRoot(true);
        remount();
        FolderForDevices.name.set("DEVICES");
        ViewManager.getInstance().newWindow(FolderForDevices, FolderForDevices);  
    } 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    public static void remount(){
        File[] roots = File.listRoots();
        for(int i = 0; i < roots.length ; i++){
            System.out.println("Root["+i+"]:" + roots[i]);
            mountDevice(roots[i].getAbsolutePath());
        }
    }
    public static boolean mountDevice(String name){
        boolean result = false;   
        if(Files.isDirectory(Paths.get(name))){
            ExtFolder device = new ExtFolder(name);
            int nameCount = device.toPath().getNameCount();
            Log.write("Is direcory");
            if(nameCount == 0){
                result = true;
                String newName = name;
                //Only 1 slash, make directory the slash
                if(!name.equals(File.separator)){
                    newName = name.substring(0, name.lastIndexOf(File.separator));
                }
                Log.writeln("newName= "+newName);
                device.name.set(newName);
                device.setIsRoot(true);
                FolderForDevices.files.put(newName, device);
                device.update();
                Log.writeln("Mounted successfully");
            }
        }
        return result;
    }

}
