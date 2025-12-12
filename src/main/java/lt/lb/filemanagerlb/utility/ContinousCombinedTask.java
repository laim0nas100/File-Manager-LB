package lt.lb.filemanagerlb.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lt.lb.commons.containers.values.IntegerValue;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.threads.executors.InPlaceExecutor;
import org.tinylog.Logger;

/**
 *
 * @author laim0nas100
 */
public abstract class ContinousCombinedTask extends SimpleTask {

    private ArrayList<SimpleTask> tasks = new ArrayList<>();
    public SimpleTask currentTask = SimpleTask.temp();
    private volatile Integer currentIndex = 0;
    public SimpleBooleanProperty prepared = new SimpleBooleanProperty(false);

    private Executor exe = null;

    public static ContinousCombinedTask noPrep() {
        return new ContinousCombinedTask() {
            @Override
            protected void preparation() throws Exception {
            }
        };
    }

    public static ContinousCombinedTask noPrep(Executor exe) {
        return new ContinousCombinedTask(exe) {
            @Override
            protected void preparation() throws Exception {
            }
        };
    }

    public ContinousCombinedTask() {
        this(new InPlaceExecutor());
    }

    public ContinousCombinedTask(Executor exe) {
        this.exe = exe;
    }

    public List<SimpleTask> getTasks() {
        return tasks;
    }

    @Override
    protected Void call() throws Exception {
        if (!prepared.get()) {
            preparation();
        }
        prepared.setValue(true);
        while (currentIndex < tasks.size()) {

            if (conditionalWaitOrExit()) {
                return null;
            }
            SimpleTask task = tasks.get(currentIndex++);
            currentTask = task;
            doTask(task);
        }
        boolean[] doneArray = new boolean[tasks.size()];
        boolean hasUndone = true;
        while (hasUndone) {
            if (conditionalWaitOrExit()) {
                return null;
            }
            hasUndone = false;
            for (int i = 0; i < doneArray.length; i++) {
                if (doneArray[i]) {
                    continue;
                }
                SimpleTask task = tasks.get(i);
                task.get(1, TimeUnit.SECONDS);
                if (!task.isDone()) {
                    hasUndone = true;
                } else {
                    doneArray[i] = true;
                }
            }
        }

        return null;
    }

    protected void doTask(SimpleTask task) {
        Logger.info("ContinousTask: Begin: " + currentIndex + " " + task.getDescription());
        bindTask(task);
        updateMessage(task.getDescription());
        exe.execute(() -> {
            task.run();
            FX.submit(() -> {
                task.progressProperty().set(1d);
            });
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

    }

    protected void bindTask(SimpleTask task) {
        SimpleTask parent = this;
        task.paused.bind(parent.paused);
        IntegerValue stepProgress = new IntegerValue(0);
        task.progressProperty().addListener(listener -> {
            int ceil = (int) Math.round(task.progressProperty().get() * 1000); //0-1 -> 0-1000
            Integer prevProg = stepProgress.get();
            Integer prev = stepProgress.getAndSet(ceil);
            if (prev != ceil) {// changed
                double totalSteps = tasks.size() * 1000;
                double madeSteps = (ceil - prevProg) / totalSteps;
                double get = parent.progress.get();
                parent.progress.setValue(get + madeSteps);
            }

        });
        task.messageProperty.addListener(listener -> {
            parent.updateMessage(task.messageProperty.get());
        });
    }

}
