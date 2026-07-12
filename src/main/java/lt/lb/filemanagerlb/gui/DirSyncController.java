package lt.lb.filemanagerlb.gui;

import java.io.IOException;
import java.nio.file.Paths;
import lt.lb.filemanagerlb.logic.filestructure.*;
import lt.lb.filemanagerlb.logic.snapshots.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.iteration.streams.MakeStream;
import lt.lb.commons.javafx.*;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.utility.ContinousCombinedTask;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.SimpleTask;
import com.github.laim0nas100.uncheckedutils.Checked;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import org.tinylog.Logger;

/**
 * FXML Controller class
 *
 * @author laim0nas100
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
    public CheckBox checkHideNoAction;
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

    private Snapshot snapshot0;
    private Snapshot snapshot1;
    private Snapshot result;
    private Value<ExtPath> file0 = new Value<>();
    private Value<ExtPath> file1 = new Value<>();
    private ObservableList<TableColumn<ExtEntry, String>> tableColumns;

//    private ServiceTimeoutTask directoryCheckTask = new ServiceTimeoutTask(
//            D.exe.scheduledService("dir-sync-sched"),
//            D.exe.service("dir-sync"),
//            WaitTime.ofSeconds(1),
//            Executors.callable(this::checkDirs)
//    );
//    private TimeoutTask directoryCheckTask = new TimeoutTask(
//            1000, 100, () -> {
//                checkDirs();
//            });
    public static final Comparator<ExtEntry> cmpAsc = new Comparator<ExtEntry>() {
        @Override
        public int compare(ExtEntry f1, ExtEntry f2) {
            return f1.relativePath.compareToIgnoreCase(f2.relativePath);
        }
    };

    @Override
    public void beforeShow(String title) {

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
                ExtEntry entry = cellData.getValue();
                if (entry.isNew) {
                    s += " new";
                } else if (entry.isMissing) {
                    s += " missing";
                } else if (entry.isModified) {
                    s += " modified";
                    int ageCmp = entry.ageCmp;
                    if (ageCmp < 0) {
                        s += " older";
                    } else if (ageCmp > 0) {
                        s += " newer";
                    } else {
                        s += " same date";
                    }
                    int sizeCmp = entry.sizeCmp;
                    if (sizeCmp < 0) {
                        s += " smaller";
                    } else if (sizeCmp > 0) {
                        s += " bigger";
                    } else {
                        s += " same size";
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
                return cellData.getValue().date;
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
        MenuBuilders.ContextMenuBuilder builder = new MenuBuilders.ContextMenuBuilder();
        for (int i = 0; i < 5; i++) {
            final int action = i;
            builder = builder.addItem(new MenuBuilders.MenuItemBuilder()
                    .withText("Set " + ExtEntry.getActionDescription(action))
                    .withAction(eh -> {
                        ObservableList selectedItems = table.getSelectionModel().getSelectedItems();
                        for (Object ob : selectedItems) {
                            ExtEntry entry = (ExtEntry) ob;
                            entry.setAction(action);
                        }
                    })
            );
        }
        ContextMenu build = builder.addNestedDisableBind().addNestedVisibilityBind().build();
        MenuItem wrapSelectContextMenu = CosmeticsFX.wrapSelectContextMenu(table.getSelectionModel());
        build.getItems().add(wrapSelectContextMenu);
        this.table.setContextMenu(build);

    }

    @Override
    public void afterShow() {
        super.afterShow();
        this.directory0.textProperty().addListener(onChange -> {
            checkDirectory(status0, directory0, file0);
        });
        this.directory1.textProperty().addListener(onChange -> {
            checkDirectory(status1, directory1, file1);
        });

    }

    AtomicLong lastUpdate = new AtomicLong(0);

    public void checkDirectory(Text status, TextField directory, Value<ExtPath> file) {
        try {
            status.setText("");
            lastUpdate.incrementAndGet();
            btnSync.setDisable(true);
            btnLoad.setDisable(true);
            checkDir(directory.getText(), file);
            if (file0.isNotNull() && file1.isNotNull()) {
                btnLoad.setDisable(false);
            }
            if (file.isNotNull()) {
                status.setText("OK");
            }

        } catch (IOException e) {
            status.setText(e.getMessage());
        }
    }

    public void checkDir(String path, Value<ExtPath> file) throws IOException {
        ExtPath found = LocationAPI.getFileAndPopulate(path);
        file.set(null);
        if (!Paths.get(path).equals(found.toPath())) {

            throw new IOException(path + " not found");
        } else {
            if (!found.getIdentity().equals(Enums.Identity.FOLDER)) {
                throw new IOException(path + " is not a folder");
            }
            file.set(found);
        }
    }

    public void checkDirs() {
        FX.runAndWait(() -> {
            btnSync.setDisable(true);
            btnLoad.setDisable(true);

            status0.setText("Checking");
            status1.setText("Checking");
        });

        SafeOpt<Boolean> p1 = SafeOpt.ofAsync(directory0.getText()).map(v -> {
            file0.set(LocationAPI.getFileAndPopulate(v));
            Logger.info("Check 0");
            return file0.get().getIdentity().equals(Enums.Identity.FOLDER);
        });

        SafeOpt<Boolean> p2 = SafeOpt.ofAsync(directory1.getText()).map(v -> {
            file1.set(LocationAPI.getFileAndPopulate(v));
            Logger.info("Check 1");
            return file1.get().getIdentity().equals(Enums.Identity.FOLDER);
        });

        D.exe.submit(() -> {
            boolean c0 = p1.orElse(false);
            boolean c1 = p2.orElse(false);
            FX.runAndWait(() -> {
                mapCondition(c0, status0);
                mapCondition(c1, status1);
                if (c0 && c1) {
                    btnLoad.setDisable(false);
                }
            });
        });
    }

    private static void mapCondition(boolean cond, Text text) {
        String val = cond ? "OK" : "BAD";
        text.setText(val);
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
        long lastUpdated = lastUpdate.get();
        if (file0.isNotNull() && file1.isNotNull()) {
            CountDownLatch latch = new CountDownLatch(2);
            SimpleTask<Snapshot> task0 = TaskFactory.snapshotCreateTask(file0.get().getAbsolutePath());
            SimpleTask<Snapshot> task1 = TaskFactory.snapshotCreateTask(file1.get().getAbsolutePath());
            task0.appendOnSucceeded(eh -> {
                if (lastUpdated == lastUpdate.get()) {
                    snapshot0 = task0.get();
                    FX.runAndWait(() -> {
                        status.setText(status.getText().concat(snapshot0.folderCreatedFrom + "\n"));

                    });
                    Checked.checkedRun(() -> {
                        latch.countDown();
                        if (latch.await(1, TimeUnit.SECONDS)) {
                            btnCompare.setDisable(false);
                        }
                    });

                }

            });
            task1.appendOnSucceeded(eh -> {
                if (lastUpdated == lastUpdate.get()) {
                    snapshot1 = task1.get();
                    FX.runAndWait(() -> {
                        status.setText(status.getText().concat(snapshot1.folderCreatedFrom + "\n"));
                    });
                    Checked.checkedRun(() -> {
                        latch.countDown();
                        if (latch.await(1, TimeUnit.SECONDS)) {
                            btnCompare.setDisable(false);
                        }
                    });

                }
            });

            D.exe.submit(task0);
            D.exe.submit(task1);
        }
    }

    public void compare() {

        final long last = lastUpdate.get();
        final boolean ignoreModified = checkIgnoreModified.isSelected();
        final boolean noDelete = checkNoDelete.isSelected();
        final boolean noCopy = checkNoCopy.isSelected();
        final boolean showOnlyDifferences = checkShowOnlyDifferences.isSelected();
        final boolean ignoreFolderDate = checkIgnoreFolderDate.isSelected();
        final boolean prioritizeBigger = checkPrioritizeBigger.isSelected();
        final boolean hideNoAction = checkHideNoAction.isSelected();

        final int syncType = syncMode.getSelectionModel().getSelectedIndex();
        this.status.setText("Comparing");
        Runnable r = () -> {
            ObservableList sortOrder = table.getSortOrder();

            Long date = Instant.now().toEpochMilli();
            try {
                date = datePicker.getValue().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                Logger.info(date);
            } catch (Exception e) {
            }
            result = SnapshotAPI.compareSnapshots(snapshot0, snapshot1);
            //Log.writeln(snapshot0,snapshot1);
            if (showOnlyDifferences) {
                result = SnapshotAPI.getOnlyDifferences(result);
            }
            int modeDate = dateMode.getSelectionModel().getSelectedIndex();
            ObservableList<ExtEntry> list = FXCollections.observableArrayList();
            List<ExtEntry> entries = new ArrayList<>();
            Iterator<Entry> iterator = result.map.values().iterator();
            while (iterator.hasNext()) {
                Entry next = iterator.next();
                boolean remove = false;
                if (modeDate == 0 && next.lastModified > date) {
                    remove = true;
                } else if (modeDate == 1 && next.lastModified < date) {
                    remove = true;
                } else {
                    if (ignoreFolderDate) {
                        if (next.isFolder) {
                            if (next.isModified) {
                                next.isModified = false;
                                remove = true;
                            }
                        }
                    }
                    if (ignoreModified) {
                        if (next.isModified) {
                            remove = true;
                        }
                    }
                }
                if (!remove) {
                    entries.add(new ExtEntry(next));
                }
            }

            for (ExtEntry entry : entries) {
                entry.setAction(0);
                //Action Types
                //0 - no Action
                //1 - Missing file, copy here
                //2 - Replacable file
                //3 - New file, copy this
                //4 - Replacement file, copy this
                switch (syncType) {
                    case (0): {//Bidirectional
                        if (entry.isMissing) {
                            entry.setAction(1);
                        } else if (entry.isNew) {
                            entry.setAction(2);
                        } else {
                            if (entry.isModified && !ignoreModified) {
                                if (prioritizeBigger) {
                                    if (entry.sizeCmp > 0) {
                                        entry.setAction(2);
                                    } else if (entry.sizeCmp < 0) {
                                        entry.setAction(1);
                                    }
                                } else {
                                    if (entry.ageCmp > 0) {
                                        entry.setAction(1);
                                    } else if (entry.ageCmp < 0) {
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
                            if (entry.isModified && !ignoreModified) {
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
                            if (entry.isModified && !ignoreModified) {
                                entry.setAction(1);
                            }
                        }
                        break;
                    }
                }
                int actionType = entry.actionType.get();
                if ((actionType == 3 || actionType == 4) && noDelete) {
                    entry.setAction(0);
                } else if ((actionType == 1 || actionType == 2) && noCopy) {
                    entry.setAction(0);
                }
            }

            list.addAll(MakeStream.from(entries).filter(e -> {
                return !(hideNoAction && e.actionType.get() == 0);
            }).toList());

            FX.submit(() -> {
                table.setItems(list);
                table.getSortOrder().setAll(sortOrder);
                table.sort();
                if (last == lastUpdate.get()) {
                    this.status.textProperty().set("Done");
                    this.btnSync.setDisable(false);
                } else {
                    this.status.textProperty().set("Directories has been modified, resync");
                    this.btnSync.setDisable(true);
                }

            });
        };
        D.exe.execute(r);
    }

    public void synchronize() {
        this.btnSync.setDisable(true);
        Logger.info("Syncronize!");
        ArrayList<ExtEntry> list = new ArrayList<>();
        ArrayList<ExtEntry> listDelete = new ArrayList<>();
//        table.sort();
        for (Object object : table.getItems()) {
            ExtEntry entry = (ExtEntry) object;
            int actionType = entry.actionType.get();
            if (actionType == 3 || actionType == 4) {
                listDelete.add(entry);
            } else if (actionType != 0) {
                list.add(entry);
            }
        }
        listDelete.sort(cmpAsc.reversed());
        list.sort(cmpAsc);

        if (checkDeleteFirst.selectedProperty().get()) {
            list.addAll(0, listDelete);
        } else {
            list.addAll(listDelete);
        }
        for (ExtEntry en : list) {
            Logger.info(en.toString());
        }

        ContinousCombinedTask task = TaskFactory.syncronizeTask(this.snapshot0.folderCreatedFrom, this.snapshot1.folderCreatedFrom, list);

        task.setDescription("Synchronization: " + "\n"
                + "Source:" + this.snapshot0.folderCreatedFrom + "\n"
                + "Compared:" + this.snapshot1.folderCreatedFrom);

        ViewManager.newProgressDialog(task);

    }

    @Override
    public void update() {
    }

    @Override
    public void exitLogic() {
    }
}
