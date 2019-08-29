package lt.lb.filemanagerlb.gui;

import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.logic.Enums;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lt.lb.commons.Log;
import lt.lb.commons.containers.collections.ParametersMap;
import lt.lb.commons.io.AutoBackupMaker;
import lt.lb.commons.io.FileReader;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FavouriteLink;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB {

    public static final String HOME_DIR = System.getProperty("user.dir") + File.separator;
    public static final String VIRTUAL_FOLDERS_DIR = HOME_DIR + "VIRTUAL_FOLDERS" + File.separator;
    public static final String ARTIFICIAL_ROOT_DIR = HOME_DIR + "ARTIFICIAL_ROOT";
    public static String USER_DIR = HOME_DIR;
    public static String ROOT_NAME = "ROOT";
    public static int MAX_THREADS_FOR_TASK = 10;
    public static VirtualFolder ArtificialRoot;// = new VirtualFolder(ARTIFICIAL_ROOT_DIR);
    public static VirtualFolder VirtualFolders;// = new VirtualFolder(VIRTUAL_FOLDERS_DIR);
    public static String logPath;
    public static int DEPTH = 1;
    public static SimpleBooleanProperty DEBUG = new SimpleBooleanProperty(false);
    public static int LogBackupCount = 1;
    public static SimpleBooleanProperty useBufferedFileStreams = new SimpleBooleanProperty(true);
    public static ParametersMap parameters;
    public static PathStringCommands customPath = new PathStringCommands(HOME_DIR);
    public static ObservableList<ExtPath> remountUpdateList = FXCollections.observableArrayList();


    public static void main(String[] args) {
        lt.lb.commons.javafx.scenemanagement.MultiStageManager sm = new MultiStageManager();
        System.err.println("STARTING");
        reInit();
        if (DEBUG.not().get()) {
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
        }
        
    }

    public static void remount() {
        remountUpdateList.clear();
//        remountUpdateList.add(VirtualFolders);

        ArtificialRoot.files.put(VirtualFolders.propertyName.get(), VirtualFolders);
        for (ExtPath f : ArtificialRoot.getFilesCollection()) {
            if (!Files.isDirectory(f.toPath()) && !f.isVirtual.get()) {
                ArtificialRoot.files.remove(f.propertyName.get());
            } else {
                remountUpdateList.add(f);
            }
        }

        File[] roots = File.listRoots();
        for (File root : roots) {
            mountDevice(root.getAbsolutePath());
        }
        remountUpdateList.setAll(ArtificialRoot.getFilesCollection());
    }

    public static boolean mountDevice(String name) {
        boolean result = false;
        name = name.toUpperCase();
        Log.print("Mount: " + name);
        Path path = Paths.get(name);
        if (Files.isDirectory(path)) {
            ExtFolder device = new ExtFolder(name);
            int nameCount = path.getNameCount();
            if (nameCount == 0) {
                result = true;
                String newName = path.getRoot().toString();
                device.propertyName.set(newName);
                if (!ArtificialRoot.files.containsKey(newName)) {
                    ArtificialRoot.files.put(newName, device);
                    if (!remountUpdateList.contains(device)) {
                        remountUpdateList.add(device);
                    }

                } else {
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean folderIsVirtual(ExtPath fileToCheck) {
        ExtFolder baseFolder = FileManagerLB.VirtualFolders;
        HashSet<String> set = new HashSet<>();
        for (ExtPath file : baseFolder.files.values()) {
            set.add(file.getAbsoluteDirectory());
        }
        return set.contains(fileToCheck.getAbsoluteDirectory());
    }

    public static Set<String> getRootSet() {
        return ArtificialRoot.files.keySet();
    }

    public static void doOnExit() {
        Log.print("Exit call invoked");
        ViewManager.getInstance().closeAllFramesNoExit();
        try {

//            lt.lb.commons.FileManaging.FileReader.writeToFile(USER_DIR+"Log.txt", Log.getInstance().list);
            AutoBackupMaker BM = new AutoBackupMaker(LogBackupCount, USER_DIR + "BUP", "YYYY-MM-dd HH.mm.ss");
            Log.close();
            Collection<Runnable> makeNewCopy = BM.makeNewCopy(logPath);
            makeNewCopy.forEach(th -> {
                th.run();
            });
            BM.cleanUp().run();
            Files.delete(Paths.get(logPath));

        } catch (Exception ex) {
            ErrorReport.report(ex);
        }

    }

    public static void reInit() {
        Log.print("INITIALIZE");
        ViewManager.getInstance().closeAllFramesNoExit();
        MediaPlayerController.VLCfound = false;
        MainController.actionList = new ArrayList<>();
        MainController.dragList = FXCollections.observableArrayList();
        MainController.errorLog = FXCollections.observableArrayList();
        MainController.links = FXCollections.observableArrayList();
        MainController.markedList = FXCollections.observableArrayList();
        MainController.propertyMarkedSize = Bindings.size(MainController.markedList);
        ArtificialRoot = new VirtualFolder(ARTIFICIAL_ROOT_DIR);
        VirtualFolders = new VirtualFolder(VIRTUAL_FOLDERS_DIR);
        ArtificialRoot.setIsAbsoluteRoot(true);
        CommandWindowController.executor.stopEverything();
        CommandWindowController.executor.setRunnerSize(0);
        readParameters();
        logPath = USER_DIR + Log.getZonedDateTime("HH-MM-ss") + " Log.txt";
        try {
            Path userdir = Paths.get(USER_DIR);
            if (!Files.isDirectory(userdir)) {
                Files.createDirectories(userdir);
            }
            Log.changeStream(Log.LogStream.FILE, logPath);
            Log.main().stackTrace = true;
            Logger logger = LoggerFactory.getLogger("MainLogger");
            Consumer<Supplier<String>> sl4jConsumer = (Supplier<String> str)->{
                logger.debug(str.get());
            };
//            Log.override = sl4jCosnsumer;
//            Log.flushBuffer();
        } catch (Exception e) {
            ErrorReport.report(e);
        }
        Log.print("Before start executor");
        CommandWindowController.executor.setRunnerSize(CommandWindowController.maxExecutablesAtOnce);
        Log.print("After start executor");
        ArtificialRoot.propertyName.set(ROOT_NAME);
        MainController.links.add(new FavouriteLink(ROOT_NAME, ArtificialRoot));
        ViewManager.getInstance().newWindow(ArtificialRoot);
        Log.print("After new window");
        //Create directories
        try {
            Files.createDirectories(Paths.get(FileManagerLB.USER_DIR + MediaPlayerController.PLAYLIST_DIR));
        } catch (Exception e) {
            ErrorReport.report(e);
        }

    }

    public static void readParameters() {
        ArrayDeque<String> list = new ArrayDeque<>();
        try {
            list.addAll(FileReader.readFromFile(HOME_DIR + "Parameters.txt", "//", "/*", "*/"));
        } catch (Exception e) {
            ErrorReport.report(e);
        }
        parameters = new ParametersMap(list, "=");
        Log.print("Parameters", parameters);

        DEBUG.set(parameters.defaultGet("debug", true));
        DEPTH = parameters.defaultGet("lookDepth", 2);
        LogBackupCount = parameters.defaultGet("logBackupCount", 5);
        ROOT_NAME = parameters.defaultGet("ROOT_NAME", ROOT_NAME);
        MAX_THREADS_FOR_TASK = parameters.defaultGet("maxThreadsForTask", TaskFactory.PROCESSOR_COUNT);
        USER_DIR = new PathStringCommands(parameters.defaultGet("userDir", HOME_DIR)).getPath() + File.separator;
        FileManagerLB.useBufferedFileStreams.setValue(parameters.defaultGet("bufferedFileStreams", true));
        VirtualFolder.VIRTUAL_FOLDER_PREFIX = parameters.defaultGet("virtualPrefix", "V");
        MediaPlayerController.VLC_SEARCH_PATH = new PathStringCommands(parameters.defaultGet("vlcPath", HOME_DIR + "lib")).getPath() + File.separator;
        PathStringCommands.number = parameters.defaultGet("filter.number", "#");
        PathStringCommands.fileName = parameters.defaultGet("filter.name", "<n>");
        PathStringCommands.nameNoExt = parameters.defaultGet("filter.nameNoExtension", "<nne>");
        PathStringCommands.filePath = parameters.defaultGet("filter.path", "<ap>");
        PathStringCommands.extension = parameters.defaultGet("filter.nameExtension", "<ne>");
        PathStringCommands.parent1 = parameters.defaultGet("filter.parent1", "<p1>");
        PathStringCommands.parent2 = parameters.defaultGet("filter.parent2", "<p2>");
        PathStringCommands.custom = parameters.defaultGet("filter.custom", "<c>");
        PathStringCommands.relativeCustom = parameters.defaultGet("filter.relativeCustom", "<rc>");
        CommandWindowController.commandInit = parameters.defaultGet("code.init", "init");
        CommandWindowController.truncateAfter = parameters.defaultGet("code.truncateAfter", 100000);
        CommandWindowController.maxExecutablesAtOnce = parameters.defaultGet("code.maxExecutables", 2);
        CommandWindowController.commandGenerate = parameters.defaultGet("code.commandGenerate", "generate");
        CommandWindowController.commandApply = parameters.defaultGet("code.commandApply", "apply");
        CommandWindowController.commandClear = parameters.defaultGet("code.clear", "clear");
        CommandWindowController.commandCancel = parameters.defaultGet("code.cancel", "cancel");
        CommandWindowController.commandList = parameters.defaultGet("code.list", "list");
        CommandWindowController.commandListRec = parameters.defaultGet("code.listRec", "listRec");
        CommandWindowController.commandSetCustom = parameters.defaultGet("code.setCustom", "setCustom");
        CommandWindowController.commandHelp = parameters.defaultGet("code.help", "help");
        CommandWindowController.commandListParams = parameters.defaultGet("code.listParameters", "listParams");
        CommandWindowController.maxExecutablesAtOnce = parameters.defaultGet("code.maxThreadsForCommand", TaskFactory.PROCESSOR_COUNT);
        CommandWindowController.commandCopyFolderStructure = parameters.defaultGet("code.copyFolderStructure", "copyStructure");

    }

    public static void restart() {
        try {
            Log.print("Restart request");
            FileManagerLB.doOnExit();
            System.err.println("Restart request");//Message to parent process
            Thread.sleep(10000);
            System.err.println("Failed to respond");
            System.err.println("Terminating");
        } catch (InterruptedException ex) {
        }
        System.exit(707);
    }

}
