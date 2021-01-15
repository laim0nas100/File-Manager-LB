package lt.lb.filemanagerlb.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import javax.swing.JFrame;
import lt.lb.commons.F;
import lt.lb.commons.SafeOpt;
import lt.lb.commons.containers.values.IntegerValue;
import lt.lb.commons.io.TextFileIO;
import lt.lb.commons.javafx.CosmeticsFX;
import lt.lb.commons.javafx.CosmeticsFX.ExtTableView;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.MenuBuilders;
import lt.lb.commons.javafx.TimeoutTask;
import lt.lb.commons.javafx.properties.ViewProperties;
import lt.lb.commons.javafx.scenemanagement.StageFrame;
import lt.lb.commons.threads.Futures;
import lt.lb.commons.threads.executors.FastExecutor;
import lt.lb.commons.threads.sync.EventQueue;
import lt.lb.fastid.FastID;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.dialog.RenameDialogController.FileCallback;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.LocationInRoot;
import lt.lb.filemanagerlb.logic.LocationInRootNode;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.utility.ContinousCombinedTask;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.ExtStringUtils;
import lt.lb.filemanagerlb.utility.SimpleTask;
import org.tinylog.Logger;
import uk.co.caprica.vlcj.binding.RuntimeUtil;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.ComponentVideoSurface;

/**
 * FXML Controller class
 *
 * @author Lemmin
 */
public class MediaPlayerController extends MyBaseController {

    public static class PlayerEventType {

        public static final String STOP = "STOP";
        public static final String PLAY = "PLAY";
        public static final String PLAY_OR_PAUSE = "PLAY OR PAUSE";
        public static final String PLAY_TASK = "PLAY_TASK";
        public static final String SEEK = "SEEK";
        public static final String VOL = "VOL";

    }

    public final static boolean seamlessDisabled = true;
    public final static boolean oldMode = false;

    public static enum PlayerState {
        PLAYING, PAUSED, STOPPED, NEW
    }

    public PlayerState playerState = PlayerState.NEW;
    public final static String PLAY_SYMBOL = "✓";
    public static String VLC_SEARCH_PATH;
    public static boolean VLCfound = false;

    @FXML
    public Label labelCurrent;
    @FXML
    public Label labelTimePassed;
    @FXML
    public Label labelDuration;
    @FXML
    public Label labelStatus;
    @FXML
    public Slider volumeSlider;
    @FXML
    public Slider seekSlider;
    @FXML
    public TableView table;
    @FXML
    public CheckBox showVideo;
    @FXML
    public ChoiceBox playType;
    @FXML
    public CheckBox seamless;
    @FXML
    public Button buttonPlayPrev;
    @FXML
    public Button buttonPlayNext;
    @FXML
    public TextField loadState;
    @FXML
    public TextField saveState;

    private ViewProperties<ExtPath> tableProperties;

    private MediaPlayerFactory factory;
    volatile private MediaPlayer oldplayer;
    private boolean startedWithVideo = false;
    private int index = 0;
    private Float minDelta = 0.005f;
    private long seamlessSecondsMax = 12;
    private long currentLength = 0;
    private SimpleBooleanProperty released = new SimpleBooleanProperty(false);
    private AtomicInteger lastVolume = new AtomicInteger(-1);
    private boolean stopping = false;
    private boolean inSeamless = false;
    private boolean ignoreSeek = false;
    private String typeLoopSong = "Loop file";
    private String typeLoopList = "Loop list";
    private String typeRandom = "Random";
    private String typeStopAfterFinish = "Don't loop list";
    private ExtTableView extTableView;
    private ExtPath filePlaying;
    private ArrayList<ExtPath> backingList = new ArrayList<>();

    private HashMap<FastID, Player> pls = new HashMap<>();
    private ArrayDeque<FastID> playerIDs = new ArrayDeque<>();

    private static class Player {

        public MediaPlayer media;
        public StageFrame stageFrame;
        public JFrame jFrame;
        public final FastID id = FastID.getAndIncrementGlobal();

    }

    private ScheduledExecutorService execService = Executors.newScheduledThreadPool(3);
    private EventQueue events = new EventQueue(new FastExecutor(1));
    TimeoutTask dragTask = new TimeoutTask(300, 20, () -> {
        float val = seekSlider.valueProperty().divide(100).floatValue();
        float position = getCurrentPlayer().status().position();
        Logger.info(getCurrentPlayer().status().position() + ", " + val);
        if (Math.abs(position - val) > minDelta) {
            val = (float) ExtStringUtils.normalize(val, 3);
            getCurrentPlayer().controls().setPosition(val);
            Logger.info("Set new seek");
        }
    });

    public static void discover() throws VLCException {
        if (!VLCfound) {
            MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory();
            mediaPlayerFactory.release();
            VLCfound = true;
            /*
            NativeDiscoveryStrategy[] array = new NativeDiscoveryStrategy[]{
                new WindowsNativeDiscoveryStrategy(),
                new LinuxNativeDiscoveryStrategy(),
                new OsxNativeDiscoveryStrategy()
            };
            int supportedOS = -1;
            for (int i = 0; i < array.length; i++) {
                if (array[i].supported()) {
                    supportedOS = i;
                    break;
                }
            }
            switch (supportedOS) {
                case 0: {
                    array[supportedOS] = new WindowsNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 1: {
                    array[supportedOS] = new LinuxNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 2: {
                    array[supportedOS] = new OsxNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                default: {
                    //unsupported OS?
                }
            }
            if (supportedOS != -1) {
                VLCfound = new NativeDiscovery(array[supportedOS]).discover();
            }
             */
            if (VLCfound) {
                Logger.info(RuntimeUtil.getLibVlcLibraryName());
            } else {
                throw new VLCException("Could not locate VLC, \n configure vlcPath in Parameters.txt");
            }
        }

    }

    private Player gcp() {
        if (pls.isEmpty()) {
            throw new VLCException("No available players");
        }
        return pls.get(playerIDs.getLast());
    }

    private MediaPlayer getCurrentPlayer() {
        return gcp().media;
    }

    private StageFrame getCurrentFrame() {
        return gcp().stageFrame;
    }

    private JFrame getCurrentFrameOld() {
        return gcp().jFrame;
    }

    public static class VLCException extends RuntimeException {

        VLCException(String str) {
            super(str);
        }
    }

    private Player getPreparedMediaPlayer() {
        if (oldMode) {
            return getPreparedMediaPlayerOld();
        } else {
            return getPreparedMediaPlayerNew();
        }
    }

    private Player getPreparedMediaPlayerNew() {

        EmbeddedMediaPlayer newPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        newPlayer.videoSurface().set(ImageViewVideoSurfaceFactory.videoSurfaceForImageView(imageView));
        imageView.setPreserveRatio(true);
        Future<StageFrame> future = D.sm.newStageFrame("VLC VIDEO OUTPUT", () -> {
            return new Group(imageView);
        });

        SafeOpt<StageFrame> error = Futures.mappable(future).map(stageFrame -> {
            if (showVideo.selectedProperty().get()) {
                stageFrame.show();
            } else {
                stageFrame.hide();
            }
            Stage stage = stageFrame.getStage();
            stage.setOnCloseRequest(eh -> {
                showVideo.selectedProperty().set(false);
                eh.consume();
                stage.hide();
            });
            imageView.fitHeightProperty().bind(stage.heightProperty());
            imageView.fitWidthProperty().bind(stage.widthProperty());

            return stageFrame;

        }).safeGet();

        if (error.hasError()) {
            error.getError().ifPresent(err -> {
                Logger.error(err);
            });
            return null;
        } else {
            Player pl = new Player();
            pl.media = newPlayer;
            pl.stageFrame = error.get();
            return pl;
        }

    }

    private Player getPreparedMediaPlayerOld() {
        EmbeddedMediaPlayer newPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        Canvas canvas = new Canvas();
        ComponentVideoSurface newVideoSurface = factory.videoSurfaces().newVideoSurface(canvas);
        newPlayer.videoSurface().set(newVideoSurface);

        JFrame jframe = new JFrame();
        jframe.setExtendedState(JFrame.ICONIFIED);
        jframe.add(canvas);
        jframe.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        jframe.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                showVideo.setSelected(false);
            }
        });
        if (showVideo.selectedProperty().get()) {
            jframe.setExtendedState(JFrame.NORMAL);
            jframe.setVisible(true);
        } else {
            jframe.setVisible(false);
        }
        jframe.setBackground(Color.black);

        if (!pls.isEmpty()) {
            jframe.setSize(getCurrentFrameOld().getSize());
            jframe.setLocation(getCurrentFrameOld().getLocation());
        } else {
            jframe.setSize(800, 600);
        }

        Player pl = new Player();
        pl.jFrame = jframe;
        pl.media = newPlayer;
        return pl;
    }

    public void beforeShow() {

    }

    public void setUpTable() {
        events.preventSelfTagCancel = true;

        if (D.DEBUG.get()) {
            events.eventCallbackBefore = event -> {
                if (!event.tag.equals(PlayerEventType.SEEK)) {
                    Logger.info("START " + event.tag);
                }
            };
            events.eventCallbackAfter = event -> {
                if (!event.tag.equals(PlayerEventType.SEEK)) {
                    Logger.info("END " + event.tag + " Cancel:" + event.isCancelled());
                }
            };
        }

        extTableView = new ExtTableView(table);
        tableProperties = ViewProperties.ofTableView(table);

        TableColumn<ExtPath, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtPath, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtPath, String> cellData) {
                return cellData.getValue().propertyName;
            }
        });
        TableColumn<ExtPath, String> indexCol = new TableColumn<>("");
        indexCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> {
            return new SimpleStringProperty(1 + getIndex(cellData.getValue()) + "");
        });
        TableColumn<ExtPath, String> selectedCol = new TableColumn<>("");
        selectedCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> {
            String str = "";
            if (isSelected(cellData.getValue())) {
                str = PLAY_SYMBOL;
            }
            return new SimpleStringProperty(str);
        });
        indexCol.setSortable(false);
        selectedCol.setSortable(false);
        nameCol.setSortable(false);
        selectedCol.setPrefWidth(30);
        selectedCol.setMaxWidth(30);
        selectedCol.setMinWidth(30);
        indexCol.setMinWidth(30);
        indexCol.setMaxWidth(60);
        indexCol.setPrefWidth(50);
        table.getColumns().addAll(indexCol, selectedCol, nameCol);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setOnMousePressed((MouseEvent event) -> {
            if (event.isPrimaryButtonDown()) {
                if (event.getClickCount() > 1) {
                    playSelected();
                }
            }
        });

        table.setOnDragDetected((MouseEvent event) -> {
            if (this.extTableView.recentlyResized.get()) {
                Logger.info("recently resized");
                return;
            }
            MainController.dragList = table.getSelectionModel().getSelectedItems();
            TaskFactory.dragInitWindowID = this.getID();
            Logger.info(TaskFactory.dragInitWindowID, MainController.dragList);
            if (!MainController.dragList.isEmpty()) {
                Dragboard db = table.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                content.putString("Ready");
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        });

        table.setOnDragOver((DragEvent event) -> {
            if (this.getID().equals(TaskFactory.dragInitWindowID)) {
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
        table.setOnDragDropped((DragEvent event) -> {
            Logger.info(() -> "Drag dropped! " + "\n" + MainController.dragList);
            if (this.getID().equals(TaskFactory.dragInitWindowID)) {
                Logger.info("Same window");
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!MainController.dragList.isEmpty()) {
                MainController.dragList.forEach(item -> {
                    addIfAbsent(item);
                });

                update();
                success = true;

            }
            event.setDropCompleted(success);
            event.consume();
        });

        extTableView.updateContentsAndSort(backingList);
    }

    private void setVolume(MediaPlayer player, int vol) {
        events.cancelAll(PlayerEventType.VOL);

        events.add(PlayerEventType.VOL, () -> {
            int tries = 100;
            while (player.status().isPlaying() && player.audio().volume() != vol) {
                player.audio().setVolume(vol);
                Thread.sleep(50);
                tries--;
                if (tries <= 0) {
                    return;
                }
            }
        });

    }

    private void addPlayer(Player player) {
        pls.put(player.id, player);
        playerIDs.addLast(player.id);
    }

    private void removePlayer(Player player) {
        pls.remove(player.id);
        playerIDs.remove(player.id);
    }

    @Override
    public void afterShow() {

//        JFrame tempFrame = new JFrame();
//        tempFrame.setSize(800, 480);
//        framesOld.add(tempFrame);
//        if (oldMode) {
//            getCurrentFrameOld().setVisible(false);
//        } else {
//            getCurrentFrame().hide();
//        }
        factory = new MediaPlayerFactory();
//        Log.write("Factory");
        addPlayer(getPreparedMediaPlayer());
//        framesOld.pollFirst().setVisible(false);
//        Log.write("Player INIT");
        SimpleDoubleProperty prop = new SimpleDoubleProperty(100);
        SimpleBooleanProperty changeResetComplete = new SimpleBooleanProperty(true);
//        TimeoutTask t = new TimeoutTask(100, 20, () -> {
//            changeResetComplete.set(true);
//            volumeSlider.valueProperty().setValue(prop.get());
//        });

        volumeSlider.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                double volume = volumeSlider.getValue();
                int rounded = (int) Math.round(volume);
                lastVolume.set(rounded);

                if (pls.isEmpty() || !getCurrentPlayer().status().isPlaying() || stopping) {
                    return;
                }
                setVolume(getCurrentPlayer(), rounded);
            }
        });
        volumeSlider.setValue(100);
        dragTask.conditionalCheck = (released);
        seekSlider.setOnMousePressed(event -> {
            dragTask.update();
            released.set(false);
        });
        seekSlider.setOnMouseDragged(value -> {
            dragTask.update();

        });
        seekSlider.setOnMouseReleased(event -> {
            released.set(true);
        });

        buttonPlayPrev.setOnAction(event -> {
            playNext(-1, true);
        });
        buttonPlayNext.setOnAction(event -> {
            playNext(1, true);
        });

        setUpTable();

        ContextMenu build = new MenuBuilders.ContextMenuBuilder()
                .addItemMenu(new MenuBuilders.MenuBuilder()
                        .withText("Marked...")
                        .addItem(new MenuBuilders.MenuItemBuilder()
                                .withText("Add to marked")
                                .withAction(eh -> {
                                    tableProperties.selectedItems().forEach(item -> {
                                        TaskFactory.getInstance().addToMarked(item);
                                    });
                                })
                                .visibleWhen(tableProperties.selectedItemNotNull())
                        )
                        .addItem(new MenuBuilders.MenuItemBuilder()
                                .withText("Add marked")
                                .withAction(eh -> {
                                    MainController.markedList.forEach(item -> {
                                        addIfAbsent(item);
                                    });
                                    update();
                                })
                                .visibleWhen(MainController.propertyMarkedSize.greaterThan(0))
                        )
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Remove")
                        .withAction(eh -> {
                            ArrayList<ExtPath> selected = new ArrayList<>(tableProperties.selectedItems());
                            selected.forEach(item -> {
                                if (item.equals(filePlaying)) {
                                    index--;
                                }
                                backingList.remove(item);
                            });
                            extTableView.updateContentsAndSort(backingList);
                            update();
                        })
                        .visibleWhen(tableProperties.selectedItemNotNull())
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Delete")
                        .withAction(eh -> {
                            ContinousCombinedTask task = TaskFactory.getInstance().deleteFilesEx(table.getSelectionModel().getSelectedItems());
                            task.setDescription("Delete selected files");
                            ViewManager.getInstance().newProgressDialog(task);
                        })
                        .visibleWhen(tableProperties.selectedItemNotNull())
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Rename")
                        .withAction(eh -> {
                            IntegerValue numberValue = new IntegerValue(0);
                            FileCallback cb = (filePath) -> {
                                addIfAbsent(filePath, numberValue.get());
                            };
                            numberValue.set(table.getSelectionModel().getSelectedIndex());
                            ExtPath selected = (ExtPath) table.getSelectionModel().getSelectedItem();
                            String parent = selected.getParent(1);
                            ViewManager.getInstance().newRenameDialog((ExtFolder) LocationAPI.getInstance().getFileOptimized(parent), selected, cb);
                        })
                        .visibleWhen(tableProperties.selectedSize(1))
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();

        table.setContextMenu(build);
        table.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(table.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(table.getContextMenu());

        extTableView.prepareChangeListeners();

        FX.submit(() -> {
            execService.scheduleAtFixedRate(() -> {
                FX.submit(() -> {

                    updateSeek();
                });
            }, 1000, 300, TimeUnit.MILLISECONDS);

            playType.getItems().addAll(typeLoopList, typeLoopSong, typeRandom, typeStopAfterFinish);
            playType.getSelectionModel().select(0);
            showVideo.selectedProperty().addListener(listener -> {
                boolean visible = showVideo.selectedProperty().get();
                if (oldMode) {
                    getCurrentFrameOld().setVisible(visible);
                    if (visible) {
                        getCurrentFrameOld().setExtendedState(JFrame.NORMAL);
                    }
                } else {
                    if (visible) {
                        getCurrentFrame().show();
                    } else {
                        getCurrentFrame().hide();
                    }

                }

                if (visible && !startedWithVideo) {
                    if (getCurrentPlayer().status().isPlaying()) {
                        relaunch();
                    }
                }
            });
            try {
                loadState(D.HOME_DIR.PLAYLISTS.DEFAULT_PLAYLIST.absolutePath);
            } catch (Exception e) {
                ErrorReport.report(e);
            }

        });

    }

    private void updateSeekLabels(Float position, Long millisPassed) {
        FX.submit(() -> {
            if (!stopping && !pls.isEmpty() && getCurrentPlayer().status().isPlaying()) {

                this.labelTimePassed.setText(this.formatToMinutesAndSeconds(millisPassed));
                if (!this.dragTask.isInAction()) {
                    this.seekSlider.valueProperty().set(position * 100);
                }
                labelDuration.setText("/ " + formatToMinutesAndSeconds(currentLength));
            }
        });
    }

    public void updateSeek() {
        if (ignoreSeek || stopping || this.playerState != PlayerState.PLAYING) {
            return;
        }
        events.add(PlayerEventType.SEEK, () -> {
            if (ignoreSeek || stopping || this.playerState != PlayerState.PLAYING) {
                return;
            }
            Float position;
            if (!getCurrentPlayer().status().isSeekable()) {
                position = 0f;
            } else {
                position = getCurrentPlayer().status().position();
            }
            currentLength = getCurrentPlayer().status().length();
            long millisPassed = (long) (this.currentLength * position);
            double secondsLeft = (double) (this.currentLength - millisPassed) / 1000;
            this.updateSeekLabels(position, millisPassed);

            if (!seamlessDisabled && seamless.selectedProperty().get() && (secondsLeft < seamlessSecondsMax) && (secondsLeft > 2)) {
                playNext(1, false, true, this.currentLength - millisPassed);
                return;

            }
            if (secondsLeft < minDelta) {
                Logger.info("Seconds left", secondsLeft);
                playNext(1, false);
            }
        });

    }

    public void playOrPause() {

        events.add(PlayerEventType.PLAY_OR_PAUSE, () -> {
            if (getCurrentPlayer().status().isPlayable()) {
                if (getCurrentPlayer().status().isPlaying()) {
                    this.playerState = PlayerState.PAUSED;
                    getCurrentPlayer().controls().pause();
                } else {
                    this.playerState = PlayerState.PLAYING;
                    getCurrentPlayer().controls().play();
                }

                this.setVolume(getCurrentPlayer(), lastVolume.get());

            } else {
                playNext(0, true);
            }
        });

    }

    public void stop() {
        events.cancelAll(PlayerEventType.STOP, PlayerEventType.PLAY, PlayerEventType.PLAY_OR_PAUSE, PlayerEventType.PLAY_TASK);
        events.add(PlayerEventType.STOP, () -> {
            while (getCurrentPlayer().status().isPlaying()) {
                getCurrentPlayer().controls().stop();
                Thread.sleep(1);
            }
            this.playerState = PlayerState.STOPPED;
        });

    }

    public void relaunch() {
        events.add("RELAUNCH outer", () -> {
            Logger.info("Relaunch");
            relaunch(getCurrentPlayer().status().position());
        });

    }

    private void relaunch(float position) {
        events.add("RELAUNCH inner", () -> {
            onPlayTaskComplete.add(() -> {
                events.add("Set position after relaunch", () -> {
                    Logger.info("Set position", position);
                    getCurrentPlayer().controls().setPosition(position);
                });

            });
            play(filePlaying);
        });

    }

    @Override
    public void exit() {
        stopping = true;

        pls.values().forEach(player -> {
            player.media.controls().stop();
            player.media.release();
            if (oldMode) {
                player.jFrame.dispose();
            } else {
                player.stageFrame.close();
            }
        });
        saveState(D.HOME_DIR.PLAYLISTS.DEFAULT_PLAYLIST.absolutePath);
        this.execService.shutdown();
        this.events.forceShutdown();
        super.exit();

    }

    public void playNext(int increment, boolean ignoreModifiers, Object... opt) {
//        events.cancelAll("PLAY");
        events.add(PlayerEventType.PLAY, () -> {
            if (backingList.isEmpty()) {
                return;
            }
            updateIndex();
            if (!ignoreModifiers) {
                if (playType.getSelectionModel().getSelectedItem().equals(typeRandom)) {
                    index = (int) (Math.random() * backingList.size());
                } else if (playType.getSelectionModel().getSelectedItem().equals(typeLoopSong)) {
                    if (increment == 1) {
                        index--;
                    }
                } else if (playType.getSelectionModel().getSelectedItem().equals(typeStopAfterFinish)) {
                    if (index + increment == table.getItems().size()) {
//                        stop();
                        filePlaying = null;
                        update();
                        return;
                    }
                }
            }
            //default loop song
            index = (index + increment) % backingList.size();
            ExtPath item = (ExtPath) backingList.get(index);
            if (item == null) {
                return;
            }

            if (!seamlessDisabled && this.pls.size() == 1 && opt.length > 1 && (boolean) opt[0]) {
//                playSeemless(item, (long) opt[1]);
                play(item);
            } else {
                play(item);
            }
        });

    }

    @Override
    public void update() {
        LocationAPI.getInstance().filterIfExists(backingList);
        FX.submit(() -> {

            extTableView.updateContentsAndSort(backingList);
            updateIndex();
            int in = this.getIndex(filePlaying) + 1;
            labelCurrent.setText("[" + in + "] " + filePlaying.getAbsolutePath());

        });

    }

    public void playSelected() {
        play(F.cast(table.getSelectionModel().getSelectedItem()));
    }
    private ArrayDeque<Runnable> onPlayTaskComplete = new ArrayDeque<>();

    private void play(ExtPath item) {

        play(item, lastVolume.get());
    }

    private Future play(ExtPath item, final Integer volume) {
        events.cancelAll(PlayerEventType.PLAY_TASK);
        return events.add(PlayerEventType.PLAY_TASK, () -> {
            ignoreSeek = true;
            Logger.info("Execute play task");
            int i = this.getIndex(item);
            if (i < 0) {
                Logger.info("Play next");
                playNext(0, true);
                return null;
            }
            filePlaying = item;

            stop();
            if (oldMode) {
                getCurrentFrameOld().setTitle(filePlaying.getName(true));
                startedWithVideo = getCurrentFrameOld().isVisible();
            } else {
                StageFrame currentFrame = getCurrentFrame();
                Stage stage = currentFrame.getStage();
                String title = filePlaying.getName(true);
                FX.submit(() -> {
                    stage.setTitle(title);
                });

                startedWithVideo = stage.isShowing();
            }

            boolean playable = getCurrentPlayer().media().prepare(filePlaying.getAbsolutePath(), getOptions());
            if (!playable) {
                table.getItems().remove(filePlaying);

            } else {
                getCurrentPlayer().controls().start();

                //wait to start playing
                while (!getCurrentPlayer().status().isPlaying()) {
                    Thread.sleep(1);
                }
                Logger.info("Started playing");
                if (volume != null && (volume >= 0 && volume <= 250)) {
                    setVolume(getCurrentPlayer(), volume);
                }
            }
            this.update();
            while (!onPlayTaskComplete.isEmpty()) {
                onPlayTaskComplete.pollFirst().run();
            }
            this.playerState = PlayerState.PLAYING;
            ignoreSeek = false;
            return null;
        });

    }

    /*
    private void playSeemless(ExtPath item, final long millisLeft) {
        if (seamlessDisabled) {
            return;
        }

        inSeamless = true;
        oldplayer = getCurrentPlayer();
        Value<Double> oldVolume = new Value<>((double) oldplayer.audio().volume());
//        oldplayer.removeMediaPlayerEventListener(defaultPlayerEventAdapter);
        oldplayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Logger.info("Finished old player");
                int i = 1;
                while (players.size() > 1) {
                    framesOld.pollFirst().dispose();
                    players.pollFirst().release();
//                    oldplayer.release();
                    Logger.info("Frame/Player collected " + i++);
                }
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                Logger.info("Finished old player");
                int i = 1;
                while (players.size() > 1) {
                    framesOld.pollFirst().dispose();
                    players.pollFirst().release();
//                    oldplayer.release();
                    Logger.info("Frame/Player collected " + i++);
                }
            }
        });
        MediaPlayer newPlayer = getPreparedMediaPlayer();
        players.add(newPlayer);

        Value<Future> promise = new Value<>();

        Thread toThread = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                promise.get().get();

                double timeChangeMillis = 1000;
                double overTime = millisLeft;

                double inc = oldVolume.get() / (overTime / timeChangeMillis);
                double difference = inc;
//                
//                  oldVolume 100
//                  over 12 seconds
//                  change volume each 500 millis
//                  12000 / 500 = 24 iterations
//                  100 / 24 ~ 4.16
//                 
//                 
//                  oldVolume 100
//                  over 8 seconds
//                 
//                  8000 / 500 = 16 iterations
//                  100 / 16 ~ 6.25
                 
                 
                long millis = millisLeft;

                while (oldVolume.get() - difference > 1 && millis > 10) {

                    int setOldVol = (int) (oldVolume.get() - difference);
                    setVolume(oldplayer, setOldVol);
                    setVolume(getCurrentPlayer(), (int) difference);
//                    Logger.info("Players==", oldplayer.mediaPlayerInstance(), getCurrentPlayer().mediaPlayerInstance());
//                    Logger.info("Volume sets:", setOldVol, (int) difference);
                    long time = System.currentTimeMillis();
                    Thread.sleep((long) timeChangeMillis);

                    time = System.currentTimeMillis() - time;

                    millis -= time;
                    difference += inc;

                }
                Logger.info("End volume resize task");
                setVolume(getCurrentPlayer(), oldVolume.get().intValue());
                inSeamless = false;

                return null;
            }
        }.toThread();
        this.onPlayTaskComplete.add(() -> {
//            inSeamless = false;
            setVolume(getCurrentPlayer(), 0); // set new player volume 0 asap
        });

        Future play = play(item, 0);
        promise.set(play);
        toThread.start();
    }
     */
    private String formatToMinutesAndSeconds(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        if (seconds < 10) {
            return minutes + ":0" + seconds;
        } else {
            return minutes + ":" + seconds;
        }
    }

    private void updateIndex() {
        if (filePlaying == null) {
            index = 0;
        } else {
            int potIndex = this.getIndex(filePlaying);
            if (potIndex != -1) {
                index = potIndex;
            } else {
                index = 0;
            }
        }

    }

    public String[] getOptions() {
        ArrayList<String> options = new ArrayList<>();
        if (showVideo.selectedProperty().not().get()) {
            options.add("no-video");
        } else {
//        options.add("audio-visual=visual");
//        options.add("effect-list=spectrometer");
//        options.add("effect-width="+getCurrentFrame().getWidth());
//        options.add("effect-height="+getCurrentFrame().getHeight());
        }
        return options.toArray(new String[1]);
    }

    public boolean isSelected(ExtPath path) {
        if (path == null) {
            return false;
        }
        return path.equals(this.filePlaying);
    }

    public int getIndex(ExtPath path) {
        return backingList.indexOf(path);
    }

    public void addIfAbsent(ExtPath item) {
        if (item != null && item.getIdentity().equals(Identity.FILE)) {
            if (!backingList.contains(item)) {
                backingList.add(item);
            }
        }
    }

    public void addIfAbsent(ExtPath item, int index) {
        if (item != null && item.getIdentity().equals(Identity.FILE)) {
            if (!backingList.contains(item)) {
                int size = table.getItems().size();
                if (index >= 0 && index < size) {
                    backingList.add(index, item);
                } else {
                    backingList.add(item);
                }
                this.extTableView.updateContentsAndSort(backingList);
            }
        }
    }

    public static class PlaylistState {

        public LocationInRootNode root;
        public Integer index;
        public String type;

        public PlaylistState() {
        }

    }

    public PlaylistState getPlaylistState() {
        PlaylistState state = new PlaylistState();
        state.root = new LocationInRootNode("", -1);
        state.index = Math.max(0, getIndex(filePlaying));
        state.type = (String) this.playType.getSelectionModel().getSelectedItem();
        int i = 0;
        for (Object item : backingList) {
            ExtPath path = (ExtPath) item;
            state.root.add(new LocationInRoot(path.getAbsoluteDirectory(), false), i++);
        }
        Logger.info("Got items", i);
        return state;
    }

    public void loadPlaylistState(PlaylistState state) {
        stop();
        events.add(() -> {

            this.table.getItems().clear();
            IntegerValue num = new IntegerValue(0);
            state.root.resolve(false).forEach(item -> {
                ExtPath file = LocationAPI.getInstance().getFileOptimized(item);
                addIfAbsent(file);
                num.incrementAndGet();
            });
            Logger.info("Loaded files:", num.get());
            FX.submit(() -> {
                this.playType.getSelectionModel().select(state.type);
            });
            this.index = state.index;
            if (backingList.size() > index) {
                this.filePlaying = (ExtPath) backingList.get(index);
            }
            update();
        });

    }

    public void saveState(String path) {

        Logger.info("Set busy");
        FX.submit(() -> {
            labelStatus.setText("Busy");
        });

        Logger.info("Init state save");
        try {
            ArrayList<String> list = new ArrayList<>();
            PlaylistState state = getPlaylistState();
            list.add(state.index + "");
            list.add(state.type);
            list.addAll(state.root.specialString());
            TextFileIO.writeToFile(path, list);
            Logger.info("Write to file size:", list.size());
        } catch (Exception e) {
            ErrorReport.report(e);
        }
        Logger.info("After state save");
        FX.submit(() -> {
            labelStatus.setText("Ready");
        });
    }

    public void saveState() {
        saveState(D.HOME_DIR.PLAYLISTS.getAbsolutePathWithSeparator() + saveState.getText().trim());
    }

    public void loadState() {
        loadState(D.HOME_DIR.PLAYLISTS.getAbsolutePathWithSeparator() + loadState.getText().trim());
    }

    private void loadState(String path) {
        labelStatus.setText("Busy");
        SimpleTask task = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                PlaylistState state = new PlaylistState();
                try {
                    ArrayList<String> readFromFile = TextFileIO.readFromFile(path);
                    state.index = Integer.parseInt(readFromFile.remove(0));
                    state.type = readFromFile.remove(0);
                    state.root = LocationInRootNode.nodeFromFile(readFromFile);
                    loadPlaylistState(state);
                    update();
                } catch (Exception e) {
                    ErrorReport.report(e);
                }
                return null;
            }

        };
        task.setOnDone(value -> {
            FX.submit(() -> {
                labelStatus.setText("Ready");
            });
        });
        task.toThread().start();
    }

    public void shuffle() {
        Collections.shuffle(table.getItems());
    }

}
