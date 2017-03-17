/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import LibraryLB.Log;
import LibraryLB.Threads.RepeatableTask;
import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.Identity;
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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import utility.ErrorReport;
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
    public BooleanProperty isVirtual;
    public BooleanProperty isAbsoluteRoot;
    public BooleanProperty isDisabled;
    public StringProperty propertyName;
    public StringProperty propertyType;
    public LongProperty propertySize;
    public LongProperty propertyLastModified;
    public StringProperty propertyDate;
    public StringProperty propertySizeAuto;
    public LongProperty readyToUpdate;
    
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private Callable getSizeTask = new RepeatableTask(new Callable() {
            @Override
            public Object call() throws Exception {
//                Log.write("getSizeTask" ,absolutePath);
                size =  Files.size(toPath());
                propertySize.set(size);
//                sizeTaskComplete.set(true);
                return null;
            }
    });
    private Callable getDateTask = new RepeatableTask(new Callable() {
            @Override
            public Void call() throws Exception {
//                Log.write("getDateTask ",absolutePath);
                lastModified =  Files.getLastModifiedTime(toPath()).toMillis();
                propertyLastModified.set(lastModified);
//                dateTaskComplete.set(true);
                return null;
            }
        });
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
            this.path = Paths.get(this.getAbsoluteDirectory());
        }
        return this.path;
//        return Paths.get(this.getAbsoluteDirectory());
    }
    private void init(){
        this.propertyName = new SimpleStringProperty(this.getName(true));
        this.propertyType = new SimpleStringProperty(this.getIdentity().toString());
        this.isDisabled = new SimpleBooleanProperty(false);
        this.propertySize = new SimpleLongProperty(){
            @Override
            public long get() {
                ExtPath.executor.submit(getSizeTask);                
                return size;
                    
            }
        };
        this.propertyLastModified = new SimpleLongProperty(){
            @Override
            public long get() {
                ExtPath.executor.submit(getDateTask);
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
        this.isVirtual = new SimpleBooleanProperty(){
            @Override
            public boolean get(){
                return getIdentity().equals(Identity.VIRTUAL);
            };
        };
    }
    public Collection<ExtPath> getListRecursive(boolean applyDisable){
        ArrayList<ExtPath> list = new ArrayList<>();
        if(!applyDisable || !this.isDisabled.get()){
            list.add(this);
        }
        return list; 
    }
    public boolean isRoot(){
        return FileManagerLB.getRootSet().contains(this.getAbsoluteDirectory());
    }
    public String getAbsoluteDirectory(){
        return this.getAbsolutePath();
    }
    public String getAbsolutePath(){
        return absolutePath;
    }
    public LocationInRoot getMapping(){
        return new LocationInRoot(this.getAbsoluteDirectory());
    }
    public Enums.Identity getIdentity(){
        return Enums.Identity.FILE;
    }
    public void setIsAbsoluteRoot(boolean b){
        this.isAbsoluteRoot.set(b);
    }
    public boolean isNotWriteable(){
        return (this.isAbsoluteRoot.get()||(this.equals(FileManagerLB.VirtualFolders)));
    }
    public long size(){
        long get = this.propertySize.get();
        if(get==-1){
            try {
                this.getSizeTask.call();
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }
        return this.size;
    }
    public long lastModified(){
        long get = this.propertyLastModified.get();
        if(get==-1){
            try {
                this.getDateTask.call();
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
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
        return new PathStringCommands(this.absolutePath).getName(extension);
    }

    public String getExtension(){
        return new PathStringCommands(this.absolutePath).getExtension();
    }
    public String getParent(int timesToGoUp){
        return new PathStringCommands(this.absolutePath).getParent(timesToGoUp);
    }
    public String relativeFrom(String possibleParent){
        return new PathStringCommands(absolutePath).relativePathFrom(possibleParent);
    }
    public String relativeTo(String possibleChild){
        return new PathStringCommands(absolutePath).relativePathTo(possibleChild);
    }
    
    public PathStringCommands getPathCommands(){
        return new PathStringCommands(absolutePath);
    }
    @Override
    public String toString(){
        return this.getAbsolutePath();
    }
}
