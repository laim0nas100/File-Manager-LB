package lt.lb.filemanagerlb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public HashMap<Serializable, Props<String>> frameInfo = new HashMap<>();
    public List<String> favoriteLinks = new ArrayList<>();
    public Set<String> disabledFiles = new HashSet<>();
}
