package lt.lb.filemanagerlb.gui;

import org.tinylog.Logger;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

/**
 *
 * @author Lemmin
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

    public static MediaPlayerFactory getFactory() throws VLCException {
        if (factoryInstance != null) {
            return factoryInstance;
        }
        discover();
        factoryInstance = new MediaPlayerFactory();

        return factoryInstance;
    }

    public static class VLCException extends RuntimeException {

        public VLCException(String str) {
            super(str);
        }
    }

    public static void discover() throws VLCException {
        if (!VLCfound) {
            MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory();
            mediaPlayerFactory.release();
            VLCfound = true;
            /*
            NativeDiscoveryStrategy[] array = new NativeDiscoveryStrategy[]{
                new WindowsNativeDiscoveryStrategy(),
                new LinuxNativeDiscoveryStrategy(),
                new OsxNativeDiscoveryStrategy()
            };
            int supportedOS = -1;
            for (int i = 0; i < array.length; i++) {
                if (array[i].supported()) {
                    supportedOS = i;
                    break;
                }
            }
            switch (supportedOS) {
                case 0: {
                    array[supportedOS] = new WindowsNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 1: {
                    array[supportedOS] = new LinuxNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 2: {
                    array[supportedOS] = new OsxNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                default: {
                    //unsupported OS?
                }
            }
            if (supportedOS != -1) {
                VLCfound = new NativeDiscovery(array[supportedOS]).discover();
            }
             */
            if (VLCfound) {
                Logger.info(RuntimeUtil.getLibVlcLibraryName());
            } else {
                throw new VLCException("Could not locate VLC, \n configure vlcPath in Parameters.txt");
            }
        }

    }

}
