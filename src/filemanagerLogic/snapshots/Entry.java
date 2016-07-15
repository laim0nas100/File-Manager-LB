/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.snapshots;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums;
import filemanagerLogic.fileStructure.ExtFile;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 *
 * @author Laimonas Beniušis
 */
public class Entry{
        public boolean isMissing;
        public boolean isModified;
        public boolean isNew;
        public boolean isOlder;
        public boolean isBigger;
        public boolean isFolder;
        
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
            isFolder = oldEntry.isFolder;
        }
        public Entry(ExtFile file,String relPath){
            size = file.length();
            lastModified = file.lastModified();
            relativePath = relPath;
            absolutePath = file.getAbsolutePath();
            isFolder = file.getIdentity().equals(Enums.Identity.FOLDER);
        }
        @Override
        public String toString(){
            String s="";
            s+= new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(lastModified))) +"\t" +relativePath +"\t "+(double)size/Enums.DATA_SIZE.KB.size;
            if(isNew){
                s+=" new";
            }else if(isMissing){
                s+=" missing";
            }else if(isModified){
                s+=" modified";
                if(isOlder){
                    s+= " older";
                }else{
                    s+= " newer";
                }
                if(isBigger){
                    s+= " bigger";
                }else{
                    s+= " smaller";
                }
            }
            return s;
        }
    }
