/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

/**
 *
 * @author Lemmin
 */
public class ExtPath {
    public Path path;
    public BooleanProperty isAbsoluteRoot;
    public BooleanProperty isDisabled;
    public StringProperty propertyName;
    public StringProperty propertyType;
    public LongProperty propertySize;
    public LongProperty propertyLastModified;
    
    public StringProperty propertyDate;
    public StringProperty propertySizeAuto;
    public LongProperty readyToUpdate;
    protected final Enums.Identity identity = Enums.Identity.FILE;
    private long size = -1;
    private long lastModified = -1;
    private Task getSizeTask = new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                size =  Files.size(path);
                return null;
            }
        };
    private Task getDateTask = new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                lastModified =  Files.getLastModifiedTime(path).toMillis();
                return null;
            }
        };
    public ExtPath(String str){
        this(Paths.get(str));
    }
    public ExtPath(Path p){
        this.path = p;
        this.getSizeTask.setOnSucceeded(event->{
            this.propertySize.set(size);
        });
        this.getDateTask.setOnSucceeded(event->{
            this.propertyLastModified.set(lastModified);
        });
        this.propertyName = new SimpleStringProperty(this.path.getFileName().toString());
        this.propertyType = new SimpleStringProperty(this.identity.toString());
        this.isDisabled = new SimpleBooleanProperty(false);
        this.propertySize = new SimpleLongProperty(){
            @Override
            public long get() {
                new Thread(getSizeTask).start();
                return size;
                    
            }
        };
        this.propertyLastModified = new SimpleLongProperty(){
            @Override
            public long get() {
                new Thread(getDateTask).start();
                return lastModified;
                    
            }
        };
        this.propertyDate = new SimpleStringProperty(){
            @Override
            public String get() {
                if(propertyLastModified.get() == -1){
                    return "LOADING";
                }
                return new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(propertyLastModified.get())));
            }
        };
        this.propertySizeAuto = new SimpleStringProperty(){
            @Override
            public String get() {
                if(propertySize.get() == -1){
                    return "LOADING";
                }
                String stringSize = propertySize.asString().get();
                
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

                return ("("+sizeType+") "+stringSize.substring(0, Math.min(stringSize.length(), indexOf+3))); //To change body of generated methods, choose Tools | Templates.
            }

        };
        this.isAbsoluteRoot = new SimpleBooleanProperty(false);
        this.isAbsoluteRoot.set(false);
        
    }
    
    public Collection<ExtPath> getListRecursive(){
        ArrayList<ExtPath> list = new ArrayList<>();
        if(!this.isDisabled.get()){
            list.add(this);
        }
        return list; 
    }
    public boolean isRoot(){
        String strPath = this.getAbsoluteDirectory();
        Iterator<String> iterator = FileManagerLB.getRootSet().iterator();
        while(iterator.hasNext()){
            if(strPath.equalsIgnoreCase(iterator.next())){
                return true;
            }
        }
        return false;
    }
    public String getAbsoluteDirectory(){
        return this.path.toString();
    }
    public LocationInRoot getMapping(){
        return LocationAPI.getInstance().getLocationMapping(getAbsoluteDirectory());
    }
    public Enums.Identity getIdentity(){
        return this.identity;
    }
}
