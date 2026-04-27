package lt.lb.filemanagerlb.gui;

import java.io.Serializable;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.FXActionDelegator;
import lt.lb.commons.javafx.FXDefs;
import lt.lb.commons.javafx.fxrows.FXDrows;
import lt.lb.commons.javafx.scenemanagement.Frame;
import lt.lb.commons.javafx.scenemanagement.InjectableController;
import lt.lb.commons.javafx.scenemanagement.StageFrame;
import static lt.lb.filemanagerlb.D.sm;
import lt.lb.filemanagerlb.utility.ErrorReport;

/**
 *
 * @author laim0nas100
 * @param <T>
 */
public abstract class MyBaseController<T extends MyBaseController> implements InjectableController<T> {

    protected FXActionDelegator fxDelegator = new FXActionDelegator(ErrorReport::report);
    protected Frame frame;

    @Override
    public Frame getFrame() {
        return frame;
    }

    protected void beforeShow(String title) {
    }

    public Serializable getID() {
        return this.frame.getID();
    }

    protected void afterShow() {

    }

    protected Stage getStage() {
        return frame.getStage();
    }

    protected boolean closing = false;
    protected boolean exitInvoked = false;

    @Override
    public void close() {
        if (closing) {
            return; // prevent recusrion
        }
        closing = true;
        try {
            exit();
            InjectableController.super.close();
            if (!FileManagerLB.shutdown) {
                ViewManager.getInstance().updateAllFrames(getFrameID());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
//            ErrorReport.report(ex);
        }

    }

    public void exit() {
        if (exitInvoked) {
            return; // prevent recusrion
        }
        exitInvoked = true;
        exitLogic();
        close();
    }

    public abstract void exitLogic();

    public abstract void update();

    @Override
    public void inject(Frame frame, URL url, ResourceBundle rb) {
        this.frame = frame;
    }

    @Override
    public void init(Consumer<T> cons) {
        T me = (T) this;
        cons.accept(me);
    }
    
    private final String ALERT_FRAME_ID = "ALERT_FRAME_ID";

    protected void displayMessage(String title, String message) {
        boolean defaultSizing = !FileManagerLB.frameInfo.typeMap.containsKey(ALERT_FRAME_ID);
        FX.submit(() -> {
            FXDrows rows = FXDefs.fxrows();

            StageFrame dialogFrame = sm.newFxrowsFrame(ALERT_FRAME_ID, title, rows).get();
            Stage stage = dialogFrame.getStage();
            stage.initOwner(getStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setAlwaysOnTop(true);
            if (defaultSizing) {
                stage.setHeight(200);
                stage.setWidth(400);
            }

            // minimal UI
            rows.getNew()
                    .addLabel(message)
                    .withRowStyleClass("alert-message")
                    .display();
            rows.getNew()
                    .addButton("  OK  ", eh -> {
                        dialogFrame.close();
                    }).display();
            rows.renderEverything();
            stage.show();
        });
    }

}
