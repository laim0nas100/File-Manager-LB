/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Laimonas Beniušis
 */
public class ActionFile {
    public Path[] paths;
    public ActionFile(String...strings){
        paths = new Path[2];
        int i =0;
        for(String s:strings){
            paths[i++] = Paths.get(s);
        }
    }
    @Override
    public String toString(){
        String s="";
        for(Path p:paths){
            if(p!=null){
                s+=p+" | ";
            }
        }
        return s;
    }
    
}
