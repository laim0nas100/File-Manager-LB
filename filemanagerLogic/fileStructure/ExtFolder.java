/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import static filemanagerGUI.FileManagerLB.rootSet;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.property.SimpleDoubleProperty;
import utility.Log;

/**
 *
 * @author Laimonas Beiušis
 * Extended Folder for custom actions
 */
public class ExtFolder extends ExtFile{


    
    private boolean populated;
    public ConcurrentHashMap <String,ExtFile> files;
    
    public Collection<ExtFile> getFilesCollection(){
        return this.files.values();
    }


    @Override
    public String getIdentity(){
        return "folder";
    }
    
    @Override
    protected final void setDefaultValues(){
        this.files = new ConcurrentHashMap<>();
        this.populated = false;
        super.setDefaultValues();
        this.propertySize.setValue(null);
    }
    public ExtFolder(String src){
        super(src);
        setDefaultValues();
    }
    
    public ExtFolder(File src){
        super(src.getAbsolutePath());
        setDefaultValues();
        
    }
    
    public void populateFolder(){
        try{
            if(null != this.list()){
                for(File f:this.listFiles()){
                    if(!this.files.containsKey(f.getName())){//NEW LINE
                        ExtFile file = new ExtFile(f.getAbsolutePath());
                        if(f.isDirectory()){
                            file = new ExtFolder(f);             
                        }else if(Files.isSymbolicLink(f.toPath())){
                            file = new ExtLink(f.getAbsolutePath());
                        }
                        LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
                        LocationAPI.getInstance().putByLocation(location, file);
                    }
                }
            }
            this.populated = true;
        }catch(Exception e){
            Log.writeln(e.getMessage());
        }
    }
    public void populateRecursive(){
        populateRecursiveInner(this);
    }
    private void populateRecursiveInner(ExtFolder fold){
        fold.update();
        Log.writeln("Iteration "+fold.getAbsolutePath());
        for(ExtFolder folder:fold.getFoldersFromFiles()){
            folder.populateRecursiveInner(folder);
            fold.files.replace(folder.getName(), folder);  
        }
        this.populated = true;
        
    };
    public Collection<ExtFolder> getFoldersFromFiles(){
        ArrayList<ExtFolder> folders = new ArrayList<>();
        for(ExtFile file:this.getFilesCollection()){
            if(file.getIdentity().equals("folder")){
                ExtFolder fold = (ExtFolder) file.getTrueForm();
                folders.add(fold);
            }
        }
        return folders;   
    }
    public Collection<ExtFile> getListRecursive(){
        ArrayList<ExtFile> list = new ArrayList<>();
        getRootList(list,this,this);
        return list; 
    }
    private void getRootList(Collection<ExtFile> list,ExtFolder folder,ExtFolder root){
        if(!folder.populated){
            folder.populateRecursive();
        }
        list.addAll(folder.getFilesCollection());
        for(ExtFolder fold:folder.getFoldersFromFiles()){
            getRootList(list,fold,root);
        }
    }
    public void update(){
        if(this.isAbsoluteRoot){
            
            FileManagerLB.remount();
        }else if(this.isPopulated()){
            Log.writeln("Update:"+this.getAbsolutePath());
            
            for(ExtFile file:this.getFilesCollection()){
                if(!Files.exists(file.toPath())){
                    Log.writeln(file+" dont exist");
                    LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
                    LocationAPI.getInstance().removeByLocation(location);
                }
            }
            this.populateFolder();
        }else{
            this.populateFolder();
        }
        
    }
    public boolean isRoot(){
        String path = this.getAbsolutePath();
        return rootSet.contains(path);
    }
    
    
    //GETTERS & SETTERS
    

    @Override
    public ExtFolder getTrueForm(){
        return this;
    } 
    public boolean isPopulated(){
        return populated;
        
    }
    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
}
