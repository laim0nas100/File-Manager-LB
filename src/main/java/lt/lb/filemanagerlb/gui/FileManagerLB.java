package lt.lb.filemanagerlb.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lt.lb.commons.containers.collections.CollectionOp;
import lt.lb.commons.io.serialization.VSManager;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.commons.javafx.scenemanagement.frames.FrameState;
import lt.lb.commons.javafx.scenemanagement.frames.WithDecoration;
import lt.lb.commons.javafx.scenemanagement.frames.WithFrameTypeMemoryPositionAndSize;
import lt.lb.commons.javafx.scenemanagement.frames.WithIcon;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.P;
import lt.lb.filemanagerlb.SessionInfo;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.logic.filestructure.ExtRealFolder;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FavouriteLink;
import lt.lb.uncheckedutils.Checked;
import lt.lb.uncheckedutils.SafeOpt;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.Logger;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB {

    public static ObservableList<ExtPath> remountUpdateList = FXCollections.observableArrayList();
    public static VirtualFolder ArtificialRoot;// = new VirtualFolder(ARTIFICIAL_ROOT_DIR);
    public static VirtualFolder VirtualFolders;// = new VirtualFolder(VIRTUAL_FOLDERS_DIR);
    public static WithFrameTypeMemoryPositionAndSize frameInfo = new WithFrameTypeMemoryPositionAndSize();
//    public static WithFrameTypeMemorySize sizeInfo = new WithFrameTypeMemorySize();

    public static VSManager vsManager = prepareVSManager();

    static {
        java.util.logging.LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    private static boolean init = false;

    public static boolean shutdown = false;

    public static void main(String[] args) {

        D.sm = new MultiStageManager(
                D.cLoader,
                frameInfo,
                new WithIcon(new Image(D.cLoader.getResourceAsStream("images/ico.png"))),
                new WithDecoration(FrameState.FrameStateClose.instance, d -> {
                    if (shutdown) {
                        return;
                    }
                    if (!init && D.sm.getAllControllers(MainController.class).count() == 0) {
                        doOnExit();
                    }
                })
        );
        D.exe.service("date-size");
        D.exe.scheduleWithFixedDelay(System::gc, 30, 30, TimeUnit.MINUTES);

        Logger.info("Manifest");

        Checked.checkedRun(() -> {
            URL res = D.cLoader.getResource("stamped/version.txt");
            ArrayList<String> lines = lt.lb.commons.io.text.TextFileIO.readFrom(res);
            Logger.info("LINES");
            Logger.info(() -> lines.stream().collect(Collectors.joining("\n")));
        }).ifPresent(ErrorReport::report);
        Checked.checkedRun(() -> {
            reInit();
        }).ifPresent(ErrorReport::report);

        if (P.showAbout.resolve(P.parameters)) {
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
        }

    }

    private static VSManager prepareVSManager() {
        VSManager manager = new VSManager();
        manager.includeCustom(SessionInfo.class, 0L);
        //nothing to ignore
        //add version changes if needed

        return manager;
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
        Logger.info("Mount: " + name);
        Path path = Paths.get(name);
        if (Files.isDirectory(path)) {
            ExtFolder device = new ExtRealFolder(name);
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
        VirtualFolder baseFolder = FileManagerLB.VirtualFolders;
        HashSet<String> set = new HashSet<>();
        for (ExtPath file : baseFolder.getFilesCollection()) {
            set.add(file.getAbsoluteDirectory());
        }
        return set.contains(fileToCheck.getAbsoluteDirectory());
    }

    public static Set<String> getRootSet() {
        return ArtificialRoot.files.keySet();
    }

    public static void doOnExit() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        Logger.info("Exit call invoked");
        Stream<MyBaseController> allControllers = D.sm.getAllControllers(MyBaseController.class);
        allControllers.forEach(c -> c.exit());
        VLCInit.release();
        TaskFactory.getInstance().jobsExecutor.shutdown();
        D.exe.shutdown();
        Logger.info("Write seesion info");
        try {
            writeSessionInfo();
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
        try {
            Logger.info("Await termination");
            D.exe.awaitTermination(1, TimeUnit.DAYS);
        } catch (Exception ex) {
        }

    }

    public static void writeSessionInfo() throws IOException {
        SessionInfo si = D.sessionInfo;
        CollectionOp.replace(si.frameInfo, frameInfo.typeMap);
        CollectionOp.replace(si.favoriteLinks,
                MainController.favoriteLinks.stream()
                        .map(m -> m.location)
                        .filter(f -> !f.isArtificial())
                        .map(m -> m.getAbsolutePath())
                        .distinct()
                        .collect(Collectors.toList())
        );
        ViewManager vm = ViewManager.getInstance();
        si.autoCloseProgressDialogs = vm.autoCloseProgressDialogs.get();
        si.autoStartProgressDialogs = vm.autoStartProgressDialogs.get();
        si.pinProgressDialogs = vm.pinProgressDialogs.get();
        si.pinTextInputDialogs = vm.pinTextInputDialogs.get();
        si.copyReplaceExisting = TaskFactory.getInstance().copyReplaceExisting.get();
        
        vsManager.serializingXMLStream().objectToPathOverwrite(si, D.HOME_DIR.session_info.getPath());

//        serialize(path, vm);
    }

    public static void readSessionInfo() throws IOException {

        if (D.HOME_DIR.session_info.isReadable()) {
            
            SafeOpt<SessionInfo> deserialize = vsManager.<SessionInfo>serializingXMLStream().pathToObject(D.HOME_DIR.session_info.getPath());
            D.sessionInfo = deserialize.peekError(ErrorReport::report).orElseGet(SessionInfo::new);
        }

        frameInfo.typeMap.putAll(D.sessionInfo.frameInfo);
        ViewManager vm = ViewManager.getInstance();
        vm.autoCloseProgressDialogs.set(D.sessionInfo.autoCloseProgressDialogs);
        vm.autoStartProgressDialogs.set(D.sessionInfo.autoStartProgressDialogs);
        vm.pinProgressDialogs.set(D.sessionInfo.pinProgressDialogs);
        vm.pinTextInputDialogs.set(D.sessionInfo.pinTextInputDialogs);
        TaskFactory.getInstance().copyReplaceExisting.set(D.sessionInfo.copyReplaceExisting);
        for (String str : D.sessionInfo.favoriteLinks) {
            MainController.favoriteLinks.add(new FavouriteLink(str));
        }
    }

    public static void reInit() {
        Logger.info("INITIALIZE");
        init = true;
        D.sm.getFrames().forEach(frame -> frame.close());
        VLCInit.release();

        MainController.actionList = new ArrayList<>();
        MainController.dragList = FXCollections.observableArrayList();
        MainController.errorLog = FXCollections.observableArrayList();
        MainController.favoriteLinks = FXCollections.observableArrayList();

        MainController.markedList = FXCollections.observableArrayList();
        MainController.propertyMarkedSize = Bindings.size(MainController.markedList);
        MainController.globalDisabledMap = new HashSet<>();
        ArtificialRoot = new VirtualFolder(D.ARTIFICIAL_ROOT_DIR);
        VirtualFolders = new VirtualFolder(D.VIRTUAL_FOLDERS_DIR);
        ArtificialRoot.setIsAbsoluteRoot(true);
        remount();

        try {
            Path userdir = Paths.get(D.USER_DIR);
            if (!Files.isDirectory(userdir)) {
                Files.createDirectories(userdir);
            }
        } catch (IOException e) {
            ErrorReport.report(e);
        }
        P.reload();
        ArtificialRoot.propertyName.set(D.ROOT_NAME);
        MainController.favoriteLinks.add(new FavouriteLink(D.ROOT_NAME, ArtificialRoot));
        try {
            readSessionInfo();
        } catch (IOException ex) {
            ErrorReport.report(ex);
        }
        ViewManager.getInstance().newWindow(ArtificialRoot);
        Logger.info("After new window");

        init = false;

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
