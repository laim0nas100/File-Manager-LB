package lt.lb.filemanagerlb.gui;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.stage.Stage;
import lt.lb.commons.javafx.scenemanagement.Frame;
import lt.lb.commons.javafx.scenemanagement.InjectableController;
import lt.lb.filemanagerlb.utility.ErrorReport;

/**
 *
 * @author laim0nas100
 * @param <T>
 */
public abstract class MyBaseController<T extends MyBaseController> implements InjectableController<T> {

    protected Frame frame;

    @Override
    public Frame getFrame() {
        return frame;
    }

    protected void beforeShow(String title) {
    }

    public String getID() {
        return this.frame.getID();
    }

    protected void afterShow() {

    }

    protected Stage getStage() {
        return frame.getStage();
    }

    protected boolean closing = false;

    @Override
    public void close() {
        if (closing) {
            return; // prevent recusrion
        }
        closing = true;
        try {
            exit();
            InjectableController.super.close();
            ViewManager.getInstance().updateAllFrames(getFrameID());
        } catch (Exception ex) {
            ErrorReport.report(ex);
        } finally{
            closing = false;
        }

    }

    public void exit() {
        close();
    }

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

}
