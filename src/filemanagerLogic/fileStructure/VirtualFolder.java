/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.Identity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolder extends ExtFolder {
    public static String VIRTUAL_FOLDER_PREFIX = "V";
    public static void createVirtualFolder() throws IOException{
        int index = 0;
        String name = VIRTUAL_FOLDER_PREFIX+index;
        while(FileManagerLB.VirtualFolders.files.containsKey(name)){
            index+=1;
            name = VIRTUAL_FOLDER_PREFIX+index;
        }
//        Files.createFile(Paths.get(FileManagerLB.VIRTUAL_FOLDERS_DIR+name));
        VirtualFolder VF = new VirtualFolder(FileManagerLB.VIRTUAL_FOLDERS_DIR+name);
        FileManagerLB.VirtualFolders.files.put(name, VF);
    }
    public VirtualFolder(String src) {
        super(src);
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
        Collection<ExtPath> filesCol = this.getFilesCollection();
        for(ExtPath file:filesCol){
            if(!Files.exists(file.toPath())){
                this.files.remove(file.propertyName.get());
            }
        }
        Collection<ExtFolder> folders = this.getFoldersFromFiles();
        for(ExtFolder folder:folders){
            folder.update();
        }
        
    }
    @Override
    public Collection<ExtPath> getListRecursive(){
        ArrayList<ExtPath> listRecursive = new ArrayList(super.getListRecursive());
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
    public void populateFolder(){
        
    }
    
    @Override
    public String getAbsoluteDirectory(){
        return this.propertyName.get();
    }
    
}
