/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui.dialog;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import lt.lb.commons.javafx.FXTask;
import lt.lb.filemanagerlb.gui.MyBaseController;
import lt.lb.filemanagerlb.gui.ViewManager;
import lt.lb.filemanagerlb.utility.CustomClock;

import java.util.Arrays;
import lt.lb.commons.javafx.FX;

/**
 * FXML Controller class
 *
 * @author laim0nas100
 */
public class ProgressDialogController extends MyBaseController {

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
    private FXTask task;
    private SimpleBooleanProperty paused;
    private String fullText = "";

    public void afterShow(FXTask newTask) {
        super.afterShow();
        boolean pause = !ViewManager.getInstance().autoStartProgressDialogs.get();
        paused = new SimpleBooleanProperty(pause);
        this.task = newTask;
        task.paused.bind(paused);

        progressBar.progressProperty().bind(task.progressProperty());
        this.labelProgress.textProperty().bind(this.progressBar.progressProperty().multiply(100).asString("%1$.2f").concat("%"));

        cancelButton.disableProperty().bind(task.runningProperty().not());
        okButton.disableProperty().bind(cancelButton.disableProperty().not());
        pauseButton.disableProperty().bind(cancelButton.disabledProperty());
        text.textProperty().bind(task.messageProperty());
        task.messageProperty().addListener(onChange -> {
            fullText += text.getText() + "\n";

        });

        taskDescription.setText(task.getDescription());

        Thread t = new Thread(task);
        clock = new CustomClock();

        t.setDaemon(true);
        timeWasted.textProperty().bind(clock.timeProperty);
        clock.paused.bind(paused);

        task.setOnSucceeded((e) -> {
            clock.stopTimer();
            if (task.childTask != null) {
                task.run();
            }
            if (ViewManager.getInstance().autoCloseProgressDialogs.get()) {
                this.exit();
            }
        });

        if (paused.get()) {
            pauseButton.setText("START");
        }
        FX.submit(t::start);

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
        if (task.isPaused() && task.isRunning()) {
            pauseButton.setText("PAUSE");
            paused.set(false);
        } else if (!task.isPaused() && task.isRunning()) {
            pauseButton.setText("CONTINUE");
            paused.set(true);
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exit() {
        super.exit();
    }

}
