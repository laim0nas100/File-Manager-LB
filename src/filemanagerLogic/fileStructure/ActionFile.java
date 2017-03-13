/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Laimonas Beniušis
 */
public class ActionFile {
    public String[] paths;
    public ActionFile(String...strings){
        paths = strings;
    }
    @Override
    public String toString(){
        String s="";
        for(String p:paths){
            if(p!=null){
                s+=p+" | ";
            }
        }
        return s;
    }
    
    public void delete() throws IOException{
        Files.delete(Paths.get(paths[0]));
    }
    
    public static Path toPath(String s){
        return Paths.get(s);
    }
    public void move() throws IOException{
        Files.move(toPath(paths[0]), toPath(paths[1]));
    }
    public void copy() throws IOException{
        Files.copy(toPath(paths[0]), toPath(paths[1]));
    }
    
}
