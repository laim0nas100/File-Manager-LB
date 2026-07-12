package lt.lb.filemanagerlb.logic.snapshots;

import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 *
 * @author laim0nas100
 */
public class Entry {

    public boolean isMissing;
    public boolean isModified;
    public boolean isNew;
    public int ageCmp;
    public int sizeCmp;
    public boolean isFolder;

    public long lastModified;
    public long size;
    public String relativePath;
    public String absolutePath;

    public Entry() {
    }

    public Entry(Entry oldEntry) {
        size = oldEntry.size;
        lastModified = oldEntry.lastModified;
        relativePath = oldEntry.relativePath;
        absolutePath = oldEntry.absolutePath;
        isModified = oldEntry.isModified;
        isNew = oldEntry.isNew;
        isMissing = oldEntry.isMissing;
        ageCmp = oldEntry.ageCmp;
        sizeCmp = oldEntry.sizeCmp;
        isFolder = oldEntry.isFolder;
    }

    public Entry(ExtPath file, String relPath) {
        size = file.size();
        lastModified = file.lastModified();
        relativePath = relPath;
        absolutePath = file.getAbsolutePath();
        isFolder = file.getIdentity().equals(Enums.Identity.FOLDER);
    }

    @Override
    public String toString() {
        String s = "";
        s += "'" + this.relativePath + "' ";
        s += FastDateFormat.getInstance("YYYY-MM-dd HH:mm:ss").format(lastModified) + "\t " + (double) size / Enums.DATA_SIZE.KB.size;
        if (isNew) {
            s += " new";
        } else if (isMissing) {
            s += " missing";
        } else if (isModified) {
            s += " modified";
            if (ageCmp < 0) {
                s += " older";
            } else if (ageCmp > 0) {
                s += " newer";
            } else {
                s += " same date";
            }

            if (sizeCmp < 0) {
                s += " smaller";
            } else if (sizeCmp > 0) {
                s += " bigger";
            } else {
                s += " same size";
            }
        }
        return s;
    }
}
