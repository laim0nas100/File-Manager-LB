package lt.lb.filemanagerlb.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javax.swing.JFrame;
import lt.lb.commons.F;
import lt.lb.commons.Nulls;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.containers.values.IntegerValue;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.io.directoryaccess.Fil;
import lt.lb.commons.iteration.streams.MakeStream;
import lt.lb.commons.javafx.CosmeticsFX;
import lt.lb.commons.javafx.CosmeticsFX.ExtTableView;
import lt.lb.commons.javafx.FXDefs;
import lt.lb.commons.javafx.MenuBuilders;
import lt.lb.commons.javafx.fxrows.FXDrow;
import lt.lb.commons.javafx.fxrows.FXDrows;
import lt.lb.commons.javafx.fxrows.FXSync;
import lt.lb.commons.javafx.scenemanagement.StageFrame;
import lt.lb.commons.threads.executors.FastWaitingExecutor;
import lt.lb.commons.threads.executors.scheduled.DelayedTaskExecutor;
import lt.lb.commons.threads.sync.EventQueue;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.fastid.FastID;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.VLCInit.VLCException;
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
import lt.lb.uncheckedutils.Checked;
import org.tinylog.Logger;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.ComponentVideoSurface;
import lt.lb.commons.javafx.properties.SelectableViewProperties;
import lt.lb.uncheckedutils.SafeOpt;

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
    public static boolean oldMode = true;

    public static enum PlayerState {
        PLAYING, PAUSED, STOPPED, NEW
    }

    public PlayerState playerState = PlayerState.NEW;
    public final static String PLAY_SYMBOL = "✓";
    @FXML
    public Label labelCurrent;
    @FXML
    public Label labelTimePassed;
    @FXML
    public Label labelDuration;
    @FXML
    public Slider volumeSlider;
    @FXML
    public Slider seekSlider;
    @FXML
    public TableView table;
    @FXML
    public CheckBox showVideo;
    @FXML
    public ChoiceBox<String> playType;
    @FXML
    public CheckBox seamless;
    @FXML
    public Button buttonPlayPrev;
    @FXML
    public Button buttonPlayNext;

    private SelectableViewProperties<ExtPath> tableProperties;

    private volatile MediaPlayer oldplayer;
    private boolean startedWithVideo = false;
    private int index = 0;
    private Float minDelta = 0.0001f;
    private long seamlessSecondsMax = 12;
    private long currentLength = 0;
    private AtomicInteger lastVolume = new AtomicInteger(-1);
    private boolean stopping = false;
    private boolean inSeamless = false;
    private boolean ignoreSeek = false;
    private static final String typeLoopSong = "Loop file";
    private static final String typeLoopList = "Loop list";
    private static final String typeRandom = "Random";
    private static final String typeStopAfterFinish = "Don't loop list";

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
    private ExecutorService exe = new FastWaitingExecutor(1);
    private ExecutorService exe2 = new FastWaitingExecutor(1);

    private EventQueue events = new EventQueue(exe);

    private DelayedTaskExecutor execService = new DelayedTaskExecutor(exe2);

    private void setPosition(float pos) { // 0-100
        float position = getCurrentPlayer().status().position();// 0-1.00
        float val = pos / 100f;

        Logger.info(getCurrentPlayer().status().position() + ", " + val);
        if (Math.abs(position - val) > minDelta) {
            getCurrentPlayer().controls().setPosition(val);
            Logger.info("Set new seek");
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

    private Player getPreparedMediaPlayer() {
        if (oldMode) {
            return getPreparedMediaPlayerOld();
        } else {
            return getPreparedMediaPlayerNew();
        }
    }

    private Player getPreparedMediaPlayerNew() {

        EmbeddedMediaPlayer newPlayer = VLCInit.getFactory().mediaPlayers().newEmbeddedMediaPlayer();
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        newPlayer.videoSurface().set(ImageViewVideoSurfaceFactory.videoSurfaceForImageView(imageView));
        imageView.setPreserveRatio(true);

        return D.sm.newStageFrame("VLC VIDEO OUTPUT", () -> {
            return new Group(imageView);
        }).map(stageFrame -> {
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

        }).peekError(err -> {
            Logger.error(err);
        }).map(videoFrame -> {
            Player pl = new Player();
            pl.media = newPlayer;
            pl.stageFrame = videoFrame;
            return pl;
        }).orNull();

    }

    private Player getPreparedMediaPlayerOld() {
        EmbeddedMediaPlayer newPlayer = VLCInit.getFactory().mediaPlayers().newEmbeddedMediaPlayer();
        Canvas canvas = new Canvas();
        ComponentVideoSurface newVideoSurface = VLCInit.getFactory().videoSurfaces().newVideoSurface(canvas);
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
        events.preventRunningSelfTagCancel = true;
        events.preventRunningTagCancel = false;

        if (D.DEBUG.get()) {
            events.eventCallbackBefore = event -> {
                List<String> tags = event.tags;
                if (!tags.contains(PlayerEventType.SEEK)) {
                    Logger.info("START " + event.tags);
                }
            };
            events.eventCallbackAfter = event -> {
                List<String> tags = event.tags;
                if (!tags.contains(PlayerEventType.SEEK)) {
                    Logger.info("END " + event.tags + " Cancel:" + event.isCancelled());
                }
            };
        }

        extTableView = new ExtTableView(table);
        tableProperties = SelectableViewProperties.ofTableView(table);

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
            ObservableList<ExtPath> selectedItems = table.getSelectionModel().getSelectedItems();
            D.dragInitWindowID = this.getID();
            if (!selectedItems.isEmpty()) {
                Dragboard db = table.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putFiles(selectedItems.stream().map(m -> m.toFile()).toList());
                db.setContent(content);
                event.consume();
            }
        });

        table.setOnDragOver((DragEvent event) -> {
            if (this.getID().equals(D.dragInitWindowID)) {
                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        table.setOnDragDropped((DragEvent event) -> {
            if (this.getID().equals(D.dragInitWindowID)) {
                Logger.info("Same window");
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                LocationAPI.fromFiles(db.getFiles()).forEach(this::addIfAbsent);
                update();
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
        table.setOnDragDone(event -> {
            if (!event.isDropCompleted()) {
                update();
            }
            D.dragInitWindowID = "";

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

        addPlayer(getPreparedMediaPlayer());
        volumeSlider.valueProperty().addListener(FXDefs.SimpleChangeListener.of(val -> {
            int rounded = (int) Math.round(val.doubleValue());
            lastVolume.set(rounded);

            if (pls.isEmpty() || !getCurrentPlayer().status().isPlaying() || stopping) {
                return;
            }
            setVolume(getCurrentPlayer(), rounded);
        }));
        volumeSlider.setValue(100);
        seekSlider.valueProperty().addListener(FXDefs.SimpleChangeListener.of(val -> {
            if (inSeekChange.get()) {
                return;
            }
            events.cancelAll(PlayerEventType.SEEK);
            events.add(PlayerEventType.SEEK, () -> {
                setPosition(val.floatValue());
                updateSeek();
            });
        }));
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
                                        TaskFactory.addToMarked(item);
                                    });
                                })
                                .visibleWhen(tableProperties.selectedSomething())
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
                        .visibleWhen(tableProperties.selectedSomething())
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Delete")
                        .withAction(eh -> {
                            ContinousCombinedTask task = TaskFactory.deleteFilesEx(table.getSelectionModel().getSelectedItems());
                            task.setDescription("Delete selected files");
                            ViewManager.newProgressDialog(task);
                        })
                        .visibleWhen(tableProperties.selectedSomething())
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Rename")
                        .withAction(eh -> {
                            ExtPath selected = (ExtPath) table.getSelectionModel().getSelectedItem();
                            FileCallback cb = (filePath) -> {
                                int indexOf = backingList.indexOf(selected);
                                if (indexOf >= 0) {
                                    backingList.remove(selected);
                                    addIfAbsent(filePath, indexOf);
                                }

                            };

                            String parent = selected.getParent(1);
                            ViewManager.newRenameDialog((ExtFolder) LocationAPI.getPathNearest(parent), selected, cb);
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
        execService.scheduleWithFixedDelay(WaitTime.ofMillis(500), () -> {
            if (ignoreSeek || stopping || this.playerState != PlayerState.PLAYING) {
                return;
            }
            updateSeek();
        });

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
            loadState(true, D.HOME_DIR.PLAYLISTS.DEFAULT_PLAYLIST.absolutePath);
        } catch (Exception e) {
            ErrorReport.report(e);
        }

    }

    AtomicBoolean inSeekChange = new AtomicBoolean(false);

    private void updateSeekLabels(Float position, Long millisPassed) {
        fxDelegator.update("updateSeekLabels", () -> {
            if (!stopping && !pls.isEmpty()) {

                this.labelTimePassed.setText(formatTimeFull(millisPassed));
                if (inSeekChange.compareAndSet(false, true)) {
                    this.seekSlider.valueProperty().set(position * 100d);
                    inSeekChange.set(false);
                }

                labelDuration.setText("/ " + formatTimeFull(currentLength));
            }
        });
//        FX.submit(() -> {
//            if (!stopping && !pls.isEmpty()) {
//
//                this.labelTimePassed.setText(formatTimeFull(millisPassed));
//                if (inSeekChange.compareAndSet(false, true)) {
//                    this.seekSlider.valueProperty().set(position * 100);
//                    inSeekChange.set(false);
//                }
//
//                labelDuration.setText("/ " + formatTimeFull(currentLength));
//            }
//        });
    }

    public void updateSeek() {
        final boolean seamlessVal = !seamlessDisabled && seamless.selectedProperty().get();
        events.add(PlayerEventType.SEEK, () -> {
            if (ignoreSeek || stopping) {
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

            if (seamlessVal && (secondsLeft < seamlessSecondsMax) && (secondsLeft > 2)) {
                playNext(1, false, true, this.currentLength - millisPassed);

            } else if (secondsLeft < minDelta) {
                Logger.info("Seconds left" + secondsLeft);
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
        Set<PlayerState> stoppableStates = ImmutableCollections.setOf(PlayerState.PAUSED, PlayerState.PLAYING);
        events.dequeueAll(PlayerEventType.STOP, PlayerEventType.PLAY, PlayerEventType.PLAY_OR_PAUSE, PlayerEventType.PLAY_TASK);
        events.add(PlayerEventType.STOP, () -> {
            while (getCurrentPlayer().status().isPlaying() && stoppableStates.contains(playerState)) {
                getCurrentPlayer().controls().stop();
                LockSupport.parkNanos(WaitTime.ofMillis(100).toNanos());
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
    public void exitLogic() {
        Logger.info("CLOSE MEDIA PLAYER");
        stopping = true;

        stop();
        pls.values().forEach(player -> {
            Checked.checkedRun(() -> {
                player.media.controls().stop();
                player.media.release();
                if (oldMode) {
                    player.jFrame.dispose();
                } else {
                    player.stageFrame.close();
                }
                Logger.info("Released vlc player");
            }).ifPresent(ErrorReport::report);

        });
        saveState(D.HOME_DIR.PLAYLISTS.DEFAULT_PLAYLIST.absolutePath);
        execService.shutdown();
        exe.shutdown();
        exe2.shutdown();
        events.shutdown();
//        events = null;

        Logger.info("FINAL EXIT " + extTableView.resizeTask.isInAction());
    }

    public void playNext(int increment, boolean ignoreModifiers, Object... opt) {
//        events.cancelAll("PLAY");
        events.add(PlayerEventType.PLAY, () -> {

            ExtPath item = null;
            while (item == null) {
                update();
                if (backingList.isEmpty()) {
                    return;
                }
                if (!ignoreModifiers) {
                    String selectedPlayType = playType.getValue();
                    if (Objects.equals(selectedPlayType, typeRandom)) {
                        index = (int) (Math.random() * backingList.size());
                    } else if (Objects.equals(selectedPlayType, typeLoopSong)) {
                        index = index - increment; // replay the same song
                    } else if (Objects.equals(selectedPlayType, typeStopAfterFinish)) {
                        if (index + increment >= table.getItems().size()) {

                            filePlaying = null;
                            update();
                            stop();
                            return;
                        }
                    }
                }
                //default loop song
                index = (index + increment) % backingList.size();
                item = (ExtPath) backingList.get(index);
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

        LocationAPI.filterIfExists(backingList);
        fxDelegator.update("update", () -> {
            extTableView.updateContentsAndSort(backingList);
            if (filePlaying != null) {
                updateIndex();
                int in = this.getIndex(filePlaying) + 1;
                labelCurrent.setText("[" + in + "] " + filePlaying.getAbsolutePath());
            }

        });

    }

    public void playSelected() {
        Object selectedItem = table.getSelectionModel().getSelectedItem();
        if (table.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        update();

        play(F.cast(selectedItem), lastVolume.get());

    }
    private ArrayDeque<Runnable> onPlayTaskComplete = new ArrayDeque<>();

    private void play(ExtPath item) {

        update();
        play(item, lastVolume.get());
//        FX.submit(() -> {
//            play(item, lastVolume.get());
//        });

    }

    private Future play(ExtPath item, final Integer volume) {
        events.dequeueAll(PlayerEventType.PLAY_TASK);
        return events.add(PlayerEventType.PLAY_TASK, () -> {
            ignoreSeek = true;
            Logger.info("Execute play task");
            int i = this.getIndex(item);
            if (i < 0) {
                Logger.info("Play next");
                playNext(0, true);//increment by zero 
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
                fxDelegator.set(stage.titleProperty(), title);

                startedWithVideo = stage.isShowing();
            }

            boolean playable = getCurrentPlayer().media().prepare(filePlaying.getAbsolutePath(), getOptions());
            if (!playable) {
                table.getItems().remove(filePlaying);

            } else {
                getCurrentPlayer().controls().start();

                //wait to start playing
                while (!getCurrentPlayer().status().isPlaying()) {
                    Logger.info("Keep sleeping");
                    LockSupport.parkNanos(WaitTime.ofMillis(100).toNanos());

                }
                Logger.info("Started playing");
                if (volume != null && (volume >= 0 && volume <= 100)) {
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
    private static String format2Digit(long time) {
        return time >= 10 ? ":" + time : ":0" + time;
    }

    private static String formatTimeFull(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        String str = format2Digit(seconds);
        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return hours + format2Digit(minutes) + str;
        } else {
            return minutes + str;
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
            }
        }
    }

    public static class PlaylistState {

        public LocationInRootNode root;
        public Integer index;
        public String type;
        public Integer volume = 100;

        public PlaylistState() {
        }

    }

    public PlaylistState getPlaylistState() {
        PlaylistState state = new PlaylistState();
        state.root = new LocationInRootNode("", -1);
        state.index = Math.max(0, getIndex(filePlaying));
        state.type = (String) this.playType.getSelectionModel().getSelectedItem();
        state.volume = (int) Math.round(volumeSlider.getValue());
        int i = 0;
        for (Object item : backingList) {
            ExtPath path = (ExtPath) item;
            state.root.add(new LocationInRoot(path.getAbsoluteDirectory(), false), i++);
        }
        Logger.info("Got items", i);
        return state;
    }

    public void loadPlaylistState(boolean replace, PlaylistState state) {
        if (replace) {
            stop();
        }
        events.add("LOAD_PLAYLIST", () -> {

            Logger.info("INSIDE LOAD PLAYLIST");
//            this.table.getItems().clear();
            IntegerValue num = new IntegerValue(0);
            if (replace) {
                backingList.clear();
            }
            state.root.resolve(false).forEach(item -> {
                LocationAPI.getFileIfExists(item).ifPresent(this::addIfAbsent);
                num.incrementAndGet();
            });
            Logger.info("Loaded files:", num.get());
            fxDelegator.update("loadState", () -> {
                playType.getSelectionModel().select(state.type);
                volumeSlider.setValue(state.volume);
            });
            if (replace) {
                this.index = state.index;
            }
            if (replace && backingList.size() > index) {
                this.filePlaying = (ExtPath) backingList.get(index);
            }
            update();
        });

    }

    public void saveState(String path) {

        try {
            ArrayList<String> list = new ArrayList<>();
            PlaylistState state = getPlaylistState();
            list.add(String.valueOf(state.index));
            list.add(state.type);
            list.add(String.valueOf(state.volume));
            list.addAll(state.root.specialString());
            lt.lb.commons.io.text.TextFileIO.writeToFile(path, list);
        } catch (Exception e) {
            ErrorReport.report(e);
        }
    }

    private void loadState(boolean replace, String path) {
        D.exe.execute(() -> {
            PlaylistState state = new PlaylistState();
            try {
                ArrayList<String> readFromFile = lt.lb.commons.io.text.TextFileIO.readFromFile(path);
                state.index = Integer.parseInt(readFromFile.remove(0));
                state.type = readFromFile.remove(0);
                state.volume = Integer.parseInt(readFromFile.remove(0));
                state.root = LocationInRootNode.nodeFromFile(readFromFile);
                loadPlaylistState(replace, state);
                update();
            } catch (Exception e) {
                ErrorReport.report(e);
            }
        });
    }

    public void shuffle() {
        Collections.shuffle(table.getItems());
    }

    public void stateChange() {
        FXDrows rows = FXDefs.fxrows();

        CheckBox load = new CheckBox();
        load.setSelected(true);

        FXDrow firstRow = rows.getNew()
                .addLabel("Load?")
                .add(load)
                .display();

        load.setOnAction(eh -> {
            firstRow.update();
            rows.clearInvalidationPersist(null);
            rows.renderAfterVisibilityChange();
        });

        Value<Fil> fileToLoad = new Value<>();
        try {
            D.HOME_DIR.PLAYLISTS.rescan();
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
        List<Fil> files = MakeStream.from(D.HOME_DIR.PLAYLISTS.getFiles())
                .nonNull()
                .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()))
                .toList();

        Value<Boolean> replace = new Value<>(true);

        rows.getOrCreate("Load1")
                .addLabel("Replace current state (append otherwise):")
                .addFxSync(FXSync.ofCheckBox(new CheckBox(), replace))
                .bindBindableUpdatesFrom(firstRow)
                .withUpdateRefresh(r -> {
                    r.setVisible(load.isSelected());
                })
                .display();

        FXSync.ComboBoxSync<Fil> comboSync = FXSync.ofComboBox(new ComboBox<>(), fileToLoad, files, f -> f.getName());
        comboSync.addPersistValidation("Make a selection", Nulls::nonNull);
        rows.getOrCreate("Load2")
                .addLabel("Select state to load:")
                .addFxSync(comboSync)
                .bindBindableUpdatesFrom(firstRow)
                .withUpdateRefresh(r -> {
                    r.setVisible(load.isSelected());
                    r.clearInvalidationPersist(null);
                })
                .display();

        Value<String> newName = new Value<>("");//will only be set if name passes validation
        FXSync.TextFieldSync<String> newNameSync = FXSync.ofTextField(newName);
        newNameSync.addPersistValidation("Illegal file name", str -> SafeOpt.ofNullable(str).peek(TaskFactory::assertLegalName).isPresent());
        rows.getOrCreate("Save")
                .addLabel("Write state name (legal filename)")
                .addFxSync(newNameSync)
                .bindBindableUpdatesFrom(firstRow)
                .withUpdateRefresh(r -> {
                    r.setVisible(!load.isSelected());
                    r.clearInvalidationPersist(null);
                })
                .display();

        StageFrame dialog = D.sm.newFormFrame("Media player state change", rows, () -> {
            if (load.isSelected()) {
                loadState(replace.get(), fileToLoad.get().absolutePath);
            } else {
                saveState(D.HOME_DIR.PLAYLISTS.getAbsolutePathWithSeparator() + newName.get());
            }
        }).get();
        dialog.getStage().setAlwaysOnTop(true);
        dialog.getStage().initModality(Modality.APPLICATION_MODAL);
        dialog.getStage().showAndWait();
    }

}
