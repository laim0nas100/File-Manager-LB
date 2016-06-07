/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerGUI.FileManagerLB;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import utility.ExtStringUtils;

/**
 * Location Mapping Class
 * Use in LocationAPI
 * @author Laimonas Beniušis
 */
public class LocationInRoot {

    public LinkedList<String> coordinates;
    
    
    public LocationInRoot(String filePath){
        coordinates = new LinkedList<>();
        String rootLoc = "";
        if(!filePath.isEmpty()){
            ArrayList<String> roots = new ArrayList<>();
            FileManagerLB.getRootSet().forEach(root ->{
                roots.add(root);
            });
            for(String s:roots){
                if(ExtStringUtils.contains(filePath, s)){
                    rootLoc = s;
                    break;
                }
                
            }
            rootLoc = rootLoc.toUpperCase();
            coordinates.add(rootLoc);
            if(!filePath.toUpperCase().equals(rootLoc)){
                filePath = ExtStringUtils.replaceOnce(filePath, rootLoc, "");
                String[] fileArray = ExtStringUtils.split(filePath, File.separatorChar);
                List<String> asList = Arrays.asList(fileArray);
                ArrayList<String> list = new ArrayList<>();
                list.addAll(asList);
                
                Iterator<String> iterator = list.iterator();
                while(iterator.hasNext()){
                    String next = iterator.next();
                    if(next.isEmpty()){
                        iterator.remove();
                    }
                }
                coordinates.addAll(list);
            }
            //Log.writeln(coordinates);
        }
    }
    public LocationInRoot(LocationInRoot loc){
        this.coordinates.addAll(loc.coordinates);
    }
    private LocationInRoot(List<String> coord){
        coordinates = new LinkedList<>();
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
    public LocationInRoot getRoot(){
        LinkedList<String> list = new LinkedList<>();
        list.add(this.coordinates.getFirst());
        list.add(this.coordinates.get(1));
        return new LocationInRoot(list);
    }
    public int length(){
        return this.coordinates.size();
    }
    public String at(int i){
        return this.coordinates.get(i);
    }
    public LocationInRoot getParentLocation(){
        LinkedList<String> list = new LinkedList<>();
        list.addAll(this.coordinates);
        list.removeLast();
        return new LocationInRoot(list);
    }
    @Override
    public String toString(){
        String str ="";
        for(String s:this.coordinates){
            str+=s+",";
        }
        return "<"+str.substring(0, str.length()-1)+">";
    }
    
}
