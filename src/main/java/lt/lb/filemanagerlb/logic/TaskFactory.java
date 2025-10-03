package lt.lb.filemanagerlb.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lt.lb.commons.io.CopyOptions;
import lt.lb.commons.io.autopath.AutoPath;
import lt.lb.commons.javafx.*;
import lt.lb.commons.threads.Futures;
import lt.lb.commons.threads.executors.layers.NestedTaskSubmitionExecutorLayer;
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

    private static final HashSet<Character> illegalCharacters = new HashSet<>();
    private static final TaskFactory INSTANCE = new TaskFactory();

    public final ScheduledJobExecutor jobsExecutor;
    public final SimpleBooleanProperty copyReplaceExisting = new SimpleBooleanProperty(false);
    public static String dragInitWindowID = "";

    public static TaskFactory getInstance() {

        return INSTANCE;
    }

    protected TaskFactory() {
        jobsExecutor = new ScheduledJobExecutor(D.exe.service("jobs"));
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

    public static void assertLegalName(String newName) throws FileNameException {
        for (Character c : newName.toCharArray()) {
            if (illegalCharacters.contains(c)) {
                throw new FileNameException(newName + " contains illegal character " + c);
            }
        }
    }

//RENAME
    public String renameTo(String fileToRename, String newName) throws IOException, FileNameException {
        assertLegalName(newName);

        AutoPath toRename = AutoPath.fs(fileToRename);

        if (!Files.exists(toRename.toPath())) {
            throw new FileNameException(fileToRename + " was not found");
        }

        String parent = toRename.getParent();
        AutoPath newFile = AutoPath.fs(parent, newName);

        Logger.info("Rename: " + toRename + " New Name:" + newFile);
        if (toRename.getName().equalsIgnoreCase(newFile.getName())) {
            AutoPath fallback = AutoPath.fs(parent, newName + System.currentTimeMillis());
            Files.move(toRename.toPath(), fallback.toPath());
            Files.move(fallback.toPath(), newFile.toPath());
        } else {
            Files.move(toRename.toPath(), newFile.toPath());
        }
        return newFile.getStringPath();
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
            ExtPath parentFile = LocationAPI.getInstance().getPathIfExists(file.getMapping().getParentLocation());
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

    private CopyOptions getCopyOptions() {
        CopyOptions options = new CopyOptions();
        if (copyReplaceExisting.get()) {
            options = options.with(StandardCopyOption.REPLACE_EXISTING);
        }
        if (D.useBufferedFileStreams.get()) {
            options = options.withStreams();
        }
        options = options.with(StandardCopyOption.COPY_ATTRIBUTES);
        return options;
    }

    private CopyOptions getMoveOptions() {
        CopyOptions options = new CopyOptions();
        if (copyReplaceExisting.get()) {
            options = options.with(StandardCopyOption.REPLACE_EXISTING);
        }
        if (D.useBufferedFileStreams.get()) {
            options = options.withStreams();
        } else {
            options = options.with(StandardCopyOption.ATOMIC_MOVE);
        }
        return options;
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
            ExtPath parentFile = LocationAPI.getInstance().getPathIfExists(file.getMapping().getParentLocation());
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

                            ExtTask copy = FileUtils.copy(file.paths[0], file.paths[1], getCopyOptions());
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
                                    ExtTask move = FileUtils.move(file.paths[0], file.paths[1], getMoveOptions());
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

    //MISC
    public static AutoPath resolveAvailablePath(ExtFolder folder, String name) {
        String path = folder.getAbsoluteDirectory();
        String newName = name;
        int index = 1;
        while (folder.hasFileIgnoreCase(newName)) {
            newName = name+"("+index+")";
            index++;
        }
        return AutoPath.fs(path,newName);
    }

    public FXTask markFiles(Collection<String> list) {
        return new FXTask() {
            @Override
            protected Void call() {
                list.forEach(file -> {
                    addToMarked(LocationAPI.getInstance().getPathNearest(file));
                });
                return null;
            }
        };
    }

    public SimpleTask<Snapshot> snapshotCreateTask(String folder) {
        return new SimpleTask() {
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

    public ContinousCombinedTask syncronizeTask(String folder1, String folder2, Collection<ExtEntry> listFirst) {
        return new ContinousCombinedTask() {
            @Override
            protected void preparation() throws Exception {
                for (ExtEntry entry : listFirst) {
                    ActionFile actionFile = new ActionFile(folder1 + entry.relativePath, folder2 + entry.relativePath);
                    SimpleTask task = actionTask(actionFile, entry);
                    addTask(task);
                }

            }

//            @Override
//            protected Void call() throws InterruptedException {
//
//                int i = 0;
//                final int size = listFirst.size();
//                Logger.info("List");
//                for (ExtEntry e : listFirst) {
//                    Logger.info(e.relativePath, "  ", e.action.get());
//                }
//                //Log.writeln("Size "+size);
//                for (ExtEntry entry : listFirst) {
//                    if (conditionalWaitOrExit()) {
//                        return null;
//                    }
//                    final int current = i;
//                    ActionFile actionFile = new ActionFile(folder1 + entry.relativePath, folder2 + entry.relativePath);
//                    ExtTask task = actionTask(actionFile, entry);
//
//                    task.progress.addListener(FXDefs.SimpleChangeListener.of(val -> {
//                        FX.submit(() -> {
//                            updateProgress(current + task.progress.get(), size);
//                        });
//                    }));
//                    task.setOnDone(handle -> {
//                        Logger.info("Task done");
//                    });
//                    task.run();
//
//                    if (task.failed.get()) {
//                        Logger.info("Task failed");
//                        ErrorReport.report(task.getException());
//                    }
        
    

    ////                    try{
////                        action(actionFile,entry);
////                    }catch(Exception e){
////                        ErrorReport.report(e);
////                    }
//                    this.updateProgress(++i, size);
//                    this.updateMessage(entry.action.get() + "\n" + entry.relativePath);
//                }
//                return null;
//
//            }

        };
    }

    private SimpleTask actionTask(ActionFile action, ExtEntry entry) {
        Logger.info(action);
        final int type = entry.actionType.get();
        CopyOptions options = getCopyOptions().with(StandardCopyOption.REPLACE_EXISTING);
        SimpleTask task = new SimpleTask() {
            @Override
            protected Object call() throws Exception {
                switch (type) {

                    case (1): {
                        try {

                            ExtTask t = FileUtils.copy(action.paths[1], action.paths[0], options);
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
                            ExtTask t = FileUtils.copy(action.paths[0], action.paths[1], options);
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
                long prog = index;
                PathStringCommands file = array.get(index);
                String name = file.getName(true);
                for (int j = index + 1; j < array.size(); j++) {
                    prog++;
                    if (this.isCancelled()) {
                        return prog;
                    }

                    PathStringCommands file1 = array.get(j);
                    String otherName = file1.getName(true);
                    String key = name + "/$/" + otherName;
                    DuplicateFinderController.SimpleTableItem item;
                    Double rat;
                    if (map.containsKey(key)) {
                        rat = (Double) map.get(key);

                    } else {
                        rat = ExtStringUtils.fuzzyScore(name, otherName);
                        map.put(key, rat);
                    }
                    if (rat >= ratio) {
                        item = new DuplicateFinderController.SimpleTableItem(file, file1, rat);
                        list.add(item);
                    }
                }
                return prog;
            }
        };
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
                    double rat = ExtStringUtils.fuzzyScore(name, file1.getName(true));
                    DuplicateFinderController.SimpleTableItem item = new DuplicateFinderController.SimpleTableItem(file, file1, rat);
                    if (rat >= ratio) {
                        list.add(item);
                    }

                }
                return progress;
            }
        };
    }
}
