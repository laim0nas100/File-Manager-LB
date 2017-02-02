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
public class ExtFolder extends ExtPath{


    
    private boolean populated;
    public ConcurrentHashMap <String,ExtPath> files;
    
    public Collection<ExtPath> getFilesCollection(){
        return this.files.values();
    }


    @Override
    public Identity getIdentity(){
        return Identity.FOLDER;
    }
    public ExtFolder(String src,Object...optional){
        super(src,optional);
        this.files = new ConcurrentHashMap<>(1,0.75f);
        this.populated = false;
    }
    
    
    public void populateFolder(){
        try{
            if(Files.isDirectory(this.toPath())){
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
                        ExtPath file;
                        if(Files.isDirectory(f)){
                            file = new ExtFolder(filePathStr,f);             
                        }else if(Files.isSymbolicLink(f)){
                            file = new ExtLink(filePathStr,f);
                        }else{
                            file = new ExtPath(filePathStr,f);
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
            fold.files.replace(folder.propertyName.get(), folder);  
        }
        this.populated = true;
        
    };
    public Collection<ExtFolder> getFoldersFromFiles(){
        LinkedList<ExtFolder> folders = new LinkedList<>();
        for(ExtPath file:this.getFilesCollection()){
            if(file.getIdentity().equals(Identity.FOLDER)){
                ExtFolder fold = (ExtFolder) file;
                folders.add(fold);
            }
        }
        return folders;   
    }
    @Override
    public Collection<ExtPath> getListRecursive(){
        LinkedList<ExtPath> list = new LinkedList<>();
        list.add(this);
        getRootList(list,this);
        Iterator<ExtPath> iterator = list.iterator();
        while(iterator.hasNext()){
            ExtPath next = iterator.next();
            if(next.isDisabled.get()){
                iterator.remove();
            }
        }
        return list; 
    }
    private void getRootList(Collection<ExtPath> list,ExtFolder folder){
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
            for(ExtPath file:this.getFilesCollection()){
                if(!Files.exists(file.toPath())){
                    Log.writeln(file.getAbsolutePath()+" dont exist");
                    LocationInRoot location = new LocationInRoot(file.getAbsolutePath());
                    LocationAPI.getInstance().removeByLocation(location);
                }
            }   
        }
        this.populateFolder();
    }
    public ExtPath getIgnoreCase(String name){
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
        return this.getAbsolutePath()+File.separator;
    }
    @Override
    public boolean isAbsoluteRoot(){
        return this.getIdentity().equals(Identity.VIRTUAL)||super.isAbsoluteRoot();
    }
    
    //GETTERS & SETTERS
    
    public boolean isPopulated(){
        return populated;
        
    }
    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
}
