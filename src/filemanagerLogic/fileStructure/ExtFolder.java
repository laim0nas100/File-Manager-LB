/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import utility.ErrorReport;
import LibraryLB.Log;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.SimpleTask;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import utility.ExtStringUtils;

/**
 *
 * @author Laimonas Beiušis
 * Extended Folder for custom actions
 */
public class ExtFolder extends ExtPath{

    public SimpleTask updateTask = new SimpleTask() {
        @Override
        protected Void call() throws Exception {
            return null;
        }
    };
    private boolean populated;
    public ConcurrentHashMap <String,ExtPath> files;
    
  
    public ExtFolder(String src,Object...optional){
        super(src,optional);
        files = new ConcurrentHashMap<>(1,0.75f,2);        
        populated = false;
    }
    public Collection<ExtPath> getFilesCollection(){
        return files.values();
    }
    @Override
    public Identity getIdentity(){
        return Identity.FOLDER;
    }
    public void populateFolder(){
        try{
            if(Files.isDirectory(toPath())){
                String parent = getAbsoluteDirectory();
                this.populated = true;
                Files.newDirectoryStream(Paths.get(parent)).forEach(f->{
                    String name = ExtStringUtils.replaceOnce(f.toString(), parent, "");
                    String filePathStr = f.toString();
                    if(Files.exists(f)&&!files.containsKey(name)){
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
                });
            }
            
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
        ArrayDeque<ExtFolder> folders = new ArrayDeque<>();
        for(ExtPath file:getFilesCollection()){
            if(file.getIdentity().equals(Identity.FOLDER)){
                folders.add((ExtFolder) file);
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
        Log.writeln("Update:"+this.getAbsoluteDirectory());
        this.updateTask.cancel();
        if(isAbsoluteRoot.get()){
            FileManagerLB.remount();
        }
        if(isPopulated()){
            
            for(ExtPath file:getFilesCollection()){
                if(!Files.exists(file.toPath())){
                    Log.writeln(file.getAbsolutePath()+" dont exist");
                    files.remove(file.propertyName.get());
                }
            }   
        }
        populateFolder();
    }
    public void startUpdateTask(){
        updateTask = new SimpleTask() {
        @Override
        protected Void call() throws Exception {
            Log.writeln("Update Task:"+getAbsolutePath());
            if(isAbsoluteRoot.get()){
                FileManagerLB.remount();
            }
            if(isPopulated()){
                
                for(ExtPath file:getFilesCollection()){
                    if(this.isCancelled()){
                        return null;
                    }
                    if(!Files.exists(file.toPath())){
                        Log.writeln(file.getAbsolutePath()+" dont exist");
                        files.remove(file.propertyName.get());
                    }
                }   
            }
            try{
                if(Files.isDirectory(toPath())){
                    String parent = getAbsoluteDirectory();
                    populated = true;
                    Files.newDirectoryStream(Paths.get(parent)).forEach(f ->{
                        if(this.isCancelled()){
                            return;
                        }

                        String name = ExtStringUtils.replaceOnce(f.toString(), parent, "");
                        String filePathStr = f.toString();
                        if((!Files.exists(f))&&(!files.containsKey(name))){
                            
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
                    });
                }
                
            }catch(Exception e){
                ErrorReport.report(e);
            }
            return null;
        }
        
    };
        new Thread(updateTask).start();
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
        if(isAbsoluteRoot.get()){
            return FileManagerLB.ROOT_NAME;
        }
        return this.getAbsolutePath()+File.separator;
    }

    public boolean isPopulated(){
        return populated;
        
    }
    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
}
