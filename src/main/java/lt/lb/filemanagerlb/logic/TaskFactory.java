package lt.lb.filemanagerlb.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lt.lb.commons.containers.values.DoubleValue;
import lt.lb.commons.javafx.*;
import lt.lb.commons.threads.executors.FastWaitingExecutor;
import lt.lb.commons.threads.executors.TaskPooler;
import lt.lb.commons.threads.executors.layers.NestedTaskSubmitionExecutorLayer;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.MainController;
import lt.lb.filemanagerlb.gui.ViewManager;
import lt.lb.filemanagerlb.gui.dialog.DuplicateFinderController;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.logic.filestructure.ActionFile;
import lt.lb.filemanagerlb.logic.snapshots.ExtEntry;
import lt.lb.filemanagerlb.logic.snapshots.Snapshot;
import lt.lb.filemanagerlb.logic.snapshots.SnapshotAPI;
import lt.lb.filemanagerlb.utility.ContinousCombinedTask;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.ExtStringUtils;
import lt.lb.filemanagerlb.utility.FileNameException;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import lt.lb.filemanagerlb.utility.SimpleTask;
import lt.lb.jobsystem.ScheduledJobExecutor;
import org.tinylog.Logger;

/**
 *
 * @author Laimonas Beniušis Produces Tasks
 */
//
public class TaskFactory {

    public static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
    private static final HashSet<Character> illegalCharacters = new HashSet<>();
    private static final TaskFactory INSTANCE = new TaskFactory();
//    private static final FastWaitingExecutor innerExe = new FastWaitingExecutor(Math.max(PROCESSOR_COUNT * 5, 10), WaitTime.ofSeconds(120));
//    public static final Executor mainExecutor = new NestedTaskSubmitionExecutorLayer(innerExe);
    public static final ScheduledJobExecutor jobsExecutor = new ScheduledJobExecutor(D.exe);
    public static String dragInitWindowID = "";

    public static TaskFactory getInstance() {

        return INSTANCE;
    }

    protected TaskFactory() {
        D.exe.setMainService("MAIN");
        D.exe.setService("MAIN", () -> {
            FastWaitingExecutor exe = new FastWaitingExecutor(Math.max(PROCESSOR_COUNT * 5, 10), WaitTime.ofSeconds(120));
            return new NestedTaskSubmitionExecutorLayer(exe);
        });
        Character[] arrayWindows = new Character[]{
            '\\',
            '/',
            '<',
            '*',
            '>',
            '|',
            '?',
            ':',
            '\"'
        };
        if (File.separator.equals('/')) {
            illegalCharacters.add('/');
        } else {
            illegalCharacters.addAll(Arrays.asList(arrayWindows));
        }
    }

//RENAME
    public String renameTo(String fileToRename, String newName, String fallbackName) throws IOException, FileNameException {
        for (Character c : newName.toCharArray()) {
            if (illegalCharacters.contains(c)) {
                throw new FileNameException(newName + " contains illegal character " + c);
            }
        }
        LocationInRoot location = new LocationInRoot(fileToRename);

        ExtPath file = LocationAPI.getInstance().getFileIfExists(location);
        if (file == null) {
            throw new FileNameException(fileToRename + " was not found");
        }
        ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileIfExists(location.getParentLocation());
        String path1 = file.getAbsolutePath();
        String path2 = parent.getAbsoluteDirectory() + newName;
        String fPath = parent.getAbsoluteDirectory() + fallbackName;
        LocationAPI.getInstance().removeByLocation(location);
        Logger.info("Rename: " + path1 + " New Name:" + newName + " Fallback:" + fallbackName);
        if (path1.equalsIgnoreCase(path2)) {
            Files.move(Paths.get(path1), Paths.get(fPath));
            Files.move(Paths.get(fPath), Paths.get(path2));
        } else {
            Files.move(Paths.get(path1), Paths.get(path2));
        }
        return path2;
    }

//PREPARE FOR TASKS
    public void addToMarked(ExtPath file) {
        FX.submit(() -> {
            if (file != null && !MainController.markedList.contains(file)) {
                MainController.markedList.add(file);
            }
        });
    }

    public Collection<String> populateStringFileList(Collection<ExtPath> filelist) {
        Collection<String> collection = FXCollections.observableArrayList();
        filelist.forEach(item -> {
            collection.add(item.getAbsoluteDirectory());
        });
        return collection;
    }

    private ArrayList<ActionFile> prepareForCopy(Collection<ExtPath> fileList, ExtPath dest) {
        Logger.info("List recieved in task");

        for (ExtPath file : fileList) {
            Logger.info(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for (ExtPath file : fileList) {
            Collection<ExtPath> listRecursive = new ArrayList<>();
            listRecursive.addAll(file.getListRecursive(true));
            ExtPath parentFile = LocationAPI.getInstance().getFileIfExists(file.getMapping().getParentLocation());
            for (ExtPath f : listRecursive) {
                String relativePath = parentFile.relativeTo(f.getAbsoluteDirectory());
                list.add(new ActionFile(f.getAbsoluteDirectory(), dest.getAbsoluteDirectory() + relativePath));
            }
        }
        list.sort(ActionFile.COMP_DESCENDING);
        Logger.info("List after computing");
        for (ActionFile array1 : list) {
            Logger.info(array1.paths[0] + " -> " + array1.paths[1]);
        }
        return list;

    }

    private ArrayList<ActionFile> prepareForCopy(Collection<ExtPath> fileList, ExtPath dest, ExtPath root) {
        Logger.info("List recieved in task test");

        for (ExtPath file : fileList) {
            Logger.info(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for (ExtPath file : fileList) {
            String relativePath = file.relativeFrom(root.getAbsoluteDirectory());
            ActionFile af = new ActionFile(file.getAbsoluteDirectory(), dest.getAbsoluteDirectory() + relativePath);
            list.add(af);
        }
        list.sort(ActionFile.COMP_DESCENDING);
        Logger.info("List after computing");

        for (ActionFile array1 : list) {
            Logger.info(array1.paths[0] + " -> " + array1.paths[1]);
        }
        return list;

    }

    private ArrayList<ActionFile> prepareForDelete(Collection<ExtPath> fileList) {
        Logger.info("List recieved in task");
        for (ExtPath file : fileList) {
            Logger.info(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for (ExtPath file : fileList) {
            Collection<ExtPath> listRecursive = file.getListRecursive(true);
            for (ExtPath f : listRecursive) {
//                f.path = null;
                list.add(new ActionFile(f.getAbsoluteDirectory()));
            }
        }
        list.sort(ActionFile.COMP_ASCENDING);
        Logger.info("List after computing");
        for (ActionFile file : list) {
            Logger.info(file.toString());
        }
        return list;

    }

    private ArrayList<ActionFile> prepareForMove(Collection<ExtPath> fileList, ExtPath dest) {
        Logger.info("List recieved in task");

        for (ExtPath file : fileList) {
            Logger.info(file.getAbsolutePath());
        }
        ArrayList<ActionFile> list = new ArrayList<>();
        for (ExtPath file : fileList) {
            Collection<ExtPath> listRecursive = file.getListRecursive(true);
            ExtPath parentFile = LocationAPI.getInstance().getFileIfExists(file.getMapping().getParentLocation());
            for (ExtPath f : listRecursive) {
                try {
                    String relativePath = f.relativeFrom(parentFile.getAbsolutePath());
                    ActionFile AF = new ActionFile(f.getAbsoluteDirectory(), dest.getAbsoluteDirectory() + relativePath);
                    list.add(AF);
                } catch (Exception e) {
                    ErrorReport.report(e);
                }
            }

        }
        list.sort(ActionFile.COMP_DESCENDING);
        Logger.info("List after computing");
        for (ActionFile array1 : list) {
            Logger.info(array1.paths[0] + " -> " + array1.paths[1]);
        }
        return list;
    }

//TASKS
    public ContinousCombinedTask copyFilesEx(Collection<ExtPath> fileList, ExtPath dest, ExtPath root) {

        ContinousCombinedTask fullTask = new ContinousCombinedTask() {
            @Override
            protected void preparation() throws Exception {
                List<ActionFile> list;
                if (root == null) {
                    list = prepareForCopy(fileList, dest);
                } else {
                    Logger.info("Test copy");
                    list = prepareForCopy(fileList, dest, root);
                }
                Logger.info("In a task now");
                Logger.info(list);

                for (int i = 0; i < list.size(); i++) {
                    String str;
                    ActionFile file = list.get(i);
                    str = "Source: \t\t" + file.paths[0] + "\n";
                    str += "Destination: \t" + file.paths[1];
                    String strmsg = str;
                    SimpleTask nested = new SimpleTask() {
                        @Override
                        protected Void call() throws Exception {
                            this.updateMessage(strmsg);
                            ExtTask copy = FileUtils.copy(file.paths[0], file.paths[1], D.useBufferedFileStreams.getValue());
                            copy.progress.addListener(FXDefs.numberDiffListener(0.0001d, val -> {
                                FX.submit(() -> {
                                    progressProperty().setValue(val);
                                });
                            }));

                            copy.paused.bind(this.paused);
                            this.canceled.addListener(FXDefs.SimpleChangeListener.of(val -> {
                                copy.cancel(true);
                            }));
                            copy.run();
                            if (copy.failed.get()) {
                                ErrorReport.report(copy.getException());
                            }
                            return null;
                        }
                    ;
                    };
                    nested.setDescription(str);
                    this.addTask(nested);
                }
            }
        };
        return fullTask;
    }

    public ContinousCombinedTask moveFilesEx(Collection<ExtPath> fileList, ExtPath dest) {
        ContinousCombinedTask finalTask = new ContinousCombinedTask() {
            @Override
            protected void preparation() throws Exception {
                ArrayList<ActionFile> leftFolders = new ArrayList<>();
                String str;
                updateMessage("Populating list for move");
                List<ActionFile> list = prepareForMove(fileList, dest);
                updateMessage("Begin");
                int index1 = 0;
                for (; index1 < list.size(); index1++) {

                    ActionFile file = list.get(index1);
                    str = "Source: \t\t" + file.paths[0] + "\n";
                    str += "Destination: \t" + file.paths[1];
                    String msgStr = str;

                    SimpleTask task = new SimpleTask() {
                        @Override
                        protected Void call() throws Exception {
                            updateMessage(msgStr);
                            try {
                                if (Files.isDirectory(file.paths[0])) {
                                    leftFolders.add(file);
                                    Files.createDirectory(file.paths[1]);
                                    Logger.info("Added to folders:" + file.paths[1]);
                                } else {
                                    ExtTask move = FileUtils.move(file.paths[0], file.paths[1], D.useBufferedFileStreams.getValue());
                                    move.progress.addListener(FXDefs.numberDiffListener(0.0001d, val -> {
                                        FX.submit(() -> {
                                            progressProperty().setValue(val);
                                        });
                                    }));
                                    move.paused.bind(paused);
                                    this.canceled.addListener(FXDefs.SimpleChangeListener.of(val -> {
                                        move.cancel(true);
                                    }));
                                    move.run();
                                    if (move.failed.get()) {
                                        ErrorReport.report(move.getException());
                                    }

                                }
                            } catch (Exception e) {
                                ErrorReport.report(e);

                            }
                            return null;
                        }
                    };
                    task.setDescription(msgStr);
                    this.addTask(task);
                }

                SimpleTask deleteTask = new SimpleTask() {
                    @Override
                    protected Void call() throws Exception {
                        updateMessage("Deleting leftover folders");
                        Logger.info("Folders size: " + leftFolders.size());
                        leftFolders.sort(ActionFile.COMP_DESCENDING);
                        for (ActionFile f : leftFolders) {
                            try {
                                Logger.info("Deleting " + f.paths[0]);
                                f.delete();
                            } catch (Exception x) {
                                ErrorReport.report(x);
                            }
                        }
                        return null;
                    }
                };
                deleteTask.setDescription("Delete leftover folders");
                this.addTask(deleteTask);
            }

        };
        return finalTask;
    }

    public ContinousCombinedTask deleteFilesEx(Collection<ExtPath> fileList) {
        ContinousCombinedTask finalTask = new ContinousCombinedTask() {
            @Override
            protected void preparation() throws Exception {
                ArrayList<ActionFile> list = prepareForDelete(fileList);
                for (ActionFile file : list) {
                    SimpleTask deleteTask = new SimpleTask() {
                        @Override
                        protected Void call() throws Exception {
                            try {
                                file.delete();
                            } catch (Exception e) {
                                this.report(e);
                            }
                            return null;
                        }
                    };
                    String str = "Delete: \t" + file.paths[0];
                    deleteTask.setDescription(str);
                    this.addTask(deleteTask);
                }
            }
        ;
        };
        finalTask.setDescription("Delete files");
        return finalTask;
    }

    private Future populateRecursiveParallelInner(ExtFolder folder, int depth, Executor exe) {
        if (0 < depth) {
            Callable task = (Callable) () -> {
                Logger.info("Folder Iteration " + depth + "::" + folder.getAbsoluteDirectory());
                folder.update();
                for (ExtFolder fold : folder.getFoldersFromFiles()) {
                    populateRecursiveParallelInner(fold, depth - 1, exe);
                }
                return null;
            };
            FutureTask t = new FutureTask(task);
            exe.execute(t);
            return t;
        } else {
            FutureTask t = new FutureTask(() -> null);
            t.run();
            return t;
        }
    }

    public Future populateRecursiveParallelContained(ExtFolder folder, int depth) {
        return populateRecursiveParallelInner(folder, depth, D.exe);
    }

    public Runnable populateRecursiveParallel(ExtFolder folder, int depth) {
        TaskPooler pooler = new TaskPooler(PROCESSOR_COUNT);
        populateRecursiveParallelInner(folder, depth, pooler);
        return pooler;
    }

    //MISC
    public static String resolveAvailablePath(ExtFolder folder, String name) {
        String path = folder.getAbsoluteDirectory();
        String newName = name;
        while (folder.hasFileIgnoreCase(newName)) {
            newName = "New " + newName;
        }
        return path + newName;
    }

    public FXTask markFiles(Collection<String> list) {
        return new FXTask() {
            @Override
            protected Void call() {
                list.forEach(file -> {
                    addToMarked(LocationAPI.getInstance().getFileOptimized(file));
                });
                return null;
            }
        };
    }

    public Task<Snapshot> snapshotCreateTask(String folder) {
        return new Task() {
            @Override
            protected Snapshot call() throws Exception {
                return new Snapshot((ExtFolder) LocationAPI.getInstance().getFileAndPopulate(folder));
            }

        };
    }

    public ExtTask snapshotCreateWriteTask(String windowID, ExtFolder folder, File file) {
        return new ExtTask() {
            @Override
            protected Void call() throws Exception {

                TaskFactory.getInstance().populateRecursiveParallelContained(folder, 50);

                ObjectMapper mapper = new ObjectMapper();
                Snapshot currentSnapshot = SnapshotAPI.createSnapshot(folder);

                return FX.submit(() -> {
                    MainController controller = (MainController) ViewManager.getInstance().getController(windowID);
                    controller.snapshotView.getItems().clear();
                    try {
                        mapper.writeValue(file, currentSnapshot);
                        controller.snapshotView.getItems().add("Snapshot:" + file + " created");
                    } catch (IOException ex) {
                        ErrorReport.report(ex);
                        controller.snapshotView.getItems().add("Snapshot:" + file + " failed");
                    }
                    ViewManager.getInstance().updateAllWindows();
                }).get();

            }

        };
    }

    public FXTask snapshotLoadTask(String windowID, ExtFolder folder, File nextSnap) {
        return new FXTask() {
            @Override
            protected Void call() throws Exception {

                MainController frame = (MainController) ViewManager.getInstance().getController(windowID);

                FX.submit(() -> {
                    frame.snapshotView.getItems().clear();
                    frame.snapshotView.getItems().add("Snapshot Loading");
                });
//                    TaskFactory.getInstance().populateRecursiveParallelNew(folder, 50);
                Thread thread = new Thread(TaskFactory.getInstance().populateRecursiveParallel(folder, 50));
                thread.setDaemon(true);
                thread.start();
                thread.join();

                ObjectMapper mapper = new ObjectMapper();
                Snapshot currentSnapshot = SnapshotAPI.createSnapshot(folder);
                Snapshot sn = SnapshotAPI.getEmptySnapshot();
                sn = mapper.readValue(nextSnap, sn.getClass());

                Snapshot result = SnapshotAPI.getOnlyDifferences(SnapshotAPI.compareSnapshots(currentSnapshot, sn));
                ObservableList list = FXCollections.observableArrayList();
                list.addAll(result.map.values());

                frame.snapshotTextDate.setText(sn.dateCreated);
                frame.snapshotTextFolder.setText(sn.folderCreatedFrom);
                frame.snapshotTextDate.setVisible(true);
                frame.snapshotTextFolder.setVisible(true);
                FX.submit(() -> {
                    frame.snapshotView.getItems().clear();
                    if (list.size() > 0) {
                        frame.snapshotView.getItems().addAll(list);
                    } else {
                        frame.snapshotView.getItems().add("No Differences Detected");
                    }
                });

                return null;
            }

        };
    }

    public FXTask syncronizeTask(String folder1, String folder2, Collection<ExtEntry> listFirst) {
        return new FXTask() {
            @Override
            protected Void call() throws InterruptedException {

                int i = 0;
                final int size = listFirst.size();
                Logger.info("List");
                for (ExtEntry e : listFirst) {
                    Logger.info(e.relativePath, "  ", e.action.get());
                }
                //Log.writeln("Size "+size);
                for (ExtEntry entry : listFirst) {
                    while (this.isPaused()) {
                        Thread.sleep(this.getRefreshDuration());
                        if (this.isCancelled()) {
                            break;
                        }
                    }
                    final int current = i;
                    ActionFile actionFile = new ActionFile(folder1 + entry.relativePath, folder2 + entry.relativePath);
                    ExtTask task = actionTask(actionFile, entry);

                    task.progress.addListener(FXDefs.SimpleChangeListener.of(val -> {
                        FX.submit(() -> {
                            updateProgress(current + task.progress.get(), size);
                        });
                    }));
                    task.setOnDone(handle -> {
                        Logger.info("Task done");
                    });
                    task.run();

                    if (task.failed.get()) {
                        Logger.info("Task failed");
                        ErrorReport.report(task.getException());
                    }
//                    try{
//                        action(actionFile,entry);
//                    }catch(Exception e){
//                        ErrorReport.report(e);
//                    }
                    this.updateProgress(++i, size);
                    this.updateMessage(entry.action.get() + "\n" + entry.relativePath);
                }
                return null;

            }
        };
    }

    private ExtTask actionTask(ActionFile action, ExtEntry entry) {
        Logger.info(action);
        final int type = entry.actionType.get();
        DoubleProperty progress = new SimpleDoubleProperty(0);
        ExtTask task = new ExtTask() {
            @Override
            protected Object call() throws Exception {
                switch (type) {

                    case (1): {
                        try {

                            ExtTask t = FileUtils.copy(action.paths[1], action.paths[0], true, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                            t.progress.addListener(FXDefs.SimpleChangeListener.of(val -> {
                                FX.submit(() -> {
                                    progress.setValue(val);
                                });
                            }));

                            t.run();
                            if (t.failed.get()) {
                                ErrorReport.report(t.getException());
                            }
                        } catch (Exception e) {
                            ErrorReport.report(e);

                        }
                        break;
                    }
                    case (2): {
                        try {
                            ExtTask t = FileUtils.copy(action.paths[0], action.paths[1], true, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                            t.progress.addListener(FXDefs.SimpleChangeListener.of(val -> {
                                FX.submit(() -> {
                                    progress.setValue(val);
                                });
                            }));
                            t.run();
                            if (t.failed.get()) {
                                ErrorReport.report(t.getException());
                            }
                        } catch (Exception e) {
                            ErrorReport.report(e);
                        }
                        break;
                    }
                    case (3): {

                        Files.delete(action.paths[0]);
                        break;
                    }
                    case (4): {
                        Files.delete(action.paths[1]);
                        break;
                    }
                    default: {
                        break;
                    }
                }
                entry.actionCompleted.set(true);
                progress.set(1);
                return null;
            }
        };

        return task;
    }

    private void action(ActionFile action, ExtEntry entry) throws Exception {
        Logger.info(action);
        switch (entry.actionType.get()) {

            case (1): {
                try {
                    Files.copy(action.paths[1], action.paths[0], StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (Exception e) {
                    ErrorReport.report(e);
                    if (Files.isDirectory(action.paths[0])) {
//                        Files.setLastModifiedTime(action.paths[0], Files.getLastModifiedTime(action.paths[1]));
                        entry.isModified = false;
                    }

                }
                break;
            }
            case (2): {
                try {
                    Files.copy(action.paths[0], action.paths[1], StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (Exception e) {
                    ErrorReport.report(e);
                    if (Files.isDirectory(action.paths[1])) {
//                        Files.setLastModifiedTime(action.paths[1], Files.getLastModifiedTime(action.paths[0]));
                        entry.isModified = false;
                    }
                }
                break;
            }
            case (3): {

                Files.delete(action.paths[0]);
                break;
            }
            case (4): {
                Files.delete(action.paths[1]);
                break;
            }
            default: {
                break;
            }
        }
        entry.actionCompleted.set(true);
    }

    public FXTask duplicateFinderTask(ArrayList<PathStringCommands> array, double ratio, List list, Map map) {
        FXTaskPooler executor = new FXTaskPooler(D.MAX_THREADS_FOR_TASK, 0);
        for (int i = 0; i < array.size(); i++) {
            ExtTask<Long> task;
            if (map == null) {
                task = duplicateCompareTask(i, array, ratio, list);
            } else {
                task = duplicateCompareTaskLookUp(i, array, ratio, list, map);
            }
            executor.submit(task);
        }
        return executor;

    }

    public ExtTask<Long> duplicateCompareTaskLookUp(int index, ArrayList<PathStringCommands> array, double ratio, List list, Map map) {
        return new ExtTask<Long>() {
            @Override
            public Long call() throws Exception {
                long progress = index;
                PathStringCommands file = array.get(index);
                String name = file.getName(true);
                for (int j = index + 1; j < array.size(); j++) {
                    progress++;
                    if (this.isCancelled()) {
                        return progress;
                    }

                    PathStringCommands file1 = array.get(j);
                    String otherName = file1.getName(true);
                    String key = name + "/$/" + otherName;
                    DuplicateFinderController.SimpleTableItem item;
                    Double rat;
                    if (map.containsKey(key)) {
                        rat = (Double) map.get(key);

                    } else {
                        rat = ExtStringUtils.correlationRatio(name, otherName);
                        map.put(key, rat);
                    }
                    if (rat >= ratio) {
                        item = new DuplicateFinderController.SimpleTableItem(file, file1, rat);
                        list.add(item);
                    }
                }
                return progress;
            }
        ;
    }

    ;

    }
    public ExtTask<Long> duplicateCompareTask(int index, ArrayList<PathStringCommands> array, double ratio, List list) {
        return new ExtTask<Long>() {
            @Override
            public Long call() throws Exception {
                long progress = index;
                PathStringCommands file = array.get(index);
                String name = file.getName(true);
                for (int j = index + 1; j < array.size(); j++) {
                    progress++;
                    if (this.isCancelled()) {
                        return progress;
                    }

                    PathStringCommands file1 = array.get(j);
                    double rat = ExtStringUtils.correlationRatio(name, file1.getName(true));
                    DuplicateFinderController.SimpleTableItem item = new DuplicateFinderController.SimpleTableItem(file, file1, rat);
                    if (rat >= ratio) {
                        list.add(item);
                    }

                }
                return progress;
            }
        ;
    }
;
}
}
