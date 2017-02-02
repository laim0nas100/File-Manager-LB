/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

/**
 *
 * @author Laimonas Beniušis
 */
public class Enums {
    public static enum Identity{
        FILE("file"),
        FOLDER("folder"),
        LINK("link"),
        VIRTUAL("virtual_folder");
        public String identity;
        Identity(String identity){
            this.identity = identity;
        }
    }
    public static enum WebDialog{
         About("","About.html")
        ,Regex("https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html","Pattern (Java Platform SE 7 ).html")

        ;
        public String address,local;
        WebDialog(String add,String loc){
            this.address = add;
            this.local = loc;
        }
    }
    public static enum FrameTitle{
        WINDOW("File Manager LB","fxml/Main.fxml"),
        PROGRESS_DIALOG("Progress Dialog","fxml/ProgressDialog.fxml"),
        TEXT_INPUT_DIALOG("Text Input Dialog","fxml/RenameDialog.fxml"),
        MESSAGE_DIALOG("Message Dialog",""),
        ADVANCED_RENAME_DIALOG("Advanced Rename","fxml/AdvancedRename.fxml"),
        DIR_SYNC_DIALOG("Directory Synchronization","fxml/DirSync.fxml"),
        WEB_DIALOG("Web Dialog","fxml/WebDialog.fxml"),
        DUPLICATE_FINDER_DIALOG("Duplicate Finder","fxml/DuplicateFinder.fxml"),
        VIRTUAL_FOLDER_DIALOG("Virtual Folder","fxml/VirtualFolderDialog.fxml"),
        TASK_MANAGER_DIALOG("Task Manager","fxml/TaskManager.fxml"),
        LIST_FRAME("List","fxml/List.fxml"),
        MUSIC_PLAYER("Media Payer","fxml/MediaPlayer.fxml"),
        COMMAND_DIALOG("Command Dialog","fxml/CommandWindow.fxml");
        public final String recourse;
        private final String title;
        public String getTitle(){
            return this.title+" ";
        }
        
        FrameTitle(String title,String r){
            this.title = title;
            this.recourse = r;
        }
    }
    public static enum DATA_SIZE{
        B  (1,"B"),
        KB (1024,"KB"),
        MB (1024*1024,"MB"),
        GB (1024*1024*1024,"GB");
        public long size;
        public String sizename;
        DATA_SIZE(long size,String s){
            this.size = size;
            this.sizename = s;
        }
        public void set(DATA_SIZE e,String sizename){
            e.size = DATA_SIZE.valueOf(sizename).size;
            e.sizename = DATA_SIZE.valueOf(sizename).sizename;
        }
    }
}
