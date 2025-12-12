package lt.lb.filemanagerlb.logic.snapshots;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lt.lb.filemanagerlb.logic.filestructure.ActionFile;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 *
 * @author Laimonas Beniušis
 */
public class ExtEntry extends Entry {

    public SimpleStringProperty action;
    public SimpleBooleanProperty actionCompleted;
    public SimpleIntegerProperty actionType;
    public SimpleStringProperty date;
    public String path1;
    public String path2;

    public ExtEntry(Entry entry) {
        super(entry);
        action = new SimpleStringProperty("(0) No Action");
        actionCompleted = new SimpleBooleanProperty(false);
        actionType = new SimpleIntegerProperty(0);
        date = new SimpleStringProperty(FastDateFormat.getInstance("YYYY-MM-dd HH:mm:ss").format(lastModified));
    }

    public static String getActionDescription(int act) {
        //Action Types
        //0 - no Action
        //1 - copy to source
        //2 - copy to compared
        //3 - delete from source
        //4 - delete from compared
        switch (act) {
            case (1): {
                return ("(1) Copy from compared to source");
            }
            case (2): {
                return ("(2) Copy from source to compared");
            }
            case (3): {
                return ("(3) Delete from source");
            }
            case (4): {
                return ("(4) Delete from compared");
            }
            default: {
                return ("(0) No Action");
            }
        }
    }

    public void setAction(int act) {
        if (act < 0 || act > 4) {
            act = 0;
        }
        if (actionType.get() == act) {
            return;//no change
        }
        this.actionType.set(act);
        this.action.set(getActionDescription(act));

    }


    public static String fullActionDescription(ExtEntry entry, ActionFile file) {
        int act = entry.actionType.get();
        switch (act) {
            case 0:
                return "No action";
            case 1:
                if (!file.assertPathCount(2)) {
                    return "Invalid action";
                }
                return "Copy " + file.paths[1] + " to " + file.paths[0];

            case 2:
                if (!file.assertPathCount(2)) {
                    return "Invalid action";
                }
                return "Copy " + file.paths[0] + " to " + file.paths[1];
            case 3:
                if (file.paths[0] == null) {
                    return "Invalid action";
                }
                return "Delete " + file.paths[0];
            case 4:
                if (file.paths[1] == null) {
                    return "Invalid action";
                }
                return "Delete " + file.paths[1];

            default:
                return "Invalid action";
        }

    }

    @Override
    public String toString() {
        return super.toString() + " " + getActionDescription(actionType.get());
    }
}
