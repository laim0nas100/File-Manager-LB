/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import utility.ErrorReport;
import LibraryLB.Log;
import static filemanagerGUI.FileManagerLB.ROOT_NAME;
import filemanagerLogic.Enums.Identity;
import java.util.Iterator;
import java.util.LinkedList;
import utility.ExtStringUtils;

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
    public Identity getIdentity(){
        return Identity.FOLDER;
    }
    
    @Override
    protected void setDefaultValues(){
        this.files = new ConcurrentHashMap<>(2,0.75f,25);
        this.populated = false;
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
                String parent = this.getAbsoluteDirectory();
                
                Iterator<Path> iterator = Files.newDirectoryStream(Paths.get(parent)).iterator();
                while(iterator.hasNext()){
                    Path f = iterator.next();
                    String name = ExtStringUtils.replaceOnce(f.toString(), parent, "");
                    String filePathStr = f.toString();
                    if(!this.files.containsKey(name)){
                        if(!Files.exists(f)){
                            continue;
                        }
                        ExtFile file;
                        if(Files.isDirectory(f)){
                            file = new ExtFolder(filePathStr);             
                        }else if(Files.isSymbolicLink(f)){
                            file = new ExtLink(filePathStr);
                        }else{
                            file = new ExtFile(filePathStr);
                        }
                        files.put(file.propertyName.get(), file);
                    }
                }
            }
            this.populated = true;
        }catch(Exception e){
            ErrorReport.report(e);
        }
    }
    public void populateRecursive(){
        populateRecursiveInner(this);
    }
    private void populateRecursiveInner(ExtFolder fold){
        fold.update();
        Log.writeln("Iteration "+fold.getAbsoluteDirectory());
        for(ExtFolder folder:fold.getFoldersFromFiles()){
            folder.populateRecursiveInner(folder);
            fold.files.replace(folder.getName(), folder);  
        }
        this.populated = true;
        
    };
    public Collection<ExtFolder> getFoldersFromFiles(){
        LinkedList<ExtFolder> folders = new LinkedList<>();
        for(ExtFile file:this.getFilesCollection()){
            if(file.getIdentity().equals(Identity.FOLDER)){
                ExtFolder fold = (ExtFolder) file;
                folders.add(fold);
            }
        }
        return folders;   
    }
    @Override
    public Collection<ExtFile> getListRecursive(){
        LinkedList<ExtFile> list = new LinkedList<>();
        list.add(this);
        getRootList(list,this);
        Iterator<ExtFile> iterator = list.iterator();
        while(iterator.hasNext()){
            ExtFile next = iterator.next();
            if(next.isDisabled.get()){
                iterator.remove();
            }
        }
        return list; 
    }
    private void getRootList(Collection<ExtFile> list,ExtFolder folder){
        folder.update();
        if(!folder.isDisabled.get()){
            list.addAll(folder.getFilesCollection());
            for(ExtFolder fold:folder.getFoldersFromFiles()){
                getRootList(list,fold);
            }
        }
    }
    public void update(){
        if(isAbsoluteRoot()){
            FileManagerLB.remount();
        }
        if(this.isPopulated()){
            Log.writeln("Update:"+this.getAbsolutePath());
            for(ExtFile file:this.getFilesCollection()){
                if(!Files.exists(file.toPath())){
                    Log.writeln(file+" dont exist");
                    LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
                    LocationAPI.getInstance().removeByLocation(location);
                }
            }   
        }
        this.populateFolder();
    }
    public ExtFile getIgnoreCase(String name){
        if(hasFileIgnoreCase(name)){
            String request = getKey(name);
            return files.get(request);
        }else{
            return null;
        }
    }
    public boolean hasFileIgnoreCase(String name){
        String key = getKey(name);
        return !key.isEmpty();
    }
    public String getKey(String name){
        ArrayList<String> keys = new ArrayList<>();
        keys.addAll(files.keySet());
        String request = "";
        for(String key:keys){
            if(name.equalsIgnoreCase(key)){
                request = key;
            }
        }
        return request;
    }
    @Override
    public String getAbsoluteDirectory(){
        if(isAbsoluteRoot()){
            return ROOT_NAME;
        }
        String dir = this.getAbsolutePath();
        if(!dir.endsWith(File.separator)){
            dir+=File.separator;
        }
        return dir;
    }
    
    
    //GETTERS & SETTERS
    
    public boolean isPopulated(){
        return populated;
        
    }
    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
}
