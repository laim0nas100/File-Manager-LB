package lt.lb.filemanagerlb.gui;

import lt.lb.filemanagerlb.vlc.VLCInit;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.property.SimpleBooleanProperty;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.FXTask;
import lt.lb.commons.javafx.scenemanagement.FXMLFrame;
import lt.lb.commons.javafx.scenemanagement.Frame;
import lt.lb.commons.javafx.scenemanagement.FrameException;
import lt.lb.commons.javafx.scenemanagement.frames.Util;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.dialog.AdvancedRenameController;
import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.gui.dialog.DuplicateFinderController;
import lt.lb.filemanagerlb.gui.dialog.ListController;
import lt.lb.filemanagerlb.gui.dialog.ProgressDialogController;
import lt.lb.filemanagerlb.gui.dialog.ProgressDialogControllerExt;
import lt.lb.filemanagerlb.gui.dialog.RenameDialogController;
import lt.lb.filemanagerlb.gui.dialog.RenameDialogController.FileCallback;
import lt.lb.filemanagerlb.gui.dialog.WebDialogController;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.Enums.FrameTitle;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.utility.ContinousCombinedTask;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FXJob;
import lt.lb.filemanagerlb.utility.SafeJob;
import com.github.laim0nas100.uncheckedutils.Checked;
import org.tinylog.Logger;

/**
 *
 * @author laim0nas100
 */
public class ViewManager {

    public static final SimpleBooleanProperty autoCloseProgressDialogs = new SimpleBooleanProperty(false);
    public static final SimpleBooleanProperty autoStartProgressDialogs = new SimpleBooleanProperty(false);
    public static final SimpleBooleanProperty pinProgressDialogs = new SimpleBooleanProperty(false);
    public static final SimpleBooleanProperty pinTextInputDialogs = new SimpleBooleanProperty(false);

    protected ViewManager() {
    }

// WINDOW ACTIONS
    public static void newWindow(ExtFolder currentFolder) {
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Logger.info("NEW WINDOW");

                    FXMLFrame<MainController> frame = newFrame(FrameTitle.WINDOW);
                    MainController controller = frame.getController();
                    controller.beforeShow(frame.getTitle(), currentFolder);
                    frame.getStage().show();
                    controller.afterShow();
                    Logger.info("AFTER SHOW");
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();

    }

    public static void updateAllWindows() {
        Stream<MainController> allControllers = D.sm.getAllControllers(MainController.class);
        FX.submit(() -> {
            allControllers.forEach(conrt -> {
                conrt.update();
            });
        });

    }

    public static void updateAllFrames(Serializable exception) {
        Stream<MyBaseController> allControllers = D.sm.getAllControllers(MyBaseController.class);
        if (exception != null) {
            allControllers = allControllers.filter(f -> !f.getFrameID().equals(exception));
        }
        Stream<MyBaseController> stream = allControllers;
        FX.submit(() -> {
            stream.forEach(con -> {
                con.update();
            });
        });

    }

    public static FXMLFrame getFxmlFrame(Serializable id) {
        return (FXMLFrame) D.sm.getFrame(id).get();
    }

    public static <T extends MyBaseController> T getController(Serializable id) {
        return (T) getFxmlFrame(id).getController();
    }

    public static boolean isFrameVisible(String windowID) {
        return D.sm.getFrame(windowID).isPresent();
    }

//DIALOG ACTIONS
    public static void newProgressDialog(FXTask task) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {

                    Frame frame = newFrame(FrameTitle.PROGRESS_DIALOG);
                    ProgressDialogController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
//                    frame.getStage().setMaxHeight(300);
                    frame.getStage().setMinHeight(250);
                    frame.getStage().setMinWidth(400);
                    frame.getStage().show();
                    frame.getStage().setAlwaysOnTop(pinProgressDialogs.get());
                    controller.afterShow(task);
                    frame.getStage().requestFocus();
                    frame.getStage().toFront();
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();
    }

    public static void newProgressDialog(ContinousCombinedTask task) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {

                    Frame frame = newFrame(FrameTitle.PROGRESS_DIALOG_EXT);
                    ProgressDialogControllerExt controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
//                frame.getStage().setMaxHeight(300);
                    frame.getStage().setMinHeight(250);
                    frame.getStage().setMinWidth(400);
                    frame.getStage().show();
                    frame.getStage().setAlwaysOnTop(pinProgressDialogs.get());
                    controller.afterShow(task);
                    frame.getStage().requestFocus();
                    frame.getStage().toFront();
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();
    }

    public static void newRenameDialog(ExtFolder folder, ExtPath itemToRename) {
        newRenameDialog(folder, itemToRename, null);
    }

    public static void newRenameDialog(ExtFolder folder, ExtPath itemToRename, FileCallback callback) {
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.TEXT_INPUT_DIALOG);
                    RenameDialogController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.getStage().setMaxHeight(200);
                    frame.getStage().setMinHeight(200);
                    frame.getStage().setMinWidth(500);
                    frame.show();
                    frame.getStage().setAlwaysOnTop(pinTextInputDialogs.get());
                    controller.afterShow(folder, itemToRename);
                    controller.callback = callback;
                    frame.getStage().requestFocus();
                    frame.getStage().toFront();
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();
    }

    public static void newAdvancedRenameDialog(ExtFolder folder) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.ADVANCED_RENAME_DIALOG);
                    AdvancedRenameController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle(), folder);
                    frame.show();
                    controller.afterShow();
                    frame.getStage().toFront();
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();
    }

    public static void newDirSyncDialog() {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.DIR_SYNC_DIALOG);
                    DirSyncController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.show();
                    controller.afterShow();
                    frame.getStage().toFront();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();

    }

    public static void newDuplicateFinderDialog(ExtFolder root) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.DUPLICATE_FINDER_DIALOG);
                    DuplicateFinderController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle(), root);
                    frame.show();
                    controller.afterShow();
                    frame.getStage().toFront();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();
    }

    public static void newWebDialog(Enums.WebDialog info) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.WEB_DIALOG);
                    WebDialogController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.show();
                    controller.afterShow(info);
                    frame.getStage().toFront();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }

                return null;
            }
        };
        et.runOnPlatform();
    }

    public static void newCommandDialog() {
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.COMMAND_DIALOG);
                    CommandWindowController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.show();
                    controller.afterShow();
                    frame.getStage().toFront();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();
    }

    public static void newListFrame(String description, Collection<String> list) {
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.LIST_FRAME);
                    ListController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle(), description);

                    frame.getStage().show();
                    controller.afterShow(list);
                    frame.getStage().toFront();

                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
        et.runOnPlatform();

    }

    public static void newMediaPlayer() {

        SafeJob showJob = new FXJob(me -> {

            if (!VLCInit.VLCfound) {
                Checked.checkedRun(() -> {
                    VLCInit.getOrInitFactory();// init a media player factory here
                }).ifPresent(ErrorReport::report);

                if (!VLCInit.VLCfound) {
                    return;
                }
            }

            FXMLFrame frame = newFrame(FrameTitle.MEDIA_PLAYER);

            MediaPlayerController controller = getController(frame.getID());
            controller.beforeShow();
            frame.show();
            frame.getStage().toFront();
            controller.afterShow();

        });

        D.jobsExecutor.submitAll(showJob);

    }

    private static <T> FXMLFrame newFrame(FrameTitle info) throws FrameException, InterruptedException, ExecutionException {
        return newFrame(info, false, Util.emptyConsumer);
    }

    private static <T extends MyBaseController> FXMLFrame newFrame(FrameTitle info, boolean singleton, Consumer<T> cons) throws FrameException, InterruptedException, ExecutionException {
        if (singleton) {
            return D.sm.newFxmlFrameSingleton(info.recourse, info.title, cons).get();
        }
        return D.sm.newFxmlFrame(info.recourse, info.title, cons).get();
    }

    public static void closeFrame(String windowID) {
        FX.submit(() -> {
            D.sm.closeFrame(windowID);
        });
    }

}
