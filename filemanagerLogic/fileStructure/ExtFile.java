/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;


import utility.DesktopApi;
import filemanagerLogic.Movable;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import static filemanagerGUI.FileManagerLB.FolderForDevices;

/**
 *
 * @author Laimonas Beiušis
 * Extended File for custom actions
 */
public class ExtFile extends FileAbs implements Movable {
    protected String relativePath;
    protected Path destination;
    protected boolean isRoot;
    protected boolean isAbsoluteRoot;

    public boolean isAbsoluteRoot() {
        return isAbsoluteRoot;
    }

    public void setIsAbsoluteRoot(boolean isAbsoluteRoot) {
        this.isAbsoluteRoot = isAbsoluteRoot;
    }
    public boolean isRoot() {
        return isRoot;
    }

    public void setIsRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }
    

    
    
    public boolean doOnOpen() throws IOException{
        return DesktopApi.open(this);
    }
     protected void setDefaultValues(){         
        this.relativePath = "";
        this.destination = Paths.get(""); 
        this.name = new SimpleStringProperty(this.getName());
        this.isRoot = false;
        this.isAbsoluteRoot = false;
    }   
    public ExtFile(File file){
        super(file.getAbsolutePath());
        setDefaultValues();
    }
    public ExtFile(String path){
        super(path);
        setDefaultValues();
    }
    
    
    

    /**
     * get true form
     * @return
     */
    public ExtFile getTrueForm(){
        return this;
    }
    public String getIdentity(){
        return "file";
    }
    
    public String getRelativePath() {
        return relativePath;
    }



    @Override
    public Path getDestination() {
        return destination;
    }

    @Override
    public void setDestination(Path destination) {
        this.destination = destination;
    }
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    @Override
    public void setRelativePath(ExtFile dest){
        String path = dest.getName()+File.separator+this.getAbsolutePath().replace(dest.getAbsolutePath(), "");
        this.relativePath = path;
    }
    


    

}
