/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lt.lb.commons.F;
import lt.lb.commons.Log;
import lt.lb.commons.javafx.CosmeticsFX;
import lt.lb.commons.javafx.CosmeticsFX.ExtTableView;
import lt.lb.commons.javafx.ExtTask;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.FXTask;
import lt.lb.commons.javafx.TimeoutTask;
import lt.lb.commons.parsing.StringOp;
import lt.lb.commons.threads.FastWaitingExecutor;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.filemanagerlb.gui.custom.FileAddressField;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.Enums.DATA_SIZE;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.LocationInRoot;
import lt.lb.filemanagerlb.logic.ManagingClass;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtLink;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ContinousCombinedTask;
import lt.lb.filemanagerlb.utility.DesktopApi;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FavouriteLink;
import lt.lb.filemanagerlb.utility.Finder;
import lt.lb.filemanagerlb.utility.SimpleTask;


/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class MainController extends BaseController {

    @FXML
    public CheckMenuItem autoClose;
    @FXML
    public CheckMenuItem autoStart;
    @FXML
    public CheckMenuItem pinDialogs;
    @FXML
    public CheckMenuItem pinTextInput;
    @FXML
    public SplitPane splitPane;

    @FXML
    public TableView tableView;
    @FXML
    public TextField localSearch;

    private ObservableList<ExtPath> selectedList = FXCollections.observableArrayList();

    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static ObservableList<ExtPath> dragList;
    public static ObservableList<ExtPath> markedList;
    public static IntegerBinding propertyMarkedSize;
    public static ArrayList<ExtPath> actionList;

    @FXML
    public CheckBox useRegex;
    @FXML
    public Label itemCount;
    @FXML
    public ListView searchView;
    @FXML
    public TextField searchField;
    @FXML
    public Text searchStatus;

    @FXML
    public ListView markedView;
    @FXML
    public Text markedSize;

    @FXML
    public ListView linkView;
    @FXML
    public ListView errorView;

    @FXML
    public TextField currentDirText;
    @FXML
    public Button buttonPrev;
    @FXML
    public Button buttonParent;
    @FXML
    public Button buttonForw;
    @FXML
    public Button buttonGo;

    @FXML
    public TextField snapshotLoadField;
    @FXML
    public TextField snapshotCreateField;
    @FXML
    public Text snapshotTextDate;
    @FXML
    public Text snapshotTextFolder;
    @FXML
    public ListView snapshotView;

    @FXML
    public Menu menuSizeUnits;

    @FXML
    public MenuItem miAdvancedRenameFolder;
    @FXML
    public MenuItem miAdvancedRenameMarked;
    @FXML
    public MenuItem miDuplicateFinderFolder;
    @FXML
    public MenuItem miDuplicateFinderMarked;

    @FXML
    public MenuItem menuItemAbout;
    @FXML
    public MenuItem menuItemTest;

    private ContextMenu tableContextMenu;
    private ContextMenu markedContextMenu;
    private ContextMenu tableDragContextMenu;
    private ContextMenu searchContextMenu;
    private ContextMenu linksContextMenu;
    private ContextMenu errorContextMenu;

    private FileAddressField fileAddress;
    private ManagingClass MC;
    private Finder finder;
    private DATA_SIZE unitSize = DATA_SIZE.KB;
    private SimpleStringProperty propertyUnitSizeName = new SimpleStringProperty(unitSize.sizename);
    private SimpleLongProperty propertyUnitSize = new SimpleLongProperty(unitSize.size);
    private SimpleBooleanProperty propertyUnitSizeAuto = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyReadyToSearch = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty propertyDeleteCondition = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyRenameCondition = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyIsVirtualFolders = new SimpleBooleanProperty(false);

    private SimpleIntegerProperty propertySelectedSize = new SimpleIntegerProperty(0);
    private SimpleIntegerProperty propertyMarkedSelectedSize = new SimpleIntegerProperty(0);
    private SimpleBooleanProperty selectedIsFolder = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty writeableFolder = new SimpleBooleanProperty(false);
    private SimpleTask searchTask;
    public ExtTableView extTableView;
    public ArrayDeque<Future> deq = new ArrayDeque<>();
    private Executor localSearchExecutor = new FastWaitingExecutor(1, WaitTime.ofSeconds(10));
//private Executor localSearchExecutor = Executors.newSingleThreadExecutor();
    private TimeoutTask localSearchTask = new TimeoutTask(1000, 10, () -> {
        FX.submit(this::localSearch);

    });
    private boolean firstTime = true;
    private TimeoutTask searchTimeoutTask = new TimeoutTask(500, 100, () -> {
        FX.submit(this::search);
    });

    public void beforeShow(String title, ExtFolder currentDir) {

        super.beforeShow(title);
        MC = new ManagingClass(currentDir);

    }

    @Override
    public void afterShow() {
        menuItemTest.visibleProperty().bind(FileManagerLB.DEBUG);

        autoClose.selectedProperty().bindBidirectional(ViewManager.getInstance().autoCloseProgressDialogs);
        autoStart.selectedProperty().bindBidirectional(ViewManager.getInstance().autoStartProgressDialogs);
        pinDialogs.selectedProperty().bindBidirectional(ViewManager.getInstance().pinProgressDialogs);
        pinTextInput.selectedProperty().bindBidirectional(ViewManager.getInstance().pinTextInputDialogs);

        propertyDeleteCondition.bind(writeableFolder.and(propertySelectedSize.greaterThan(0)));
        propertyRenameCondition.bind(writeableFolder.and(propertySelectedSize.isEqualTo(1)));

        finder = new Finder("", useRegex.selectedProperty());
        Bindings.bindContentBidirectional(finder.list, searchView.getItems());
        finder.isCanceled.bind(this.propertyReadyToSearch);

        itemCount.textProperty().bind(Bindings.size(finder.list).asString());

        LOAD();
        fileAddress = new FileAddressField(currentDirText);
        propertyIsVirtualFolders.bind(MC.isAbsoluteRoot.not().and(miAdvancedRenameFolder.disableProperty()));
        propertySelectedSize.bind(Bindings.size(selectedList));
        propertyMarkedSelectedSize.bind(Bindings.size(markedView.getSelectionModel().getSelectedItems()));
        Bindings.bindContentBidirectional(selectedList, this.tableView.getSelectionModel().getSelectedItems());
        extTableView = new ExtTableView(tableView);
        extTableView.sortable = true;
        extTableView.prepareChangeListeners();

        //default sort order
        FX.submit(() -> {

            TableColumn typeCol = (TableColumn) tableView.getColumns().get(1);
            TableColumn nameCol = (TableColumn) tableView.getColumns().get(0);
            typeCol.setSortType(TableColumn.SortType.DESCENDING);
            nameCol.setSortType(TableColumn.SortType.ASCENDING);
            tableView.getSortOrder().clear();
            tableView.getSortOrder().add(typeCol);
            tableView.getSortOrder().add(nameCol);

            update();
        });

    }

    @Override
    public void exit() {
        ViewManager.getInstance().closeFrame(windowID);
    }

    @Override
    public void update() {
        FX.submit(() -> {
            if (firstTime) {
                firstTime = false;
            } else {
                localSearch();
            }
            try {
                LocationAPI.getInstance().filterIfExists(MainController.markedList);
            } catch (Exception e) {
                ErrorReport.report(e);
            }
            this.buttonForw.setDisable(!MC.hasForward());
            this.buttonPrev.setDisable(!MC.hasPrev());
            this.buttonParent.setDisable(!MC.hasParent());
            this.miAdvancedRenameFolder.setDisable(MC.currentDir.isNotWriteable());
            this.miDuplicateFinderFolder.disableProperty().bind(miAdvancedRenameFolder.disableProperty());
            this.miAdvancedRenameMarked.disableProperty().bind(Bindings.size(MainController.markedList).isEqualTo(0));
            this.miDuplicateFinderMarked.disableProperty().bind(miAdvancedRenameMarked.disableProperty());
            this.writeableFolder.set(!MC.currentDir.isNotWriteable());

            if (MC.currentDir.isAbsoluteRoot.get()) {
                fileAddress.field.setText(FileManagerLB.ROOT_NAME);
            } else if (MC.currentDir.getIdentity().equals(Identity.VIRTUAL)) {
                fileAddress.field.setText(MC.currentDir.propertyName.get());
            } else {
                fileAddress.field.setText(MC.currentDir.getAbsoluteDirectory());
            }
            fileAddress.field.positionCaret(fileAddress.field.getLength());

            fileAddress.folder = MC.currentDir;
            fileAddress.f = null;
        });

    }

    public void closeAllWindows() {
        FileManagerLB.doOnExit();
        System.exit(0);
    }

    public void createNewWindow() {
        ViewManager.getInstance().newWindow(MC.currentDir);
    }

    public void restart() throws InterruptedException {
        FileManagerLB.restart();
    }

    public void advancedRenameFolder() {
        if (!MC.currentDir.isAbsoluteRoot.get()) {
            ViewManager.getInstance().newAdvancedRenameDialog(MC.currentDir);
        }
    }

    public void advancedRenameMarked() {
        VirtualFolder folder = new VirtualFolder("Marked Files");
        folder.addAll(markedList);
        ViewManager.getInstance().newAdvancedRenameDialog(folder);

    }

    public void duplicateFinderMarked() {
        VirtualFolder folder = new VirtualFolder("Marked Files");
        folder.addAll(markedList);
        ViewManager.getInstance().newDuplicateFinderDialog(folder);
    }

    public void duplicateFinderFolder() {
        if (!MC.currentDir.isAbsoluteRoot.get()) {
            VirtualFolder folder = new VirtualFolder(MC.currentDir.getAbsoluteDirectory());
            folder.addAll(MC.currentDir.getFilesCollection());
            ViewManager.getInstance().newDuplicateFinderDialog(folder);
        }
    }

    public void mediaPlayer() {
        ViewManager.getInstance().newMediaPlayer();

    }

    public void test() throws Exception {
        Log.print("TEST");
//        LocationInRootNode root = new LocationInRootNode("",-1);
//        int i = 0;
//
//        ArrayList<Sfor(ExtPath item:this.MC.currentDir.getListRecursive()){
//            root.add(new LocationInRoot(item.getAbsoluteDirectory(),false),i++);
//        }tring> resolve = LocationInRootNode.nodeFromFile(lt.lb.commons.FileManaging.FileReader.readFromFile("Here.txt")).resolve(true);
//        resolve.forEach(file ->{
//            Log.write(file);
//        });
////        lt.lb.commons.FileManaging.FileReader.writeToFile("Raw.txt", resolve);
//        Log.write(root.specialString());
//        lt.lb.commons.FileManaging.FileReader.writeToFile("Here.txt", Arrays.asList(new String[]{root.specialString()}));
//        Log.write("RESOLVED");
//        for(String p:root.resolve(true)){
//            Log.write(p);
//        }
//
//        Log.write(ViewManager.getInstance().frames.keySet());
//        Log.write(FileManagerLB.getRootSet());
//        Log.write(Log.getInstance().list);
//        Log.writeln(MainController.class.getFields());
//        Log.write("Try to get field");
//        Field f = this.getClass().getField("autoClose");
//        CheckMenuItem get = (CheckMenuItem) f.get(ViewManager.getInstance().getFrame(windowID).getController());
//        get.selectedProperty().set(true);
//        ExtTask copyFiles = TaskFactory.getInstance().copyFiles(MC.currentDir.getListRecursiveFolders(true), LocationAPI.getInstance().getFileOptimized("E:\\Dev\\dest"),
//                LocationAPI.getInstance().getFileOptimized(MC.currentDir.getPathCommands().getParent(1)));
//        ViewManager.getInstance().newProgressDialog(copyFiles);

//        CodeSource codeSource = FileManagerLB.class.getProtectionDomain().getCodeSource();
//        File jarFile = new File(codeSource.getLocation().toURI().getPath());
//        System.out.println(jarFile.getAbsolutePath());
//        ExtPath path = new ExtPath("E:\\FileZZZ\\General Music Folder\\[CHILLSTEP]\\Unsorted\\2 Senses - Found.mp3");
//        LocationInRoot folder = new LocationInRoot("E:\\FileZZZ\\General Music Folder\\[CHILLSTEP]");
//        LocationInRoot file = new LocationInRoot(path.getAbsoluteDirectory());
//        ExtPath closestFileByLocation = LocationAPI.getInstance().getFileByLocation(file);
//        Log.write("Closest",closestFileByLocation);
//        LocationAPI.getInstance().putByLocation(file, path);
//        closestFileByLocation = LocationAPI.getInstance().getFileByLocation(file);
//        Log.write("folder",LocationAPI.getInstance().existByLocation(folder));
//        Log.write("Closest",closestFileByLocation);
//        Log.write("file",LocationAPI.getInstance().existByLocation(file));
//        LocationAPI.getInstance().removeByLocation(file);
//        closestFileByLocation = LocationAPI.getInstance().getFileByLocation(file);
//        Log.write("file",LocationAPI.getInstance().existByLocation(file));
//        Log.write("Closest",closestFileByLocation);
//        Log.println(1,2,3);
//            Thread t = new Thread(TaskFactory.getInstance().populateRecursiveParallel(MC.currentDir,FileManagerLB.DEPTH));
//            t.start();
        TaskFactory.getInstance().populateRecursiveParallelContained(MC.currentDir, 4);
        Log.print("END TEST");
    }

    public void openCustomDir() {
        changeToCustomDir(fileAddress.field.getText().trim());
    }

    private void changeToCustomDir(String possibleDir) {
        try {
            if (possibleDir.equals(FileManagerLB.ROOT_NAME) || possibleDir.isEmpty()) {
                changeToDir(FileManagerLB.ArtificialRoot);
            } else {
                ExtFolder fileAndPopulate = (ExtFolder) LocationAPI.getInstance().getFileAndPopulate(possibleDir);
                if (!MC.currentDir.equals(fileAndPopulate)) {
                    this.changeToDir(fileAndPopulate);
                } else {
                    update();
                }

            }
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
    }

    public void changeToParent() {
        MC.changeToParent();
        this.localSearch.clear();
        update();
    }

    public void changeToPrevious() {
        MC.changeToPrevious();
        this.localSearch.clear();
        update();

    }

    public void changeToForward() {
        MC.changeToForward();
        this.localSearch.clear();
        update();
    }

    private void changeToDir(ExtFolder dir) {
//        return new Thread(() -> {
        Future future = TaskFactory.getInstance().populateRecursiveParallelContained(dir, 1);
        F.checkedRun(future::get);
//            t.start();
        if (!localSearch.getText().isEmpty()) {
            localSearch.clear();
        }
//            F.unsafeRunWithHandler(ErrorReport::report, future::get);
        FX.submit(() -> {
            MC.changeDirTo(dir);
            update();
        });

//        });
    }

    public void searchTyped() {
        if (!this.useRegex.isSelected()) {
            this.searchTimeoutTask.update();
        }
    }

    public void search() {
        String pattern = this.searchField.getText();
        this.propertyReadyToSearch.set(true);
        finder.list.clear();
        searchView.getItems().clear();
        if (searchTask != null) {
            searchTask.cancel();
        }
        if (pattern.length() > 1) {
            this.propertyReadyToSearch.set(false);

            this.searchStatus.setText("Searching");
            searchTask = new SimpleTask() {
                @Override
                protected Void call() throws Exception {
                    finder.newTask(pattern);

                    if (!MC.currentDir.isVirtual.get()) {
                        try {
                            Files.walkFileTree(MC.currentDir.toPath(), finder);

                        } catch (Exception ex) {
                            ErrorReport.report(ex);
                        }
                    } else if (!MC.currentDir.isNotWriteable() && !MC.currentDir.isAbsoluteRoot.get()) {
                        try {
                            for (ExtPath file : MC.currentDir.getFilesCollection()) {
                                Files.walkFileTree(file.toPath(), finder);
                            }
                        } catch (Exception ex) {
                            ErrorReport.report(ex);
                        }
                    }
                    return null;
                }
            ;
            };
            searchTask.setOnSucceeded(eh -> {
                searchStatus.setText("Waiting");
            });
            new Thread(searchTask).start();

        }
    }

    public void localSearchTask() {
        localSearchTask.update();
    }

    public void localSearch() {
        ObservableList<ExtPath> newList = FXCollections.observableArrayList();
        deq.forEach(action -> {
            action.cancel(true);
        });
        deq.clear();
        ExtTask asynchronousSortTask = extTableView.asynchronousSortTask(newList);
        final FutureTask asyncFuture = new FutureTask(Executors.callable(asynchronousSortTask));
        ExtFolder folderInitiated = MC.currentDir;
        ExtTask r = new ExtTask() {
            @Override
            protected Void call() throws Exception {

                FX.submit(() -> {
                    extTableView.saveSortPrefereces();
                    TaskFactory.mainExecutor.execute(asyncFuture);

                });

                if (canceled.get()) {
                    Log.print("Canceled from task before start");
                    return null;
                }

                SimpleBooleanProperty can = new SimpleBooleanProperty(canceled.get());
                can.bind(canceled);
                Future update = folderInitiated.update(newList, can);

                if (canceled.get()) {
                    Log.print("Canceled from task");
                    return null;
                }
                update.get();
                newList.setAll(folderInitiated.files.values());
                String lookFor = localSearch.getText().trim();
                if (!lookFor.isEmpty()) {
                    ArrayList<ExtPath> list = new ArrayList<>();
                    newList.forEach(item -> {
                        ExtPath path = (ExtPath) item;
                        String name = path.propertyName.get();
                        if (StringOp.containsIgnoreCase(name, lookFor)) {
                            list.add(path);
                        }
                    });
                    newList.setAll(list);
                }
                return null;
            }
        };
        deq.addFirst(r);
        r.setOnCancelled(event -> {
            Log.print("Actually canceled");
        });
        r.setOnDone(event -> {
            asynchronousSortTask.setOnDone(handle -> {
                if (r.canceled.get()) {
                    return;
                }
                final int viewSize = extTableView.table.getItems().size();
                final int neededSize = newList.size();
                Log.print("View size", viewSize, "Needed size", neededSize);
                if (viewSize != neededSize) {
                    FX.submit(() -> {
                        extTableView.updateContentsAndSort(newList);
                    });
                }
            });
            asynchronousSortTask.cancel();
        });
        localSearchExecutor.execute(r);
    }

    public void loadSnapshot() {
        try {
            String possibleSnapshot = this.snapshotLoadField.getText().trim();

            File file = new File(possibleSnapshot);

            if (file.exists()) {
                new Thread(TaskFactory.getInstance().snapshotLoadTask(this.windowID, MC.currentDir, file)).start();
            } else {
                ErrorReport.report(new Exception("No such File:" + file.getAbsolutePath()));
            }
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
    }

    public void createSnapshot() {
        if (MC.currentDir.isVirtual.get()) {
            ErrorReport.report(new Exception("Cannot create stapshots from virtual folders"));
            return;
        }
        this.snapshotView.getItems().add("Creating snapshot at " + MC.currentDir.getAbsoluteDirectory());
        String possibleSnapshot = this.snapshotCreateField.getText().trim();
        File file = new File(TaskFactory.resolveAvailablePath(MC.currentDir, possibleSnapshot));
        new Thread(TaskFactory.getInstance().snapshotCreateWriteTask(windowID, MC.currentDir, file)).start();

    }

    public void dirSync() {
        ViewManager.getInstance().newDirSyncDialog();

    }

    public void regexHelp() {
        FX.submit(() -> {
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.Regex);
        });
    }

    public void aboutPage() {
        FX.submit(() -> {
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
        });
    }

    public void commandWindow() {
        ViewManager.getInstance().newCommandDialog();
    }

    private void handleOpen(ExtPath file) {
        if (file instanceof ExtFolder) {
            Log.print("Change to dir " + file.getAbsoluteDirectory());
            changeToDir((ExtFolder) file);
        } else {

            try {
                if (file.getIdentity().equals(Enums.Identity.LINK)) {
                    ExtLink link = (ExtLink) file;
                    LocationInRoot location = new LocationInRoot(link.getTargetDir());
                    if (link.isPointsToDirectory()) {
                        changeToDir((ExtFolder) LocationAPI.getInstance().getFileIfExists(location));
                    } else {
                        DesktopApi.open(LocationAPI.getInstance().getFileIfExists(location).toPath().toFile());
                    }
                } else if (file.getIdentity().equals(Enums.Identity.FILE)) {
                    DesktopApi.open(file.toPath().toFile());
                }
            } catch (Exception x) {
                ErrorReport.report(x);
            }
        }
    }

    private void hideAllContextMenus() {
        tableContextMenu.hide();
        markedContextMenu.hide();
        linksContextMenu.hide();
        tableDragContextMenu.hide();
        errorContextMenu.hide();
        searchContextMenu.hide();
    }

    private void setUpContextMenus() {
        tableDragContextMenu = new ContextMenu();
        tableContextMenu = new ContextMenu();
        markedContextMenu = new ContextMenu();
        searchContextMenu = new ContextMenu();
        linksContextMenu = new ContextMenu();
        errorContextMenu = new ContextMenu();

        Menu submenuCreate = new Menu("Create...");
        submenuCreate.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Create New Folder",
                        event -> {
                            Log.print("Create new folder");
                            try {
                                ExtPath createNewFolder = MC.createNewFolder();
                                ViewManager.getInstance().newRenameDialog(MC.currentDir, createNewFolder);
                                MainController.this.update();
                            } catch (Exception ex) {
                                ErrorReport.report(ex);
                            }
                        }, MC.isVirtual.not()),
                CosmeticsFX.simpleMenuItem("Create New File",
                        event -> {
                            Log.print("Create new file");
                            try {
                                ExtPath createNewFile = MC.createNewFile();
                                ViewManager.getInstance().newRenameDialog(MC.currentDir, createNewFile);
                                MainController.this.update();
                            } catch (IOException ex) {
                                ErrorReport.report(ex);
                            }
                        }, MC.isVirtual.not()));

        searchContextMenu.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Go to",
                        event -> {
                            String selectedItem = (String) searchView.getSelectionModel().getSelectedItem();
                            try {
                                Path path = Paths.get(selectedItem);
                                if (Files.isDirectory(path)) {
                                    changeToCustomDir(selectedItem);
                                } else {
                                    changeToCustomDir(path.getParent().toString());
                                }
                            } catch (Exception ex) {
                                ErrorReport.report(ex);
                            }
                        }, Bindings.size(searchView.getSelectionModel().getSelectedItems()).isEqualTo(1)),
                CosmeticsFX.simpleMenuItem("Mark selected",
                        event -> {
                            FX.submit(() -> {
                                ArrayDeque<String> list = new ArrayDeque<>(searchView.getSelectionModel().getSelectedItems());
                                FXTask markFiles = TaskFactory.getInstance().markFiles(list);
                                TaskFactory.mainExecutor.execute(markFiles);
                            });
                        }, Bindings.size(searchView.getSelectionModel().getSelectedItems()).greaterThan(0))
        );

        errorContextMenu.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Remove this entry",
                        event -> {
                            errorLog.remove(errorView.getSelectionModel().getSelectedIndex());
                        }, Bindings.size(errorView.getSelectionModel().getSelectedItems()).greaterThan(0)),
                CosmeticsFX.simpleMenuItem("Clean this list",
                        event -> {
                            errorLog.clear();
                        }, Bindings.size(errorView.getSelectionModel().getSelectedItems()).greaterThan(0))
        );

        linksContextMenu.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Add current directory as link",
                        event -> {
                            FavouriteLink link = new FavouriteLink(MC.currentDir.propertyName.get(), MC.currentDir);
                            if (!links.contains(link)) {
                                links.add(link);
                            }
                        }, null),
                CosmeticsFX.simpleMenuItem("Remove this link",
                        event -> {
                            links.remove(linkView.getSelectionModel().getSelectedIndex());
                        }, Bindings.size(this.linkView.getSelectionModel().getSelectedItems()).greaterThan(0))
        );

        markedContextMenu.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Clean this list",
                        event -> {
                            markedView.getItems().clear();
                        }, propertyMarkedSize.greaterThan(0)),
                CosmeticsFX.simpleMenuItem("Remove selected",
                        event -> {
                            ObservableList selectedItems = markedView.getSelectionModel().getSelectedItems();
                            MainController.markedList.removeAll(selectedItems);
                        }, propertyMarkedSize.greaterThan(0).and(Bindings.size(
                                markedView.getSelectionModel().getSelectedItems()).greaterThan(0))),
                CosmeticsFX.simpleMenuItem("Delete selected",
                        event -> {
                            ContinousCombinedTask task = TaskFactory.getInstance().deleteFilesEx(markedView.getSelectionModel().getSelectedItems());
                            task.setDescription("Delete selected marked files");
                            ViewManager.getInstance().newProgressDialog(task);
                        }, propertyMarkedSize.greaterThan(0).and(Bindings.size(
                                markedView.getSelectionModel().getSelectedItems()).greaterThan(0)))
        );

        tableDragContextMenu.getItems().setAll(CosmeticsFX.simpleMenuItem("Move here selected",
                        event -> {
                            MainController.actionList.clear();
                            MainController.actionList.addAll(MainController.dragList);
                            ContinousCombinedTask task = TaskFactory.getInstance().moveFilesEx(MainController.actionList, MC.currentDir);
                            task.setDescription("Move Dragged files");
                            ViewManager.getInstance().newProgressDialog(task);
                        }, null),
                CosmeticsFX.simpleMenuItem("Copy here selected",
                        event -> {
                            MainController.actionList.clear();
                            MainController.actionList.addAll(MainController.dragList);
                            ContinousCombinedTask task = TaskFactory.getInstance().copyFilesEx(MainController.actionList, MC.currentDir, null);
                            task.setDescription("Copy Dragged files");
                            ViewManager.getInstance().newProgressDialog(task);
                        }, null)
        );

        Menu submenuMarkFiles = new Menu("Mark...");
        Callback<ExtPath, Void> addToMarkedCallback = (ExtPath p) -> {
            TaskFactory.getInstance().addToMarked(p);
            return null;
        };
        submenuMarkFiles.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Selected",
                        event -> {
                            selectedList.forEach((file) -> {
                                TaskFactory.getInstance().addToMarked(file);
                            });
                        }, miDuplicateFinderFolder.disableProperty().not().and(propertySelectedSize.greaterThan(0))),
                CosmeticsFX.simpleMenuItem("Recursive only files",
                        event -> {
                            selectedList.forEach((file) -> {

                                Runnable run = () -> {
                                    file.collectRecursive(ExtPath.IS_NOT_DISABLED.and(ExtPath.IS_FILE), addToMarkedCallback);
                                };
                                TaskFactory.mainExecutor.execute(run);

                            });
                        }, miDuplicateFinderFolder.disableProperty().not().and(propertySelectedSize.greaterThan(0))),
                CosmeticsFX.simpleMenuItem("Recursive only folders",
                        event -> {
                            selectedList.forEach((file) -> {
                                Runnable run = () -> {
                                    file.collectRecursive(ExtPath.IS_NOT_DISABLED.and(ExtPath.IS_FOLDER), addToMarkedCallback);
                                };
                                TaskFactory.mainExecutor.execute(run);
                            });
                        }, miDuplicateFinderFolder.disableProperty().not().and(propertySelectedSize.greaterThan(0)))
        );

        Menu submenuMarked = new Menu("Marked...");
        submenuMarked.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Copy here marked",
                        event -> {
                            Log.print("Copy Marked");
                            ContinousCombinedTask task = TaskFactory.getInstance().copyFilesEx(markedList, MC.currentDir, null);
                            task.setDescription("Copy marked files");
                            ViewManager.getInstance().newProgressDialog(task);
                        }, propertyMarkedSize.greaterThan(0).and(MC.isVirtual.not())),
                CosmeticsFX.simpleMenuItem("Move here marked",
                        event -> {
                            Log.print("Move Marked");
                            ContinousCombinedTask task = TaskFactory.getInstance().moveFilesEx(markedList, MC.currentDir);
                            task.setDescription("Move marked files");
                            ViewManager.getInstance().newProgressDialog(task);
                        }, propertyMarkedSize.greaterThan(0).and(MC.isVirtual.not())),
                CosmeticsFX.simpleMenuItem("Delete all marked",
                        event -> {
                            ContinousCombinedTask task = TaskFactory.getInstance().deleteFilesEx(markedList);
                            task.setDescription("Delete marked files");
                            ViewManager.getInstance().newProgressDialog(task);
                        }, propertyMarkedSize.greaterThan(0)),
                CosmeticsFX.simpleMenuItem("Add Marked to Virtual Folder",
                        event -> {
                            FX.submit(() -> {
                                MainController.markedList.forEach((f) -> {
                                    MC.currentDir.files.put(f.propertyName.get(), f);
                                });
                                update();
                            });
                        }, propertyMarkedSize.greaterThan(0).and(writeableFolder).and(MC.isVirtual))
        );

        tableContextMenu.getItems().setAll(
                CosmeticsFX.simpleMenuItem("Open",
                        event -> {
                            handleOpen((ExtPath) tableView.getSelectionModel().getSelectedItem());
                        }, propertySelectedSize.isEqualTo(1)),
                CosmeticsFX.simpleMenuItem("Open in new window",
                        event -> {
                            ViewManager.getInstance().newWindow((ExtFolder) tableView.getSelectionModel().getSelectedItem());
                        }, selectedIsFolder),
                CosmeticsFX.simpleMenuItem("Rename",
                        event -> {
                            rename();
                        }, propertyRenameCondition),
                CosmeticsFX.simpleMenuItem("Delete",
                        event -> {
                            delete();
                        }, propertyDeleteCondition),
                CosmeticsFX.simpleMenuItem("Copy path",
                        event -> {
                            String absolutePath = selectedList.get(0).getAbsolutePath();
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(absolutePath), null);
                        }, propertyRenameCondition),
                CosmeticsFX.simpleMenuItem("Toggle Enable/Disable",
                        event -> {
                            FX.submit(() -> {
                                selectedList.stream().forEach(c -> {
                                    c.isDisabled.setValue(c.isDisabled.not().get());
                                });
                            });
                        }, propertyDeleteCondition),
                CosmeticsFX.simpleMenuItem("Create Virtual Folder",
                        event -> {
                            FX.submit(() -> {
                                VirtualFolder.createVirtualFolder();
                                update();
                            });
                        }, propertyIsVirtualFolders),
                CosmeticsFX.simpleMenuItem("Remove selected from Virtual Folder",
                        event -> {
                            FX.submit(() -> {
                                ObservableList<ExtPath> selectedItems = tableView.getSelectionModel().getSelectedItems();
                                selectedItems.forEach((item) -> {
                                    ExtPath remove = MC.currentDir.files.remove(item.propertyName.get());
                                    if (remove instanceof ExtFolder) {
                                        ExtFolder folder = (ExtFolder) remove;
                                        folder.files.clear();
                                    }
                                });
                                update();
                            });
                        }, MC.isVirtual.and(propertySelectedSize.greaterThan(0))),
                submenuCreate,
                submenuMarked,
                submenuMarkFiles
        );

    }

    private void setUpTableView() {
        //TABLE VIEW SETUP

        TableColumn<ExtPath, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().propertyName);
        nameCol.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<ExtPath, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().propertyType);

        TableColumn<ExtPath, String> sizeCol = new TableColumn<>();
        sizeCol.textProperty().bind(this.propertyUnitSizeName);
        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtPath, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtPath, String> cellData) {

                if (cellData.getValue() instanceof ExtFolder) {
                    return new SimpleStringProperty("");
                } else if (propertyUnitSizeAuto.get()) {
                    return cellData.getValue().propertySizeAuto;
                } else {
                    SimpleStringProperty string = new SimpleStringProperty();
                    Double get = cellData.getValue().propertySize.divide((double) propertyUnitSize.get()).get();
                    if (get > 0.001) {
                        string.set(StringOp.extractNumber(get));
                    } else {
                        string.set("0");
                    }
                    return string;
                }
            }
        });
        sizeCol.setComparator(ExtPath.COMPARE_SIZE_STRING);
        TableColumn<ExtPath, String> dateCol = new TableColumn<>("Last Modified");
        dateCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().propertyDate);
        TableColumn<ExtPath, String> disabledCol = new TableColumn<>("Disabled");
        disabledCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().isDisabled.asString());
        tableView.getColumns().addAll(nameCol, typeCol, sizeCol, dateCol, disabledCol);

        tableView.setContextMenu(tableContextMenu);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(tableView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(tableView.getContextMenu());
        //TABLE VIEW ACTIONS

        tableView.setOnMousePressed((MouseEvent event) -> {
            hideAllContextMenus();

            if (event.isPrimaryButtonDown()) {
                if (!tableView.getSelectionModel().isEmpty()) {
                    if (event.getClickCount() > 1) {
                        ExtPath file = (ExtPath) tableView.getSelectionModel().getSelectedItem();
                        handleOpen(file);
                    } else {
                        selectedList = tableView.getSelectionModel().getSelectedItems();
                    }
                }
            } else {
                selectedIsFolder.set(tableView.getSelectionModel().getSelectedItem() instanceof ExtFolder);
            }
        });
        tableView.setOnKeyReleased(eh -> {
            KeyCode code = eh.getCode();
            if (code.equals(KeyCode.DELETE)) {
                delete();
            } else if (code.equals(KeyCode.F2)) {
                rename();
            }
        });
        tableView.setOnDragDetected((MouseEvent event) -> {
            if (MC.currentDir.isNotWriteable()) {
                return;
            }
            TaskFactory.dragInitWindowID = this.windowID;
            if (this.extTableView.recentlyResized.get()) {
                return;
            }
            // drag was detected, start drag-and-drop gesture
            MainController.dragList = selectedList;
            if (!MainController.dragList.isEmpty()) {
                Dragboard db = tableView.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                content.putString("Ready");
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        }); //drag

        tableView.setOnDragOver((DragEvent event) -> {
            if (MC.currentDir.isNotWriteable()) {
                return;
            }
            if (this.windowID.equals(TaskFactory.dragInitWindowID)) {

                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);

                //Log.writeln(event.getDragboard().getString());
            }
            event.consume();
        });

        tableView.setOnDragDropped((DragEvent event) -> {
//            Log.print("Drag dropped");
            if (MC.currentDir.isVirtual.get()) {
                return;
            }
            if (this.windowID.equals(TaskFactory.dragInitWindowID)) {
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!MainController.dragList.isEmpty()) {

                tableDragContextMenu.show(tableView, event.getScreenX(), event.getScreenY());
                tableDragContextMenu.getOwnerNode().requestFocus();
                tableDragContextMenu.getOwnerWindow().requestFocus();
                //ViewManager.getInstance().windows.get(title).getStage().requestFocus();
                success = true;
            } else {
                Log.print("Drag list is empty");
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setUpListViews() {

        //***************************************
        //Marked View
        markedView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        markedView.setItems(MainController.markedList);
        IntegerBinding size = Bindings.size(markedView.getItems());
        markedSize.textProperty().bind(size.asString());
        markedView.setContextMenu(markedContextMenu);
        markedView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(markedView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(markedView.getContextMenu());

        markedView.setOnDragDetected((MouseEvent event) -> {
            TaskFactory.dragInitWindowID = "MARKED";
            if (MC.currentDir.isNotWriteable()) {
                return;
            }

            // drag was detected, start drag-and-drop gesture
            dragList = markedView.getSelectionModel().getSelectedItems();
            if (!MainController.dragList.isEmpty()) {
                Dragboard db = markedView.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                content.putString("Ready");
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        }); //drag

        markedView.setOnDragOver((DragEvent event) -> {
            if (MC.currentDir.isNotWriteable()) {
                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);

                //Log.writeln(event.getDragboard().getString());
            }
            event.consume();
        });

        markedView.setOnDragDropped((DragEvent event) -> {
            if (MC.currentDir.isNotWriteable()) {
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!MainController.dragList.isEmpty()) {
                for (ExtPath f : MainController.dragList) {
                    TaskFactory.getInstance().addToMarked(f);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        //***************************************
        //Search View
        searchView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        searchView.getItems().setAll(finder.list);
        searchView.setContextMenu(searchContextMenu);
        searchView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(searchView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(searchView.getContextMenu());

        //***************************************
        //Link View
        linkView.setContextMenu(linksContextMenu);
        linkView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(linkView.getSelectionModel()));

        linkView.setItems(links);
        linkView.setCellFactory(new Callback<ListView<FavouriteLink>, ListCell<FavouriteLink>>() {
            @Override
            public ListCell<FavouriteLink> call(ListView<FavouriteLink> p) {
                ListCell<FavouriteLink> cell = new ListCell<FavouriteLink>() {
                    @Override
                    protected void updateItem(FavouriteLink t, boolean bln) {
                        super.updateItem(t, bln);
                        if (t != null) {
                            setText(t.getPropertyName().get());
                            if (!t.getPropertyName().get().equals(FileManagerLB.ROOT_NAME)) {
                                setTooltip(t.getToolTip());
                            }
                        } else {
                            setText("");
                            this.tooltipProperty().unbind();
                        }
                    }
                };

                return cell;
            }
        });
        linkView.setOnMousePressed((MouseEvent eh) -> {
            if (eh.isPrimaryButtonDown()) {
                if (eh.getClickCount() > 1) {
                    FavouriteLink selectedItem = (FavouriteLink) linkView.getSelectionModel().getSelectedItem();
                    changeToDir((ExtFolder) selectedItem.location);
                }
            }
        });

        //***************************************
        //Error View
        errorView.setItems(errorLog);
        errorView.setContextMenu(errorContextMenu);
        errorView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(errorView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(errorView.getContextMenu());

        errorView.setCellFactory(new Callback<ListView<ErrorReport>, ListCell<ErrorReport>>() {
            @Override
            public ListCell<ErrorReport> call(ListView<ErrorReport> p) {
                ListCell<ErrorReport> cell = new ListCell<ErrorReport>() {

                    @Override
                    protected void updateItem(ErrorReport t, boolean bln) {
                        FX.submit(() -> {
                            super.updateItem(t, bln);
                            if (t != null) {
                                setText(t.getErrorName().get());
                                setTooltip(t.getTooltip());
                            } else {
                                setText("");
                                this.tooltipProperty().unbind();
                            }
                        });

                    }
                };

                return cell;
            }
        });

    }

    private void LOAD() {
        setUpContextMenus();
        setUpTableView();
        setUpListViews();

        getStage().getScene().setOnMouseClicked((eh) -> {
            markedView.getSelectionModel().clearSelection();
            tableView.getSelectionModel().clearSelection();
            searchView.getSelectionModel().clearSelection();
            errorView.getSelectionModel().clearSelection();
            linkView.getSelectionModel().clearSelection();
        });

        for (MenuItem item : menuSizeUnits.getItems()) {
            String sizeType = item.getText();

            if ("Auto".equalsIgnoreCase(sizeType)) {
                item.setOnAction(eh -> {
                    setSizeAuto();
                });
            } else {
                item.setOnAction(eh -> {
                    this.propertyUnitSizeAuto.set(false);
                    this.unitSize = DATA_SIZE.valueOf(sizeType);

                    Log.print(unitSize);
                    this.propertyUnitSizeName.set("Size " + unitSize.sizename);
                    this.propertyUnitSize.set(unitSize.size);
                    this.update();
                });
            }
        }
        FX.submit(() -> {
            setSizeAuto();
//            tableView.getColumns().setAll(columns);
        });

    }

    private void setSizeAuto() {
        this.propertyUnitSizeAuto.set(true);
        this.propertyUnitSizeName.set("Size Auto");
        this.update();
    }

    private void delete() {
        if (this.propertyDeleteCondition.get()) {
            Log.print("Deleting");
            ContinousCombinedTask task = TaskFactory.getInstance().deleteFilesEx(selectedList);
            task.setDescription("Delete selected files");
            ViewManager.getInstance().newProgressDialog(task);
        }

    }

    private void rename() {
        if (this.propertyRenameCondition.get()) {
            Log.print("Invoke rename dialog");
            ExtPath path = (ExtPath) tableView.getSelectionModel().getSelectedItem();
            ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileOptimized(path.getParent(1));
            ViewManager.getInstance().newRenameDialog(parent, path);
        }
    }
}
