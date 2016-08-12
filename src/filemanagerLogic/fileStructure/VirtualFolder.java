/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerLogic.Enums;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolder {
    ConcurrentHashMap<String,ExtFile> files;
    public VirtualFolder(){
        files = new ConcurrentHashMap<>();
    }
    public Collection<ExtFile> getListRecursive(){
        ArrayList<ExtFile> list = new ArrayList<>();
        files.values().forEach(file ->{
            if(file.getIdentity().equals(Enums.Identity.FOLDER)){
                ExtFolder folder = (ExtFolder) file;
                list.addAll(folder.getListRecursive());
            }else{
                list.add(file);
            }
        });
        return list;
    }
    public Collection<ExtFile> getList(){
        ArrayList<ExtFile> list = new ArrayList<>();
        list.addAll(files.values());
        return list;
    }
    
}
