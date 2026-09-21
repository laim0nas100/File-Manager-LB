package lt.lb.filemanagerlb.logic.filestructure;

/**
 *
 * @author laim0nas100
 */
public class PathState {

    public boolean disabled;

    public static final PathState DEFAULT = new PathState();

    public boolean sameAsDefault() {
        return this.equals(DEFAULT);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.disabled ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PathState other = (PathState) obj;
        return this.disabled == other.disabled;
    }

}
