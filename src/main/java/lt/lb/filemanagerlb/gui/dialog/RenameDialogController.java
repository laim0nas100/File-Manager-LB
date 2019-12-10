/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui.dialog;

import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import java.nio.file.Files;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.TimeoutTask;
import lt.lb.commons.parsing.StringOp;
import lt.lb.filemanagerlb.utility.*;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class RenameDialogController extends TextInputDialogController {

    public static interface FileCallback {

        public void callback(ExtPath path);
    }

    @FXML
    public Label nameAvailable;

    public FileCallback callback;
    private ExtPath itemToRename;
    private ExtFolder folder;
    private ObservableList<String> listToCheck = FXCollections.observableArrayList();
    private TimeoutTask folderUpdateTask = new TimeoutTask(500, 100, () -> {
        update();
        FX.submit(() -> {
            String trim = textField.getText().trim();
            if (!listToCheck.contains(trim) && trim.length() > 0) {
                nameAvailable.setText("Available");
                nameIsAvailable.set(true);
            }
        });
    });

    @Override
    public void exit() {
        super.exit();
    }

    public void afterShow(ExtFolder folder, ExtPath itemToRename) {
        this.description.setText("Rename " + itemToRename.propertyName.get());
        this.itemToRename = itemToRename;
        this.textField.setText(itemToRename.propertyName.get());
        nameIsAvailable.set(false);
        this.folder = folder;
        this.textField.textProperty().addListener(listener -> {
            checkAvailable();
        });

    }

    @Override
    public void beforeShow(String title) {
        super.beforeShow(title);
    }

    @Override
    public void checkAvailable() {
        if (!Files.exists(itemToRename.toPath())) {
            exit();
        }
        nameIsAvailable.set(false);
        nameAvailable.setText("Taken");
        folderUpdateTask.update();

    }

    @Override
    public void apply() {
        if (nameIsAvailable.get()) {
            try {
                PathStringCommands fallback = new PathStringCommands(TaskFactory.resolveAvailablePath(folder, itemToRename.propertyName.get()).trim());
                String renameTo = TaskFactory.getInstance().renameTo(itemToRename.getAbsolutePath(), StringOp.trimEnd(textField.getText()), fallback.getName(true));
                if (callback != null) {
                    ExtPath fileOptimized = LocationAPI.getInstance().getFileOptimized(renameTo);
                    callback.callback(fileOptimized);
                }
                exit();
            } catch (FileNameException ex) {
                this.nameAvailable.setText(ex.getMessage());
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }
    }

    @Override
    public void update() {

        listToCheck.clear();
        folder.update();
        for (ExtPath file : folder.getFilesCollection()) {
            listToCheck.add(file.propertyName.get());
        }
    }
}
