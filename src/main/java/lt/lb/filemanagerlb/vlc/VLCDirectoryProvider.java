package lt.lb.filemanagerlb.vlc;

import java.util.ArrayList;
import java.util.List;
import lt.lb.commons.iteration.streams.MakeStream;
import org.apache.commons.lang3.StringUtils;
import org.tinylog.Logger;
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider;
import uk.co.caprica.vlcj.factory.discovery.provider.LinuxWellKnownDirectoryProvider;
import uk.co.caprica.vlcj.factory.discovery.provider.MacOsWellKnownDirectoryProvider;
import uk.co.caprica.vlcj.factory.discovery.provider.SystemPathDirectoryProvider;
import uk.co.caprica.vlcj.factory.discovery.provider.WindowsInstallDirectoryProvider;

/**
 *
 * @author laim0nas100
 */
public class VLCDirectoryProvider implements DiscoveryDirectoryProvider {

    private List<DiscoveryDirectoryProvider> discovery = MakeStream
            .fromValues(
                    new WindowsInstallDirectoryProvider(),
                    new LinuxWellKnownDirectoryProvider(),
                    new MacOsWellKnownDirectoryProvider(),
                    new SystemPathDirectoryProvider()
            )
            .filter(p -> p.supported())
            .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
            .toList();

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String[] directories() {
        Logger.info("vlc path called");
        ArrayList<String> dirs = new ArrayList<>();

        if (StringUtils.isNotBlank(VLCInit.VLC_SEARCH_PATH)) {
            dirs.add(VLCInit.VLC_SEARCH_PATH);
        }
        for (DiscoveryDirectoryProvider disc : discovery) {
            String[] directories = disc.directories();
            for (String dir : directories) {
                dirs.add(dir);
            }
        }

        return dirs.toArray(s -> new String[s]);
    }

    @Override
    public boolean supported() {
        return true;
    }

}
