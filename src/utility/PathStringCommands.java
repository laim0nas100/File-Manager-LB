/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import filemanagerLogic.fileStructure.ExtFile;
import java.io.File;

/**
 *
 * @author lemmin
 */
public class PathStringCommands {
    public static String fileName,nameNoExt,filePath,parent1,parent2,number,custom,relativeCustom,extension;
    private String absolutePath;
    public PathStringCommands(String path){
        absolutePath = path;
        if(absolutePath.endsWith(File.separator)){
            absolutePath = absolutePath.substring(0,absolutePath.length()-1);
        }
    }
    public String getName(boolean extension){
        String name = this.getName(absolutePath);
        
        if(!extension){
            if(name.contains(".")){
                int index = ExtStringUtils.lastIndexOf(name, ".");
                name = name.substring(0,index);
            }
        }
        return name;
    }
    public String getName(String path){
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
            current = this.goUp(current);
            timesToGoUp--;
        }
        return current;
    }
    public String goUp(String current){
        int index = ExtStringUtils.lastIndexOf(current, this.getName(current))-1;
        if(index<0){
            index = 0;
        }
        current = current.substring(0, index);
        if(current.length()==0){
            current = File.separator;
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
