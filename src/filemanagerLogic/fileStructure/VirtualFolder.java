/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.Identity;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;

/**
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolder extends ExtFolder {
    public static String VIRTUAL_FOLDER_PREFIX = "V";
    public static void createVirtualFolder(){
        int index = 0;
        String name = VIRTUAL_FOLDER_PREFIX+index;
        while(FileManagerLB.VirtualFolders.files.containsKey(name)){
            index+=1;
            name = VIRTUAL_FOLDER_PREFIX+index;
        }
        VirtualFolder VF = new VirtualFolder(FileManagerLB.VIRTUAL_FOLDERS_DIR+name);
        FileManagerLB.VirtualFolders.files.put(name, VF);
    }
    public VirtualFolder(String src) {
        super(src);
        this.populated = true;
    }
    
    @Override
    public void update(){
        if(this.isAbsoluteRoot.get()){
            FileManagerLB.remount();
            return;
        }
        if(this.equals(FileManagerLB.VirtualFolders)){
            return;
        }
        Iterator<ExtPath> iter = this.getFilesCollection().iterator();
        while(iter.hasNext()){
            if(!Files.exists(iter.next().toPath())){
                iter.remove();
            }
        }
        
    }
    
    @Override
    public void update(ObservableList<ExtPath> list, BooleanProperty isCanceled){
        update();
        list.setAll(this.getFilesCollection());
    }
    @Override
    public Collection<ExtPath> getListRecursive(boolean applyDisable){
        ArrayList<ExtPath> listRecursive = new ArrayList(super.getListRecursive(applyDisable));
        listRecursive.remove(0);
        return listRecursive;
    }
    @Override
    public Enums.Identity getIdentity(){
        return Identity.VIRTUAL;
    }
    
    public void add(ExtPath file){
        String name = file.getName(true);
        if(!files.containsKey(name)){
            files.put(name, file);
        }
    }
    public void addAll(Collection<ExtPath> list){
        list.forEach(item ->{
            add(item);
        });
    }
    
    @Override
    public void populateFolder(Collection<ExtPath> list, BooleanProperty isCanceled){
        
    }
    
    @Override
    public String getAbsoluteDirectory(){
        return this.propertyName.get();
    }
    
}
