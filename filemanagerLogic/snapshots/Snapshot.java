/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.snapshots;

import filemanagerLogic.ExtTask;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
        ArrayList<ExtFile> listRecursive = (ArrayList<ExtFile>) folder.getListRecursive();
        listRecursive.remove(0);
        listRecursive.forEach(file ->{
            String relPath = TaskFactory.resolveRelativePath(file, folder);
            map.put(relPath, new Entry(file,relPath));
        });


        folderCreatedFrom = folder.getAbsolutePath();
    }

    private void init(){
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String format = dateFormat.format(date);
        dateCreated = format;
        folderCreatedFrom = "";
        map = new LinkedHashMap<>();
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
