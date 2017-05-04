/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import LibraryLB.Threads.ExtTask;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 *
 * @author lemmin
 */
public class FileUtils {
    
    public static final String PROGRESS_KEY = "progress_key";
    public static ExtTask copy(Path src, Path dest,boolean useStream, CopyOption...options){
        ExtTask task = new ExtTask() {
            @Override
            protected Object call() throws Exception {
                DoubleProperty progress = (DoubleProperty) this.valueMap.get(PROGRESS_KEY);
                progress.set(0);
                if(!Files.isDirectory(src) && useStream){
                    ExtInputStream stream = new ExtInputStream(src);
                    this.paused.addListener(listener ->{
                        if(paused.get()){
                            stream.waitingTool.requestWait();
                        }
                        else{
                            stream.waitingTool.wakeUp();
                        }
                    });
                    
                    stream.progress.addListener(listener ->{
                        progress.set(stream.progress.get());
                    });
                    Files.copy(stream, dest,options); 
                }
                else{
                    Files.copy(src, dest, options);
                }  
                progress.set(1);
                return null;
            }
        };
        SimpleDoubleProperty progress = new SimpleDoubleProperty();
        task.valueMap.put(PROGRESS_KEY,progress);       
        return task;
    }
    
    public static ExtTask move(Path src, Path dest,boolean useStream, CopyOption...options){
        ExtTask task = new ExtTask() {
            @Override
            protected Object call() throws Exception {
                DoubleProperty progress = (DoubleProperty) this.valueMap.get(PROGRESS_KEY);
                progress.set(0);
                FileSystemProvider providerSrc = src.getFileSystem().provider();
                FileSystemProvider providerDest = dest.getFileSystem().provider();
                if(!useStream || (Files.isDirectory(src) || (providerSrc == providerDest))){
                    providerSrc.move(src, dest, options);
                }
                else{
                    ExtInputStream stream = new ExtInputStream(src);
                    this.paused.addListener(listener ->{
                        if(paused.get()){
                            stream.waitingTool.requestWait();
                        }
                        else{
                            stream.waitingTool.wakeUp();
                        }
                    });                    
                    stream.progress.addListener(listener ->{
                        progress.set(stream.progress.get());
                    });
                    Files.copy(stream, dest,options); 
                    
                    Files.delete(src);
                }
                progress.set(1);
                return null;
            }
        };
        SimpleDoubleProperty progress = new SimpleDoubleProperty();
        task.valueMap.put(PROGRESS_KEY,progress);
        return task;
    }
}
