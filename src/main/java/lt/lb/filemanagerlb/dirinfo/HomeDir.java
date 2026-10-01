package lt.lb.filemanagerlb.dirinfo;

import com.github.laim0nas100.commonslb.io.directoryaccess.Dir;
import com.github.laim0nas100.commonslb.io.directoryaccess.Fil;
import com.github.laim0nas100.commonslb.io.directoryaccess.FileInfo;

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

    @FileInfo(extension = "xml")
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
