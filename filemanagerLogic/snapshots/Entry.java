/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.snapshots;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.fileStructure.ExtFile;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Laimonas Beniušis
 */
public class Entry{
        public SimpleBooleanProperty isMissing;
        public SimpleBooleanProperty isModified;
        public SimpleBooleanProperty isNew;
        public SimpleBooleanProperty isOlder;
        public SimpleBooleanProperty isBigger;

        public long lastModified;
        public long size;
        public String relativePath;
        public String absolutePath;
        public Entry(){}
        public Entry(Entry oldEntry){
            size = oldEntry.size;
            lastModified = oldEntry.lastModified;
            relativePath = oldEntry.relativePath;
            absolutePath = oldEntry.absolutePath;
            isModified = oldEntry.isModified;
            isNew = oldEntry.isNew;
            isMissing = oldEntry.isMissing;
            isOlder = oldEntry.isOlder;
            isBigger = oldEntry.isBigger;
        }
        public Entry(ExtFile file,String relPath){
            size = file.length();
            lastModified = file.lastModified();
            relativePath = relPath;
            absolutePath = file.getAbsolutePath();
            isModified = new SimpleBooleanProperty();
            isNew = new SimpleBooleanProperty();
            isMissing = new SimpleBooleanProperty();
            isOlder = new SimpleBooleanProperty();
            isBigger = new SimpleBooleanProperty();
        }
        @Override
        public String toString(){
            String s="";
            s+= new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(lastModified))) +"\t" +relativePath +"\t "+(double)size/FileManagerLB.DATA_SIZE.KB.size;
            if(isNew.get()){
                s+=" new";
            }else if(isMissing.get()){
                s+=" missing";
            }else if(isModified.get()){
                s+=" modified";
                if(isOlder.get()){
                    s+= " older";
                }else{
                    s+= " newer";
                }
                if(isBigger.get()){
                    s+= " bigger";
                }else{
                    s+= " smaller";
                }
            }
            return s;
        }
    }