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

    public HashMap<String, Props> position = new HashMap<>();
    public HashMap<String, Props> size = new HashMap<>();
    public List<String> favoriteLinks = new ArrayList<>();
}
