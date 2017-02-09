/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javax.swing.JFrame;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import utility.ErrorReport;
import static utility.ExtStringUtils.normalize;
import static utility.ExtStringUtils.mod;
import static utility.ExtStringUtils.mod;
import static utility.ExtStringUtils.mod;
import static utility.ExtStringUtils.mod;

/**
 * FXML Controller class
 *
 * @author Lemmin
 */
public class MediaPlayerController extends BaseController {
    public final static String ID = "MEDIA_PLAYER";
    public final static String PLAY_SYMBOL = "✓";
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
    private Float delta = 0f;
    private int inDrag;
    private long currentLength = 0;
    private boolean freeze;
    private boolean released = false;
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

    private SimpleFloatProperty valueToSet = new SimpleFloatProperty(0f);
    ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);
       
    private MediaPlayer getCurrentPlayer(){
        return this.players.getLast();
    }
    private JFrame getCurrentFrame(){
        return this.frames.getLast();
    }
    private SimpleTask endDrag = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                return null;
            }
        };
    public class VLCNotFoundException extends Exception{
        VLCNotFoundException(String str){
            super(str);
        }
    }
    public void discover() throws InterruptedException, VLCNotFoundException{
        
        int tries=0;
        while(!VLCfound){

            VLCfound = new NativeDiscovery().discover();
            Thread.sleep(500);
            tries++;
            if(tries>5){
                throw new VLCNotFoundException("Could not locate VLC");
            }
        }
        Log.write(RuntimeUtil.getLibVlcLibraryName()+" "+LibVlc.INSTANCE.libvlc_get_version());
        
    }
    
    public MediaPlayer getPreparedMediaPlayer(){
        EmbeddedMediaPlayer newPlayer = factory.newEmbeddedMediaPlayer();
//        Log.write("Inside: newPlayer");
        Canvas canvas = new Canvas();
        CanvasVideoSurface newVideoSurface = factory.newVideoSurface(canvas);
        newPlayer.setVideoSurface(newVideoSurface);
//        Log.write("Inside: done with surface");
//        
//        if(getCurrentFrame()!=null)
//            videoFrame.setVisible(false);
//        Log.write("New JFrame");
//        videoFrame = new JFrame("VIDEO");
        JFrame jframe = new JFrame();
        jframe.setBackground(Color.black);
        frames.add(jframe);
//        Log.write("New JFrame done");
        getCurrentFrame().toBack();
        getCurrentFrame().setSize(800, 480);
        getCurrentFrame().setVisible(true);
//        Log.write("Set visible");
        getCurrentFrame().add(canvas);
        getCurrentFrame().setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        getCurrentFrame().addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                showVideo.setSelected(false);
            }
        });
//        Log.write("Inside: done with frame");
        getCurrentFrame().setVisible(showVideo.selectedProperty().get());
        newPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter(){
            @Override
            public void stopped(MediaPlayer mediaPlayer) {

            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Log.write("Finished",filePlaying.toPath());
                SimpleTask task = new SimpleTask() {
                    @Override
                    protected Void call() throws Exception {
                            Thread.sleep(1000);
                                playNext(1,false);                     
                        return null;
                    }

                };
                task.run();

            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                Log.write("Error at",filePlaying.toPath());
                SimpleTask task = new SimpleTask() {
                    @Override
                    protected Void call() throws Exception {
                            Thread.sleep(1000);
                                playNext(1,false);                     
                        return null;
                    }

                };
                task.run();
            }
        });
        return newPlayer;
    };
    public void beforeShow(Collection<ExtPath> files){
        JFrame tempFrame = new JFrame();
        frames.add(tempFrame); 
        getCurrentFrame().setVisible(false);
        factory = new MediaPlayerFactory();
//        Log.write("Factory");
        players.add(getPreparedMediaPlayer());
        frames.pollFirst().setVisible(false);
//        Log.write("Player INIT");
        volumeSlider.valueProperty().addListener(listener->{
            
            getCurrentPlayer().setVolume((int) Math.round(volumeSlider.getValue()*2));
        });
        this.volumeSlider.setValue(50);
        
        valueToSet.bind(seekSlider.valueProperty().divide(100f));
        seekSlider.setOnMousePressed(event->{
            this.inDrag = 10;
            this.delta = getCurrentPlayer().getPosition();
            this.released = false;
            startThread();
        });
        seekSlider.setOnMouseDragged(value->{
            this.inDrag = 10;        

        });
        seekSlider.setOnMouseReleased(event->{
            this.released = true;
        });
        setUpTable();
        
        files.forEach(file->{
            addIfAbsent(file);
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
        });
            execService.scheduleAtFixedRate(()->{
                if(!stopping&&!players.isEmpty()&&getCurrentPlayer().isPlaying()){
                    volumeSlider.setValue(getCurrentPlayer().getVolume()/2);
                }
            update();
            }, 1, 3, TimeUnit.SECONDS);
            
        
        Platform.runLater(()->{
            playType.getItems().addAll(typeLoopList,typeLoopSong,typeRandom,typeStopAfterFinish);
            playType.getSelectionModel().select(0);
            showVideo.selectedProperty().addListener(listener->{
            
                getCurrentFrame().setVisible(showVideo.selectedProperty().get());
                if(showVideo.selectedProperty().get() && !startedWithVideo){
                    relaunch();
                }

            });
        });
        
    }
    public void setUpTable(){
        this.extTableView = new ExtTableView(table);
        TableColumn<ExtPath, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().propertyName);
        TableColumn<ExtPath, String> indexCol = new TableColumn<>("");
        indexCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> {
            return new SimpleStringProperty(getIndex(cellData.getValue())+"");
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
                Log.write("recently resized");
                return;
            }
            MainController.dragList = table.getSelectionModel().getSelectedItems();
            TaskFactory.dragInitWindowID = this.windowID;
            Log.write(TaskFactory.dragInitWindowID,MainController.dragList);
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
        if(item.getIdentity().equals(Identity.FILE)){
            if(!table.getItems().contains(item)){
                table.getItems().add(item);
            }
        }
        
    }
    @Override
    public void afterShow(){
        MenuTree tree = new MenuTree(null);
        MenuItem addToMarked = new MenuItem("Add to marked");
        addToMarked.setOnAction(event->{
            ObservableList<ExtPath> selectedItems = table.getSelectionModel().getSelectedItems();
            selectedItems.forEach(item->{
                TaskFactory.getInstance().addToMarked(item);
            });
        });
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
        addMarked.visibleProperty().bind(Bindings.size(MainController.markedList).greaterThan(0));
        Menu marked = new Menu("Marked:");
        tree.addMenuItem(marked, marked.getText());
        tree.addMenuItem(addToMarked, marked.getText(),addToMarked.getText());
        tree.addMenuItem(addMarked, marked.getText(),addMarked.getText());
        marked.visibleProperty().bind(remove.visibleProperty().or(addMarked.visibleProperty()));
        tree.addMenuItem(remove, remove.getText());
        table.setContextMenu(tree.constructContextMenu());
        extTableView.prepareChangeListeners();
    }
    public void startThread(){
        
        freeze = true;
        endDrag.cancel();
        endDrag = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                while(!released || inDrag>0){
                    if(this.isCancelled()){
                        return null;
                    }
                    if(inDrag>0){
                        inDrag--;
                    }
                    Thread.sleep(20);
                }
                if(Math.abs(delta-valueToSet.get())>minDelta){
                    float val = (float) normalize(valueToSet.doubleValue(),3);
                    getCurrentPlayer().setPosition(val);
                    
                }
                return null;
            }
        };
        endDrag.setOnSucceeded(event->{
            freeze = false;
        });
        new Thread(endDrag).start();
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
        if(!freeze){
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
//            getCurrentPlayer().setPosition(0);
            
        }
    }
    public void relaunch(){
        Log.write("Relaunch");
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
                Log.write("Play task over");
                return null;
            }
        };
        new Thread(task).start();
        playNext(0,true);
    }
    @Override
    public void exit(){
        stopping = true;
        this.execService.shutdownNow();
        players.forEach(player ->{
            player.stop();
            player.release();
        });
        frames.forEach(frame->{
            frame.setVisible(false);
        });
        super.exit();
    }
    public void playNext(int increment,Object... opt){
//        if(!canPlayNext){
//            return;
//        }
        Platform.runLater(()->{
            if(table.getItems().isEmpty()){
                playTaskComplete = true;
                return;
            }
            
            if(filePlaying==null){
                index=0;
            }else{
                int potIndex = this.getIndex(filePlaying);
                if(potIndex!=-1){
                    index = potIndex;
                }
            }
            if(!(boolean)opt[0]){
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
            if(opt.length>1&&(boolean)opt[1]){
                playSeemless(item,(long)opt[2]);
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
                Iterator iterator = table.getItems().iterator();
                while(iterator.hasNext()){
                    ExtPath path = (ExtPath) iterator.next();
                    if(!Files.exists(path.toPath())){
                        iterator.remove();
                    }
                }
                Platform.runLater(()->{
                  extTableView.updateContents(table.getItems());  
                });
                
                return null;
            }
        };
        Platform.runLater(t);      
    }
    public void playSelected(){
        update();
        play((ExtPath) table.getSelectionModel().getSelectedItem());
    }
    private void play(ExtPath item){
        if(item==null){
            playTaskComplete = true;
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
                    if(getCurrentPlayer().isPlaying()){
                       getCurrentPlayer().stop();
                    }
                    
                    do{
                        
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
        t.setDaemon(true);
        t.start();
    }

    private void playSeemless(ExtPath item,final long millisLeft){
        oldplayer = getCurrentPlayer();
        oldplayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter(){
                        @Override
                        public void finished(MediaPlayer mediaPlayer) {
                            Log.write("Finished old player");
                            int i = 1;
                            while(frames.size()>1){
                                frames.pollFirst().setVisible(false);
                                players.pollFirst();
                                Log.write("Frame/Player collected" + i++);
                            }
                            
                        }
                        @Override
                        public void stopped(MediaPlayer mediaPlayer) {
                            Log.write("Finished old player");
                            int i = 1;
                            while(frames.size()>1){
                                frames.pollFirst().setVisible(false);
                                players.pollFirst();
                                Log.write("Frame/Player collected" + i++);
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
                Log.write("End volume resize task");
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
    
    
    public String[] getOptions(){
        ArrayList<String> options = new ArrayList<>();   
        if(showVideo.selectedProperty().not().get()){
            options.add("no-video");
        }
//        options.add("audio-visual=visual");
//        options.add("effect-list=spectrum");
//        options.add("effect-width="+videoFrame.getWidth());
//        options.add("effect-height="+videoFrame.getHeight());
        return options.toArray(new String[1]);
    }
    public boolean isSelected(ExtPath path){
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
        state.index = this.getIndex(filePlaying);
        state.type = (String) this.playType.getSelectionModel().getSelectedItem();
        int i =0;
        for(Object item:table.getItems()){
            ExtPath path = (ExtPath) item;
            state.root.add(new LocationInRoot(path.getAbsolutePath(),false),i++);
        }
        return state;
    }
    
    public void loadPlaylistState(PlaylistState state){
        this.stop();
        this.table.getItems().clear();
        state.root.resolve(false).forEach(item ->{
            ExtPath file;
            LocationInRoot locationMapping = LocationAPI.getInstance().getLocationMapping(item);
            if(!LocationAPI.getInstance().existByLocation(locationMapping)){
                Log.write("Populate",locationMapping);
                file = LocationAPI.getInstance().getFileAndPopulate(item);
            }else{
                file = LocationAPI.getInstance().getFileByLocation(locationMapping);
            }
            if(item.equals(file.getAbsolutePath())){
                addIfAbsent(file);
            }
        });
        Platform.runLater(()->{
            this.playType.getSelectionModel().select(state.type);
        });
        this.index = state.index;
        if(table.getItems().size()>index){
            this.filePlaying = (ExtPath) table.getItems().get(index);
        }
        
    }
    public void saveState(){
        labelStatus.setText("Busy");
        
        String path = FileManagerLB.HOME_DIR + saveState.getText().trim();
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
    public void loadState(){
        labelStatus.setText("Busy");
        SimpleTask task = new SimpleTask(){
            @Override
            protected Void call() throws Exception {
                PlaylistState state = new PlaylistState();
                String path = FileManagerLB.HOME_DIR + loadState.getText().trim();
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
//        playNext(1,false,true);
        Collections.shuffle(table.getItems());
        
    }
    
}