package lt.lb.filemanagerlb.utility;

import java.util.ArrayList;
import javafx.beans.property.*;
import lt.lb.commons.javafx.ExtTask;
import lt.lb.commons.javafx.FX;

/**
 *
 * @author Lemmin
 */
public abstract class SimpleTask extends ExtTask {

    protected SimpleStringProperty messageProperty = new SimpleStringProperty("");
    protected ArrayList<String> messages = new ArrayList<>();

    public ReadOnlyStringProperty messageProperty() {
        return this.messageProperty;
    }

    public DoubleProperty progressProperty() {
        return this.progress;
    }

    protected void updateMessage(String msg) {
        messages.add(msg);
        FX.submit(() -> {
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
        FX.submit(this);

    }
}
