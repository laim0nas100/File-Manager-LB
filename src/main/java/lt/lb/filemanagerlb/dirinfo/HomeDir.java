package lt.lb.filemanagerlb.dirinfo;

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

    @FileInfo
    public Dir COMMAND_WINDOW_LOGS;

    @FileInfo
    public Dir SCRIPTS;

    @FileInfo(extension = "yaml")
    public Fil session_info;
    
    @FileInfo(extension ="properties")
    public Fil Parameters;

    public static class PlaylistDir extends Dir {

        public PlaylistDir(String absolutePath) throws Exception {
            super(absolutePath);
        }

        @FileInfo
        public Fil DEFAULT_PLAYLIST;
    }

}
