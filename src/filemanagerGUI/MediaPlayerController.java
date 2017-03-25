/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.Log;
import LibraryLB.Threads.ExtTask;
import LibraryLB.Threads.TimeoutTask;
import filemanagerGUI.customUI.CosmeticsFX;
import filemanagerGUI.customUI.CosmeticsFX.ExtTableView;
import filemanagerGUI.customUI.CosmeticsFX.MenuTree;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.LocationInRootNode;
import filemanagerLogic.SimpleTask;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtPath;
import java.awt.Canvas;
import java.awt.Color;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import javax.swing.JFrame;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.discovery.NativeDiscoveryStrategy;
import uk.co.caprica.vlcj.discovery.StandardNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.discovery.linux.DefaultLinuxNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.discovery.mac.DefaultMacNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.discovery.windows.DefaultWindowsNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import utility.ErrorReport;
import static utility.ExtStringUtils.normalize;
import static utility.ExtStringUtils.mod;

/**
 * FXML Controller class
 *
 * @author Lemmin
 */
public class MediaPlayerController extends BaseController {
    
    public final static String PLAY_SYMBOL = "✓";
    public final static String PLAYLIST_FILE_NAME = "DEFAULT_PLAYLIST";
    public static String VLC_SEARCH_PATH;
    public static boolean VLCfound = false;
    
    @FXML public Label labelCurrent;
    @FXML public Label labelTimePassed;
    @FXML public Label labelDuration;
    @FXML public Label labelStatus;
    @FXML public Slider volumeSlider;
    @FXML public Slider seekSlider;
    @FXML public TableView table;
    @FXML public CheckBox showVideo;
    @FXML public ChoiceBox playType;
    @FXML public CheckBox seamless;
    @FXML public Button buttonPlayPrev;
    @FXML public Button buttonPlayNext;
    @FXML public TextField loadState;
    @FXML public TextField saveState;

    
    private MediaPlayerFactory factory;
    private MediaPlayer oldplayer;
    private boolean startedWithVideo = false;
    private int index = 0;
    private Float minDelta = 0.005f;
    private long currentLength = 0;
    private SimpleBooleanProperty released = new SimpleBooleanProperty(false);
    private boolean stopping = false;
    private boolean playTaskComplete = false;
    private String typeLoopSong = "Loop file";
    private String typeLoopList = "Loop list";
    private String typeRandom = "Random";
    private String typeStopAfterFinish = "Don't loop list";
    private ExtTableView extTableView;
    private ExtPath filePlaying;
    private ArrayDeque<JFrame> frames = new ArrayDeque<>();
    private ArrayDeque<MediaPlayer> players = new ArrayDeque<>();

    ScheduledExecutorService execService = Executors.newScheduledThreadPool(3);
    TimeoutTask dragTask = new TimeoutTask(300,20,()->{
           float val = seekSlider.valueProperty().divide(100).floatValue();
           Log.print(getCurrentPlayer().getPosition(),val);
           if(Math.abs(getCurrentPlayer().getPosition()-val)>minDelta){
                    val = (float) normalize(val,3);
                    getCurrentPlayer().setPosition(val);
                    Log.print("Set new seek");
            } 
        });
       
    private MediaPlayer getCurrentPlayer(){
        return this.players.getLast();
    }
    private JFrame getCurrentFrame(){
        return this.frames.getLast();
    }
    public class VLCNotFoundException extends Exception{
        VLCNotFoundException(String str){
            super(str);
        }
    }
    public void discover() throws InterruptedException, VLCNotFoundException{    
        if(!VLCfound){
            NativeDiscoveryStrategy[] array = new StandardNativeDiscoveryStrategy[]{
                new DefaultWindowsNativeDiscoveryStrategy(),
                new DefaultLinuxNativeDiscoveryStrategy(),
                new DefaultMacNativeDiscoveryStrategy()
            };
            int supportedOS = -1;
            for(int i = 0; i<array.length; i++){
                if(array[i].supported()){
                    supportedOS = i;
                    break;
                }
            }
            switch(supportedOS){
                case 0:{
                    array[supportedOS] = new DefaultWindowsNativeDiscoveryStrategy(){
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 1:{
                    array[supportedOS] = new DefaultLinuxNativeDiscoveryStrategy(){
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) {
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                case 2:{
                    array[supportedOS] = new DefaultMacNativeDiscoveryStrategy(){
                        @Override
                        protected void onGetDirectoryNames(List<String> directoryNames) { 
                            super.onGetDirectoryNames(directoryNames);
                            directoryNames.add(0, VLC_SEARCH_PATH);
                        }
                    };
                    break;
                }
                default:{
                    //unsupported OS?
                } 
            }
            if(supportedOS != -1){
                VLCfound = new NativeDiscovery(array[supportedOS]).discover();
            }
            if(VLCfound){
                Log.print(RuntimeUtil.getLibVlcLibraryName()+" "+LibVlc.INSTANCE.libvlc_get_version());
            }else{
                throw new VLCNotFoundException("Could not locate VLC, \n configure vlcPath in Parameters.txt");
            }
        }  
    }
    
    public MediaPlayer getPreparedMediaPlayer(){
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
        if(showVideo.selectedProperty().get()){
            jframe.setExtendedState(JFrame.NORMAL);
            jframe.setVisible(true);
        }else{
            jframe.setVisible(false);
        }
        jframe.setBackground(Color.black);
        jframe.setSize(getCurrentFrame().getSize());
        jframe.setLocation(getCurrentFrame().getLocation());
        frames.add(jframe);
        newPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter(){
            @Override
            public void stopped(MediaPlayer mediaPlayer) {

            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Log.print("Finished",filePlaying.toPath());
                playNext(1,false); 
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                Log.print("Error at",filePlaying.toPath());
                playNext(1,false);  
            }
        });
        return newPlayer;
    };
    public void beforeShow(){
        
        
    }
    public void setUpTable(){
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
            return new SimpleStringProperty(1+getIndex(cellData.getValue())+"");
        });
        TableColumn<ExtPath, String> selectedCol = new TableColumn<>("");
        selectedCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> {
            String str = "";
            if(isSelected(cellData.getValue())){
               str = PLAY_SYMBOL;
            }
            return new SimpleStringProperty(str);
        });
        indexCol.setSortable(false);
        selectedCol.setSortable(false);
        nameCol.setSortable(false);
        selectedCol.setMinWidth(20);
        selectedCol.setMaxWidth(20);
        indexCol.setMinWidth(30);
        indexCol.setMaxWidth(50);
        table.getColumns().addAll(indexCol,selectedCol,nameCol);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setOnMousePressed((MouseEvent event)->{
            if(event.isPrimaryButtonDown()){
                if(event.getClickCount()>1){
                    playSelected();
                }
            }
        });
        table.setOnDragDetected((MouseEvent event) ->{
            if(this.extTableView.recentlyResized.get()){
                Log.print("recently resized");
                return;
            }
            MainController.dragList = table.getSelectionModel().getSelectedItems();
            TaskFactory.dragInitWindowID = this.windowID;
            Log.print(TaskFactory.dragInitWindowID,MainController.dragList);
            if(!MainController.dragList.isEmpty()){
                Dragboard db = table.startDragAndDrop(TransferMode.COPY_OR_MOVE);
               ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                    content.putString("Ready");
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        });
        
        table.setOnDragOver((DragEvent event)-> {
            if(this.windowID.equals(TaskFactory.dragInitWindowID)){
                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (event.getDragboard().hasString()){
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                
                //Log.writeln(event.getDragboard().getString());
            }
            event.consume();
        });
        table.setOnDragDropped((DragEvent event)-> {
            if(this.windowID.equals(TaskFactory.dragInitWindowID)){
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!MainController.dragList.isEmpty()) {
                MainController.dragList.forEach(item->{
                    addIfAbsent(item);
                });
                update();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });   
    }
    public void addIfAbsent(ExtPath item){
        if(item!=null && item.getIdentity().equals(Identity.FILE)){
            if(!table.getItems().contains(item)){
                table.getItems().add(item);
            }
        }
        
    }
    @Override
    public void afterShow(){
        JFrame tempFrame = new JFrame();
        tempFrame.setSize(800, 480);
        frames.add(tempFrame); 
        getCurrentFrame().setVisible(false);
        factory = new MediaPlayerFactory();
//        Log.write("Factory");
        players.add(getPreparedMediaPlayer());
        frames.pollFirst().setVisible(false);
//        Log.write("Player INIT");
        volumeSlider.valueProperty().addListener(listener->{
            getCurrentPlayer().setVolume((int) Math.round(volumeSlider.getValue()));
        });
        volumeSlider.setValue(100);
        dragTask.conditionalCheck = (released);
        seekSlider.setOnMousePressed(event->{
            dragTask.update();
            released.set(false);
        });
        seekSlider.setOnMouseDragged(value->{
            dragTask.update();

        });
        seekSlider.setOnMouseReleased(event->{
            released.set(true);
        });
            
        buttonPlayPrev.setOnAction(event ->{
            playNext(-1,true);
        });
        buttonPlayNext.setOnAction(event ->{
            playNext(1,true);
        });
        
        
        
        Platform.runLater(()->{
            execService.scheduleAtFixedRate(()->{
                Platform.runLater(()->{
                    
                    if(!stopping&&!players.isEmpty()&&getCurrentPlayer().isPlaying()){
                        updateSeek();
                }
                });
                }, 1000, 300, TimeUnit.MILLISECONDS);
        
            execService.scheduleAtFixedRate(()->{
                if(!stopping&&!players.isEmpty()&&getCurrentPlayer().isPlaying()){
                    volumeSlider.setValue(getCurrentPlayer().getVolume());
                }
            }, 1, 3, TimeUnit.SECONDS);
            
            playType.getItems().addAll(typeLoopList,typeLoopSong,typeRandom,typeStopAfterFinish);
            playType.getSelectionModel().select(0);
            showVideo.selectedProperty().addListener(listener->{
                boolean visible = showVideo.selectedProperty().get();
                getCurrentFrame().setVisible(visible);
                if(visible){
                    getCurrentFrame().setExtendedState(JFrame.NORMAL);
                }
                
                if(visible && !startedWithVideo){
                    if(getCurrentPlayer().isPlaying()){
                        relaunch();
                    }
                    
                }

            });
            loadState(FileManagerLB.USER_DIR+PLAYLIST_FILE_NAME);
        });
        setUpTable();  
        MenuTree tree = new MenuTree(null);
        MenuItem addToMarked = new MenuItem("Add to marked");
        addToMarked.setOnAction(event->{
            ObservableList<ExtPath> selectedItems = table.getSelectionModel().getSelectedItems();
            selectedItems.forEach(item->{
                TaskFactory.getInstance().addToMarked(item);
            });
        });
        addToMarked.visibleProperty().bind(Bindings.size(table.getSelectionModel().getSelectedItems()).greaterThan(0));
        MenuItem remove = new MenuItem("Remove");
        remove.setOnAction(event ->{
            ArrayList<ExtPath> full = new ArrayList<>(table.getItems());
            ArrayList<ExtPath> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            selected.forEach(item->{
                if(item.equals(filePlaying)){
                    index--;
                }
                full.remove(item);   
            });
            extTableView.updateContents(full);
            update();
        });
        remove.visibleProperty().bind(Bindings.size(table.getSelectionModel().getSelectedItems()).greaterThan(0));
        MenuItem addMarked = new MenuItem("Add marked");
        addMarked.setOnAction(event->{
            MainController.markedList.forEach(item->{
                addIfAbsent(item);
            });
        });
        addMarked.visibleProperty().bind(MainController.propertyMarkedSize.greaterThan(0));
        Menu marked = new Menu("Marked");
        tree.addMenuItem(marked, marked.getText());
        tree.addMenuItem(addToMarked, marked.getText(),addToMarked.getText());
        tree.addMenuItem(addMarked, marked.getText(),addMarked.getText());
        marked.visibleProperty().bind(remove.visibleProperty().or(addMarked.visibleProperty()));
        tree.addMenuItem(remove, remove.getText());
        MenuItem deleteMenuItem = CosmeticsFX.simpleMenuItem("Delete",
            event -> {
                Log.print("Deleting");
                ExtTask task = TaskFactory.getInstance().deleteFiles(table.getSelectionModel().getSelectedItems());
                task.setTaskDescription("Delete selected files");
                ViewManager.getInstance().newProgressDialog(task);
            }, Bindings.size(table.getSelectionModel().getSelectedItems()).greaterThan(0));
        tree.addMenuItem(deleteMenuItem, deleteMenuItem.getText());
        table.setContextMenu(tree.constructContextMenu());
        table.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(table.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(table.getContextMenu());
        
        extTableView.prepareChangeListeners();

    }

    public void updateSeek() {
        Float position;
        if(stopping){
            return;
        }
        if(!getCurrentPlayer().isSeekable()){
            position = 0f;
        }else{
            position = getCurrentPlayer().getPosition();
        }
        
        long millisPassed = (long)(this.currentLength*position);
        this.labelTimePassed.setText(this.formatToMinutesAndSeconds(millisPassed));
        if(!this.dragTask.isInAction()){
            this.seekSlider.valueProperty().set(position*100);
        }
        
        currentLength = getCurrentPlayer().getLength();
        labelDuration.setText("/ "+formatToMinutesAndSeconds(currentLength));
        double secondsLeft = (this.currentLength - millisPassed) /1000;
        if((secondsLeft>2)&&(secondsLeft <10) && seamless.selectedProperty().get()){
            playNext(1,false,true,this.currentLength - millisPassed);
            
        }
            

    }
    
    public void playOrPause(){
        if(getCurrentPlayer().isPlayable()){
            getCurrentPlayer().pause();
        }else{
            playNext(0,true);
        }
    }
    public void stop(){
        if(getCurrentPlayer().isPlaying()){
            getCurrentPlayer().stop();
            
        }
    }
    public void relaunch(){
        Log.print("Relaunch");
        relaunch(getCurrentPlayer().getPosition());
    }
    private void relaunch(float position){
        stop();
        
        playTaskComplete = false;
        SimpleTask task = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                do{
                    Thread.sleep(10);
                }while(!playTaskComplete);
                getCurrentPlayer().setPosition(position);
                Log.print("Play task over");
                return null;
            }
        };
        new Thread(task).start();
        playNext(0,true);
    }
    @Override
    public void exit(){
        stopping = true;
        this.execService.shutdown();
        players.forEach(player ->{
            player.stop();
            player.release();
        });
        frames.forEach(frame->{
            frame.setVisible(false);
        });
        saveState(FileManagerLB.USER_DIR+PLAYLIST_FILE_NAME);
        super.exit();
    }
    public void playNext(int increment,boolean ignoreModifiers,Object... opt){
        Platform.runLater(()->{
            if(table.getItems().isEmpty()){
                playTaskComplete = true;
                return;
            }
            
            updateIndex();
            if(!ignoreModifiers){
                if(playType.getSelectionModel().getSelectedItem().equals(typeRandom)){
                    index = (int) (Math.random() * table.getItems().size());
                }else if(playType.getSelectionModel().getSelectedItem().equals(typeLoopSong)){
                    if(increment==1){
                        index--;
                    }
                }else if(playType.getSelectionModel().getSelectedItem().equals(typeStopAfterFinish)){
                    if(index + increment == table.getItems().size()){
                        getCurrentPlayer().stop();
                        filePlaying = null;
                        update();
                        playTaskComplete = true;
                        return;
                    }
                }
            }
                //default loop song

            index = mod((index + increment ), table.getItems().size());
            ExtPath item = (ExtPath) table.getItems().get(index);
            if(item==null){
                playTaskComplete = true;
                return;
            }
            if(opt.length>1&&(boolean)opt[0]){
                playSeemless(item,(long)opt[1]);
            }else{
                play(item);
            }
        });
        
        
        
    }
    @Override
    public void update(){
        SimpleTask t = new SimpleTask(){
            @Override
            protected Void call() throws Exception {
                ArrayDeque deque = new ArrayDeque(table.getItems());
                Iterator iterator = deque.iterator();
                while(iterator.hasNext()){
                    ExtPath path = (ExtPath) iterator.next();
                    if(!Files.exists(path.toPath())){
                        iterator.remove();
                    }
                }
                extTableView.updateContents(deque);  
                updateIndex();
                return null;
            }
        };
        Platform.runLater(t);      
    }
    public void playSelected(){
        play((ExtPath) table.getSelectionModel().getSelectedItem());
    }
    private void play(ExtPath item){
        update();
        int i = this.getIndex(item);
        if(i<0){
            playNext(0,true);
            return;
        }
        if(getCurrentPlayer().isPlaying()){
            getCurrentPlayer().stop();
            getCurrentPlayer().setVolume(getCurrentPlayer().getVolume());
            
        }
        filePlaying = item;
        playTaskComplete = false;
        SimpleTask playTask = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                    
                    int i =0;
                    do{
                        getCurrentPlayer().stop();
                        Log.print("Sleep play task " + i++);
                        Thread.sleep(10);
                    }while(getCurrentPlayer().isPlaying());
                    getCurrentFrame().setTitle(filePlaying.getName(true));
                    startedWithVideo = getCurrentFrame().isVisible();
                    boolean playable = getCurrentPlayer().prepareMedia(filePlaying.getAbsolutePath(),getOptions());
                    if(!playable){
                        table.getItems().remove(filePlaying);
                        
                    }else{
                        getCurrentPlayer().start();
                    }
                Platform.runLater(()->{
                        labelCurrent.setText(filePlaying.getAbsolutePath());
                        update();

                });
                playTaskComplete = true;
                return null;
            }
        };
        Thread t = new Thread(playTask);
        t.start();
    }

    private void playSeemless(ExtPath item,final long millisLeft){
        oldplayer = getCurrentPlayer();
        oldplayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter(){
                        @Override
                        public void finished(MediaPlayer mediaPlayer) {
                            Log.print("Finished old player");
                            int i = 1;
                            while(frames.size()>1){
                                frames.pollFirst().setVisible(false);
                                players.pollFirst();
                                Log.print("Frame/Player collected " + i++);
                            }
                            
                        }
                        @Override
                        public void stopped(MediaPlayer mediaPlayer) {
                            Log.print("Finished old player");
                            int i = 1;
                            while(frames.size()>1){
                                frames.pollFirst().setVisible(false);
                                players.pollFirst();
                                Log.print("Frame/Player collected " + i++);
                            }
                        }
                    });

        players.add(getPreparedMediaPlayer());
        SimpleTask volumeChangeTask = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                double ratio = ((double)10000/millisLeft);
                double oldVolume = oldplayer.getVolume();
                double difference = 1;
                double inc =  oldVolume/75 * ratio;
                
                if(inc<1){
                    inc = 1 * ratio;
                }
                long millis = millisLeft;
                getCurrentPlayer().setVolume(0);
                
                while(oldVolume-difference>1 && millis>2000){
                    oldplayer.setVolume((int) (oldVolume-difference));
                    getCurrentPlayer().setVolume((int)difference);
                    difference+=inc;
                    Thread.sleep(100);
                    millis-=100;
                    
                }
                Log.print("End volume resize task");
                getCurrentPlayer().setVolume((int)oldVolume);
                oldplayer.stop();
                return null;
            }
        };
        Thread t = new Thread(volumeChangeTask);
        t.start();
        
        play(item);
    }
    
    private String formatToMinutesAndSeconds(long millis){
        long minutes = (millis / 1000)  / 60;
        long seconds = (millis / 1000) % 60;
        if(seconds<10){
            return minutes+":0"+seconds;
        }else{
            return minutes+":"+seconds;
        }
    }
    private void updateIndex(){
        if(filePlaying==null){
                index=0;
            }else{
                int potIndex = this.getIndex(filePlaying);
                if(potIndex!=-1){
                    index = potIndex;
                }else{
                    index = 0;
                }
            }
    }
    
    public String[] getOptions(){
        ArrayList<String> options = new ArrayList<>();   
        if(showVideo.selectedProperty().not().get()){
            options.add("no-video");
        }
        else{
//        options.add("audio-visual=visual");
//        options.add("effect-list=spectrometer");
//        options.add("effect-width="+getCurrentFrame().getWidth());
//        options.add("effect-height="+getCurrentFrame().getHeight());
        }
        return options.toArray(new String[1]);
    }
    public boolean isSelected(ExtPath path){
        if(path == null){
            return false;
        }
        return path.equals(this.filePlaying);
    }
    public int getIndex(ExtPath path){
        return table.getItems().indexOf(path);
    }
    
    public static class PlaylistState{
        public LocationInRootNode root;
        public Integer index;
        public String type;
        public PlaylistState(){};
        
    }
    public PlaylistState getPlaylistState(){
        PlaylistState state = new PlaylistState();
        state.root = new LocationInRootNode("",-1);
        state.index = Math.max(0,getIndex(filePlaying));
        state.type = (String) this.playType.getSelectionModel().getSelectedItem();
        int i =0;
        for(Object item:table.getItems()){
            ExtPath path = (ExtPath) item;
            state.root.add(new LocationInRoot(path.getAbsoluteDirectory(),false),i++);
        }
        return state;
    }  
    public void loadPlaylistState(PlaylistState state){
        this.stop();
        this.table.getItems().clear();
        state.root.resolve(false).forEach(item ->{
            ExtPath file = LocationAPI.getInstance().getFileOptimized(item);
            addIfAbsent(file);
        });
        Platform.runLater(()->{
            this.playType.getSelectionModel().select(state.type);
        });
        this.index = state.index;
        if(table.getItems().size()>index){
            this.filePlaying = (ExtPath) table.getItems().get(index);
        }
        
    }
    public void saveState(String path){
       labelStatus.setText("Busy");
        SimpleTask task = new SimpleTask(){
            @Override
            protected Void call() throws Exception {
                try{
                ArrayList<String> list = new ArrayList<>();
                PlaylistState state = getPlaylistState();
                list.add(state.index+"");
                list.add(state.type);
                list.add(state.root.specialString());
                LibraryLB.FileManaging.FileReader.writeToFile(path, list);
                }catch(Exception e){
                    report(e);
                }
                return null;
            }
        };
        task.setOnSucceeded(value ->{
                labelStatus.setText("Ready");
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start(); 
    }
    public void saveState(){
        saveState(FileManagerLB.USER_DIR + saveState.getText().trim());
    }
    public void loadState(){
        loadState(FileManagerLB.USER_DIR + loadState.getText().trim());
    }
    private void loadState(String path){
        labelStatus.setText("Busy");
        SimpleTask task = new SimpleTask(){
            @Override
            protected Void call() throws Exception {
                PlaylistState state = new PlaylistState();
                try{
                    LinkedList<String> readFromFile = (LinkedList<String>) LibraryLB.FileManaging.FileReader.readFromFile(path);
                    state.index = Integer.parseInt(readFromFile.pollFirst());
                    state.type = readFromFile.pollFirst();
                    state.root = LocationInRootNode.nodeFromFile(readFromFile);
                    loadPlaylistState(state);
                    update();
                }catch(Exception e){
                    ErrorReport.report(e);
                }
                return null;
            };
            
        };
        task.setOnSucceeded(value ->{
                labelStatus.setText("Ready");
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
    public void shuffle(){
        Collections.shuffle(table.getItems());
        
    }
    
}