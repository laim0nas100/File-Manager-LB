package lt.lb.filemanagerlb.logic.filestructure;

import java.io.File;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.utility.ErrorReport;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import lt.lb.filemanagerlb.utility.BulkConsumer;

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
        return SafeOpt.ofFuture(populateFolder(true, null, null))
                .peekError(ErrorReport::report)
                .orElse(ImmutableCollections.mapOf());
    }

    public Collection<ExtPath> getFilesCollection() {
        return getFilesMap().values();
    }

    protected abstract Future<Map<String, ExtPath>> populateFolder(boolean auto, Consumer<ExtPath> buffer, Supplier<Boolean> isCanceled);

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
    public void collectRecursive(Predicate<ExtPath> predicate, BulkConsumer<ExtPath> receiver) {
        List<ExtPath> local = new ArrayList<>();
        if (predicate.test(this)) {
            local.add(this);
        }

        Future<?> update = this.update(path -> {
            if (predicate.test(path)) {
                local.add(path);
            }
            if (path instanceof ExtFolder) {
                //start new local list for each folder
                path.collectRecursive(predicate, receiver);
            }//ExtPath just adds it to the receiver, so we ignore that

        }, null);
        SafeOpt.ofFuture(update)
                .peek(ignored -> {
                    //if no error, just feed to to the receiver
                    receiver.acceptAll(local);
                })
                .peekError(ErrorReport::report)
                .orNull();//await
    }

    @Override
    public void collectLocal(Predicate<ExtPath> predicate, BulkConsumer<ExtPath> receiver) {
        List<ExtPath> local = new ArrayList<>();
        if (predicate.test(this)) {
            local.add(this);
        }

        Future<?> update = this.update(path -> {
            if (predicate.test(path)) {
                local.add(path);
            }

        }, null);
        SafeOpt.ofFuture(update)
                .peek(ignored -> {
                    //if no error, just feed to to the receiver
                    receiver.acceptAll(local);
                })
                .peekError(ErrorReport::report)
                .orNull();//await
    }

    public abstract void update();

    public abstract Future update(Consumer<ExtPath> receiver, Supplier<Boolean> isCanceled);

    @Override
    public String getAbsoluteDirectory() {
        if (isAbsoluteRoot.get()) {
            return D.ROOT_NAME;
        }
        return this.getAbsolutePath() + File.separator;
    }
}
