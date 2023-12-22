
package lt.lb.filemanagerlb.logic.filestructure;

import lt.lb.filemanagerlb.logic.Enums.Identity;
import java.io.IOException;
import java.nio.file.Files;

/**
 *
 * @author Laimonas Beniušis
 */
public class ExtLink extends ExtPath{
    
    public ExtLink(String link,Object...optional){
        super(link,optional);
        
    }
    public String getTargetDir() throws IOException {
        return Files.readSymbolicLink(this.toPath()).toString();
    }
    
    public boolean isPointsToDirectory() {
        return Files.isDirectory(this.toPath());
    }
    
    @Override
    public Identity getIdentity(){
        return Identity.LINK;
    }
}
