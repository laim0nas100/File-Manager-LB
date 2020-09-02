/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.logic.filestructure;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.util.Callback;
import lt.lb.commons.func.Lambda;
import lt.lb.commons.Log;
import lt.lb.commons.containers.collections.ObjectBuffer;
import lt.lb.commons.parsing.StringOp;
import lt.lb.commons.threads.Promise;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.FileManagerLB;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.utility.ErrorReport;

/**
 *
 * @author Laimonas Beiušis Extended Folder for custom actions
 */
public class ExtFolder extends ExtPath {

    protected boolean populated;
    public ConcurrentHashMap<String, ExtPath> files;

    public ExtFolder(String src, Object... optional) {
        super(src, optional);
        files = new ConcurrentHashMap<>(1, 0.75f, 2);
        populated = false;

    }

    public Collection<ExtPath> getFilesCollection() {
        return files.values();
    }

    @Override
    public Identity getIdentity() {
        return Identity.FOLDER;
    }

    Lambda.L0R<FutureTask> emptyFuture = Lambda.of(() -> {
        FutureTask t = new FutureTask(() -> null);
        t.run();
        return t;
    });
    private AtomicBoolean populating = new AtomicBoolean(false);
    private AtomicReference<FutureTask> populatingFuture = new AtomicReference<>(emptyFuture.apply());

    protected Future populateFolder(ObjectBuffer buffer, BooleanProperty isCanceled) {

        Callable call = () -> {
            try {
                ArrayList<Promise> promises = new ArrayList<>();
                if (Files.isDirectory(toPath())) {
                    String parent = getAbsoluteDirectory();

                    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(parent))) {
                        for (Path f : dirStream) {
                            if (isCanceled != null) {
                                if (isCanceled.get()) {
                                    Log.print("Canceled form populate");
                                    break;
                                }
                            }

                            new Promise(() -> {
                                if (isCanceled != null) {
                                    if (isCanceled.get()) {
                                        return;
                                    }
                                }
                                final String name = StringOp.replaceOnce(f.toString(), parent, "");
                                final String filePathStr = f.toString();
                                ExtPath file = null;
                                if (Files.exists(f) && !files.containsKey(name)) {
                                    if (Files.isDirectory(f)) {
                                        file = new ExtFolder(filePathStr, f);
                                    } else if (Files.isSymbolicLink(f)) {
                                        file = new ExtLink(filePathStr, f);
                                    } else {
                                        file = new ExtPath(filePathStr, f);
                                    }
                                    files.put(file.propertyName.get(), file);
                                    if (buffer != null) {
                                        buffer.add(file);

                                    }
                                }
                            }).collect(promises).execute(TaskFactory.mainExecutor);

                        }
                    }
                }

                try {
                    new Promise().waitFor(promises).execute(TaskFactory.mainExecutor).get();
                } catch (InterruptedException e) {
                }

            } catch (Exception e) {
                ErrorReport.report(e);
            }
            if (buffer != null) {
                buffer.flush();
            }
            populating.set(false);
            return null;
        };
        if (populating.compareAndSet(false, true)) {
            this.populatingFuture.set(new FutureTask<>(call));
            populatingFuture.get().run();
            populated = true;
        } 
        return populatingFuture.get();

    }

    protected void populateRecursive() {
        populateRecursiveInner(this);
    }

    private void populateRecursiveInner(ExtFolder fold) {
        fold.update();
        Log.print("Iteration " + fold.getAbsoluteDirectory());
        for (ExtFolder folder : fold.getFoldersFromFiles()) {
            fold.files.replace(folder.propertyName.get(), folder);
            folder.populateRecursiveInner(folder);

        }
        fold.populated = true;

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
    public void collectRecursive(Predicate<ExtPath> predicate, Callback<ExtPath, Void> call) {
        this.update();
        super.collectRecursive(predicate, call);
        this.getFilesCollection().forEach(f -> {
            f.collectRecursive(predicate, call);
        });

    }

    public void update() {
        Log.print("Update:" + this.getAbsoluteDirectory());
        if (isAbsoluteRoot.get()) {
            FileManagerLB.remount();
            return;
        }
        if (isPopulated()) {
            for (ExtPath file : getFilesCollection()) {
                if (!Files.exists(file.toPath())) {
                    Log.print(file.getAbsoluteDirectory() + " doesn't exist");
                    files.remove(file.propertyName.get());
                }
            }
        }
        populateFolder(null, null);
    }

    public Future update(ObservableList<ExtPath> list, BooleanProperty isCanceled) {
        Log.print("Update observable:" + this.getAbsoluteDirectory());
        ObjectBuffer<ExtPath> buffer = new ObjectBuffer(list, 5);
        if (isPopulated()) {
            for (ExtPath file : getFilesCollection()) {
                if (!Files.exists(file.toPath())) {
                    Log.print(file.getAbsoluteDirectory() + " doesn't exist");
                    files.remove(file.propertyName.get());
                }
                if (isCanceled.get()) {
                    return CompletableFuture.completedFuture(null);
                }
            }
        }
        buffer.addAll(getFilesCollection());
        return populateFolder(buffer, isCanceled);
    }

    public ExtPath getIgnoreCase(String name) {
        if (hasFileIgnoreCase(name)) {
            String request = getKey(name);
            return files.get(request);
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
        for (String key : files.keySet()) {
            if (name.equalsIgnoreCase(key)) {
                request = key;
            }
        }
        return request;
    }

    @Override
    public String getAbsoluteDirectory() {
        if (isAbsoluteRoot.get()) {
            return D.ROOT_NAME;
        }
        return this.getAbsolutePath() + File.separator;
    }

    public boolean isPopulated() {
        return populated;

    }

    public void setPopulated(boolean populated) {
        this.populated = populated;
    }
}
