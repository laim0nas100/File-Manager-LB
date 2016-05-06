/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.snapshots;

import filemanagerLogic.fileStructure.ExtFile;

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
        public long lastModified;
        public long size;
        public String relativePath;
        public Entry(){}
        public Entry(Entry oldEntry){
            size = oldEntry.size;
            lastModified = oldEntry.lastModified;
            relativePath = oldEntry.relativePath;
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
        }
        @Override
        public String toString(){
            String s="";
            s+= relativePath +" "+size +" "+lastModified;
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
