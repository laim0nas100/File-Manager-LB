/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui;

import java.net.URL;
import lt.lb.filemanagerlb.logic.filestructure.*;
import lt.lb.filemanagerlb.logic.snapshots.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.javafx.CosmeticsFX.MenuTree;
import lt.lb.commons.javafx.*;
import lt.lb.commons.javafx.scenemanagement.Frame;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.utility.ErrorReport;
import org.tinylog.Logger;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class DirSyncController extends MyBaseController {

    @FXML
    public TextField directory0;
    @FXML
    public TextField directory1;
    @FXML
    public Text status0;
    @FXML
    public Text status1;
    @FXML
    public Text status;
    @FXML
    public TableView table;
    @FXML
    public DatePicker datePicker;
    @FXML
    public CheckBox checkShowAbsolutePath;
    @FXML
    public CheckBox checkPrioritizeBigger;
    @FXML
    public CheckBox checkShowOnlyDifferences;
    @FXML
    public CheckBox checkIgnoreFolderDate;
    @FXML
    public CheckBox checkNoDelete;
    @FXML
    public CheckBox checkNoCopy;
    @FXML
    public CheckBox checkIgnoreModified;
    @FXML
    public CheckBox checkDeleteFirst;
    @FXML
    public Button btnLoad;
    @FXML
    public Button btnCompare;
    @FXML
    public Button btnSync;
    @FXML
    public ComboBox syncMode;
    @FXML
    public ComboBox dateMode;

    private Value<Boolean> cond0 = new Value<>(false);
    private Value<Boolean> cond1 = new Value<>(false);
    private Snapshot snapshot0;
    private Snapshot snapshot1;
    private Snapshot result;
    private Value<ExtPath> file0 = new Value<>();
    private Value<ExtPath> file1 = new Value<>();
    private ObservableList<TableColumn<ExtEntry, String>> tableColumns;

    private TimeoutTask directoryCheckTask = new TimeoutTask(
            1000, 100, () -> {
                checkDirs();
            });

    public static final Comparator<ExtEntry> cmpAsc = new Comparator<ExtEntry>() {
        @Override
        public int compare(ExtEntry f1, ExtEntry f2) {
            return f1.relativePath.compareToIgnoreCase(f2.relativePath);
        }
    };

    @Override
    public void beforeShow(String title) {
        FX.submit(() -> {

            this.directoryCheckTask.addOnUpdate(() -> {
                this.btnLoad.setDisable(true);
                this.btnCompare.setDisable(true);
                this.btnSync.setDisable(true);
            });
            this.btnLoad.setDisable(true);
            this.btnCompare.setDisable(true);
            this.btnSync.setDisable(true);
            ObservableList<String> options = FXCollections.observableArrayList();
            options.add("Bidirectional");
            options.add("Make B like A");
            options.add("Make A like B");
            syncMode.getItems().setAll(options);
            syncMode.getSelectionModel().selectFirst();

            ObservableList<String> dateModeOptions = FXCollections.observableArrayList();
            dateModeOptions.add("Ignore After");
            dateModeOptions.add("Ignore Before");
            dateMode.getItems().setAll(dateModeOptions);
            dateMode.getSelectionModel().selectFirst();

            Locale.setDefault(Locale.ROOT);
            datePicker.setValue(LocalDate.now().plusDays(1));

            tableColumns = table.getColumns();

            checkIgnoreFolderDate.setSelected(true);
            checkShowOnlyDifferences.setSelected(true);
            tableColumns.add(new TableColumn<>("Path"));
            tableColumns.get(0).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                    String path = cellData.getValue().relativePath;
                    SimpleStringProperty string = new SimpleStringProperty(path);
                    if (checkShowAbsolutePath.selectedProperty().get()) {
                        path = cellData.getValue().absolutePath;
                        string.set(path);
                    }
                    return string;
                }
            });
            tableColumns.add(new TableColumn<>("Condition"));
            tableColumns.get(1).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {

                    SimpleStringProperty string = new SimpleStringProperty("No changes");
                    String s = "";
                    if (cellData.getValue().isNew) {
                        s += " new";
                    } else if (cellData.getValue().isMissing) {
                        s += " missing";
                    } else if (cellData.getValue().isModified) {
                        s += " modified";
                        if (cellData.getValue().isOlder) {
                            s += " older";
                        } else {
                            s += " not older";
                        }
                        if (cellData.getValue().isBigger) {
                            s += " bigger";
                        } else {
                            s += " not bigger";
                        }
                    }
                    string.set(s);
                    return string;
                }
            });
            tableColumns.add(new TableColumn<>("Last Modified"));
            tableColumns.get(2).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                    SimpleStringProperty string = new SimpleStringProperty(new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(cellData.getValue().lastModified))));
                    return string;
                }
            });
            tableColumns.add(new TableColumn<>("Action"));
            tableColumns.get(3).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {

                    return cellData.getValue().action;
                }
            });
            tableColumns.add(new TableColumn<>("Sync Complete"));
            tableColumns.get(4).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                    return cellData.getValue().actionCompleted.asString();
                }
            });
            MenuTree menuTree = new MenuTree(null);
            for (int i = 0; i < 5; i++) {
                final int action = i;
                MenuItem item = new MenuItem("Set " + ExtEntry.getActionDescription(action));
                item.setOnAction(eh -> {
                    ObservableList selectedItems = table.getSelectionModel().getSelectedItems();
                    for (Object ob : selectedItems) {
                        ExtEntry entry = (ExtEntry) ob;
                        entry.setAction(action);
                    }
                });
                menuTree.addMenuItem(item, item.getText());
            }
            this.table.setContextMenu(menuTree.constructContextMenu());

        });
    }

    @Override
    public void afterShow() {
        super.afterShow();
        this.directoryCheckTask.addOnUpdate(() -> {
            this.btnCompare.setDisable(true);
        });
        this.directory0.textProperty().addListener(onChange -> {
            this.directoryCheckTask.update();
        });
        this.directory1.textProperty().addListener(onChange -> {
            this.directoryCheckTask.update();
        });

    }

    public void checkDirs() {

        CompletableFuture<Void> s1 = FX.submit(() -> {
            btnSync.setDisable(true);
            btnLoad.setDisable(true);

            cond0.set(false);
            cond1.set(false);
            status0.setText("BAD");
            status1.setText("BAD");
        });
        String text0 = directory0.getText();
        String text1 = directory1.getText();

        CompletableFuture<Void> s2 = FX.submitAsync(() -> {

            file0.set(LocationAPI.getInstance().getFileAndPopulate(text0));
            cond0.set(file0.get().getIdentity().equals(Enums.Identity.FOLDER));
            Logger.info("Check 0");
        }, TaskFactory.mainExecutor);
        CompletableFuture<Void> s3 = FX.submitAsync(() -> {

            file1.set(LocationAPI.getInstance().getFileAndPopulate(text1));
            cond1.set(file1.get().getIdentity().equals(Enums.Identity.FOLDER));
            Logger.info("Check 1");
        }, TaskFactory.mainExecutor);

        FX.join(s1, s2, s3);
        Logger.info("After join");

        FX.submit(() -> {
            if (cond0.get()) {
                status0.setText("OK");
            }
            if (cond1.get()) {
                status1.setText("OK");
            }
            if (cond1.get() && cond0.get()) {
                btnLoad.setDisable(false);
            }
        });

    }

    public void setDirs() throws Exception {
        if (!file0.get().isVirtual.get()) {
            directory0.setText(file0.get().getAbsoluteDirectory());
        } else {
            throw new Exception("Bad directory setup");
        }
        if (!file1.get().isVirtual.get()) {
            directory1.setText(file1.get().getAbsoluteDirectory());
        } else {
            throw new Exception("Bad directory setup");
        }
    }

    public void load() {
        try {
            setDirs();
        } catch (Exception ex) {
            ErrorReport.report(ex);
            return;
        }

        snapshot0 = SnapshotAPI.getEmptySnapshot();
        snapshot1 = SnapshotAPI.getEmptySnapshot();
        this.status.textProperty().set("Populating directories:\n");
        this.btnSync.setDisable(true);
        this.btnCompare.setDisable(true);
        if (cond0.get() && cond1.get()) {
            Task<Snapshot> task0 = TaskFactory.getInstance().snapshotCreateTask(file0.get().getAbsolutePath());
            Task<Snapshot> task1 = TaskFactory.getInstance().snapshotCreateTask(file1.get().getAbsolutePath());
            task0.setOnSucceeded(eh -> {
                snapshot0 = task0.getValue();
                status.setText(status.getText().concat(snapshot0.folderCreatedFrom + "\n"));
            });
            task1.setOnSucceeded(eh -> {
                snapshot1 = task1.getValue();
                status.setText(status.getText().concat(snapshot1.folderCreatedFrom + "\n"));

            });

            FXTaskPooler executor = new FXTaskPooler(2, 5);
            executor.submit(task0);
            executor.submit(task1);
            executor.neverStop = false;
            executor.setOnSucceeded(eh -> {
                btnCompare.setDisable(false);
            });
            executor.toThread().start();
        }
    }

    public void compare() {
        Runnable r = () -> {
            ObservableList sortOrder = table.getSortOrder();

            this.status.textProperty().set("Comparing");
            Long date = Instant.now().toEpochMilli();
            try {
                date = datePicker.getValue().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                Logger.info(date);
            } catch (Exception e) {
            }
            result = SnapshotAPI.compareSnapshots(snapshot0, snapshot1);
            //Log.writeln(snapshot0,snapshot1);
            if (checkShowOnlyDifferences.selectedProperty().get()) {
                result = SnapshotAPI.getOnlyDifferences(result);
            }
            int modeDate = dateMode.getSelectionModel().getSelectedIndex();
            ObservableList<ExtEntry> list = FXCollections.observableArrayList();
            Iterator<Entry> iterator = result.map.values().iterator();
            while (iterator.hasNext()) {
                Entry next = iterator.next();
                boolean remove = false;
                if (modeDate == 0 && next.lastModified > date) {
                    remove = true;
                } else if (modeDate == 1 && next.lastModified < date) {
                    remove = true;
                } else {
                    if (checkIgnoreFolderDate.selectedProperty().get()) {
                        if (next.isFolder) {
                            if (next.isModified) {
                                next.isModified = false;
                                remove = true;
                            }
                        }
                    }
                    if (checkIgnoreModified.selectedProperty().get()) {
                        if (next.isModified) {
                            remove = true;
                        }
                    }
                }
                if (!remove) {
                    list.add(new ExtEntry(next));
                }
            }
            //Action Types
            //0 - no Action
            //1 - Missing file, copy here
            //2 - Replacable file
            //3 - New file, copy this
            //4 - Replacement file, copy this
            //5 - Delete this
            int mode = syncMode.getSelectionModel().getSelectedIndex();
            for (ExtEntry entry : list) {
                entry.setAction(0);
                switch (mode) {
                    case (0): {//Bidirectional
                        if (entry.isMissing) {
                            entry.setAction(1);
                        } else if (entry.isNew) {
                            entry.setAction(2);
                        } else {
                            if (entry.isModified && checkIgnoreModified.selectedProperty().not().get()) {
                                if (this.checkPrioritizeBigger.selectedProperty().get()) {
                                    if (entry.isBigger) {
                                        entry.setAction(2);
                                    } else {
                                        entry.setAction(1);
                                    }
                                } else {
                                    if (entry.isOlder) {
                                        entry.setAction(1);
                                    } else {
                                        entry.setAction(2);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case (1): {//A dominant
                        if (entry.isMissing) {
                            entry.setAction(4);
                        } else if (entry.isNew) {
                            entry.setAction(2);
                        } else {
                            if (entry.isModified && checkIgnoreModified.selectedProperty().not().get()) {
                                entry.setAction(2);
                            }
                        }
                        break;
                    }
                    case (2): {//B dominant
                        if (entry.isMissing) {
                            entry.setAction(1);
                        } else if (entry.isNew) {
                            entry.setAction(3);
                        } else {
                            if (entry.isModified && checkIgnoreModified.selectedProperty().not().get()) {
                                entry.setAction(1);
                            }
                        }
                        break;
                    }
                }
                int actionType = entry.actionType.get();
                if ((actionType == 3 || actionType == 4) && checkNoDelete.selectedProperty().get()) {
                    entry.setAction(0);
                } else if ((actionType == 1 || actionType == 2) && checkNoCopy.selectedProperty().get()) {
                    entry.setAction(0);
                }
            }

            FX.submit(() -> {
                table.setItems(list);
                table.getSortOrder().setAll(sortOrder);
                table.sort();
                this.status.textProperty().set("Done");
                this.btnSync.setDisable(false);
            });
        };
        new Thread(r).start();

    }

    public void synchronize() {
        this.btnSync.setDisable(true);
        Logger.info("Syncronize!");
        ArrayList<ExtEntry> list = new ArrayList<>();
        ArrayList<ExtEntry> listDelete = new ArrayList<>();
        table.sort();
        for (Object object : table.getItems()) {
            ExtEntry entry = (ExtEntry) object;
            if (entry.actionType.get() > 2) {
                listDelete.add(entry);
            } else {
                list.add(entry);
            }
        }
        listDelete.sort(cmpAsc.reversed());
        list.sort(cmpAsc);
        FXTask task;

        if (checkDeleteFirst.selectedProperty().get()) {
            list.addAll(0, listDelete);
        } else {
            list.addAll(listDelete);
        }
        for (ExtEntry en : list) {
            Logger.info(en.toString());
        }

        task = TaskFactory.getInstance().syncronizeTask(this.snapshot0.folderCreatedFrom, this.snapshot1.folderCreatedFrom, list);

        task.setDescription("Synchronization: " + "\n"
                + "Source:" + this.snapshot0.folderCreatedFrom + "\n"
                + "Compared:" + this.snapshot1.folderCreatedFrom);

        ViewManager.getInstance().newProgressDialog(task);

    }

    @Override
    public void update() {
    }

    @Override
    public void exit() {
        super.exit();
    }
}
