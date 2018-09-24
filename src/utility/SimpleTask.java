/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.util.ArrayList;
import javafx.application.Platform;
import javafx.beans.property.*;
import lt.lb.commons.threads.ExtTask;

/**
 *
 * @author Lemmin
 */
public abstract class SimpleTask extends ExtTask {

    protected SimpleStringProperty messageProperty = new SimpleStringProperty("");
    protected SimpleDoubleProperty progressProperty = new SimpleDoubleProperty(0);
    protected ArrayList<String> messages = new ArrayList<>();

    public ReadOnlyStringProperty messageProperty() {
        return this.messageProperty;
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return this.progressProperty;
    }

    protected void updateMessage(String msg) {
        messages.add(msg);
        Platform.runLater(() -> {
            messageProperty.set(msg);
        });

    }

    private String description = "";

    @Override
    protected abstract Void call() throws Exception;

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getDescription() {
        return this.description;
    }

    public void report(Exception e) {
        ErrorReport.report(e);
    }

    public SimpleTask() {
    }

    public static SimpleTask temp() {
        return new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                return null;
            }
        };
    }

    public void runOnPlatform() {
        new Thread(() -> {
            Platform.runLater(this);
        }).start();

    }
}
