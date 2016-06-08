/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;


import filemanagerGUI.FileManagerLB;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Laimonas Beiušis
 * Extended File for custom actions
 */
public class ExtFile extends FileAbs{
    

    public boolean isAbsoluteRoot() {
        return isAbsoluteRoot.get();
    }

    public void setIsAbsoluteRoot(boolean isAbsoluteRoot) {
        this.isAbsoluteRoot.set(isAbsoluteRoot);
    }
    protected void setDefaultValues(){         
        this.propertyName = new SimpleStringProperty(this.getName());
        this.propertyType = new SimpleStringProperty(this.getIdentity());
        this.propertySize = new SimpleLongProperty(this.length());
        this.propertyDate = new SimpleStringProperty(new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(this.lastModified()))));
        String stringSize = this.propertySize.asString().get();
        Double size = Double.valueOf(stringSize);
        String sizeType = "B";
        if(size>=1024){
            size =size /1024;
            sizeType = "KB";
        }
        if(size>=1024){
            size =size /1024;
            sizeType = "MB";
        }
        if(size>=1024){
            size =size /1024;
            sizeType = "GB";
        }
        stringSize = String.valueOf(size);
        int indexOf = stringSize.indexOf('.');
        this.propertySizeAuto = new SimpleStringProperty("("+sizeType+") "+stringSize.substring(0, Math.min(stringSize.length(), indexOf+3)));
        this.isAbsoluteRoot = new SimpleBooleanProperty(false);
        this.isAbsoluteRoot.set(false);
        
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
        String path = this.getAbsoluteDirectory();
        Iterator<String> iterator = FileManagerLB.getRootSet().iterator();
        while(iterator.hasNext()){
            if(path.equalsIgnoreCase(iterator.next())){
                return true;
            }
        }
        return false;
    }
    public String getAbsoluteDirectory(){
        return this.getAbsolutePath();
    }
}
