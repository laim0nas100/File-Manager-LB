package lt.lb.filemanagerlb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lt.lb.commons.containers.collections.Props;

/**
 *
 * @author Lemmin
 */
public class SessionInfo {

    public boolean autoCloseProgressDialogs;
    public boolean autoStartProgressDialogs;
    public boolean pinProgressDialogs;
    public boolean pinTextInputDialogs;
    public boolean copyReplaceExisting;

    public HashMap<String, Props<String>> frameInfo = new HashMap<>();
    public List<String> favoriteLinks = new ArrayList<>();
}
