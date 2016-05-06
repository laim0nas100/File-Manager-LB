/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;


import static filemanagerGUI.FileManagerLB.rootSet;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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
    public Collection<ExtFile> getListRecursive(){
        ArrayList<ExtFile> list = new ArrayList<>();
        list.add(this);
        return list; 
    }
    public boolean isRoot(){
        String path = this.getAbsolutePath();
        return rootSet.contains(path);
    }
    public String getAbsoluteDirectory(){
        return this.getAbsolutePath();
    }
}
