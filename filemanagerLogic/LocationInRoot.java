/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerGUI.FileManagerLB;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import utility.Log;

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
                if(StringUtils.contains(filePath, s)){
                    rootLoc = s;
                    break;
                }
            }
            coordinates.add(rootLoc);
            if(!filePath.equals(rootLoc)){
                filePath = StringUtils.replaceOnce(filePath, rootLoc, "");
                String[] fileArray = StringUtils.split(filePath, File.separatorChar);
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
        return "<"+str+">";
    }
    
}
