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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
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
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import lt.lb.commons.F;
import lt.lb.commons.javafx.CosmeticsFX;
import lt.lb.commons.javafx.CosmeticsFX.ExtTableView;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.FXDefs;
import lt.lb.commons.javafx.FXTask;
import lt.lb.commons.javafx.MenuBuilders;
import lt.lb.commons.threads.service.ServiceTimeoutTask;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.filemanagerlb.D;
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
import lt.lb.filemanagerlb.utility.ExtStringUtils;
import lt.lb.filemanagerlb.utility.FavouriteLink;
import lt.lb.filemanagerlb.utility.Finder;
import lt.lb.filemanagerlb.utility.SimpleTask;
import lt.lb.uncheckedutils.PassableException;
import lt.lb.uncheckedutils.SafeOpt;
import org.tinylog.Logger;
import lt.lb.commons.javafx.properties.SelectableViewProperties;
import lt.lb.filemanagerlb.utility.SafeJob;
import lt.lb.jobsystem.Job;
import lt.lb.jobsystem.events.SystemJobEventName;
import lt.lb.uncheckedutils.Checked;
import org.apache.commons.lang3.Strings;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class MainController extends MyBaseController<MainController> {

    @FXML
    public CheckMenuItem autoClose;
    @FXML
    public CheckMenuItem autoStart;
    @FXML
    public CheckMenuItem pinDialogs;
    @FXML
    public CheckMenuItem pinTextInput;
    @FXML
    public CheckMenuItem replaceOnCopy;
    @FXML
    public SplitPane splitPane;

    @FXML
    public TableView tableView;
    @FXML
    public TextField localSearch;

    @FXML
    public Label localSearchLabel;

    public static ObservableList<FavouriteLink> favoriteLinks;
    public static ObservableList<ErrorReport> errorLog;

    public static ObservableList<ExtPath> markedList;
    public static IntegerBinding propertyMarkedSize;

    public List<ExtPath> dragList = new ArrayList<>();

    @FXML
    public CheckBox useRegex;
    @FXML
    public Label itemCount;
    @FXML
    public ListView searchView;
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
    private DATA_SIZE unitSize = DATA_SIZE.KB;
    private SimpleStringProperty propertyUnitSizeName = new SimpleStringProperty(unitSize.sizename);
    private SimpleLongProperty propertyUnitSize = new SimpleLongProperty(unitSize.size);
    private SimpleBooleanProperty propertyUnitSizeAuto = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyDeleteCondition = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyRenameCondition = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyIsVirtualFolders = new SimpleBooleanProperty(false);

    private SimpleBooleanProperty selectedIsFolder = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty writeableFolder = new SimpleBooleanProperty(false);

    private SelectableViewProperties<ExtPath> markedProperties;
    private SelectableViewProperties<ExtPath> filesProperties;
    private SelectableViewProperties<ErrorReport> errorProperties;
    private SimpleTask searchTask;
    public ExtTableView extTableView;
    public Job<Void> localSearchJob = new Job<>(m -> {
    });
    private ServiceTimeoutTask localSearchTask2 = new ServiceTimeoutTask(D.exe.scheduledService("localSearch-sched"), FX::submit, WaitTime.ofMillis(200), Executors.callable(this::localSearch));
    private ServiceTimeoutTask searchTimeoutTask2 = new ServiceTimeoutTask(D.exe.scheduledService("search-sched"), FX::submit, WaitTime.ofMillis(200), Executors.callable(this::search));
    private boolean firstTime = true;

    public void beforeShow(String title, ExtFolder currentDir) {
        super.beforeShow(title);
        MC = new ManagingClass(currentDir);

    }

    @Override
    public void afterShow() {

        filesProperties = SelectableViewProperties.ofTableView(tableView);
        markedProperties = SelectableViewProperties.ofListView(markedView);
        errorProperties = SelectableViewProperties.ofListView(errorView);

        BooleanBinding createBooleanBinding = Bindings.createBooleanBinding(() -> {
            return SafeOpt.ofNullable(filesProperties.selectedItem().get()).select(ExtFolder.class).isPresent();
        }, filesProperties.selectedItem());
        selectedIsFolder.bind(createBooleanBinding);
        menuItemTest.visibleProperty().bind(D.DEBUG);

        autoClose.selectedProperty().bindBidirectional(ViewManager.autoCloseProgressDialogs);
        autoStart.selectedProperty().bindBidirectional(ViewManager.autoStartProgressDialogs);
        pinDialogs.selectedProperty().bindBidirectional(ViewManager.pinProgressDialogs);
        pinTextInput.selectedProperty().bindBidirectional(ViewManager.pinTextInputDialogs);
        replaceOnCopy.selectedProperty().bindBidirectional(TaskFactory.copyReplaceExisting);

        propertyDeleteCondition.bind(filesProperties.selectedSomething().and(writeableFolder));
        propertyRenameCondition.bind(filesProperties.selectedSize(1).and(writeableFolder));

        itemCount.textProperty().bind(Bindings.size(searchView.getItems()).asString());

        fileAddress = new FileAddressField(currentDirText);
        propertyIsVirtualFolders.bind(MC.isAbsoluteRoot.not().and(miAdvancedRenameFolder.disableProperty()));

        LOAD();

        update();

    }

    @Override
    public void update() {
        fxDelegator.addAction("update", () -> {
            if (firstTime) {
                firstTime = false;
            } else {
                localSearch();
            }
            try {
                LocationAPI.filterIfExists(MainController.markedList);
            } catch (Exception e) {
                ErrorReport.report(e);
            }
            this.buttonForw.setDisable(!MC.hasForward());
            this.buttonPrev.setDisable(!MC.hasPrev());
            this.buttonParent.setDisable(!MC.hasParent());
            this.miAdvancedRenameFolder.setDisable(MC.currentDir.isArtificial());
            this.miDuplicateFinderFolder.disableProperty().bind(miAdvancedRenameFolder.disableProperty());
            this.miAdvancedRenameMarked.disableProperty().bind(Bindings.size(MainController.markedList).isEqualTo(0));
            this.miDuplicateFinderMarked.disableProperty().bind(miAdvancedRenameMarked.disableProperty());
            this.writeableFolder.set(!MC.currentDir.isArtificial());

            if (MC.currentDir.isAbsoluteRoot.get()) {
                fileAddress.field.setText(D.ROOT_NAME);
            } else if (MC.currentDir.getIdentity().equals(Identity.VIRTUAL)) {
                fileAddress.field.setText(MC.currentDir.propertyName.get());
            } else {
                fileAddress.field.setText(MC.currentDir.getAbsoluteDirectory());
            }
            fileAddress.field.positionCaret(fileAddress.field.getLength());

            fileAddress.folder = MC.currentDir;
            fileAddress.entered = null;
        });

    }

    public void closeAllWindows() {
        FileManagerLB.doOnExit();
    }

    public void createNewWindow() {
        ViewManager.newWindow(MC.currentDir);
    }

    public void restart() throws InterruptedException {
        FileManagerLB.restart();
    }

    public void advancedRenameFolder() {
        if (!MC.currentDir.isAbsoluteRoot.get()) {
            ViewManager.newAdvancedRenameDialog(MC.currentDir);
        }
    }

    public void advancedRenameMarked() {
        VirtualFolder folder = new VirtualFolder("Marked Files");
        folder.addAll(markedList);
        ViewManager.newAdvancedRenameDialog(folder);

    }

    public void duplicateFinderMarked() {
        VirtualFolder folder = new VirtualFolder("Marked Files");
        folder.addAll(markedList);
        ViewManager.newDuplicateFinderDialog(folder);
    }

    public void duplicateFinderFolder() {
        if (!MC.currentDir.isAbsoluteRoot.get()) {
            VirtualFolder folder = new VirtualFolder(MC.currentDir.getAbsoluteDirectory());
            folder.addAll(MC.currentDir.getFilesCollection());
            ViewManager.newDuplicateFinderDialog(folder);
        }
    }

    public void mediaPlayer() {
        ViewManager.newMediaPlayer();

    }

    public void test() throws Exception {

        Logger.info("TEST");
        Stage stage = new Stage();

        Label secondLabel = new Label("I'm a Label on new Window");

        StackPane secondaryLayout = new StackPane();
        secondaryLayout.getChildren().add(secondLabel);

        Scene secondScene = new Scene(secondaryLayout, 230, 100);

        // New window (Stage)
        Stage newWindow = new Stage();
        newWindow.setTitle("Second Stage");
        newWindow.setScene(secondScene);

        // Set position of second window, related to primary window.
        newWindow.show();

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
//        Log.write(ViewManager.frames.keySet());
//        Log.write(FileManagerLB.getRootSet());
//        Log.write(Log.getInstance().list);
//        Log.writeln(MainController.class.getFields());
//        Log.write("Try to get field");
//        Field f = this.getClass().getField("autoClose");
//        CheckMenuItem get = (CheckMenuItem) f.get(ViewManager.getFrame(windowID).getController());
//        get.selectedProperty().set(true);
//        ExtTask copyFiles = TaskFactory.copyFiles(MC.currentDir.getListRecursiveFolders(true), LocationAPI.getFileOptimized("E:\\Dev\\dest"),
//                LocationAPI.getFileOptimized(MC.currentDir.getPathCommands().getParent(1)));
//        ViewManager.newProgressDialog(copyFiles);
//        CodeSource codeSource = FileManagerLB.class.getProtectionDomain().getCodeSource();
//        File jarFile = new File(codeSource.getLocation().toURI().getPath());
//        System.out.println(jarFile.getAbsolutePath());
//        ExtPath path = new ExtPath("E:\\FileZZZ\\General Music Folder\\[CHILLSTEP]\\Unsorted\\2 Senses - Found.mp3");
//        LocationInRoot folder = new LocationInRoot("E:\\FileZZZ\\General Music Folder\\[CHILLSTEP]");
//        LocationInRoot file = new LocationInRoot(path.getAbsoluteDirectory());
//        ExtPath closestFileByLocation = LocationAPI.getFileByLocation(file);
//        Log.write("Closest",closestFileByLocation);
//        LocationAPI.putByLocation(file, path);
//        closestFileByLocation = LocationAPI.getFileByLocation(file);
//        Log.write("folder",LocationAPI.existByLocation(folder));
//        Log.write("Closest",closestFileByLocation);
//        Log.write("file",LocationAPI.existByLocation(file));
//        LocationAPI.removeByLocation(file);
//        closestFileByLocation = LocationAPI.getFileByLocation(file);
//        Log.write("file",LocationAPI.existByLocation(file));
//        Log.write("Closest",closestFileByLocation);
//        Logger.infoln(1,2,3);
//            Thread t = new Thread(TaskFactory.populateRecursiveParallel(MC.currentDir,FileManagerLB.DEPTH));
//            t.start();
//        TaskFactory.populateRecursiveParallelContained(MC.currentDir, 4);
        Logger.info("END TEST");
    }

    public void openCustomDir() {
        changeToCustomDir(fileAddress.field.getText().trim());
    }

    private void changeToCustomDir(String possibleDir) {
        try {
            if (possibleDir.equals(D.ROOT_NAME) || possibleDir.isEmpty()) {
                changeToDir(FileManagerLB.ArtificialRoot);
            } else {
                ExtFolder fileAndPopulate = (ExtFolder) LocationAPI.getFileAndPopulate(possibleDir);
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
        extTableView.resetScrollData();
        update();
    }

    public void changeToPrevious() {
        MC.changeToPrevious();
        this.localSearch.clear();
        extTableView.resetScrollData();
        update();

    }

    public void changeToForward() {
        MC.changeToForward();
        this.localSearch.clear();
        extTableView.resetScrollData();
        update();
    }

    private void changeToDir(ExtFolder dir) {
        if (!localSearch.getText().isEmpty()) {
            localSearch.clear();
        }
        MC.changeDirTo(dir);
        extTableView.resetScrollData();
        update();
    }

    public void searchTyped() {
        if (!this.useRegex.isSelected()) {
            this.searchTimeoutTask2.update();
        }
    }

    public void search() {
        String pattern = this.searchField.getText();
        searchView.getItems().clear();
        if (searchTask != null) {
            searchTask.cancel();
        }
        if (pattern.length() > 1) {
            Finder finder = new Finder(pattern, useRegex.isSelected(), searchView.getItems());
            this.searchStatus.setText("Searching");
            searchTask = new SimpleTask() {

                @Override
                protected Void call() throws Exception {
                    finder.isCanceled.bind(canceled);

                    if (!MC.currentDir.isVirtual.get()) {
                        try {
                            Files.walkFileTree(MC.currentDir.toPath(), finder);

                        } catch (Exception ex) {
                            ErrorReport.report(ex);
                        }
                    } else if (!MC.currentDir.isArtificial() && !MC.currentDir.isAbsoluteRoot.get()) {
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
            searchTask.appendOnSucceeded(eh -> {
                searchStatus.setText("Waiting");
            });
            new Thread(searchTask).start();

        }
    }

    public void localSearchTask() {
        localSearchTask2.update();
    }

    public void localSearch() {
        this.localSearchJob.cancel(true);
        final ExtFolder folderInitiated = MC.currentDir;
        final List<ExtPath> newList = new ArrayList<>();

        SafeJob<Void> sortTask = new SafeJob<>(me -> {
            Thread.sleep(200);
            while (!me.isCancelled() && MC.currentDir == folderInitiated) {
                if (extTableView.table.getItems().size() != newList.size()) {
                    FX.runAndWait(() -> {
                        extTableView.updateContentsAndSortPartial(newList);
                    });
                }
                Thread.sleep(500);
            }
        });

        SafeJob<Void> mainJob = new SafeJob<>(me -> {
            if (me.isCancelled()) {
                return null;
            }
            extTableView.saveScrollState();
            Future update = folderInitiated.update(newList, me::isCancelled);

            Checked.checkedCall(update::get);
            if (me.isCancelled()) {
                return null;
            }
            //apply local search
            String lookFor = localSearch.getText().trim();
            if (!lookFor.isEmpty()) {
                ArrayList<ExtPath> list = new ArrayList<>();
                newList.forEach(path -> {
                    if (Strings.CI.contains(path.propertyName.get(), lookFor)) {
                        list.add(path);
                    }
                });
                newList.clear();
                newList.addAll(list);
                sortTask.cancel(true);
            }
            FX.submit(() -> {

                localSearchLabel.setText("Local(" + newList.size() + ")");
                if (me.isCancelled()) {
                    return;
                }
                final int viewSize = extTableView.table.getItems().size();
                final int neededSize = newList.size();
                extTableView.updateContentsAndSort(newList);
                if (viewSize != neededSize && MC.currentDir == folderInitiated) { // still the same folder
                    Logger.info("View size {}, needed size {}", viewSize, neededSize);
                    extTableView.updateContentsAndSort(newList);
                } else {
                    extTableView.restoreScrollState();
                }
            });
            return null;
        });

        mainJob.addListener(SystemJobEventName.ON_EXECUTE, job -> {
            D.jobsExecutor.submit(sortTask);// only relevant if the directory load takes a while
        });

        mainJob.addListener(SystemJobEventName.ON_DONE, job -> {
            sortTask.cancel(true);// just in case
        });
        mainJob.addAfter(sortTask);
        localSearchJob = mainJob;
        D.jobsExecutor.submitAll(mainJob);

    }

    public void loadSnapshot() {
        try {
            String possibleSnapshot = this.snapshotLoadField.getText().trim();

            File file = new File(possibleSnapshot);

            if (file.exists()) {
                new Thread(TaskFactory.snapshotLoadTask(this.getID(), MC.currentDir, file)).start();
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
        File file = new File(TaskFactory.resolveAvailablePath(MC.currentDir, possibleSnapshot).toString());
        new Thread(TaskFactory.snapshotCreateWriteTask(getID(), MC.currentDir, file)).start();

    }

    public void dirSync() {
        ViewManager.newDirSyncDialog();
    }

    public void regexHelp() {
        ViewManager.newWebDialog(Enums.WebDialog.Regex);
    }

    public void aboutPage() {
        ViewManager.newWebDialog(Enums.WebDialog.About);
    }

    public void commandWindow() {
        ViewManager.newCommandDialog();
    }

    private void handleOpen(ExtPath file) {
        if (file == null) {
            ErrorReport.report(new PassableException(NullPointerException.class, "File to open given was null"));
            return;
        }
        if (file instanceof ExtFolder) {
            changeToDir((ExtFolder) file);
        } else {

            try {
                if (Enums.Identity.LINK.equals(file.getIdentity())) {
                    ExtLink link = (ExtLink) file;
                    LocationInRoot location = new LocationInRoot(link.getTargetDir());
                    if (link.isPointsToDirectory()) {
                        changeToDir((ExtFolder) LocationAPI.getPathIfExists(location));
                    } else {
                        DesktopApi.open(LocationAPI.getPathIfExists(location).toPath().toFile());
                    }
                } else if (Enums.Identity.FILE.equals(file.getIdentity())) {
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

        Menu submenuCreate = new MenuBuilders.MenuBuilder()
                .withText("Create...")
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("New folder")
                        .withAction(eh -> {
                            Logger.info("Create new folder");
                            try {
                                ExtPath createNewFolder = MC.createNewFolder();
                                if (createNewFolder != null) {
                                    ViewManager.newRenameDialog(MC.currentDir, createNewFolder);
                                }
                                localSearch();
                            } catch (Exception ex) {
                                ErrorReport.report(ex);
                            }
                        }).visibleWhen(MC.isVirtual.not())
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("New file")
                        .withAction(eh -> {
                            Logger.info("Create new file");
                            try {
                                ExtPath createNewFile = MC.createNewFile();
                                if (createNewFile != null) {
                                    ViewManager.newRenameDialog(MC.currentDir, createNewFile);
                                }
                                localSearch();
                            } catch (IOException ex) {
                                ErrorReport.report(ex);
                            }
                        }).visibleWhen(MC.isVirtual.not())
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        searchContextMenu = new MenuBuilders.ContextMenuBuilder()
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Go to")
                        .withAction(eh -> {
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
                        })
                        .visibleWhen(Bindings.size(searchView.getSelectionModel().getSelectedItems()).isEqualTo(1))
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Mark selected")
                        .withAction(eh -> {
                            FX.submit(() -> {
                                ArrayDeque<String> list = new ArrayDeque<>(searchView.getSelectionModel().getSelectedItems());
                                FXTask markFiles = TaskFactory.markFiles(list);
                                D.exe.execute(markFiles);
                            });
                        })
                        .visibleWhen(Bindings.size(searchView.getSelectionModel().getSelectedItems()).greaterThan(0))
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        errorContextMenu = new MenuBuilders.ContextMenuBuilder()
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Remove this entry")
                        .withAction(eh -> {
                            errorLog.remove(errorView.getSelectionModel().getSelectedIndex());
                        })
                        .visibleWhen(errorProperties.selectedSomething())
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Clean this list")
                        .withAction(eh -> {
                            errorLog.clear();
                        })
                        .visibleWhen(errorProperties.selectedSomething())
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        linksContextMenu = new MenuBuilders.ContextMenuBuilder()
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Add current directory as link")
                        .withAction(eh -> {
                            FavouriteLink link = new FavouriteLink(MC.currentDir.propertyName.get(), MC.currentDir);
                            if (!favoriteLinks.contains(link)) {
                                favoriteLinks.add(link);
                            }
                        })
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Remove this link")
                        .withAction(eh -> {
                            favoriteLinks.remove(linkView.getSelectionModel().getSelectedIndex());
                        })
                        .visibleWhen(linkView.getSelectionModel().selectedItemProperty().isNotNull())
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        markedContextMenu = new MenuBuilders.ContextMenuBuilder()
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Clean this list")
                        .withAction(eh -> {
                            markedView.getItems().clear();
                        })
                        .visibleWhen(propertyMarkedSize.greaterThan(0))
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Remove selected")
                        .withAction(eh -> {
                            ObservableList selectedItems = markedView.getSelectionModel().getSelectedItems();
                            MainController.markedList.removeAll(selectedItems);
                        })
                        .visibleWhen(
                                Bindings.and(
                                        propertyMarkedSize.greaterThan(0),
                                        markedView.getSelectionModel().selectedItemProperty().isNotNull()
                                )
                        )
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Delete selected")
                        .withAction(eh -> {
                            ContinousCombinedTask task = TaskFactory.deleteFilesEx(markedView.getSelectionModel().getSelectedItems());
                            task.setDescription("Delete selected marked files");
                            ViewManager.newProgressDialog(task);
                        })
                        .visibleWhen(
                                Bindings.and(
                                        propertyMarkedSize.greaterThan(0),
                                        markedView.getSelectionModel().selectedItemProperty().isNotNull()
                                )
                        )
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        tableDragContextMenu = new MenuBuilders.ContextMenuBuilder()
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Move here selected")
                        .withAction(eh -> {
                            ContinousCombinedTask task = TaskFactory.moveFilesEx(new ArrayList<>(dragList), MC.currentDir);
                            task.setDescription("Move Dragged files");
                            ViewManager.newProgressDialog(task);
                        })
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Copy here selected")
                        .withAction(eh -> {
                            ContinousCombinedTask task = TaskFactory.copyFilesEx(new ArrayList<>(dragList), MC.currentDir, null);
                            task.setDescription("Copy Dragged files");
                            ViewManager.newProgressDialog(task);
                        })
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        Menu submenuMarkFiles = new MenuBuilders.MenuBuilder()
                .withText("Mark...")
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Selected")
                        .withAction(eh -> {

                            filesProperties.selectedItems().forEach(TaskFactory::addToMarked);
                        }).visibleWhen(
                        Bindings.and(
                                miDuplicateFinderFolder.disableProperty().not(),
                                filesProperties.selectedSomething()
                        ))
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Recursive only files")
                        .withAction(eh -> {
                            filesProperties.selectedItems().forEach((file) -> {

                                Runnable run = () -> {
                                    file.collectRecursive(ExtPath.IS_NOT_DISABLED.and(ExtPath.IS_FILE), TaskFactory::addToMarked);
                                };
                                D.exe.execute(run);

                            });
                        })
                        .visibleWhen(
                                Bindings.and(
                                        miDuplicateFinderFolder.disableProperty().not(),
                                        filesProperties.selectedSomething()
                                )
                        )
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Recursive only folders")
                        .withAction(eh -> {
                            filesProperties.selectedItems().forEach((file) -> {

                                Runnable run = () -> {
                                    file.collectRecursive(ExtPath.IS_NOT_DISABLED.and(ExtPath.IS_FOLDER), TaskFactory::addToMarked);
                                };
                                D.exe.execute(run);

                            });
                        })
                        .visibleWhen(
                                Bindings.and(
                                        miDuplicateFinderFolder.disableProperty().not(),
                                        filesProperties.selectedItemNotNull()
                                )
                        )
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        Menu submenuMarked = new MenuBuilders.MenuBuilder()
                .withText("Marked...")
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Copy here marked")
                        .withAction(eh -> {
                            Logger.info("Copy Marked");
                            ContinousCombinedTask task = TaskFactory.copyFilesEx(markedList, MC.currentDir, null);
                            task.setDescription("Copy marked files");
                            ViewManager.newProgressDialog(task);
                        })
                        .visibleWhen(
                                Bindings.and(
                                        MC.isVirtual.not(),
                                        propertyMarkedSize.greaterThan(0)
                                )
                        )
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Move here marked")
                        .withAction(eh -> {
                            Logger.info("Move Marked");
                            ContinousCombinedTask task = TaskFactory.moveFilesEx(markedList, MC.currentDir);
                            task.setDescription("Move marked files");
                            ViewManager.newProgressDialog(task);
                        })
                        .visibleWhen(
                                Bindings.and(
                                        MC.isVirtual.not(),
                                        propertyMarkedSize.greaterThan(0)
                                )
                        )
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Delete marked")
                        .withAction(eh -> {
                            Logger.info("Delete Marked");
                            ContinousCombinedTask task = TaskFactory.deleteFilesEx(markedList);
                            task.setDescription("Delete marked files");
                            ViewManager.newProgressDialog(task);
                        })
                        .visibleWhen(
                                propertyMarkedSize.greaterThan(0)
                        )
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Add Marked to Virtual Folder")
                        .withAction(eh -> {
                            FX.submit(() -> {
                                VirtualFolder vf = F.cast(MC.currentDir);
                                MainController.markedList.forEach((f) -> {

                                    vf.files.put(f.propertyName.get(), f);
                                });
                                update();
                            });
                        })
                        .visibleWhen(
                                propertyMarkedSize.greaterThan(0)
                                        .and(writeableFolder)
                                        .and(MC.isVirtual)
                        )
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        tableContextMenu = new MenuBuilders.ContextMenuBuilder()
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Open")
                        .withAction(eh -> {
                            handleOpen((ExtPath) tableView.getSelectionModel().getSelectedItem());
                        })
                        .visibleWhen(filesProperties.selectedSize(1))
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Open in new window")
                        .withAction(eh -> {
                            ViewManager.newWindow((ExtFolder) tableView.getSelectionModel().getSelectedItem());
                        })
                        .visibleWhen(selectedIsFolder)
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Rename")
                        .withAction(eh -> {
                            rename();
                        })
                        .visibleWhen(propertyRenameCondition)
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Delete")
                        .withAction(eh -> {
                            delete();
                        })
                        .visibleWhen(propertyDeleteCondition)
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Copy path")
                        .withAction(eh -> {
                            String absolutePath = filesProperties.selectedItem().get().getAbsolutePath();
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(absolutePath), null);
                        })
                        .visibleWhen(propertyRenameCondition)
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Copy name")
                        .withAction(eh -> {
                            String name = filesProperties.selectedItem().get().getName(true);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(name), null);
                        })
                        .visibleWhen(propertyRenameCondition)
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Toggle Enable/Disable")
                        .withAction(eh -> {
                            FX.submit(() -> {
                                filesProperties.selectedItems().stream().forEach(c -> {

                                    c.isDisabled.setValue(!c.isDisabled.get());
                                });
                            });
                        })
                        .visibleWhen(propertyDeleteCondition)
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Create Virtual Folder")
                        .withAction(eh -> {
                            FX.submit(() -> {
                                VirtualFolder.createVirtualFolder();
                                update();
                            });
                        })
                        .visibleWhen(propertyIsVirtualFolders)
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Remove selected from Virtual Folder")
                        .withAction(eh -> {
                            FX.submit(() -> {
                                ObservableList<ExtPath> selectedItems = tableView.getSelectionModel().getSelectedItems();
                                VirtualFolder vf = F.cast(MC.currentDir);
                                selectedItems.forEach((item) -> {
                                    ExtPath remove = vf.files.remove(item.propertyName.get());
                                });
                                update();
                            });
                        })
                        .visibleWhen(MC.isVirtual.and(filesProperties.selectedSomething()))
                )
                .addItem(submenuCreate)
                .addItem(submenuMarked)
                .addItem(submenuMarkFiles)
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();
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
                        string.set(ExtStringUtils.extractNumber(get));
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

        FXDefs.configureDoubleClick(tableView, n -> {
            return filesProperties.selectedItemNotNull().get();
        }, ev -> {
            handleOpen(filesProperties.selectedItem().get());
        });
        getStage().getScene().setOnMousePressed(eh -> {
            hideAllContextMenus();
        });

        tableView.setOnKeyReleased(eh -> {
            KeyCode code = eh.getCode();
            if (code.equals(KeyCode.DELETE) && propertyDeleteCondition.get()) {
                delete();
            } else if (code.equals(KeyCode.F2) && propertyRenameCondition.get()) {
                rename();
            }
        });
        tableView.setOnDragDetected((MouseEvent event) -> {
            if (MC.currentDir.isArtificial()) {
                return;
            }
            D.dragInitWindowID = this.getID();
            if (this.extTableView.recentlyResized.get()) {
                return;
            }
            // drag was detected, start drag-and-drop gesture
            if (!filesProperties.selectedItems().isEmpty()) {
                Dragboard db = tableView.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                content.putFiles(filesProperties.selectedItems().stream().map(m -> m.toFile()).toList());
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        }); //drag

        tableView.setOnDragOver((DragEvent event) -> {
            if (MC.currentDir.isArtificial()) {
                return;
            }
            if (this.getID().equals(D.dragInitWindowID)) {

                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.ANY);

                //Log.writeln(event.getDragboard().getString());
            }
            event.consume();
        });

        tableView.setOnDragDropped((DragEvent event) -> {
//            Logger.info("Drag dropped");
            if (MC.currentDir.isVirtual.get()) {
                return;
            }
            if (this.getID().equals(D.dragInitWindowID)) {
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                dragList.clear();
                dragList.addAll(LocationAPI.fromFiles(db.getFiles()));
                tableDragContextMenu.show(tableView, event.getScreenX(), event.getScreenY());
                tableDragContextMenu.getOwnerNode().requestFocus();
                tableDragContextMenu.getOwnerWindow().requestFocus();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        tableView.setOnDragDone(event -> {
            if (!event.isDropCompleted()) {
                update();
            }
            D.dragInitWindowID = "";
        });

        extTableView = new ExtTableView(tableView);
        extTableView.sortable = true;
        extTableView.prepareChangeListeners();

        //default sort order
//        TableColumn typeCol = (TableColumn) tableView.getColumns().get(1);
//        TableColumn nameCol = (TableColumn) tableView.getColumns().get(0);
        typeCol.setSortType(TableColumn.SortType.DESCENDING);
        nameCol.setSortType(TableColumn.SortType.ASCENDING);
        tableView.getSortOrder().clear();
        tableView.getSortOrder().add(typeCol);
        tableView.getSortOrder().add(nameCol);
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
            D.dragInitWindowID = "MARKED";
            if (MC.currentDir.isArtificial()) {
                return;
            }

            // drag was detected, start drag-and-drop gesture
            ObservableList<ExtPath> selectedItems = markedView.getSelectionModel().getSelectedItems();
            if (!selectedItems.isEmpty()) {
                Dragboard db = markedView.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                content.putFiles(selectedItems.stream().map(m -> m.toFile()).toList());
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        }); //drag

        markedView.setOnDragOver((DragEvent event) -> {
            if (MC.currentDir.isArtificial()) {
                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.ANY);

                //Log.writeln(event.getDragboard().getString());
            }
            event.consume();
        });

        markedView.setOnDragDropped((DragEvent event) -> {
            if (MC.currentDir.isArtificial()) {
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                dragList.clear();
                dragList.addAll(LocationAPI.fromFiles(db.getFiles()));
                for (ExtPath f : dragList) {
                    TaskFactory.addToMarked(f);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        //***************************************
        //Search View
        searchView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//        searchView.getItems().setAll(finder.list);
        searchView.setContextMenu(searchContextMenu);
        searchView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(searchView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(searchView.getContextMenu());

        //***************************************
        //Link View
        linkView.setContextMenu(linksContextMenu);
        linkView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(linkView.getSelectionModel()));

        linkView.setItems(favoriteLinks);
        linkView.setCellFactory(new Callback<ListView<FavouriteLink>, ListCell<FavouriteLink>>() {
            @Override
            public ListCell<FavouriteLink> call(ListView<FavouriteLink> p) {
                ListCell<FavouriteLink> cell = new ListCell<FavouriteLink>() {
                    @Override
                    protected void updateItem(FavouriteLink t, boolean bln) {
                        super.updateItem(t, bln);
                        if (t != null) {
                            setText(t.getPropertyName().get());
                            if (!t.getPropertyName().get().equals(D.ROOT_NAME)) {
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

                    Logger.info(unitSize);
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
            Logger.info("Deleting");
            ContinousCombinedTask task = TaskFactory.deleteFilesEx(filesProperties.selectedItems());
            task.setDescription("Delete selected files");
            ViewManager.newProgressDialog(task);
        }

    }

    private void rename() {
        if (this.propertyRenameCondition.get()) {

            Logger.info("Invoke rename dialog");
            ExtPath path = (ExtPath) tableView.getSelectionModel().getSelectedItem();
            ExtFolder parent = (ExtFolder) LocationAPI.getPathNearest(path.getParent(1));
            ViewManager.newRenameDialog(parent, path);
        }
    }

    @Override
    public void exitLogic() {
    }
}
