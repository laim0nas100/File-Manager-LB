/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerLogic.fileStructure.ExtFile;
import java.nio.file.Path;

/**
 *
 * @author lemmin
 */
public interface Movable {
    public Path getDestination();
    public void setDestination(Path path);
    public String getRelativePath();
    public void setRelativePath(ExtFile file);
}
