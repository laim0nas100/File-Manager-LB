/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.Log;
import filemanagerGUI.customUI.CosmeticsFX.ExtTableView;
import filemanagerGUI.customUI.CosmeticsFX.MenuTree;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.SimpleTask;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtPath;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javax.swing.JFrame;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import static utility.ExtStringUtils.normalize;
import static utility.ExtStringUtils.mod;

/**
 * FXML Controller class
 *
 * @author Lemmin
 */
public class MediaPlayerController extends BaseController {
    public final static String ID = "MUSIC_PLAYER";
    public final static String PLAY_SYMBOL = "✓";
    public static boolean VLCfound = false;
    
    @FXML public Label labelCurrent;
    @FXML public Label labelTimePassed;
    @FXML public Label labelDuration;
    @FXML public Slider volumeSlider;
    @FXML public Slider seekSlider;
    @FXML public TableView table;
    @FXML public CheckBox showVideo;
    @FXML public ChoiceBox playType;
    @FXML public Button buttonPlayPrev;
    @FXML public Button buttonPlayNext;
    
    
    private EmbeddedMediaPlayerComponent component;
    private EmbeddedMediaPlayer  player;
    private JFrame videoFrame;
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

    private SimpleFloatProperty valueToSet = new SimpleFloatProperty(0f);
    private Timer timer = new Timer();
    private TimerTask timerTask = new TimerTask(){
        @Override
        public void run() {
            if(stopping){
                return;
            }
            Platform.runLater(()->{
                
                updateSeek();
            });
            
        }
        
    };
    private SimpleTask endDrag = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                return null;
            }
        };
    
    public void beforeShow(Collection<ExtPath> files){
        if(!VLCfound){
            new NativeDiscovery().discover();
            
        }
        Log.write(RuntimeUtil.getLibVlcLibraryName()+" "+LibVlc.INSTANCE.libvlc_get_version());
        component  = new EmbeddedMediaPlayerComponent();
        Platform.runLater(()->{
            playType.getItems().addAll(typeLoopList,typeLoopSong,typeRandom,typeStopAfterFinish);
            playType.getSelectionModel().select(0);
        });
        videoFrame = new JFrame("VIDEO");
        videoFrame.setSize(800, 480);
        videoFrame.setContentPane(component);
        showVideo.selectedProperty().addListener(listener->{
            
            videoFrame.setVisible(showVideo.selectedProperty().get());
            if(showVideo.selectedProperty().get() && !startedWithVideo){
                relaunch();
            }
            
        });
        
        videoFrame.setVisible(true);
        videoFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        videoFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                showVideo.setSelected(false);
            }
        });
        player = component.getMediaPlayer();
        player.addMediaPlayerEventListener(new MediaPlayerEventAdapter(){
            @Override
            public void stopped(MediaPlayer mediaPlayer) {

            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Log.write("Finished",filePlaying.toPath());
                Platform.runLater(()->{
                    if(playTaskComplete)
                        playNext(1,false); 
                });
                
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                Log.write("Error at",filePlaying.toPath());
                SimpleTask task = new SimpleTask() {
                    @Override
                    protected Void call() throws Exception {
                        if(playTaskComplete){
                            Thread.sleep(1000);
                                playNext(1,false);                     
                        }
                        return null;
                    }
                
                };
                Platform.runLater(task);
            }
        });
        volumeSlider.valueProperty().addListener(listener->{
            
            player.setVolume((int) Math.round(volumeSlider.getValue()*2));
        });
        this.volumeSlider.setValue(50);
        
        valueToSet.bind(seekSlider.valueProperty().divide(100f));
        seekSlider.setOnMousePressed(event->{
            this.inDrag = 10;
            this.delta = player.getPosition();
            this.released = false;
            startThread();
        });
        seekSlider.setOnMouseDragged(value->{
            this.inDrag = 10;        

        });
        seekSlider.setOnMouseReleased(event->{
            this.released = true;
        });
        files.forEach(file->{
            table.getItems().add(file);
        });
        setUpTable();
        buttonPlayPrev.setOnAction(event ->{
            playNext(-1,true);
        });
        buttonPlayNext.setOnAction(event ->{
            playNext(1,true);
        });
        
        SimpleTask task = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                timer.scheduleAtFixedRate(timerTask, 0, 500);
                videoFrame.setVisible(showVideo.selectedProperty().get());
                Thread.sleep(1000);

                update();
                return null;
            }
        };
        Platform.runLater(task);
        
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
                return;
            }
            MainController.dragList = table.getSelectionModel().getSelectedItems();
            TaskFactory.dragInitWindowID = ID;
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
            Dragboard db = event.getDragboard();
            if (event.getDragboard().hasString()){
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        table.setOnDragDropped((DragEvent event)-> {
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
            ObservableList<ExtPath> selectedItems = table.getSelectionModel().getSelectedItems();
            selectedItems.forEach(item->{
                if(item.equals(filePlaying)){
                    index--;
                }
                table.getItems().remove(item);
            });
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
                    Thread.sleep(50);
                }
                if(Math.abs(delta-valueToSet.get())>minDelta){
                    float val = (float) normalize(valueToSet.doubleValue(),3);
                    player.setPosition(val);
                    
                }
//                Log.write(delta,valueToSet.get(),minDelta);
                
                return null;
            }
        };
        endDrag.setOnSucceeded(event->{
            freeze = false;
//            if(!player.isPlaying())
//                player.play();
        });
        new Thread(endDrag).start();
    }
    public void updateSeek() {
        Float position;
        if(stopping){
            return;
        }
        if(!player.isSeekable()){
            position = 0f;
        }else{
            position = player.getPosition();
        }
        
        long millisPassed = Math.round(this.currentLength*position);
        this.labelTimePassed.setText(this.formatToMinutesAndSeconds(millisPassed));
        if(!freeze){
            this.seekSlider.valueProperty().set(position*100);
        }
        currentLength = player.getLength();
        labelDuration.setText("/ "+formatToMinutesAndSeconds(currentLength));
       

    }
    
    public void playOrPause(){
        if(player.isPlayable()){
            player.pause();
        }else{
            playNext(0,true);
        }
    }
    public void stop(){
        if(player.isPlaying()){
            player.stop();
        }
    }
    public void relaunch(){
        relaunch(player.getPosition());
    }
    private void relaunch(float position){
        stop();
        playNext(0,true);
        playTaskComplete = false;
        SimpleTask task = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                do{
                    Thread.sleep(10);
                }while(!playTaskComplete);
                player.setPosition(position);
                return null;
            }
        };
        new Thread(task).start();
    }
    @Override
    public void exit(){
        stopping = true;
        timerTask.cancel();
        timer.cancel();
        player.stop();
        component.release();
        videoFrame.setVisible(false);
        super.exit();
    }
    public void playNext(int increment,boolean disregard){
        Platform.runLater(()->{
            if(table.getItems().isEmpty()){
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
            if(!disregard){
                if(playType.getSelectionModel().getSelectedItem().equals(typeRandom)){
                    index = (int) (Math.random() * table.getItems().size());
                }else if(playType.getSelectionModel().getSelectedItem().equals(typeLoopSong)){
                    if(increment==1){
                        index--;
                    }
                }else if(playType.getSelectionModel().getSelectedItem().equals(typeStopAfterFinish)){
                    if(index + increment == table.getItems().size()){
                        player.stop();
                        filePlaying = null;
                        update();
                        return;
                    }
                }
            }
                //default loop song

            index = mod((index + increment ), table.getItems().size());
            ExtPath item = (ExtPath) table.getItems().get(index);
            if(item==null){
                return;
            }
            play(item);
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
            return;
        }
        if(player.isPlaying()){
            player.stop();
            
        }
        filePlaying = item;
        SimpleTask playTask = new SimpleTask() {
            @Override
            protected Void call() throws Exception {
                    playTaskComplete = false;
                    if(player.isPlaying()){
                       player.stop();
                    }
                    
                    do{
                        
                        Thread.sleep(10);
                    }while(player.isPlaying());
                    videoFrame.setTitle(filePlaying.getName(true));
                    startedWithVideo = videoFrame.isVisible();
                    boolean playable = player.startMedia(filePlaying.getAbsolutePath(),getOptions());
                    if(!playable){
                        table.getItems().remove(filePlaying);
                    }
                Platform.runLater(()->{
//                        Log.write(filePlaying.getAbsolutePath()+"playable:"+player.isPlayable());
                        
//                        player.play();
                        labelCurrent.setText(filePlaying.getAbsolutePath());
                        update();

                });

                return null;
            }
        };
        playTask.setOnSucceeded(event->{
            playTaskComplete = true;
        });
        Thread t = new Thread(playTask);
        t.setDaemon(true);
        t.start();
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
}


/*

/mnt/Extra-Space/FileZZZ/General Music Folder/The Eden Project/

*/