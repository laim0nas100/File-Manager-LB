/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;


import filemanagerGUI.FileManagerLB;
import filemanagerGUI.MainController;
import filemanagerLogic.Enums.DATA_SIZE;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
 * @author Laimonas Beiušis
 * Extended File for custom actions
 */
public class ExtFile extends File{

    public static final Comparator<String> COMPARE_SIZE_STRING = new Comparator<String>() {
        @Override
        public int compare(String f1, String f2) {
            if (f1.isEmpty() || f2.isEmpty()) {
                return f1.compareTo(f2);
            }
            return ExtFile.extractSize(f1).compareTo(ExtFile.extractSize(f2));
        }
    };
    public static Double extractSize(String s) {
        Long multiplier = DATA_SIZE.B.size;
        if (s.startsWith("(B)")) {
            s = s.replace("(B) ", "");
        } else if (s.startsWith("(KB)")) {
            s = s.replace("(KB) ", "");
            multiplier = DATA_SIZE.KB.size;
        } else if (s.startsWith("(MB)")) {
            s = s.replace("(MB) ", "");
            multiplier = DATA_SIZE.MB.size;
        } else if (s.startsWith("(GB)")) {
            s = s.replace("(GB) ", "");
            multiplier = DATA_SIZE.GB.size;
        }
        return Double.parseDouble(s) * multiplier;
    }

    
    
    public BooleanProperty isAbsoluteRoot;
    public StringProperty propertyName;
    public StringProperty propertyType;
    public LongProperty propertySize;
    public LongProperty propertyLastModified;
    
    public StringProperty propertyDate;
    public StringProperty propertySizeAuto;
    public LongProperty readyToUpdate;
    private long size = -1;
    private long lastModified = -1;
    public boolean isAbsoluteRoot() {
        return isAbsoluteRoot.get();
    }

    public void setIsAbsoluteRoot(boolean isAbsoluteRoot) {
        this.isAbsoluteRoot.set(isAbsoluteRoot);
    }
    protected void setDefaultValues(){         
        Task<Void> r = new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                size =  length();
                return null;
            }
            
        };
        r.setOnSucceeded(v->{
            propertySize.set(size);
        });
        Task<Void> r1 = new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                lastModified = lastModified();
                return null;
            }
            
        };
        r1.setOnSucceeded(v->{
            propertyLastModified.set(lastModified);
        });
        this.readyToUpdate = new SimpleLongProperty(){
            @Override
            public long get(){
                if((propertySize.get()!=-1)&&(propertyLastModified.get()!=-1)){
                    return 1;
                }else{
                    return 0;
                }
            }
        };
        this.propertyName = new SimpleStringProperty(this.getName());
        this.propertyType = new SimpleStringProperty(this.getIdentity().identity);
        
        this.propertySize = new SimpleLongProperty(){
            @Override
            public long get() {
                new Thread(r).start();
                return size;
                    
            }
        };
        this.propertyLastModified = new SimpleLongProperty(){
            @Override
            public long get() {
                new Thread(r1).start();
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
    public Identity getIdentity(){
        return Identity.FILE;
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
    public LocationInRoot getMapping(){
        return LocationAPI.getInstance().getLocationMapping(getAbsoluteDirectory());
    }
}
