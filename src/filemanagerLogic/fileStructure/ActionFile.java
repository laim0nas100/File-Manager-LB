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
import java.util.Comparator;

/**
 *
 * @author Laimonas Beniušis
 */
public class ActionFile {
    public static final Comparator<ActionFile> COMP_DESCENDING = (ActionFile f1, ActionFile f2) -> {
        return f1.paths[0].compareTo(f2.paths[0]);
    };
    public static final Comparator<ActionFile> COMP_ASCENDING = COMP_DESCENDING.reversed();
    
    
    public Path[] paths;
    public ActionFile(String...strings){
        paths = new Path[strings.length];
        for(int i=0; i<strings.length; i++){
            paths[i] = Paths.get(strings[i]);
        }
    }
    @Override
    public String toString(){
        String s="";
        for(Path p:paths){
            if(p!=null){
                s+=" | "+p;
            }
        }
        return s.substring(3);
    }
    
    public void delete() throws IOException{
        Files.delete(paths[0]);
    }
    
    public static Path toPath(String s){
        return Paths.get(s);
    }
    public void move() throws IOException{
        Files.move(paths[0], paths[1]);
    }
    public void copy() throws IOException{
        Files.copy(paths[0], paths[1]);
    }
    
}
