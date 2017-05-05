/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import LibraryLB.Containers.ObjectBuffer;
import filemanagerGUI.FileManagerLB;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import utility.ErrorReport;
import LibraryLB.Log;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.Identity;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Iterator;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import utility.ExtStringUtils;

/**
 *
 * @author Laimonas Beiušis
 * Extended Folder for custom actions
 */
public class ExtFolder extends ExtPath{

    protected boolean populated;
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


    protected void populateFolder(ObjectBuffer buffer, BooleanProperty isCanceled){
        
        try{
            if(Files.isDirectory(toPath())){
                String parent = getAbsoluteDirectory();
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(parent))) {
                    for(Path f  :dirStream){
                        final String name = ExtStringUtils.replaceOnce(f.toString(), parent, "");
                        final String filePathStr = f.toString();
                        ExtPath file = null;
                        if(Files.exists(f) && !files.containsKey(name)){
                            if(Files.isDirectory(f)){
                                file = new ExtFolder(filePathStr,f);
                            }else if(Files.isSymbolicLink(f)){
                                file = new ExtLink(filePathStr,f);
                            }else{
                                file = new ExtPath(filePathStr,f);
                            }
                            file.parent = this;
                            files.put(file.propertyName.get(), file);
                            if(buffer!=null){
                                buffer.add(file);
                                if(isCanceled!=null){
                                    if(isCanceled.get()){
                                        Log.print("Canceled form populate");
                                        return;
                                    }
                                }
                            }
                        }  
                    }
                }              
            }
            if(buffer!=null){
                buffer.flush();
            }
            
            
        }catch(Exception e){
            ErrorReport.report(e);
        }
        this.populated = true;
    }
    public void populateRecursive(){
        populateRecursiveInner(this);
    }
    private void populateRecursiveInner(ExtFolder fold){
        fold.update();
        Log.print("Iteration "+fold.getAbsoluteDirectory());
        for(ExtFolder folder:fold.getFoldersFromFiles()){
            fold.files.replace(folder.propertyName.get(), folder);
            folder.populateRecursiveInner(folder);
            
        }
        fold.populated = true;
        
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
    public Collection<ExtPath> getListRecursive(boolean applyDisable){
        ArrayDeque<ExtPath> list = new ArrayDeque<>();
        list.add(this);
        getRootList(list,this); 
        if(applyDisable){
            Iterator<ExtPath> iterator = list.iterator();        
            while(iterator.hasNext()){
                ExtPath next = iterator.next();
                if(next.isDisabled.get()){
                    iterator.remove();
                }
            }
        }
        return list; 
    }
    public Collection<ExtPath> getListRecursiveFolders(boolean applyDisalbe){
        Collection<ExtPath> listRecursive = this.getListRecursive(applyDisalbe);
        Iterator<ExtPath> iterator = listRecursive.iterator();
        while(iterator.hasNext()){
            ExtPath next = iterator.next();
            if(!next.getIdentity().equals(Enums.Identity.FOLDER)){
                iterator.remove();
            }
        }
        return listRecursive;
    }
    private void getRootList(Collection<ExtPath> list,ExtFolder folder){
        folder.update();
        if(!folder.isDisabled.get()){
            list.addAll(folder.getFilesCollection());
            folder.getFoldersFromFiles().forEach( fold -> {
                getRootList(list,fold);
            });
        }
    }
    public void update(){
        Log.print("Update:"+this.getAbsoluteDirectory());
        if(isAbsoluteRoot.get()){
            FileManagerLB.remount();
            return;
        }
        if(isPopulated()){           
            for(ExtPath file:getFilesCollection()){
                if(!Files.exists(file.toPath())){
                    Log.print(file.getAbsoluteDirectory()+" doesn't exist");
                    files.remove(file.propertyName.get());
                }
            }   
        }
        populateFolder(null,null);
    }
    public void update(ObservableList<ExtPath> list, BooleanProperty isCanceled){
        Log.print("Update:"+this.getAbsoluteDirectory());
        ObjectBuffer<ExtPath> buffer = new ObjectBuffer(list,5);
        if(isPopulated()){           
            for (ExtPath file : getFilesCollection()) {
                if(!Files.exists(file.toPath())){
                    Log.print(file.getAbsoluteDirectory()+" doesn't exist");
                    files.remove(file.propertyName.get());
                }
                if(isCanceled.get()){
                    return;
                }
            }   
        }
        buffer.addAll(getFilesCollection());
        populateFolder(buffer,isCanceled);
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
        String request = "";
        for(String key:files.keySet()){
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
