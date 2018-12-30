/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerGUI.dialog.RenameDialogController.FileCallback;
import filemanagerGUI.dialog.*;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.FrameTitle;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.fileStructure.ExtPath;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lt.lb.commons.Log;
import lt.lb.commons.containers.Value;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.FXTask;
import utility.*;

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
        this.frames = new HashMap<>();
        this.windows = new HashSet<>();

    }
    public final HashMap<String, Frame> frames;
    public final HashSet<String> windows;
    private boolean initStart = false;

    public static ViewManager getInstance() {
        return INSTANCE;
    }

    private int findSmallestAvailable(Map<String, Frame> map, String title) {
        int i = 1;
        while (true) {
            if (map.containsKey(title + i)) {
                i++;
            } else {
                return i;
            }
        }
    }

// WINDOW ACTIONS
    public void newWindow(ExtFolder currentFolder) {
        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    Frame frame = newFrame(FrameTitle.WINDOW);
                    MainController controller = (MainController) frame.getController();
                    windows.add(frame.getID());
                    controller.beforeShow(frame.getTitle(), currentFolder);
                    frame.getStage().show();
                    controller.afterShow();
                } catch (IOException ex) {
                    ErrorReport.report(ex);
                }
                return null;
            }
        };
//        et.toThread().start();
        et.runOnPlatform();
//        Platform.runLater(et);

    }

    public void updateAllWindows() {
        for (String s : windows) {
            TaskFactory.mainExecutor.execute(frames.get(s).getController()::update);
        }
    }

    public void updateAllFrames() {

        for (Frame frame : frames.values()) {
            TaskFactory.mainExecutor.execute(frame.getController()::update);
        }
    }

    public Frame getFrame(String windowID) {
        return frames.get(windowID);
    }

    public boolean frameIsVisible(String windowID) {
        boolean res = frames.containsKey(windowID);
//        Log.write(windowID +" isVisible call:"+res );
        return res;
    }

//DIALOG ACTIONS
    public void newProgressDialog(FXTask task) {

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {

                    Frame frame = newFrame(FrameTitle.PROGRESS_DIALOG);
                    ProgressDialogController controller = (ProgressDialogController) frame.getController();
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
                    ProgressDialogControllerExt controller = (ProgressDialogControllerExt) frame.getController();
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
                    RenameDialogController controller = (RenameDialogController) frame.getController();
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
                    AdvancedRenameController controller = (AdvancedRenameController) frame.getController();
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
                    DirSyncController controller = (DirSyncController) frame.getController();
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
                    DuplicateFinderController controller = (DuplicateFinderController) frame.getController();
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
                    WebDialogController controller = (WebDialogController) frame.getController();
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
                    CommandWindowController controller = (CommandWindowController) frame.getController();
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
                    ListController controller = (ListController) frame.getController();
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
        Value<Boolean> property = new Value<>(true);
        Value<Frame> frame = new Value<>();
        SimpleTask init = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    MediaPlayerController.discover();
                } catch (Exception e) {
                    ErrorReport.report(e);
                    property.set(false);
                }
                return null;
            }
        };
        init.setOnSucceeded(event -> {
            SimpleTask task = new SimpleTask() {
                @Override
                protected Void call() throws Exception {
                    
                    if (property.get()) {
                        frame.get().getStage().show();
                        frame.get().getStage().toFront();
                        frame.get().getController().afterShow();
                    } else {
                        closeFrame(frame.get().getController().windowID);
                    }
                    return null;
                }
            };
            task.runOnPlatform();

        });

        FXTask et = new FXTask() {
            @Override
            protected Void call() throws Exception {
                try {
                    frame.set(newFrame(FrameTitle.MEDIA_PLAYER));
                    new Thread(init).start();
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }

                return null;
            }
        };
        et.runOnPlatform();
    }

    private Frame newFrame(FrameTitle info, Object... params) throws IOException, Exception {
        Boolean frameIsSingleton = false;
        if (params.length > 0) {
            frameIsSingleton = (Boolean) params[0];
        }
        String title = info.getTitle();
        if (!frameIsSingleton) {
            int index = findSmallestAvailable(frames, info.getTitle());
            title += index;
        }
        if (frames.containsKey(title)) {
            throw new Exception("Frame:" + title + "Allready exists");
        }
        URL url = getClass().getResource("/resources/" + info.recourse);
        Log.print("URL=", url.toString());
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/images/ico.png")));

        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.getScene().getStylesheets().add("resources/css/fxml.css");
        BaseController controller = loader.getController();
        stage.setOnCloseRequest((WindowEvent we) -> {
            controller.exit();
        });
        controller.windowID = title;
        Frame frame = new Frame(stage, controller, info.title);
        this.frames.put(frame.getTitle(), frame);

        Value<Frame.Pos> pos = new Value<>();
        if (!Frame.positionMemoryMap.containsKey(info.title)) {
            pos.set(new Frame.Pos(stage.getX(), stage.getY()));
            Frame.positionMemoryMap.put(info.title, pos.get());
        }
        pos.set(Frame.positionMemoryMap.get(info.title));
        ChangeListener listenerY = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            pos.get().y.set((double) newValue);
        };
        ChangeListener listenerX = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            pos.get().x.set((double) newValue);
        };

        stage.setX(pos.get().x.get());
        stage.setY(pos.get().y.get());
        frame.listenerX = listenerX;
        frame.listenerY = listenerY;
        stage.xProperty().addListener(listenerX);
        stage.yProperty().addListener(listenerY);

        return frame;

    }

    public void closeFrame(String windowID) {
        if (this.initStart) {
            return;
        }
        FX.submit(() -> {

            Frame frame = frames.get(windowID);
            Stage stage = frame.getStage();
            stage.xProperty().removeListener(frame.listenerX);
            stage.yProperty().removeListener(frame.listenerY);
            stage.close();

            frames.remove(windowID);
            windows.remove(windowID);
            if (windows.isEmpty()) {
                closeAllFramesNoExit();
                try {
                    FileManagerLB.doOnExit();
                } catch (Exception e) {
                    ErrorReport.report(e);
                }
                System.exit(0);
            }
        });
    }

    public void closeAllFramesNoExit() {
        this.initStart = true;
        frames.keySet().forEach(key -> {
            Frame frame = frames.get(key);
            frame.getController().exit();
            Log.print("Close", frame.getID());
            frame.getStage().close();
        });
        frames.clear();
        windows.clear();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ViewManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.initStart = false;
    }

}
