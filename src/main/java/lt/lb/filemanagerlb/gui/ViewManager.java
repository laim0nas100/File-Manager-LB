package lt.lb.filemanagerlb.gui;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.property.SimpleBooleanProperty;
import lt.lb.commons.F;
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
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.utility.ContinousCombinedTask;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FXJob;
import lt.lb.jobsystem.Dependencies;
import lt.lb.jobsystem.Job;
import lt.lb.jobsystem.events.SystemJobEventName;
import lt.lb.uncheckedutils.Checked;
import org.tinylog.Logger;

/**
 *
 * @author Laimonas Beniušis
 */
public class ViewManager {

    public SimpleBooleanProperty autoCloseProgressDialogs;
    public SimpleBooleanProperty autoStartProgressDialogs;
    public SimpleBooleanProperty pinProgressDialogs;
    public SimpleBooleanProperty pinTextInputDialogs;
    private static final ViewManager INSTANCE = new ViewManager();

    protected ViewManager() {
        this.autoCloseProgressDialogs = new SimpleBooleanProperty(false);
        this.autoStartProgressDialogs = new SimpleBooleanProperty(false);
        this.pinProgressDialogs = new SimpleBooleanProperty(false);
        this.pinTextInputDialogs = new SimpleBooleanProperty(false);

    }
    public static ViewManager getInstance() {
        return INSTANCE;
    }

// WINDOW ACTIONS
    public void newWindow(ExtFolder currentFolder) {
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

    public void updateAllWindows() {
        D.sm.getAllControllers(MainController.class).forEach(conrt ->{
            
            D.exe.execute(conrt::update);
        });
    }

    public void updateAllFrames() {
        Stream<MyBaseController> allControllers = D.sm.getAllControllers(MyBaseController.class);
        allControllers.forEach(con ->{
            D.exe.execute(con::update);
        });
    }

    public FXMLFrame getFxmlFrame(String id) {
        return (FXMLFrame) D.sm.getFrame(id).get();
    }

    public <T extends MyBaseController> T getController(String id) {
        return (T) getFxmlFrame(id).getController();
    }

    public boolean frameIsVisible(String windowID) {
        return D.sm.getFrame(windowID).isPresent();
    }

//DIALOG ACTIONS
    public void newProgressDialog(FXTask task) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {

                    Frame frame = newFrame(FrameTitle.PROGRESS_DIALOG);
                    ProgressDialogController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.getStage().setMaxHeight(300);
                    frame.getStage().setMinHeight(250);
                    frame.getStage().setMinWidth(400);
                    frame.getStage().show();
                    frame.getStage().setAlwaysOnTop(ViewManager.getInstance().pinProgressDialogs.get());
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

    public void newProgressDialog(ContinousCombinedTask task) {

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
                    frame.getStage().setAlwaysOnTop(ViewManager.getInstance().pinProgressDialogs.get());
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

    public void newRenameDialog(ExtFolder folder, ExtPath itemToRename) {
        newRenameDialog(folder, itemToRename, null);
    }

    public void newRenameDialog(ExtFolder folder, ExtPath itemToRename, FileCallback callback) {
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
                    frame.getStage().show();
                    frame.getStage().setAlwaysOnTop(ViewManager.getInstance().pinTextInputDialogs.get());
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

    public void newAdvancedRenameDialog(ExtFolder folder) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.ADVANCED_RENAME_DIALOG);
                    AdvancedRenameController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle(), folder);
                    frame.getStage().show();
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

    public void newDirSyncDialog() {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.DIR_SYNC_DIALOG);
                    DirSyncController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.getStage().show();
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

    public void newDuplicateFinderDialog(ExtFolder root) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.DUPLICATE_FINDER_DIALOG);
                    DuplicateFinderController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle(), root);
                    frame.getStage().show();
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

    public void newWebDialog(Enums.WebDialog info) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.WEB_DIALOG);
                    WebDialogController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.getStage().show();
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

    public void newCommandDialog() {
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.COMMAND_DIALOG);
                    CommandWindowController controller = getController(frame.getID());
                    controller.beforeShow(frame.getStage().getTitle());
                    frame.getStage().show();
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

    public void newListFrame(String description, Collection<String> list) {
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

    public void newMediaPlayer() {

        Job<Boolean> discoverJob = new Job<>(me -> {

            Optional<Throwable> checkedRun = Checked.checkedRun(() -> {
                MediaPlayerController.discover();
            });
            checkedRun.ifPresent(ErrorReport::report);

            return !checkedRun.isPresent();
        });

        FXJob showJob = new FXJob(me -> {

            FXMLFrame frame = newFrame(FrameTitle.MEDIA_PLAYER);
            frame.getStage().show();
            frame.getStage().toFront();
            MediaPlayerController controller = getController(frame.getID());
            controller.afterShow();
            frame.getStage().toFront();

        });

        showJob.addDependency(Dependencies.standard(discoverJob, SystemJobEventName.ON_SUCCESSFUL));
        TaskFactory.jobsExecutor.submitAll(showJob, discoverJob);

    }

    private <T> FXMLFrame newFrame(FrameTitle info) throws FrameException, InterruptedException, ExecutionException {
        return newFrame(info, false, Util.emptyConsumer);
    }

    private <T extends MyBaseController> FXMLFrame newFrame(FrameTitle info, boolean singleton, Consumer<T> cons) throws FrameException, InterruptedException, ExecutionException {
        if (singleton) {
            return D.sm.newFxmlFrameSingleton(info.recourse, info.title, cons).get();
        }
        return D.sm.newFxmlFrame(info.recourse, info.title, cons).get();
    }

    public void closeFrame(String windowID) {
        FX.submit(() -> {
            D.sm.closeFrame(windowID);
        });
    }


}
