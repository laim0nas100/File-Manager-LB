package lt.lb.filemanagerlb.logic.filestructure;

import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lt.lb.commons.containers.collections.ObjectBuffer;
import lt.lb.commons.javafx.FXTask;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.FileManagerLB;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import org.tinylog.Logger;

/**
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolder extends ExtFolder {
    
    public static String VIRTUAL_FOLDER_PREFIX = "V";
    
    public ConcurrentHashMap<String, ExtPath> files;
    
    public static void createVirtualFolder() {
        int index = 0;
        String name = VIRTUAL_FOLDER_PREFIX + index;
        while (FileManagerLB.VirtualFolders.files.containsKey(name)) {
            index += 1;
            name = VIRTUAL_FOLDER_PREFIX + index;
        }
        VirtualFolder VF = new VirtualFolder(D.VIRTUAL_FOLDERS_DIR + name);
        FileManagerLB.VirtualFolders.files.put(name, VF);
    }
    
    public VirtualFolder(String src) {
        super(src);
        files = new ConcurrentHashMap<>(16, 0.75f, 2);
    }
    
    @Override
    public void update() {
        update(FXCollections.observableArrayList(), null);
    }
    
    @Override
    public Future update(List<ExtPath> receiver, BooleanProperty isCanceled) {
        
        if (this.equals(FileManagerLB.VirtualFolders)) {
            receiver.clear();
            receiver.addAll(getFilesCollection());
            return CompletableFuture.completedFuture(null);
        }
        
        if (isAbsoluteRoot.get()) {
            receiver.clear();
            Logger.info("Start update");
            FileManagerLB.remount();
            receiver.addAll(FileManagerLB.remountUpdateList);
            Logger.info("End update");
            
        } else {
            Iterator<ExtPath> iter = this.getFilesCollection().iterator();
            while (iter.hasNext()) {
                if (!Files.exists(iter.next().toPath())) {
                    iter.remove();
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public Collection<ExtPath> getListRecursive(boolean applyDisable) {
        ArrayList<ExtPath> listRecursive = new ArrayList(super.getListRecursive(applyDisable));
        listRecursive.remove(0);
        return listRecursive;
    }
    
    @Override
    public Enums.Identity getIdentity() {
        return Identity.VIRTUAL;
    }
    
    public void add(ExtPath file) {
        String name = file.getName(true);
        if (!files.containsKey(name)) {
            files.put(name, file);
        }
    }
    
    public void addAll(Collection<ExtPath> list) {
        list.forEach(item -> {
            add(item);
        });
    }
    
    @Override
    public String getAbsoluteDirectory() {
        return this.propertyName.get();
    }
    
    @Override
    protected Future<Map<String, ExtPath>> populateFolder(boolean auto, ObjectBuffer buffer, BooleanProperty isCanceled) {
        if (buffer != null) {
            buffer.addAll(getFilesCollection());
        }
        return CompletableFuture.completedFuture(null);
        
    }
    
    @Override
    public Map<String, ExtPath> getFilesMap() {
        return files;
    }
    
}
