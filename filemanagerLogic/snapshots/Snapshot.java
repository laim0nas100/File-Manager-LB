/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.snapshots;

import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.Serializable;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import utility.Log;

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
        Log.writeln("Folder size"+folder.files.size());
        folder.getListRecursive().forEach(file ->{
            String relPath = TaskFactory.resolveRelativePath(file, folder);
            map.put(relPath, new Entry(file,relPath));
        });
        map.remove(folder.getAbsolutePath());

        folderCreatedFrom = folder.getAbsoluteDirectory();
    }

    private void init(){
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String format = dateFormat.format(date);
        dateCreated = format;
        folderCreatedFrom = "";
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
                if(entry.isModified.get()){
                    reEvalueateFolder(entry.relativePath,null);
                    if(!entry.isModified.get()){
                        nonModifiedCount++;                       
                    }
                }else{
                    nonModifiedCount++;
                }
            }
            Log.write(folderPath," ",nonModifiedCount,"  ",list.size());
            if(nonModifiedCount == list.size()){
                map.get(folderPath).isModified.set(false);
            }
        }
       
        
    }
    
    @Override
    public String toString(){
        String s = "Snapshot of: ";
        s+=this.folderCreatedFrom+" ";
        s+=this.dateCreated;
        for(Entry val:map.values()){
            s+= "\n"+val.toString();
        }
        return s;

    }
}
