/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import filemanagerLogic.fileStructure.ExtFile;
import java.nio.file.Files;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *  Produces background tasks (threads)
 * @author Laimonas Beniušis
 */

//
public class TaskFactory {
    private static final TaskFactory instance = new TaskFactory();
    public static TaskFactory getInstance(){
        return instance;
    }
    protected TaskFactory(){}
    
    
    public ExtTask copyFiles(ExtFile[] list){

        return new ExtTask(){
            @Override protected Void call() throws Exception {
                String str = "";
                for(int i=0; i<list.length; i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        break;
                    }
                    
                    str = "Source: \t\t"+list[i].getAbsolutePath()+"\n";
                    str +="Destination: \t"+list[i].getDestination();
                    updateMessage(str);
                    updateProgress(i+0.5, list.length);
                    try{
                        Files.copy(list[i].toPath(),list[i].getDestination());
                        list[i].setOperationSuccessfull(true);
                        System.out.println("OK:"+list[i].getAbsolutePath());
                        
                    }catch(Exception e){
                        list[i].setOperationSuccessfull(false);
                        System.out.println("Error:"+list[i].getAbsolutePath()+" "+e.getLocalizedMessage());
                    }
                    updateProgress(i+1, list.length);
                    //updateMessage(str);
                    
                }
                return null;
            }
            
        };
    }
    public Task moveFiles(ExtFile[] list){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                String str = "";
                for(int i=0; i<list.length; i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        break;
                    }
                    
                    str = "Source: \t\t"+list[i].getAbsolutePath()+"\n";
                    str +="Destination: \t"+list[i].getDestination();
                    updateMessage(str);
                    updateProgress(i+0.5, list.length);
                    try{
                        Files.createDirectories(list[i].getDestination());
                        Files.move(list[i].toPath(),list[i].getDestination());
                        list[i].setOperationSuccessfull(true);
                        System.out.println("OK:"+list[i].getAbsolutePath());
                    }catch(Exception e){
                        list[i].setOperationSuccessfull(false);
                        System.out.println("Error:"+list[i].getAbsolutePath()+" "+e.getLocalizedMessage());
                    }
                    updateProgress(i+1, list.length);
                    //updateMessage(str);
                }
                return null;
            }
            
        };
    }
    public Task deleteFiles(ExtFile[] list){
        return new ExtTask(){
            @Override protected Void call() throws Exception {
                String str = "";
                for(int i=0; i<list.length; i++){
                    while(this.isPaused()){
                        Thread.sleep(getRefreshDuration());
                        if(this.isCancelled()){
                            break;
                        }
                    }
                    if(this.isCancelled()){
                        break;
                    }

                    str = "Deleting: \t"+list[i].getAbsolutePath();
                    updateMessage(str);
                    updateProgress(i+0.5, list.length);
                    try{
                        list[i].setOperationSuccessfull(Files.deleteIfExists(list[i].toPath()));
                     
                    }catch(Exception e){
                        list[i].setOperationSuccessfull(false);
                        System.out.println("Error:"+list[i].getAbsolutePath()+" "+e.getLocalizedMessage());
                    }
                    updateProgress(i+1, list.length);
                    //updateMessage(str);
                }
                return null;
            }
        };
    }

}
