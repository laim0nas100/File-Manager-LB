package lt.lb.filemanagerlb.gui.dialog;

import lt.lb.filemanagerlb.gui.MyBaseController;
import lt.lb.filemanagerlb.gui.ViewManager;
import java.util.Arrays;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lt.lb.commons.javafx.FX;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.utility.*;
import org.tinylog.Logger;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class ProgressDialogControllerExt extends MyBaseController {

    @FXML
    public CheckBox checkboxTasks;
    @FXML
    public TreeView treeView;
    @FXML
    public VBox base;

    @FXML
    public Button okButton;
    @FXML
    public Button cancelButton;
    @FXML
    public Button pauseButton;
    @FXML
    public ProgressBar progressBar;
    @FXML
    public Label text;
    @FXML
    public Label taskDescription;
    @FXML
    public Label timeWasted;
    @FXML
    public Label labelProgress;
    protected CustomClock clock;
    private ContinousCombinedTask task;
    private SimpleBooleanProperty paused;
    private String fullText = "";

    private <T> TreeItem<T> buildTree(TreeItem<T> node, SimpleTask task, Callback<SimpleTask, TreeItem<T>> form) {
        TreeItem leaf = form.call(task);
        if (node != null) {
            node.getChildren().add(leaf);
        }
        if (task instanceof ContinousCombinedTask) {
            ContinousCombinedTask nested = (ContinousCombinedTask) task;
            for (SimpleTask child : nested.getTasks()) {
                buildTree(leaf, child, form);
            }
        }
        return leaf;
    }

    public void afterShow(ContinousCombinedTask newTask) {
        super.afterShow();
        boolean pause = !ViewManager.getInstance().autoStartProgressDialogs.get();
        paused = new SimpleBooleanProperty(pause);
        Logger.info("Start paused:" + paused);
        this.task = newTask;
        task.paused.bind(paused);
        treeView.visibleProperty().bind(checkboxTasks.selectedProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        newTask.prepared.addListener(listener -> {

            TreeItem<String> treeRoot = this.buildTree(null, task, (SimpleTask param) -> {
                TreeItem<String> node = new TreeItem();
                node.setValue(param.getDescription());
                node.setExpanded(true);
                return node;
            });
            FX.submit(() -> {
                this.treeView.setRoot(treeRoot);
            });

        });

        this.labelProgress.textProperty().bind(this.progressBar.progressProperty().multiply(100).asString("%1$.2f").concat("%"));

        cancelButton.disableProperty().bind(task.running.not());
        okButton.disableProperty().bind(cancelButton.disableProperty().not());
        pauseButton.disableProperty().bind(cancelButton.disabledProperty());
        text.textProperty().bind(task.messageProperty());
        task.messageProperty().addListener(onChange -> {
            fullText += text.getText() + "\n";

        });

        taskDescription.setText(task.getDescription());

        clock = new CustomClock(D.exe);

        timeWasted.textProperty().bind(clock.timeProperty);
        clock.paused.bind(paused);

        task.setOnSucceeded((e) -> {
            Logger.info("Task succeeded");

            if (task.childTask != null) {
                task.run();
            }
            boolean doAutoClose = ViewManager.getInstance().autoCloseProgressDialogs.get();
            Logger.info("autoClose:" + doAutoClose);
            if (doAutoClose) {
                this.exit();
            }
        });
        task.setOnDone(e -> {
            clock.stopTimer(true);
        });

        if (paused.get()) {
            pauseButton.setText("START");
        }
        task.toThread().start();

    }

    public void showFullText() {
        ViewManager.getInstance().newListFrame("Progress so far", Arrays.asList(fullText.split("\n")));
    }

    @Override
    public void beforeShow(String title) {
        super.beforeShow(title);
    }

    public void cancelTask() {
        this.task.cancel();
        exit();
    }

    public void pauseTask() {
        if (!task.running.get()) {
            return;
        }
        if (task.paused.get()) {
            pauseButton.setText("PAUSE");
            paused.set(false);
        } else {
            pauseButton.setText("CONTINUE");
            paused.set(true);
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exitLogic() {
        if (!task.isDone()) {
            task.cancel(true);
        }
        clock.stopTimer(false);
    }

}
