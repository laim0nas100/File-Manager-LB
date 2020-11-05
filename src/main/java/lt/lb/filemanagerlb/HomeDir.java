package lt.lb.filemanagerlb;

import lt.lb.commons.io.directoryaccess.Dir;
import lt.lb.commons.io.directoryaccess.Fil;
import lt.lb.commons.io.directoryaccess.FileInfo;

/**
 *
 * @author Lemmin
 */
public class HomeDir extends Dir {
    
    public HomeDir(String absolutePath) throws Exception {
        super(absolutePath);
    }
    
    @FileInfo
    public PlaylistDir PLAYLISTS;
    
    @FileInfo(extension = "yaml")
    public Fil session_info;
    
}
