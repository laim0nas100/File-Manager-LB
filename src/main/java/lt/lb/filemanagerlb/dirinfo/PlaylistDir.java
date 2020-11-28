package lt.lb.filemanagerlb.dirinfo;

import lt.lb.commons.io.directoryaccess.Dir;
import lt.lb.commons.io.directoryaccess.Fil;
import lt.lb.commons.io.directoryaccess.FileInfo;

/**
 *
 * @author Lemmin
 */
public class PlaylistDir extends Dir {

    public PlaylistDir(String absolutePath) throws Exception {
        super(absolutePath);
    }

    @FileInfo
    public Fil DEFAULT_PLAYLIST;
}
