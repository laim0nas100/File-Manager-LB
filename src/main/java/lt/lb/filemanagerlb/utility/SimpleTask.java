package lt.lb.filemanagerlb.utility;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javafx.beans.property.*;
import lt.lb.commons.javafx.ExtTask;
import lt.lb.commons.javafx.FX;
import lt.lb.uncheckedutils.func.UncheckedRunnable;

/**
 *
 * @author Lemmin
 */
public abstract class SimpleTask<T> extends ExtTask<T> {

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
    protected abstract T call() throws Exception;

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getDescription() {
        return this.description;
    }


    public void report(Throwable e) {
        ErrorReport.report(e);
    }

    public SimpleTask() {
    }

    public static SimpleTask<Void> temp() {
        return new SimpleTask<Void>() {
            @Override
            protected Void call() throws Exception {
                return null;
            }
        };
    }

    public static <T> SimpleTask<T> of(String description, Callable<T> call) {
        Objects.requireNonNull(call);
        SimpleTask<T> task = new SimpleTask<T>() {
            @Override
            protected T call() throws Exception {
                try {
                    return call.call();
                } catch (Exception ex) {
                    report(ex);
                }
                return null;
            }
        ;
        };
        task.setDescription(description);
        return task;
    }

    public static SimpleTask<Void> of(String description, UncheckedRunnable run) {
        Objects.requireNonNull(run);
        SimpleTask<Void> task = new SimpleTask<Void>() {
            @Override
            protected Void call() throws Exception {
                run.runSafe().getError().ifPresent(this::report);
                return null;
            }
        ;
        };
        task.setDescription(description);
        return task;
    }

    public void runOnPlatform() {
        FX.submit(this);
    }
    
    public void submit(Executor exe){
        exe.execute(this);
    }
}
