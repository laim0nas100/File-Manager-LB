/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Lemmin
 */
public class ExtPath {
    public Path path;
    public long size = -1;
    public long lastModified = -1;
    public ExtPath(String str){
        this.path = Paths.get(str);
        
    }
}
