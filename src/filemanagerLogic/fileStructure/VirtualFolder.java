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
import java.util.Collection;

/**
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolder extends ExtFolder {
    public static String VIRTUAL_FOLDER_PREFIX = (String) FileManagerLB.parameters.defaultGet("virtualPrefix", "V");
    public static void createVirtualFolder() throws IOException{
        int index = 0;
        String name = VIRTUAL_FOLDER_PREFIX+index;
        while(FileManagerLB.VirtualFolders.files.containsKey(name)){
            index+=1;
            name = VIRTUAL_FOLDER_PREFIX+index;
        }
        Files.createFile(Paths.get(FileManagerLB.VIRTUAL_FOLDERS_DIR+name));
        VirtualFolder VF = new VirtualFolder(FileManagerLB.VIRTUAL_FOLDERS_DIR+name);
        FileManagerLB.VirtualFolders.files.put(name, VF);
    }
    private VirtualFolder(String src) {
        super(src);
    }
    
    @Override
    public void update(){
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
    public Collection<ExtPath> getListRecursive(){
        Collection<ExtPath> listRecursive = super.getListRecursive();
        listRecursive.remove(this);
        return listRecursive;
    }
    @Override
    public Enums.Identity getIdentity(){
        return Identity.VIRTUAL;
    }
    
    
    @Override
    public String getAbsoluteDirectory(){
        return this.propertyName.get();
    }
    
}
