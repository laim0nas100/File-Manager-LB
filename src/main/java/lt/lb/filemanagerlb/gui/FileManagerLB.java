package lt.lb.filemanagerlb.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lt.lb.TolerantConfig;
import lt.lb.commons.containers.collections.CollectionOp;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.commons.javafx.scenemanagement.frames.FrameState;
import lt.lb.commons.javafx.scenemanagement.frames.WithDecoration;
import lt.lb.commons.javafx.scenemanagement.frames.WithFrameTypeMemoryPosition;
import lt.lb.commons.javafx.scenemanagement.frames.WithFrameTypeMemorySize;
import lt.lb.commons.javafx.scenemanagement.frames.WithIcon;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.P;
import lt.lb.filemanagerlb.SessionInfo;
import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FavouriteLink;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import lt.lb.uncheckedutils.Checked;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB {

    public static ObservableList<ExtPath> remountUpdateList = FXCollections.observableArrayList();
    public static VirtualFolder ArtificialRoot;// = new VirtualFolder(ARTIFICIAL_ROOT_DIR);
    public static VirtualFolder VirtualFolders;// = new VirtualFolder(VIRTUAL_FOLDERS_DIR);
    public static WithFrameTypeMemoryPosition positionInfo = new WithFrameTypeMemoryPosition();
    public static WithFrameTypeMemorySize sizeInfo = new WithFrameTypeMemorySize();

    public static Yaml yaml;

    static {
        java.util.logging.LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    public static boolean init = false;

    public static void main(String[] args) {
        D.sm = new MultiStageManager(
                D.cLoader,
                positionInfo,
                sizeInfo,
                new WithIcon(new Image(D.cLoader.getResourceAsStream("images/ico.png"))),
                new WithDecoration(FrameState.FrameStateClose.instance, d -> {
                    if (!init && D.sm.getAllControllers(MainController.class).count() == 0) {
                        init = true;
                        FX.submit(() -> {
                            ErrorReport.with(() -> {
                                Stream<MyBaseController> allControllers = D.sm.getAllControllers(MyBaseController.class);
                                allControllers.filter(f -> !f.getFrameID().equals(d.getID())).forEach(c -> c.exit());

                                FileManagerLB.doOnExit();
                                System.exit(0);
                            });
                            init = false;
                        });

                    }
                })
        );

        Logger.info("Manifest");

        Checked.checkedRun(() -> {
            URL res = D.cLoader.getResource("stamped/version.txt");
            ArrayList<String> lines = lt.lb.commons.io.TextFileIO.readFrom(res);
            Logger.info("LINES");
            Logger.info(() -> lines.stream().collect(Collectors.joining("\n")));
        }).ifPresent(ErrorReport::report);
        Checked.checkedRun(() -> {
            reInit();
        }).ifPresent(ErrorReport::report);

        if (D.DEBUG.not().get()) {
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

    public static <T> T yamlRead(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return yaml.load(reader);
        }
    }

    public static <T> void yamlWrite(Path path, T item) throws IOException {
        try (BufferedWriter newBufferedWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            yaml.dump(item, newBufferedWriter);
        }

    }

    public static boolean mountDevice(String name) {
        boolean result = false;
        name = name.toUpperCase();
        Logger.info("Mount: " + name);
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
        Logger.info("Exit call invoked");
        D.sm.getFrames().forEach(frame -> frame.close());
        try {
            writeYaml();
//            lt.lb.commons.FileManaging.FileReader.writeToFile(USER_DIR+"Log.txt", Log.getInstance().list);
//            AutoBackupMaker BM = new AutoBackupMaker(D.LogBackupCount, D.USER_DIR + "BUP", "YYYY-MM-dd HH.mm.ss");
//            Collection<Runnable> makeNewCopy = BM.makeNewCopy(D.logPath);
//            makeNewCopy.forEach(th -> {
//                th.run();
//            });
//            BM.cleanUp().run();
//            Files.delete(Paths.get(D.logPath));
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }

    }

    public static void writeYaml() throws IOException {
        SessionInfo si = D.sessionInfo;
        CollectionOp.replace(si.position, positionInfo.memoryMap);
        CollectionOp.replace(si.size, sizeInfo.memoryMap);
        CollectionOp.replace(si.favoriteLinks,
                MainController.favoriteLinks.stream()
                        .map(m -> m.location)
                        .filter(f -> !f.isNotWriteable())
                        .map(m -> m.getAbsolutePath())
                        .distinct()
                        .collect(Collectors.toList())
        );
        ViewManager vm = ViewManager.getInstance();
        si.autoCloseProgressDialogs = vm.autoCloseProgressDialogs.get();
        si.autoStartProgressDialogs = vm.autoStartProgressDialogs.get();
        si.pinProgressDialogs = vm.pinProgressDialogs.get();
        si.pinTextInputDialogs = vm.pinTextInputDialogs.get();

        yamlWrite(D.HOME_DIR.session_info.getPath(), si);
    }

    public static void readYaml() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);

        if (D.HOME_DIR.session_info.isReadable()) {
            D.sessionInfo = yamlRead(D.HOME_DIR.session_info.getPath());
        }

        sizeInfo.memoryMap.putAll(D.sessionInfo.size);
        positionInfo.memoryMap.putAll(D.sessionInfo.position);
        ViewManager vm = ViewManager.getInstance();
        vm.autoCloseProgressDialogs.set(D.sessionInfo.autoCloseProgressDialogs);
        vm.autoStartProgressDialogs.set(D.sessionInfo.autoStartProgressDialogs);
        vm.pinProgressDialogs.set(D.sessionInfo.pinProgressDialogs);
        vm.pinTextInputDialogs.set(D.sessionInfo.pinTextInputDialogs);
        for (String str : D.sessionInfo.favoriteLinks) {
            MainController.favoriteLinks.add(new FavouriteLink(str));
        }
    }

    public static void reInit() throws IOException {
        Logger.info("INITIALIZE");
        init = true;
        D.sm.getFrames().forEach(frame -> frame.close());

        MediaPlayerController.VLCfound = false;
        MainController.actionList = new ArrayList<>();
        MainController.dragList = FXCollections.observableArrayList();
        MainController.errorLog = FXCollections.observableArrayList();
        MainController.favoriteLinks = FXCollections.observableArrayList();

        MainController.markedList = FXCollections.observableArrayList();
        MainController.propertyMarkedSize = Bindings.size(MainController.markedList);
        ArtificialRoot = new VirtualFolder(D.ARTIFICIAL_ROOT_DIR);
        VirtualFolders = new VirtualFolder(D.VIRTUAL_FOLDERS_DIR);
        ArtificialRoot.setIsAbsoluteRoot(true);
        CommandWindowController.executor.stopEverything();
        CommandWindowController.executor.setRunnerSize(0);
        readParameters();
        try {
            Path userdir = Paths.get(D.USER_DIR);
            if (!Files.isDirectory(userdir)) {
                Files.createDirectories(userdir);
            }
        } catch (Exception e) {
            ErrorReport.report(e);
        }
        Logger.info("Before start executor");
        CommandWindowController.executor.setRunnerSize(CommandWindowController.maxExecutablesAtOnce);
        Logger.info("After start executor");
        ArtificialRoot.propertyName.set(D.ROOT_NAME);
        MainController.favoriteLinks.add(new FavouriteLink(D.ROOT_NAME, ArtificialRoot));
        readYaml();
        ViewManager.getInstance().newWindow(ArtificialRoot);
        Logger.info("After new window");

        init = false;

    }
    
    public static void readParameters() {
        P.reload();
//        D.parameters = new ParametersMap(list, "=");
//        Logger.info("Parameters", D.parameters);
//
//        D.DEBUG.set(D.parameters.defaultGet("debug", true));
//        D.DEPTH = D.parameters.defaultGet("lookDepth", 2);
//        D.LogBackupCount = D.parameters.defaultGet("logBackupCount", 5);
//        D.ROOT_NAME = D.parameters.defaultGet("ROOT_NAME", D.ROOT_NAME);
//        D.MAX_THREADS_FOR_TASK = D.parameters.defaultGet("maxThreadsForTask", TaskFactory.PROCESSOR_COUNT);
//        D.USER_DIR = new PathStringCommands(D.parameters.defaultGet("userDir", D.HOME_DIR.absolutePath)).getPath() + File.separator;
//        D.useBufferedFileStreams.setValue(D.parameters.defaultGet("bufferedFileStreams", true));
//        VirtualFolder.VIRTUAL_FOLDER_PREFIX = D.parameters.defaultGet("virtualPrefix", "V");
//        MediaPlayerController.VLC_SEARCH_PATH = new PathStringCommands(D.parameters.defaultGet("vlcPath", D.HOME_DIR + File.separator + "lib")).getPath() + File.separator;
//        MediaPlayerController.oldMode = D.parameters.defaultGet("oldPlayerMode", false);
//        PathStringCommands.number = D.parameters.defaultGet("filter.number", "#");
//        PathStringCommands.fileName = D.parameters.defaultGet("filter.name", "<n>");
//        PathStringCommands.nameNoExt = D.parameters.defaultGet("filter.nameNoExtension", "<nne>");
//        PathStringCommands.filePath = D.parameters.defaultGet("filter.path", "<ap>");
//        PathStringCommands.extension = D.parameters.defaultGet("filter.nameExtension", "<ne>");
//        PathStringCommands.parent1 = D.parameters.defaultGet("filter.parent1", "<p1>");
//        PathStringCommands.parent2 = D.parameters.defaultGet("filter.parent2", "<p2>");
//        PathStringCommands.custom = D.parameters.defaultGet("filter.custom", "<c>");
//        PathStringCommands.relativeCustom = D.parameters.defaultGet("filter.relativeCustom", "<rc>");
//        CommandWindowController.commandInit = D.parameters.defaultGet("code.init", "init");
//        CommandWindowController.truncateAfter = D.parameters.defaultGet("code.truncateAfter", 100000);
//        CommandWindowController.maxExecutablesAtOnce = D.parameters.defaultGet("code.maxExecutables", 2);
//        CommandWindowController.commandGenerate = D.parameters.defaultGet("code.commandGenerate", "generate");
//        CommandWindowController.commandApply = D.parameters.defaultGet("code.commandApply", "apply");
//        CommandWindowController.commandClear = D.parameters.defaultGet("code.clear", "clear");
//        CommandWindowController.commandCancel = D.parameters.defaultGet("code.cancel", "cancel");
//        CommandWindowController.commandList = D.parameters.defaultGet("code.list", "list");
//        CommandWindowController.commandListRec = D.parameters.defaultGet("code.listRec", "listRec");
//        CommandWindowController.commandSetCustom = D.parameters.defaultGet("code.setCustom", "setCustom");
//        CommandWindowController.commandHelp = D.parameters.defaultGet("code.help", "help");
//        CommandWindowController.commandListParams = D.parameters.defaultGet("code.listParameters", "listParams");
//        CommandWindowController.maxExecutablesAtOnce = D.parameters.defaultGet("code.maxThreadsForCommand", TaskFactory.PROCESSOR_COUNT);
//        CommandWindowController.commandCopyFolderStructure = D.parameters.defaultGet("code.copyFolderStructure", "copyStructure");
    }

    public static void restart() {
        try {
            Logger.info("Restart request");
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
