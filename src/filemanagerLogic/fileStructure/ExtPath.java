/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import utility.ExtStringUtils;
import utility.PathStringCommands;

/**
 *
 * @author Lemmin
 */
public class ExtPath{
    public static final Comparator<String> COMPARE_SIZE_STRING = (String f1, String f2) -> {
        if (f1.isEmpty() || f2.isEmpty()) {
            return f1.compareTo(f2);
        }
        return ExtPath.extractSize(f1).compareTo(ExtPath.extractSize(f2));
    };
    public static Double extractSize(String s) {
        Long multiplier = Enums.DATA_SIZE.B.size;
        if (s.startsWith("(B)")) {
            s = s.replace("(B) ", "");
        } else if (s.startsWith("(KB)")) {
            s = s.replace("(KB) ", "");
            multiplier = Enums.DATA_SIZE.KB.size;
        } else if (s.startsWith("(MB)")) {
            s = s.replace("(MB) ", "");
            multiplier = Enums.DATA_SIZE.MB.size;
        } else if (s.startsWith("(GB)")) {
            s = s.replace("(GB) ", "");
            multiplier = Enums.DATA_SIZE.GB.size;
        } else{
            return (double)0;
        }
        return Double.parseDouble(s) * multiplier;
    }
    
    private Path path;
    private final String absolutePath;
    private long size = -1;
    private long lastModified = -1;
    public BooleanProperty isAbsoluteRoot;
    public BooleanProperty isDisabled;
    public StringProperty propertyName;
    public StringProperty propertyType;
    public LongProperty propertySize;
    public LongProperty propertyLastModified;
    public StringProperty propertyDate;
    public StringProperty propertySizeAuto;
    public LongProperty readyToUpdate;
    
    private Task getSizeTask = new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                size =  Files.size(toPath());
                return null;
            }
        };
    private Task getDateTask = new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                lastModified =  Files.getLastModifiedTime(toPath()).toMillis();
                return null;
            }
        };
    public ExtPath(String str,Object...optional){
        str = str.trim();
        if(str.endsWith(File.separator)){
            str = str.substring(0,str.length()-1);
        }
        this.absolutePath = str;
        init();
        if(optional.length>0){
            this.path = (Path) optional[0];
        }
        
    }
    public Path toPath(){
        if(this.path == null){
            this.path = Paths.get(absolutePath);
        }
        return this.path;
    }
    private void init(){
        this.getSizeTask.setOnSucceeded(event->{
            this.propertySize.set(size);
        });
        this.getDateTask.setOnSucceeded(event->{
            this.propertyLastModified.set(lastModified);
        });
        this.propertyName = new SimpleStringProperty(this.getName(true));
        this.propertyType = new SimpleStringProperty(this.getIdentity().toString());
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
        return this.getAbsolutePath();
    }
    public String getAbsolutePath(){
        return absolutePath;
    }
    public LocationInRoot getMapping(){
        return LocationAPI.getInstance().getLocationMapping(getAbsoluteDirectory());
    }
    public Enums.Identity getIdentity(){
        return Enums.Identity.FILE;
    }
    public void setIsAbsoluteRoot(boolean b){
        this.isAbsoluteRoot.set(b);
    }
    public boolean isAbsoluteRoot(){
        return this.isAbsoluteRoot.get();
    }
    public boolean isAbsoluteOrVirtualFolders(){
        return (this.isDisabled.get()||(this.equals(FileManagerLB.VIRTUAL_FOLDERS_DIR)));
    }
    public long size(){
        long get = this.propertySize.get();
        if(get==-1){
            this.getSizeTask.run();
        }
        return this.size;
    }
    public long lastModified(){
        long get = this.propertyLastModified.get();
        if(get==-1){
            this.getDateTask.run();
        }
        return this.lastModified;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.absolutePath);
        return hash;
    }
    
    @Override
    public boolean equals(Object e){
        if(e==null){
            return false;
        }
        if(!( e instanceof ExtPath)){
            return false;
        }else{
            ExtPath p = (ExtPath) e;
            return this.absolutePath.equals(p.getAbsolutePath());
        }
        
    }
    public String getName(boolean extension){
        String name = PathStringCommands.getName(absolutePath);
        
        if(!extension){
            if(name.contains(".")){
                int index = ExtStringUtils.lastIndexOf(name, ".");
                name = name.substring(0,index);
            }
        }
        return name;
    }

    public String getExtension(){
        String name = this.getName(true);
        if(name.contains(".")){
            int index = ExtStringUtils.lastIndexOf(name, ".")-1;
            if(index>=0){
                name = name.substring(index);
            }else{
                return "";
            }
        }else{
            return "";
        }
        return name;
    }
    public String getParent(int timesToGoUp){
        String current = this.absolutePath;
        while(timesToGoUp>0){
            current = PathStringCommands.goUp(current);
            timesToGoUp--;
        }
        return current;
    }
    public String relativeFrom(String possibleParent){
        String strPath = absolutePath+File.separator;
        if(!strPath.contains(possibleParent) || strPath.equalsIgnoreCase(possibleParent)){
            return absolutePath;
        }else{
            return ExtStringUtils.replaceOnce(strPath, possibleParent, "");
        }
    }
    public String relativeTo(String possibleChild){
        String strPath = absolutePath+File.separator;
        if(!possibleChild.contains(strPath) || possibleChild.equalsIgnoreCase(strPath)){
            return absolutePath;
        }else{
            return ExtStringUtils.replaceOnce(possibleChild, strPath, "");
        }
    }
}
