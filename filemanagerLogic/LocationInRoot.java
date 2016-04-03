/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Location Mapping Class
 * Use in LocationAPI
 * @author Laimonas Beniušis
 */
public class LocationInRoot {
    public ArrayList<String> coordinates;

    
    public LocationInRoot(String filePath){
        coordinates = new ArrayList<>();
        if(!filePath.isEmpty()){
            Path path = Paths.get(filePath);
            String rootLoc = path.getRoot().toString();
            filePath = path.toString();
            coordinates.add(rootLoc);
            if(!filePath.equals(rootLoc)){
                filePath = filePath.replace(rootLoc,"");
                String[] fileArray = filePath.split("\\"+File.separatorChar);
                List<String> asList = Arrays.asList(fileArray);
                ArrayList<String> list = new ArrayList<>();
                list.addAll(asList);
                for(int i=list.size()-1; i>=0; i--){
                    if(list.get(i).isEmpty()){
                        list.remove(i);
                    }
                }
                coordinates.addAll(list);
            }
        }
    }
//    public LocationInRoot(String filePath){
//        coordinates = new ArrayList<>();
//        
//            String[] fileArray = filePath.split("\\"+File.separatorChar);
//            List<String> asList = Arrays.asList(fileArray);
//            ArrayList<String> list = new ArrayList<>();
//            list.addAll(asList);
//            for(int i=list.size()-1; i>=0; i--){
//                if(list.get(i).isEmpty()){
//                   list.remove(i);
//                }
//            }
//            //Unix specific
//            if(filePath.startsWith(File.separator)){
//                list.add(0,File.separator);
//            }
//            coordinates.addAll(list);
//        
//    }
    public LocationInRoot(LocationInRoot loc){
        this.coordinates = (ArrayList<String>) loc.coordinates.clone();
    }
    private LocationInRoot(ArrayList<String> coord){
        coordinates = new ArrayList<>();
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
        ArrayList<String> list = new ArrayList<>();
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
    
}
