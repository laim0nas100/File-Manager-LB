package lt.lb.filemanagerlb.vlc;

import org.tinylog.Logger;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

/**
 *
 * @author laim0nas100
 */
public class VLCInit {

    public static String VLC_SEARCH_PATH;
    public static boolean VLCfound = false;
    private static MediaPlayerFactory factoryInstance;

    public static synchronized void release() {
        VLCfound = false;
        if (factoryInstance != null) {
            factoryInstance.release();
            factoryInstance = null;
        }
    }

    public static MediaPlayerFactory getFactory() {
        if (factoryInstance == null) {
            throw new IllegalStateException("Must initialize VLC first");
        }
        return factoryInstance;
    }

    public static synchronized MediaPlayerFactory getOrInitFactory() throws VLCException {
        if (factoryInstance != null) {
            return factoryInstance;
        }
        discover();
        //warm up
        factoryInstance = new MediaPlayerFactory();
        MediaPlayer mediaPlayer = factoryInstance.mediaPlayers().newMediaPlayer();
        mediaPlayer.media().prepare("");
        mediaPlayer.release();

        return factoryInstance;
    }

    public static synchronized void discover() throws VLCException {
        if (!VLCfound) {
            VLCfound = new NativeDiscovery().discover();

            if (VLCfound) {
                Logger.info(RuntimeUtil.getLibVlcLibraryName());
            } else {
                throw new VLCException("Could not locate VLC, \n configure vlcPath in Parameters.txt");
            }
        }

    }

}
