/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.utility;

import java.util.ArrayList;
import javafx.beans.property.SimpleBooleanProperty;
import lt.lb.commons.javafx.FX;
import org.tinylog.Logger;

/**
 *
 * @author Lemmin
 */
public abstract class ContinousCombinedTask extends SimpleTask {

    public ArrayList<SimpleTask> tasks = new ArrayList<>();
    public SimpleTask currentTask = SimpleTask.temp();
    private volatile Integer currentIndex = 0;
    public SimpleBooleanProperty prepared = new SimpleBooleanProperty(false);

    @Override
    protected Void call() throws Exception {
        preparation();
        prepared.setValue(true);
        paused.addListener(l -> {
            synchronized (paused) {
                paused.notifyAll();
            }
        });
        while (currentIndex < tasks.size()) {
            while (this.paused.get()) {
                synchronized (paused) {
                    paused.wait();
                }

                if (this.isCancelled()) {
                    return null;
                }
            }
            currentTask = tasks.get(currentIndex++);
            this.updateMessage(currentTask.getDescription());
            Logger.info("ContinousTask: Begin: " + currentIndex + " " + currentTask.getDescription());
            doTask(currentTask);
            if (this.isCancelled()) {
                return null;
            }

        }

        return null;
    }

    protected void doTask(SimpleTask task) throws Exception {
        task.call();
        FX.submit(() -> {
            task.progressProperty().set(1d);
        });
    }

    protected abstract void preparation() throws Exception;

    @Override
    public boolean cancel(boolean inter) {
        boolean cancel = super.cancel(inter);
        currentTask.cancel(inter);
        return cancel;
    }

    public void addTask(SimpleTask task) {
        this.tasks.add(task);
        SimpleTask parent = this;
        task.paused.bind(parent.paused);
        task.progressProperty().addListener(listener -> {
            double get = currentIndex - 1 + task.progressProperty().get();
            parent.progressProperty().set(get / tasks.size());
        });
        task.messageProperty.addListener(listener -> {
            parent.updateMessage(task.messageProperty.get());
        });
    }

}
