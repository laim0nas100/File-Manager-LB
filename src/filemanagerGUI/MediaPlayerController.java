package filemanagerGUI;

import lt.lb.commons.threads.sync.EventQueue;
import filemanagerGUI.dialog.RenameDialogController.FileCallback;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.*;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.fileStructure.ExtPath;
import java.awt.Canvas;
import java.awt.Color;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.util.Callback;
import javax.swing.JFrame;
import lt.lb.commons.Log;
import lt.lb.commons.containers.Value;
import lt.lb.commons.io.FileReader;
import lt.lb.commons.javafx.CosmeticsFX;
import lt.lb.commons.javafx.CosmeticsFX.ExtTableView;
import lt.lb.commons.javafx.CosmeticsFX.MenuTree;
import lt.lb.commons.F;
import lt.lb.commons.containers.NumberValue;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.threads.*;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.*;
import uk.co.caprica.vlcj.discovery.linux.DefaultLinuxNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.discovery.mac.DefaultMacNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.discovery.windows.DefaultWindowsNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.player.*;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import utility.*;

/**
 * FXML Controller class
 *
 * @author Lemmin
 */
public class MediaPlayerController extends BaseController {

    public static class PlayerEventType {

        public static final String STOP = "STOP";
        public static final String PLAY = "PLAY";
        public static final String PLAY_OR_PAUSE = "PLAY OR PAUSE";
        public static final String PLAY_TASK = "PLAY_TASK";
        public static final String SEEK = "SEEK";
        public static final String VOL = "VOL";

    }

    public final static boolean seamlessDisabled = true;

    public static enum PlayerState {
        PLAYING, PAUSED, STOPPED, NEW
    }

    public PlayerState playerState = PlayerState.NEW;
    public final static String PLAYLIST_DIR = "PLAYLISTS";
    public final static String PLAY_SYMBOL = "✓";
    public final static String PLAYLIST_FILE_NAME = "DEFAULT_PLAYLIST";
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

    private ArrayDeque<JFrame> frames = new ArrayDeque<>();
    private ArrayDeque<MediaPlayer> players = new ArrayDeque<>();

    private ScheduledExecutorService execService = Executors.newScheduledThreadPool(3);
    private EventQueue events = new EventQueue(execService);
    TimeoutTask dragTask = new TimeoutTask(300, 20, () -> {
        float val = seekSlider.valueProperty().divide(100).floatValue();
        Log.print(getCurrentPlayer().getPosition(), val);
        if (Math.abs(getCurrentPlayer().getPosition() - val) > minDelta) {
            val = (float) ExtStringUtils.normalize(val, 3);
            getCurrentPlayer().setPosition(val);
            Log.print("Set new seek");
        }
    });

    public static String getPlaylistsDir() {
        return FileManagerLB.USER_DIR + PLAYLIST_DIR + File.separator;
    }

    private MediaPlayer getCurrentPlayer() {
        if (players.isEmpty()) {
            throw new VLCException("No available players");
        }
        return this.players.getLast();
    }

    private JFrame getCurrentFrame() {
        if (frames.isEmpty()) {
            throw new VLCException("No available frames");
        }
        return this.frames.getLast();
    }

    public static class VLCException extends RuntimeException {

        VLCException(String str) {
            super(str);
        }
    }

    public static void discover() throws InterruptedException, VLCException {
        if (!VLCfound) {
            NativeDiscoveryStrategy[] array = new StandardNativeDiscoveryStrategy[]{
                new DefaultWindowsNativeDiscoveryStrategy(),
                new DefaultLinuxNativeDiscoveryStrategy(),
                new DefaultMacNativeDiscoveryStrategy()
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
                    array[supportedOS] = new DefaultWindowsNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 1: {
                    array[supportedOS] = new DefaultLinuxNativeDiscoveryStrategy() {
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 2: {
                    array[supportedOS] = new DefaultMacNativeDiscoveryStrategy() {
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
            if (VLCfound) {
                Log.print(RuntimeUtil.getLibVlcLibraryName() + " " + LibVlc.INSTANCE.libvlc_get_version());
            } else {
                throw new VLCException("Could not locate VLC, \n configure vlcPath in Parameters.txt");
            }
        }
    }

    public MediaPlayer getPreparedMediaPlayer() {
        EmbeddedMediaPlayer newPlayer = factory.newEmbeddedMediaPlayer();
//        Log.write("Inside: newPlayer");
        Canvas canvas = new Canvas();
        CanvasVideoSurface newVideoSurface = factory.newVideoSurface(canvas);
        newPlayer.setVideoSurface(newVideoSurface);
//        Log.write("Inside: done with surface");
        JFrame jframe = new JFrame();

//        Log.write("New JFrame done");
        jframe.setVisible(true);
        jframe.setExtendedState(JFrame.ICONIFIED);
//        Log.write("Set visible");
        jframe.add(canvas);
        jframe.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        jframe.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                showVideo.setSelected(false);
            }
        });
//        Log.write("Inside: done with frame");
        if (showVideo.selectedProperty().get()) {
            jframe.setExtendedState(JFrame.NORMAL);
            jframe.setVisible(true);
        } else {
            jframe.setVisible(false);
        }
        jframe.setBackground(Color.black);
        jframe.setSize(getCurrentFrame().getSize());
        jframe.setLocation(getCurrentFrame().getLocation());
        frames.add(jframe);
//        newPlayer.addMediaPlayerEventListener(defaultPlayerEventAdapter);
        return newPlayer;
    }

    public void beforeShow() {

    }

    public void setUpTable() {
        this.extTableView = new ExtTableView(table);

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
                Log.print("recently resized");
                return;
            }
            MainController.dragList = table.getSelectionModel().getSelectedItems();
            TaskFactory.dragInitWindowID = this.windowID;
            Log.print(TaskFactory.dragInitWindowID, MainController.dragList);
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
        table.setOnDragDropped((DragEvent event) -> {
            Log.println("Drag dropped!", MainController.dragList);
            if (this.windowID.equals(TaskFactory.dragInitWindowID)) {
                Log.print("Same window");
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

        this.extTableView.updateContentsAndSort(backingList);
    }

    private void setVolume(MediaPlayer player, int vol) {
        events.cancelAll(PlayerEventType.VOL);

        events.add(PlayerEventType.VOL, () -> {
            int tries = 100;
            while (player.isPlaying() && player.getVolume() != vol) {
                player.setVolume(vol);
                Thread.sleep(50);
                tries--;
                if (tries <= 0) {
                    return;
                }
            }
        });

    }

    @Override
    public void afterShow() {

        JFrame tempFrame = new JFrame();
        tempFrame.setSize(800, 480);
        frames.add(tempFrame);
        getCurrentFrame().setVisible(false);
        factory = new MediaPlayerFactory();
//        Log.write("Factory");
        players.add(getPreparedMediaPlayer());
        frames.pollFirst().setVisible(false);
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

                if (players.isEmpty() || !getCurrentPlayer().isPlaying() || stopping) {
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
        MenuTree tree = new MenuTree(null);
        MenuItem addToMarked = new MenuItem("Add to marked");
        addToMarked.setOnAction(event -> {
            ObservableList<ExtPath> selectedItems = table.getSelectionModel().getSelectedItems();
            selectedItems.forEach(item -> {
                TaskFactory.getInstance().addToMarked(item);
            });
        });
        addToMarked.visibleProperty().bind(Bindings.size(table.getSelectionModel().getSelectedItems()).greaterThan(0));
        MenuItem remove = new MenuItem("Remove");
        remove.setOnAction(event -> {
            ArrayList<ExtPath> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            selected.forEach(item -> {
                if (item.equals(filePlaying)) {
                    index--;
                }
                backingList.remove(item);
            });
            extTableView.updateContentsAndSort(backingList);
            update();
        });
        remove.visibleProperty().bind(Bindings.size(table.getSelectionModel().getSelectedItems()).greaterThan(0));
        MenuItem addMarked = new MenuItem("Add marked");
        addMarked.setOnAction(event -> {
            MainController.markedList.forEach(item -> {
                addIfAbsent(item);
            });
            update();
        });
        addMarked.visibleProperty().bind(MainController.propertyMarkedSize.greaterThan(0));
        Menu marked = new Menu("Marked");
        tree.addMenuItem(marked, marked.getText());
        tree.addMenuItem(addToMarked, marked.getText(), addToMarked.getText());
        tree.addMenuItem(addMarked, marked.getText(), addMarked.getText());
        marked.visibleProperty().bind(remove.visibleProperty().or(addMarked.visibleProperty()));
        tree.addMenuItem(remove, remove.getText());
        MenuItem deleteMenuItem = CosmeticsFX.simpleMenuItem(
                "Delete",
                event -> {
                    ContinousCombinedTask task = TaskFactory.getInstance().deleteFilesEx(table.getSelectionModel().getSelectedItems());
                    task.setDescription("Delete selected files");
                    ViewManager.getInstance().newProgressDialog(task);
                }, Bindings.size(table.getSelectionModel().getSelectedItems()).greaterThan(0));
        tree.addMenuItem(deleteMenuItem, deleteMenuItem.getText());
        MenuItem renameMenuItem = CosmeticsFX.simpleMenuItem(
                "Rename ",
                event -> {
                    NumberValue<Integer> numberValue = NumberValue.of(0);
                    FileCallback cb = (filePath) -> {
                        addIfAbsent(filePath, numberValue.get());
                    };
                    numberValue.set(table.getSelectionModel().getSelectedIndex());
                    ExtPath selected = (ExtPath) table.getSelectionModel().getSelectedItem();
                    String parent = selected.getParent(1);
                    ViewManager.getInstance().newRenameDialog((ExtFolder) LocationAPI.getInstance().getFileOptimized(parent), selected, cb);
                }, Bindings.size(table.getSelectionModel().getSelectedItems()).isEqualTo(1));
        tree.addMenuItem(renameMenuItem, renameMenuItem.getText());

        table.setContextMenu(tree.constructContextMenu());
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
                getCurrentFrame().setVisible(visible);
                if (visible) {
                    getCurrentFrame().setExtendedState(JFrame.NORMAL);
                }
                if (visible && !startedWithVideo) {
                    if (getCurrentPlayer().isPlaying()) {
                        relaunch();
                    }
                }
            });
            try {
                loadState(getPlaylistsDir() + PLAYLIST_FILE_NAME);
            } catch (Exception e) {
                ErrorReport.report(e);
            }

        });

    }

    private void updateSeekLabels(Float position, Long millisPassed) {
        FX.submit(() -> {
            if (!stopping && !players.isEmpty() && getCurrentPlayer().isPlaying()) {

                this.labelTimePassed.setText(this.formatToMinutesAndSeconds(millisPassed));
                if (!this.dragTask.isInAction()) {
                    this.seekSlider.valueProperty().set(position * 100);
                }
                labelDuration.setText("/ " + formatToMinutesAndSeconds(currentLength));
            }
        });
    }

    public void updateSeek() {
        events.add(PlayerEventType.SEEK, () -> {
            if (ignoreSeek || stopping || this.playerState != PlayerState.PLAYING) {
                return;
            }
            Float position;
            if (!getCurrentPlayer().isSeekable()) {
                position = 0f;
            } else {
                position = getCurrentPlayer().getPosition();
            }
            currentLength = getCurrentPlayer().getLength();
            long millisPassed = (long) (this.currentLength * position);
            double secondsLeft = (double) (this.currentLength - millisPassed) / 1000;
            this.updateSeekLabels(position, millisPassed);

            if (!seamlessDisabled && seamless.selectedProperty().get() && (secondsLeft < seamlessSecondsMax) && (secondsLeft > 2)) {
                playNext(1, false, true, this.currentLength - millisPassed);
                return;

            }
            if (secondsLeft < minDelta) {
                Log.print("Seconds left", secondsLeft);
                playNext(1, false);
            }
        });

    }

    public void playOrPause() {

        events.add(PlayerEventType.PLAY_OR_PAUSE, () -> {
            if (getCurrentPlayer().isPlayable()) {
                if (getCurrentPlayer().isPlaying()) {
                    this.playerState = PlayerState.PAUSED;
                } else {
                    this.playerState = PlayerState.PLAYING;
                }
                getCurrentPlayer().pause();
                this.setVolume(getCurrentPlayer(), lastVolume.get());

            } else {
                playNext(0, true);
            }
        });

    }

    public void stop() {
        events.cancelAll(PlayerEventType.STOP, PlayerEventType.PLAY, PlayerEventType.PLAY_OR_PAUSE, PlayerEventType.PLAY_TASK);
        events.add(PlayerEventType.STOP, () -> {
            while (getCurrentPlayer().isPlaying()) {
                getCurrentPlayer().stop();
                Thread.sleep(1);
            }
            this.playerState = PlayerState.STOPPED;
        });

    }

    public void relaunch() {
        events.add("RELAUNCH outer", () -> {
            Log.print("Relaunch");
            relaunch(getCurrentPlayer().getPosition());
        });

    }

    private void relaunch(float position) {
        events.add("RELAUNCH inner", () -> {
            onPlayTaskComplete.add(() -> {
                events.add("Set position after relaunch", () -> {
                    Log.print("Set position", position);
                    getCurrentPlayer().setPosition(position);
                });

            });
            play(filePlaying);
        });

    }

    @Override
    public void exit() {
        stopping = true;

        players.forEach(player -> {
            player.stop();
            player.release();
        });
        frames.forEach(frame -> {
//            frame.setVisible(false);
//            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            frame.dispose();
        });
        Future save = saveState(MediaPlayerController.getPlaylistsDir() + PLAYLIST_FILE_NAME);
        F.unsafeRunWithHandler(ErrorReport::report, save::get);
        this.execService.shutdown();
        this.events.shutdown();
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
                        stop();
                        filePlaying = null;
                        update();
                        return;
                    }
                }
            }
            //default loop song
            index = ExtStringUtils.mod((index + increment), backingList.size());
            ExtPath item = (ExtPath) backingList.get(index);
            if (item == null) {
                return;
            }

            if (!seamlessDisabled && this.players.size() == 1 && opt.length > 1 && (boolean) opt[0]) {
                playSeemless(item, (long) opt[1]);
            } else {
                play(item);
            }
        });

    }

    @Override
    public void update() {

        FX.submit(() -> {
            LocationAPI.getInstance().filterIfExists(backingList);

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
            Log.print("Execute play task");
            int i = this.getIndex(item);
            if (i < 0) {
                Log.print("Play next");
                playNext(0, true);
                return null;
            }
            filePlaying = item;

            stop();
            getCurrentFrame().setTitle(filePlaying.getName(true));
            startedWithVideo = getCurrentFrame().isVisible();
            boolean playable = getCurrentPlayer().prepareMedia(filePlaying.getAbsolutePath(), getOptions());
            if (!playable) {
                table.getItems().remove(filePlaying);

            } else {
                getCurrentPlayer().start();

                //wait to start playing
                while (!getCurrentPlayer().isPlaying()) {
                    Thread.sleep(1);
                }
                Log.print("Started playing");
                if (volume != null && (volume >= 0 && volume <= 250)) {
                    setVolume(getCurrentPlayer(), volume);
                }
            }
            FX.submit(this::update).get();
            while (!onPlayTaskComplete.isEmpty()) {
                onPlayTaskComplete.pollFirst().run();
            }
            this.playerState = PlayerState.PLAYING;
            ignoreSeek = false;
            return null;
        });

    }

    private void playSeemless(ExtPath item, final long millisLeft) {
        if (seamlessDisabled) {
            return;
        }

        inSeamless = true;
        oldplayer = getCurrentPlayer();
        Value<Double> oldVolume = new Value<>((double) oldplayer.getVolume());
//        oldplayer.removeMediaPlayerEventListener(defaultPlayerEventAdapter);
        oldplayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Log.print("Finished old player");
                int i = 1;
                while (players.size() > 1) {
                    frames.pollFirst().dispose();
                    players.pollFirst().release();
//                    oldplayer.release();
                    Log.print("Frame/Player collected " + i++);
                }
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                Log.print("Finished old player");
                int i = 1;
                while (players.size() > 1) {
                    frames.pollFirst().dispose();
                    players.pollFirst().release();
//                    oldplayer.release();
                    Log.print("Frame/Player collected " + i++);
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
                /*
                 * oldVolume 100
                 * over 12 seconds
                 * change volume each 500 millis
                 * 12000 / 500 = 24 iterations
                 * 100 / 24 ~ 4.16
                 *
                 *
                 * oldVolume 100
                 * over 8 seconds
                 *
                 * 8000 / 500 = 16 iterations
                 * 100 / 16 ~ 6.25
                 *
                 */
                long millis = millisLeft;

                while (oldVolume.get() - difference > 1 && millis > 10) {

                    int setOldVol = (int) (oldVolume.get() - difference);
                    setVolume(oldplayer, setOldVol);
                    setVolume(getCurrentPlayer(), (int) difference);
//                    Log.print("Players==", oldplayer.mediaPlayerInstance(), getCurrentPlayer().mediaPlayerInstance());
//                    Log.print("Volume sets:", setOldVol, (int) difference);
                    long time = System.currentTimeMillis();
                    Thread.sleep((long) timeChangeMillis);

                    time = System.currentTimeMillis() - time;

                    millis -= time;
                    difference += inc;

                }
                Log.print("End volume resize task");
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
        Log.print("Got items", i);
        return state;
    }

    public void loadPlaylistState(PlaylistState state) {
        this.stop();
        events.add(() -> {

            this.table.getItems().clear();
            NumberValue<Integer> num = NumberValue.of(0);
            state.root.resolve(false).forEach(item -> {
                ExtPath file = LocationAPI.getInstance().getFileOptimized(item);
                addIfAbsent(file);
                num.incrementAndGet();
            });
            Log.print("Loaded files:", num.get());
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

    public Future saveState(String path) {

        return events.add(() -> {
            try {
                Log.print("Set busy");
                FX.submit(() -> {
                    labelStatus.setText("Busy");
                });

                Log.print("Init state save");
                try {
                    ArrayList<String> list = new ArrayList<>();
                    PlaylistState state = getPlaylistState();
                    list.add(state.index + "");
                    list.add(state.type);
                    list.addAll(state.root.specialString());
                    FileReader.writeToFile(path, list);
                    Log.print("Write to file size:", list.size());
                } catch (Exception e) {
                    ErrorReport.report(e);
                }
                Log.print("After state save");
                FX.submit(() -> {
                    labelStatus.setText("Ready");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void saveState() {
        saveState(getPlaylistsDir() + saveState.getText().trim());
    }

    public void loadState() {
        loadState(getPlaylistsDir() + loadState.getText().trim());
    }

    private void loadState(String path) {
        labelStatus.setText("Busy");
        SimpleTask task = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                PlaylistState state = new PlaylistState();
                try {
                    ArrayList<String> readFromFile = FileReader.readFromFile(path);
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
