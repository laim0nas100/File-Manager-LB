/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.stage.Stage;
import lt.lb.commons.javafx.scenemanagement.Frame;
import lt.lb.commons.javafx.scenemanagement.InjectableController;

/**
 *
 * @author Laimonas Beniušis
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

    @Override
    public void exit() {
        ViewManager.getInstance().closeFrame(getID());
        ViewManager.getInstance().updateAllFrames();
    }

    
    public abstract void update();

    @Override
    public void inject(Frame frame, URL url, ResourceBundle rb) {
        this.frame = frame;
    }
    

    @Override
    public void init(Consumer<T> cons){
        T me = (T) this;
        cons.accept(me);
    }


}
