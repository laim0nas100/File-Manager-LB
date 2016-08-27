/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerLogic.Enums;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolder {
    ArrayList<ExtFile> files;
    public VirtualFolder(){
        files = new ArrayList<>();
    }
    public Collection<ExtFile> getListRecursive(){
        ArrayList<ExtFile> list = new ArrayList<>();
        files.forEach(file ->{
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
        return files;
    }
    public void update(){
        Iterator<ExtFile> iterator = files.iterator();
        while(iterator.hasNext()){
            try{
                if(!Files.exists(Paths.get(iterator.next().getAbsoluteDirectory()))){
                    iterator.remove();
                }
            }catch(Exception e){}
        }
    }
    
}
