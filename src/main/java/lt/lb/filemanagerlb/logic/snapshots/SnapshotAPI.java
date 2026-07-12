/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.logic.snapshots;

import java.util.Iterator;
import java.util.LinkedHashMap;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;

/**
 *
 * @author laim0nas100
 */
public class SnapshotAPI {

    private final static SnapshotAPI INSTANCE = new SnapshotAPI();

    public static SnapshotAPI getInstance() {
        return INSTANCE;
    }

    public static Snapshot getEmptySnapshot() {
        Snapshot sn = new Snapshot();
        sn.dateCreated = "";
        sn.map = new LinkedHashMap<>();
        return sn;
    }

    public static Snapshot createSnapshot(ExtFolder folder) {
        return new Snapshot(folder);
    }

    public static Snapshot compareSnapshots(Snapshot s1, Snapshot s2) {
        LinkedHashMap<String, Entry> map1 = s1.map;
        LinkedHashMap<String, Entry> map2 = s2.map;
        map1.values().stream().forEach(entry -> {
            Entry get = map2.getOrDefault(entry.relativePath, null);
            if (get == null) {
                entry.isNew = true;
            } else {
                entry.ageCmp = Long.compare(entry.lastModified, get.lastModified);
                entry.sizeCmp = Long.compare(entry.size, get.size);
                entry.isModified = entry.ageCmp != 0 || entry.sizeCmp != 0;
            }
        });
        map2.values().forEach(entry -> {
            if (!map1.containsKey(entry.relativePath)) {
                Entry newEntry = new Entry(entry);
                newEntry.isMissing = true;
                map1.put(newEntry.relativePath, newEntry);
            }
        });
        return new Snapshot(map1);
    }

    public static Snapshot getOnlyDifferences(Snapshot s1) {
        Snapshot newS = new Snapshot(s1.map);
        newS.dateCreated = s1.dateCreated;
        Iterator<Entry> iterator = newS.map.values().iterator();
        while (iterator.hasNext()) {
            Entry next = iterator.next();
            if (!next.isNew && !next.isModified && !next.isMissing) {
                iterator.remove();
            }
        }
        return newS;
    }

    public static void copySnapshot(Snapshot src, Snapshot dest) {
        dest.dateCreated = src.dateCreated;
        dest.map.clear();
        for (Entry entry : src.map.values()) {
            dest.map.put(entry.relativePath, new Entry(entry));
        }
    }
}
