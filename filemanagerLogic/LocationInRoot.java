/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.util.Arrays;
import java.util.Vector;

/**
 *
 * @author Laimonas Beniušis
 */
public class LocationInRoot {
    public Vector<String> coordinates;
    public LocationInRoot(ExtFolder root,File file) throws Exception{
        coordinates = new Vector<>();
        coordinates.clear();
        while((!file.getAbsolutePath().equals(root.getAbsolutePath()))){
            coordinates.add(0,file.getName());
            file = file.getParentFile();
            if(file == null){
                throw new Exception("Error");
            }
        }
    }
    public LocationInRoot(String filePath){
        coordinates = new Vector<>();
        String[] fileArray = filePath.split("\\"+File.separatorChar);
        coordinates.addAll(Arrays.asList(fileArray));
    }
    
    public LocationInRoot(LocationInRoot loc){
        this.coordinates = (Vector<String>) loc.coordinates.clone();
    }
    private LocationInRoot(Vector<String> coord){
        coordinates = new Vector<>();
        coordinates.addAll(coord);
        
    }
    public String getName(){
        if(this.coordinates.size()>0){
            return this.coordinates.get(this.coordinates.size()-1);
        }else{
            return null;
        }
    }
    public void setName(String name){
        this.coordinates.set((this.coordinates.size()-1),name);
    }
    public int length(){
        return this.coordinates.size();
    }
    public String at(int i){
        return this.coordinates.get(i);
    }
    public LocationInRoot getParentLocation(){
        Vector<String> list = new Vector<>();
        list.addAll(this.coordinates);
        list.remove(this.length()-1);
        return new LocationInRoot(list);
    }
    @Override
    public String toString(){
        String str ="";
        for(String s:this.coordinates){
            str+=s+",";
        }
        return "<"+str+">";
    }
    
    //SOME COMENT
    
}
