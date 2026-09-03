package lt.lb.filemanagerlb.gui;

import lt.lb.filemanagerlb.vlc.VLCInit;
import com.jthemedetecor.OsThemeDetector;
import lt.lb.commons.javafx.scenemanagement.FXWinUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import lt.lb.commons.javafx.FXDefs;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.commons.javafx.scenemanagement.frames.FrameState;
import lt.lb.commons.javafx.scenemanagement.frames.WithDecoration;
import lt.lb.commons.javafx.scenemanagement.frames.WithFrameTypeMemoryPositionAndSize;
import lt.lb.commons.javafx.scenemanagement.frames.WithIcon;
import lt.lb.commons.javafx.scenemanagement.frames.WithStylesheet;
import lt.lb.commons.threads.executors.FastExecutor;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.P;
import lt.lb.filemanagerlb.SessionInfo;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.logic.filestructure.ExtRealFolder;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FavouriteLink;
import com.github.laim0nas100.uncheckedutils.Checked;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.Logger;

/**
 *
 * @author laim0nas100
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

    public static SafeOpt<Boolean> darkMode = SafeOpt.ofLazy(() -> OsThemeDetector.getDetector().isDark());
    public static SafeOpt<String> darkCss = FXDefs.DARK_THEME_CSS.map(m -> m.toExternalForm());

    public static void main(String[] args) {

        D.sm = new MultiStageManager(
                D.cLoader,
                frameInfo,
                new WithIcon(new Image(D.cLoader.getResourceAsStream("images/ico.png"))),
                new WithStylesheet(D.cLoader.getResource("css/main.css")),
                new WithDecoration(FrameState.FrameStateOpen.instance, d -> {
                    if (darkMode.orElse(false)) {
                        darkCss.ifPresent(theme -> {
                            d.getScene().getStylesheets().add(theme);
                        });
                    }

                }),
                new WithDecoration(FrameState.FrameStateShow.instance, d -> {
                    if (darkMode.orElse(false)) {
                        FXWinUtil.setDarkMode(d, true);
                    }// use defaults

                }),
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
            ViewManager.newWebDialog(Enums.WebDialog.About);
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

        new Thread(() -> {
            Logger.info("Write session info");
            try {
                writeSessionInfo();
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
            try {
                VLCInit.release();
                D.jobsExecutor.shutdown();
                D.exe.shutdown();
                D.exe.forEach(service -> {
                    if (service instanceof FastExecutor fast) {
                        fast.cancelAll(true);
                    }
                });
                Logger.info("Await termination");
                D.exe.awaitTermination(1, TimeUnit.MINUTES);
            } catch (Exception ex) {
                Logger.error(ex, "Failed during executor shutdown, terminating");
                System.exit(0);
            }
        }, "Shutdown thread").start();

    }

    public static void writeSessionInfo() throws IOException {
        SessionInfo si = D.sessionInfo;
        CollectionOp.replace(si.frameInfo, frameInfo.typeMap);
        CollectionOp.replace(si.favoriteLinks,
                LocationAPI.toSerializableStringList(MainController.favoriteLinks, f -> f.location));
        CollectionOp.replace(si.disabledFiles, D.globalDisabledSet);

        si.autoCloseProgressDialogs = ViewManager.autoCloseProgressDialogs.get();
        si.autoStartProgressDialogs = ViewManager.autoStartProgressDialogs.get();
        si.pinProgressDialogs = ViewManager.pinProgressDialogs.get();
        si.pinTextInputDialogs = ViewManager.pinTextInputDialogs.get();
        si.copyReplaceExisting = TaskFactory.copyReplaceExisting.get();

        vsManager.serializingXMLStream().objectToPathOverwrite(si, D.HOME_DIR.session_info.getPath());

//        serialize(path, vm);
    }

    public static void readSessionInfo() throws IOException {

        if (D.HOME_DIR.session_info.isReadable()) {

            SafeOpt<SessionInfo> deserialize = vsManager.<SessionInfo>serializingXMLStream().pathToObject(D.HOME_DIR.session_info.getPath());
            D.sessionInfo = deserialize.peekError(ErrorReport::report).orElseGet(SessionInfo::new);
        }

        frameInfo.typeMap.putAll(D.sessionInfo.frameInfo);
        ViewManager.autoCloseProgressDialogs.set(D.sessionInfo.autoCloseProgressDialogs);
        ViewManager.autoStartProgressDialogs.set(D.sessionInfo.autoStartProgressDialogs);
        ViewManager.pinProgressDialogs.set(D.sessionInfo.pinProgressDialogs);
        ViewManager.pinTextInputDialogs.set(D.sessionInfo.pinTextInputDialogs);
        TaskFactory.copyReplaceExisting.set(D.sessionInfo.copyReplaceExisting);
        for (String str : D.sessionInfo.favoriteLinks) {
            MainController.favoriteLinks.add(new FavouriteLink(str));
        }
        D.globalDisabledSet.addAll(D.sessionInfo.disabledFiles);
    }

    public static void reInit() {
        Logger.info("INITIALIZE");
        init = true;
        D.sm.getFrames().forEach(frame -> frame.close());
        VLCInit.release();

        MainController.errorLog = FXCollections.observableArrayList();
        MainController.favoriteLinks = FXCollections.observableArrayList();

        MainController.markedList = FXCollections.observableArrayList();
        MainController.propertyMarkedSize = Bindings.size(MainController.markedList);
        ArtificialRoot = new VirtualFolder(D.ARTIFICIAL_ROOT_DIR);
        VirtualFolders = new VirtualFolder(D.VIRTUAL_FOLDERS_DIR);
        ArtificialRoot.setIsAbsoluteRoot(true);
        remount();

        try {
            Path userdir = Paths.get(D.USER_DIR);
            if (!Files.isDirectory(userdir)) {
                Files.createDirectories(userdir);
            }
            P.reload();
        } catch (IOException e) {
            ErrorReport.report(e);
        }

        ArtificialRoot.propertyName.set(D.ROOT_NAME);
        MainController.favoriteLinks.add(new FavouriteLink(D.ROOT_NAME, ArtificialRoot));
        try {
            readSessionInfo();
        } catch (IOException ex) {
            ErrorReport.report(ex);
        }
        ViewManager.newWindow(ArtificialRoot);
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
