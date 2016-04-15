/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Laimonas Beiušis
 * Extended File for custom actions
 */
public class ExtFile extends FileAbs{
    protected String relativePath;
    protected Path destination;
    protected boolean isAbsoluteRoot;

    public boolean isAbsoluteRoot() {
        return isAbsoluteRoot;
    }

    public void setIsAbsoluteRoot(boolean isAbsoluteRoot) {
        this.isAbsoluteRoot = isAbsoluteRoot;
    }

    
     protected void setDefaultValues(){         
        this.relativePath = "";
        this.destination = Paths.get(""); 
        this.propertyName = new SimpleStringProperty(this.getName());
        this.propertyType = new SimpleStringProperty(this.getIdentity());
        this.propertySize = new SimpleLongProperty(this.length());
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



    public Path getDestination() {
        return destination;
    }

    public void setDestination(Path destination) {
        this.destination = destination;
    }
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    public void setRelativePath(ExtFile dest){
        String path = dest.getName()+File.separator+this.getAbsolutePath().replace(dest.getAbsolutePath(), "");
        this.relativePath = path;
    }
    


    

}
