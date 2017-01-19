
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerLogic.Enums.Identity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
            this.pointsToDirectory = Files.isDirectory(path);
        }catch(Exception x){}
        
    }
    public String getTargetDir() throws IOException {
        return Files.readSymbolicLink(this.toPath()).toString();
    }
    
    public boolean isPointsToDirectory() {
        return pointsToDirectory;
    }
    
    @Override
    public Identity getIdentity(){
        return Identity.LINK;
    }
}
