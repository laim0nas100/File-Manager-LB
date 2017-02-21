/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.snapshots;

import filemanagerLogic.fileStructure.ExtFolder;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import LibraryLB.Log;

/**
 *
 * @author Laimonas Beniušis
 */
public class Snapshot implements Serializable{
    public LinkedHashMap<String,Entry> map;
    public String dateCreated;
    public String folderCreatedFrom;
    public Snapshot(){
    }
    public Snapshot(Map<String,Entry> newMap){
        init();
        newMap.values().forEach(val->{
            this.map.put(val.relativePath, new Entry(val));
        });
    }
    public Snapshot(ExtFolder folder){
        init();
        folder.update();
        this.folderCreatedFrom = folder.getAbsoluteDirectory();
        Log.writeln("Folder size"+folder.files.size());
        folder.getListRecursive().forEach(file ->{
                            
            String relPath = file.relativeFrom(folder.getAbsolutePath());
            Entry entry = new Entry(file,relPath);
            map.put(relPath, entry);
        });
        map.remove(folder.getAbsolutePath());
    }

    private void init(){
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String format = dateFormat.format(date);
        dateCreated = format;
        map = new LinkedHashMap<>();
    }
    public void reEvalueateFolder(String folderPath,ArrayList<Entry> list){
        Log.writeln("Evaluating:"+folderPath);
        if(list == null){
            list = new ArrayList<>();
            for(Entry entry:this.map.values()){
                if(entry.relativePath.startsWith(folderPath)&&!entry.relativePath.equals(folderPath)){
                    list.add(entry);
                }
            }
            this.reEvalueateFolder(folderPath,list);
        }else{
            int nonModifiedCount = 0;
            for(Entry entry:list){
                if(entry.isModified){
                    reEvalueateFolder(entry.relativePath,null);
                    if(!entry.isModified){
                        nonModifiedCount++;                       
                    }
                }else{
                    nonModifiedCount++;
                }
            }
            Log.write(folderPath,nonModifiedCount,list.size());
            if(nonModifiedCount == list.size()){
                map.get(folderPath).isModified = false;
            }
        } 
    }
    
    @Override
    public String toString(){
        String s = "Snapshot ";
        s+=this.dateCreated;
        for(Entry val:map.values()){
            s+= "\n"+val.toString();
        }
        return s;

    }
}
