package lt.lb.filemanagerlb;

import org.apache.commons.lang3.StringUtils;
import org.tinylog.Logger;
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider;

/**
 *
 * @author Lemmin
 */
public class VLCDirectoryProvider implements DiscoveryDirectoryProvider {

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String[] directories() {
        Logger.info("vlc path called");
        if (StringUtils.isNotBlank(VLCInit.VLC_SEARCH_PATH)) {
            return new String[]{VLCInit.VLC_SEARCH_PATH};
        }
        return new String[]{};
    }

    @Override
    public boolean supported() {
        return true;
    }

}
