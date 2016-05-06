
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.ArrayList;
import utility.DesktopApi;

/**
 *
 * @author Laimonas Beniušis
 */
public class ExtLink extends ExtFile{
    private boolean pointsToDirectory;

    
    public ExtLink(String link){
        super(link);
        try{
            Path path = Files.readSymbolicLink(this.toPath());
            if(Files.isDirectory(path)){
                this.pointsToDirectory = true;
            }else{
                this.pointsToDirectory = false;
            }
        }catch(Exception x){}
        
    }
    public String getTargetDir() throws IOException {
        return Files.readSymbolicLink(this.toPath()).toString();
    }
    
    public boolean isPointsToDirectory() {
        return pointsToDirectory;
    }
    
    @Override
    public ExtLink getTrueForm(){
        return this;
    }
    @Override
    public String getIdentity(){
        return "link";
    }
}
