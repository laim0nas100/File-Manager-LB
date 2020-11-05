package lt.lb.filemanagerlb;

import java.util.HashMap;
import lt.lb.commons.containers.values.Props;

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
}
