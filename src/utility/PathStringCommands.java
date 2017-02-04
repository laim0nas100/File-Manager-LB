/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import filemanagerLogic.fileStructure.ExtPath;
import java.io.File;
import java.util.Objects;

/**
 *
 * @author Laimonas Beniušis
 */
public class PathStringCommands {
    public static String fileName,nameNoExt,filePath,parent1,parent2,number,custom,relativeCustom,extension;
    private String absolutePath;
    @Override
    public boolean equals(Object e){
        boolean eq = false;
        
        if((e!=null) && (e instanceof PathStringCommands)){
            PathStringCommands ob = (PathStringCommands) e;
            eq = ob.absolutePath.equals(this.absolutePath);
        }
        return eq;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.absolutePath);
        return hash;
    }
    public PathStringCommands(String path){
        absolutePath = path;
        if(absolutePath.endsWith(File.separator)){
            absolutePath = absolutePath.substring(0,absolutePath.length()-1);
        }
    }
    public String getName(boolean extension){
        String name = PathStringCommands.getName(absolutePath);
        
        if(!extension){
            if(name.contains(".")){
                int index = ExtStringUtils.lastIndexOf(name, ".");
                name = name.substring(0,index);
            }
        }
        return name;
    }
    public static String getName(String path){
        if(path.endsWith(File.separator)){
            path = path.substring(0,path.length()-1);
        }
        int index = ExtStringUtils.lastIndexOf(path, File.separator)+1;
        path = path.substring(index);
        return path;
    }
    public String getExtension(){
        String name = this.getName(true);
        if(name.contains(".")){
            int index = ExtStringUtils.lastIndexOf(name, ".")-1;
            if(index>=0){
                name = name.substring(index);
            }else{
                return "";
            }
        }else{
            return "";
        }
        return name;
    }
    public String getParent(int timesToGoUp){
        String current = this.absolutePath;
        while(timesToGoUp>0){
            current = PathStringCommands.goUp(current);
            timesToGoUp--;
        }
        return current;
    }
    public static String goUp(String current){
        int index = Math.max(ExtStringUtils.lastIndexOf(current, PathStringCommands.getName(current))-1,0);
        current = current.substring(0, index);
        if(!ExtStringUtils.contains(current, File.separator)){
            current+=File.separator;
        } 
        return current;
       
    }
    public String relativeFrom(String possibleParent){
        String path = absolutePath+File.separator;
        if(!path.contains(possibleParent) || path.equalsIgnoreCase(possibleParent)){
            return absolutePath;
        }else{
            return ExtStringUtils.replaceOnce(path, possibleParent, "");
        }
    }
    public String relativeTo(String possibleChild){
        String path = absolutePath+File.separator;
        if(!possibleChild.contains(path) || possibleChild.equalsIgnoreCase(path)){
            return absolutePath;
        }else{
            return ExtStringUtils.replaceOnce(possibleChild, path, "");
        }
    }
    public String getPath(){
        return absolutePath;
    }
    public void setPath(String path){
        this.absolutePath = path;
    }
    public static String[] returnDefinedKeys(){
        return new String[]{fileName,nameNoExt,filePath,parent1,parent2,number,custom,relativeCustom,extension};
    }
    
}
