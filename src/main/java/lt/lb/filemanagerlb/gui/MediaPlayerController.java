package lt.lb.filemanagerlb.gui;

import lt.lb.filemanagerlb.vlc.VLCInit;
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
import com.github.laim0nas100.uncheckedutils.Checked;
import org.tinylog.Logger;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.ComponentVideoSurface;
import lt.lb.commons.javafx.properties.SelectableViewProperties;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import lt.lb.filemanagerlb.vlc.VLCException;
import lt.lb.filemanagerlb.vlc.VLCMediaPlayerEventListener;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;

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

    public static boolean oldMode = false;

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
    public Button buttonPlayPrev;
    @FXML
    public Button buttonPlayNext;

    private SelectableViewProperties<ExtPath> tableProperties;

    private boolean startedWithVideo = false;
    private int index = 0;
    private static final Float minDelta = 0.0001f;
    private long currentLength = 0;
    private AtomicInteger lastVolume = new AtomicInteger(-1);
    private boolean stopping = false;
    private boolean ignoreSeek = false;
    private static final String typeLoopSong = "Loop file";
    private static final String typeLoopList = "Loop list";
    private static final String typeRandom = "Random";
    private static final String typeStopAfterFinish = "Don't loop list";

    private ExtTableView extTableView;
    private ExtPath filePlaying;
    private ArrayList<ExtPath> backingList = new ArrayList<>();

    private ArrayDeque<Player> pls = new ArrayDeque<>();

    private static class Player {

        public VLCMediaPlayerEventListener listener;
        public MediaPlayer media;
        public StageFrame stageFrame;
        public JFrame jFrame;
    }
    private ExecutorService exe = new FastWaitingExecutor(1);
    private ExecutorService exe2 = new FastWaitingExecutor(1);

    private EventQueue events = new EventQueue(exe);

    private DelayedTaskExecutor execService = new DelayedTaskExecutor(exe2);

    private void setPosition(float pos) { // 0-100
        float position = getCurrentPlayer().status().position();// 0-1.00
        float val = pos / 100f;

        Logger.debug(getCurrentPlayer().status().position() + ", " + val);
        if (Math.abs(position - val) > minDelta) {
            getCurrentPlayer().controls().setPosition(val);
            Logger.debug("Set new seek " + val);
        }
    }

    private Player gcp() {
        if (pls.isEmpty()) {
            throw new IllegalStateException("No available players");
        }
        return pls.getLast();
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

        Value<javafx.scene.image.ImageView> view = new Value<>();
        Value<MediaPlayer> player = new Value<>();
        return D.sm.newStageFrame("VLC VIDEO OUTPUT", () -> {
            EmbeddedMediaPlayer newPlayer = VLCInit.getFactory().mediaPlayers().newEmbeddedMediaPlayer();
            player.set(newPlayer);

            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            view.set(imageView);
            newPlayer.videoSurface().set(new ImageViewVideoSurface(imageView));
            imageView.setPreserveRatio(true);
            return new Group(imageView);
        }).map(stageFrame -> {
            if (showVideo.isSelected()) {
                stageFrame.show();
            } else {
                stageFrame.hide();
            }
            Stage stage = stageFrame.getStage();
            stage.setOnCloseRequest(eh -> {
                showVideo.setSelected(false);
                eh.consume();
                stage.hide();
            });
            view.get().fitHeightProperty().bind(stage.heightProperty());
            view.get().fitWidthProperty().bind(stage.widthProperty());

            return stageFrame;

        }).peekError(err -> {
            Logger.error(err);
        }).map(videoFrame -> {
            Player pl = new Player();
            pl.media = player.get();
            VLCMediaPlayerEventListener listener = new VLCMediaPlayerEventListener();
            pl.media.events().addMediaPlayerEventListener(listener);
            pl.listener = listener;
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
                    Logger.debug("END " + event.tags + " Cancel:" + event.isCancelled());
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
                Logger.debug("recently resized");
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

        table.setOnDragDone(event -> {
            if (!event.isDropCompleted()) {
                update();
            }
            D.dragInitWindowID = "";
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
                Logger.debug("Same window");
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

        extTableView.updateContentsAndSort(backingList);
    }

    private void setVolume(MediaPlayer player, int vol) {
        events.cancelAll(PlayerEventType.VOL);

        events.add(PlayerEventType.VOL, () -> {
            int tries = 100;
            var volume = player.audio().volume();
            do {
                if (--tries < 0 || volume == vol) {
                    break;
                }
                player.audio().setVolume(vol);
                volume = player.audio().volume();
                if (volume != vol) {
                    LockSupport.parkNanos(WaitTime.ofMillis(10).toNanos());
                } else {
                    break;
                }
            } while (true);
        });

    }

    private void addPlayer(Player player) {
        pls.addLast(player);
    }

    private void removePlayer(Player player) {
        pls.remove(player);
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
            boolean visible = showVideo.isSelected();
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
    }

    public void updateSeek() {
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

            if (secondsLeft < minDelta && playerState == PlayerState.PLAYING) {
                Logger.debug("Seconds left" + secondsLeft);
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
            boolean stopped = false;
            int limit = 500;
            while (getCurrentPlayer().status().isPlaying() && stoppableStates.contains(playerState)) {
                getCurrentPlayer().controls().stop();
                if (getCurrentPlayer().status().isPlaying() && limit > 0) {
                    Thread.sleep(10);
                    limit--;
                    continue;
                }
                stopped = limit > 0;
                break;
            }
            if (stopped) {
                this.playerState = PlayerState.STOPPED;
            }
        });

    }

    public void relaunch() {
        events.add("RELAUNCH outer", () -> {
            Logger.debug("Relaunch");
            relaunch(getCurrentPlayer().status().position());
        });

    }

    private void relaunch(float position) {
        events.add("RELAUNCH inner", () -> {
            play(new PlayInfo(filePlaying, Optional.ofNullable(index), Optional.ofNullable(lastVolume.get()), Optional.of(position)));
        });

    }

    @Override
    public void exitLogic() {
        Logger.debug("CLOSE MEDIA PLAYER");
        stopping = true;

        stop();
        pls.forEach(player -> {
            Checked.checkedRun(() -> {
                player.media.controls().stop();
                player.media.release();
                if (oldMode) {
                    player.jFrame.dispose();
                } else {
                    player.stageFrame.close();
                }
                Logger.debug("Released vlc player");
            }).ifPresent(ErrorReport::report);
        });
        saveState(D.HOME_DIR.PLAYLISTS.DEFAULT_PLAYLIST.absolutePath);
        execService.shutdown();
        exe.shutdown();
        exe2.shutdown();
        events.shutdown();
//        pls.clear();
//        events = null;

        Logger.debug("FINAL EXIT ");
    }

    public void playNext(int increment, boolean ignoreModifiers) {
        events.cancelAll(PlayerEventType.PLAY);
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
//                            stop();
                            return;
                        }
                    }
                }
                //default loop song
                index = (index + increment) % backingList.size();
                item = (ExtPath) backingList.get(index);
            }
            play(PlayInfo.ofItemIndex(item, index));

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
        TableView.TableViewSelectionModel sel = table.getSelectionModel();
        Object selectedItem = sel.getSelectedItem();
        if (sel.getSelectedItem() == null) {
            return;
        }
        update();

        play(PlayInfo.ofItemIndexVolume(F.cast(selectedItem), sel.getSelectedIndex(), lastVolume.get()));

    }

    public record PlayInfo(ExtPath item, Optional<Integer> index, Optional<Integer> volume, Optional<Float> position) {

        public static PlayInfo ofItem(ExtPath item) {
            return new PlayInfo(item, Optional.empty(), Optional.empty(), Optional.empty());
        }

        public static PlayInfo ofItemIndex(ExtPath item, Integer index) {
            return new PlayInfo(item, Optional.ofNullable(index), Optional.empty(), Optional.empty());
        }

        public static PlayInfo ofItemIndexVolume(ExtPath item, Integer index, Integer vol) {
            return new PlayInfo(item, Optional.ofNullable(index), Optional.ofNullable(vol), Optional.empty());
        }
    }

    private Future play(PlayInfo playInfo) {
        events.cancelAll(PlayerEventType.PLAY_TASK);
        return events.add(PlayerEventType.PLAY_TASK, () -> {
            ignoreSeek = true;
            Logger.debug("Execute play task");
            int i = playInfo.index().orElseGet(() -> getIndex(playInfo.item()));
            if (i < 0) {
                Logger.debug("Play next");
                playNext(0, true);//increment by zero 
                return null;
            }
            filePlaying = playInfo.item();

//            stop();
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
            Player player = gcp();
            player.listener.clearEvents();

            String absolutePath = filePlaying.getAbsolutePath();
            boolean prepared = player.media.media().prepare(absolutePath, getOptions()); // this is not reliable
            boolean valid = prepared && getCurrentPlayer().media().isValid();
            boolean startedPlaying = false;
            if (!valid) {
                ErrorReport.report(new VLCException("Not valid media file, skipping " + absolutePath));
                stop();

            } else {
                getCurrentPlayer().controls().start();
                VLCMediaPlayerEventListener.VLCPlayerEvent lastEvent = player.listener.getLastEvent();
                if (lastEvent != null) {
                    if (lastEvent.event() == VLCMediaPlayerEventListener.VLCPlayerEvents.mediaPlayerReady) {

                        try {
                            int limit = 500;
                            //wait to start playing
                            while (!getCurrentPlayer().status().isPlaying() && limit > 0) {
                                Logger.debug("Keep sleeping");
                                Thread.sleep(10);
                                limit--;
                            }
                            startedPlaying = limit > 0;
                        } catch (InterruptedException inter) {

                        }

                    } else if (lastEvent.event() == VLCMediaPlayerEventListener.VLCPlayerEvents.error) {
                        startedPlaying = false;
                        // error
                    }
                }

            }

            if (startedPlaying) {
                Logger.debug("Started playing");
                // set the after initialization things
                playInfo.volume().filter(v -> v >= 0 && v <= 100).ifPresent(v -> {
                    Logger.debug("Set volume", v);
                    setVolume(getCurrentPlayer(), v);
                });
                playInfo.position.ifPresent(pos -> {
                    Logger.debug("Set position", pos);
                    getCurrentPlayer().controls().setPosition(pos);
                });
                this.playerState = PlayerState.PLAYING;
            } else {
                stop();
                this.playerState = PlayerState.STOPPED;
                ErrorReport.report(new VLCException("Failure to play " + absolutePath));

            }
            ignoreSeek = false;
            this.update();

            return null;
        });

    }

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
        Logger.debug("Got items", i);
        return state;
    }

    public void loadPlaylistState(boolean replace, PlaylistState state) {
        if (replace) {
            stop();
        }
        events.add("LOAD_PLAYLIST", () -> {

            Logger.debug("INSIDE LOAD PLAYLIST");
//            this.table.getItems().clear();
            IntegerValue num = new IntegerValue(0);
            if (replace) {
                backingList.clear();
            }
            state.root.resolve(false).forEach(item -> {
                LocationAPI.getFileIfExists(item).ifPresent(this::addIfAbsent);
                num.incrementAndGet();
            });
            Logger.debug("Loaded files:", num.get());
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
