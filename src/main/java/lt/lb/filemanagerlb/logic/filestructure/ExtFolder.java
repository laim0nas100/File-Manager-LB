package lt.lb.filemanagerlb.logic.filestructure;

import java.io.File;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.util.Callback;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.containers.collections.ObjectBuffer;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.utility.ErrorReport;
import com.github.laim0nas100.uncheckedutils.SafeOpt;

/**
 *
 * @author Laimonas Beiušis Extended Folder for custom actions
 */
public abstract class ExtFolder extends ExtPath {
    
    public ExtFolder(String src, Object... optional) {
        super(src, optional);
    }
    
    public abstract Map<String, ExtPath> getFilesMap();
    
    public Map<String, ExtPath> updateAwait() {
        return SafeOpt.ofFuture(populateFolder(true, null, null)).peekError(ErrorReport::report).orElse(ImmutableCollections.mapOf());
    }
    
    public Collection<ExtPath> getFilesCollection() {
        return getFilesMap().values();
    }
    
    protected abstract Future<Map<String, ExtPath>> populateFolder(boolean auto, ObjectBuffer buffer, Supplier<Boolean> isCanceled);
    
    public ExtPath getIgnoreCase(String name) {
        if (hasFileIgnoreCase(name)) {
            String request = getKey(name);
            return getFilesMap().get(request);
        } else {
            return null;
        }
    }
    
    public boolean hasFileIgnoreCase(String name) {
        String key = getKey(name);
        return !key.isEmpty();
    }
    
    public String getKey(String name) {
        String request = "";
        for (String key : getFilesMap().keySet()) {
            if (name.equalsIgnoreCase(key)) {
                request = key;
            }
        }
        return request;
    }
    
    public Collection<ExtFolder> getFoldersFromFiles() {
        ArrayDeque<ExtFolder> folders = new ArrayDeque<>();
        for (ExtPath file : getFilesCollection()) {
            if (file.getIdentity().equals(Identity.FOLDER)) {
                folders.add((ExtFolder) file);
            }
        }
        return folders;
    }
    
    @Override
    public Collection<ExtPath> getListRecursive(Predicate<ExtPath> predicate) {
        Collection<ExtPath> listRecursive = this.getListRecursive(false);
        Iterator<ExtPath> iterator = listRecursive.iterator();
        while (iterator.hasNext()) {
            ExtPath path = iterator.next();
            if (!predicate.test(path)) {
                iterator.remove();
            }
        }
        return listRecursive;
    }
    
    @Override
    public Collection<ExtPath> getListRecursive(boolean applyDisable) {
        ArrayDeque<ExtPath> list = new ArrayDeque<>();
        list.add(this);
        getRootList(list, this);
        if (applyDisable) {
            Iterator<ExtPath> iterator = list.iterator();
            while (iterator.hasNext()) {
                ExtPath next = iterator.next();
                if (next.isDisabled.get()) {
                    iterator.remove();
                }
            }
        }
        return list;
    }
    
    public Collection<ExtPath> getListRecursiveFolders(boolean applyDisable) {
        Collection<ExtPath> listRecursive = this.getListRecursive(applyDisable);
        Iterator<ExtPath> iterator = listRecursive.iterator();
        while (iterator.hasNext()) {
            ExtPath next = iterator.next();
            if (!next.getIdentity().equals(Enums.Identity.FOLDER)) {
                iterator.remove();
            }
        }
        return listRecursive;
    }
    
    private void getRootList(Collection<ExtPath> list, ExtFolder folder) {
        folder.update();
//        if(!folder.isDisabled.get()){
        list.addAll(folder.getFilesCollection());
        folder.getFoldersFromFiles().forEach(fold -> {
            getRootList(list, fold);
        });
//        }
    }
    
    @Override
    public void collectRecursive(Predicate<ExtPath> predicate, Consumer<ExtPath> call) {
        this.update();
        super.collectRecursive(predicate, call);
        this.getFilesCollection().forEach(f -> {
            f.collectRecursive(predicate, call);
        });
        
    }
    
    public abstract void update();
    
    public abstract Future update(List<ExtPath> receiver, Supplier<Boolean> isCanceled);
    
    @Override
    public String getAbsoluteDirectory() {
        if (isAbsoluteRoot.get()) {
            return D.ROOT_NAME;
        }
        return this.getAbsolutePath() + File.separator;
    }
}
