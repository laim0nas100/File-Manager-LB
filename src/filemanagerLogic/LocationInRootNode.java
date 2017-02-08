/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import LibraryLB.Log;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author Lemmin
 */
public class LocationInRootNode {
    public HashMap<String,LocationInRootNode> leafs;
    public String self;
    
    public LocationInRootNode(String self){
        this.self = self;
        this.leafs = new HashMap<>();
    }
    public void add(LocationInRoot loc){
        LocationInRootNode currentNode = this;
        while(!loc.coordinates.isEmpty()){
//            Log.write(loc.coordinates);
            String key = loc.coordinates.pollFirst();
            if(currentNode.leafs.containsKey(key)){
//                Log.write("Contains");
                currentNode = currentNode.leafs.get(key);
            }else{
//                Log.write("Create new");
                LocationInRootNode newNode = new LocationInRootNode(key);
                currentNode.leafs.put(key, newNode);
                currentNode = newNode;
            }
        }

    }
    @Override
    public String toString(){
        String res = "";
        if(!leafs.values().isEmpty()){
            res+="\\";
        }
        res+=""+self+"";
        if(!leafs.values().isEmpty()){
            res+="\n";
        }
        for(LocationInRootNode node:leafs.values()){
            res += node.toString();
        }if(!leafs.values().isEmpty()){
            res+="/\\";
        }
        res+="\n";
        
        return res;
    }
    public String specialString(){
        String res = "";
        for(LocationInRootNode node:this.leafs.values()){
            res+=node.toString();
        }
        return res;
    }
    public ArrayList<String> resolve(String parentPath,boolean includeFolders){
        ArrayList<String> list = new ArrayList<>();
        String path = parentPath+this.self;
        if(this.leafs.isEmpty()){
            list.add(path);
        }else if(includeFolders){
            list.add(path);
        }
        if(!path.endsWith(File.separator)&&path.length()>0){
            path+=File.separator;
        }
        for(LocationInRootNode node:this.leafs.values()){
            list.addAll(node.resolve(path,includeFolders));
        }
        return list;
    }
    public static Collection<String> createFromFile(Collection<String> lines){
        ArrayList<String> result = new ArrayList<>();
        ArrayDeque<String> folderStack = new ArrayDeque<>();
        String folderStart = "\\";
        String folderEnd="/\\";
        for(String line:lines){
            if(line.startsWith(folderStart)){
                line = line.substring(1);
                folderStack.addLast(line);
            }else if(line.startsWith(folderEnd)){
                folderStack.pollLast();
            }else{
                //resolve path
                String path ="";
                for(String pathPart:folderStack){
                    if(!pathPart.endsWith(File.separator)){
                        pathPart+=File.separator;
                    }
                    path+=pathPart;
                }
                path+=line;
                result.add(path);
            }
        }
        return result;
        
    }
}
