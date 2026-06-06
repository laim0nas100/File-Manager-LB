package lt.lb.filemanagerlb;

import org.tinylog.Logger;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

/**
 *
 * @author laim0nas100
 */
public class VLCInit {

    public static String VLC_SEARCH_PATH;
    public static boolean VLCfound = false;
    private static MediaPlayerFactory factoryInstance;
    
    public static void release(){
        VLCfound = false;
        if(factoryInstance != null){
            factoryInstance.release();
            factoryInstance = null;
        }
    }
    
    public static MediaPlayerFactory getFactory(){
        if(factoryInstance == null){
            throw new IllegalStateException("Must initialize VLC first");
        }
        return factoryInstance;
    }

    public static MediaPlayerFactory getOrInitFactory() throws VLCException {
        if (factoryInstance != null) {
            return factoryInstance;
        }
        discover();
        factoryInstance = new MediaPlayerFactory();

        return factoryInstance;
    }

    public static class VLCException extends Exception {

        public VLCException(String str) {
            super(str);
        }
    }

    public static void discover() throws VLCException {
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
