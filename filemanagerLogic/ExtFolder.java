/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Laimonas Beiušis
 * Extended Folder for custom actions
 */
public class ExtFolder extends ExtFile{


    
    private boolean populated;
    private boolean needsUpdate;
    public LinkedHashMap <String,ExtFile> files;
    
    public Collection<ExtFile> getFilesCollection(){
        return this.files.values();
    }


    /*
    public Collection<ExtFile> getFilesCollectionRecursive(){
    ArrayList<ExtFile> list = new ArrayList<>();
    for(int i=0; i<files.size(); i++){
    ArrayList<ExtFile> list1 = (ArrayList<ExtFile>) files.values();
    if
    }
    }
     */    
    

    @Override
    public boolean doOnOpen() {
        return true;
    }
    @Override
    public String getIdentity(){
        return "folder";
    }
    
    public ExtFile[] getFilesArray(){
        if(!this.populated){
            this.populateFolder();
            this.populated = true;
        }
        if(this.needsUpdate){
            this.updateRecursive();
        }
        Collection<ExtFile> values = this.files.values();
        return values.toArray(new ExtFile[0]);
    }
    
    @Override
    protected void setDefaultValues(){
        //this.folders = new LinkedHashMap<>();
        this.files = new LinkedHashMap<>();
        this.populated = false;
        this.needsUpdate = false;
        super.setDefaultValues();
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
                    ExtFile file = new ExtFile(f.getAbsolutePath());
                    if(f.isDirectory()){
                        file = new ExtFolder(f);             
                    }else if(Files.isSymbolicLink(f.toPath())){
                        file = new ExtLink(f.getAbsolutePath());
                    }
                    if(!files.containsKey(f.getName())){
                        files.put(file.getName(), file);
                        //System.out.println("Add:"+file.getName());
                    } 
                }
            }
            this.populated = true;
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void populateRecursive(){
        populateRecursiveInner(this);
    }
    private ExtFolder populateRecursiveInner(ExtFolder fold){
        fold.populateFolder();
        for(ExtFolder folder:fold.getFoldersFromFiles()){
            folder.populateRecursiveInner(folder);
            fold.files.replace(folder.getName(), folder);  
        }
        this.populated = true;
        return fold;
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
        Collection<ExtFile> list = new Vector<>();
        getRootList(list,this,this);
        return list; 
    }
    private void getRootList(Collection<ExtFile> list,ExtFolder folder,ExtFolder root){
        if(!folder.populated){
            folder.updateRecursive();
        }
        for(ExtFile file:folder.getFilesCollection()){
            file.setRelativePath(root);
            list.add(file);
        }
        for(ExtFolder fold:folder.getFoldersFromFiles()){
            getRootList(list,fold,root);
        }
    }
    public void updateRecursive(){
        for(ExtFile file:this.getListRecursive()){
            if(!file.exists()){
                this.files.remove(file.getName());
            }
        }
        this.populateRecursive();
    }
    public void update(){
        for(ExtFile file:this.getFilesCollection()){
            if(!file.exists()){
                this.files.remove(file.getName());
            }
        }
        this.populateFolder();
    }
    
    
    
    //GETTERS & SETTERS
    

    @Override
    public ExtFolder getTrueForm(){
        return this;
    }
    public boolean needsUpdate() {
        return needsUpdate;
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }
    
    public boolean isPopulated(){
        return populated;
        
    }

    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
}
