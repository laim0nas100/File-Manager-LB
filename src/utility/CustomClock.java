/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Laimonas Beniušis
 */
public class CustomClock {
    public CustomClock(){
        updateDuration = 500;
        pausedTime = 0;
        timeProperty = new SimpleStringProperty();
        paused = new SimpleBooleanProperty(false);
        execService.scheduleAtFixedRate(()->{
            Platform.runLater(()->{
                    
                    updateTimeProperty();
                    if(paused.get()){
                        pausedTime+=updateDuration;
                    }
                });
        }, 0, updateDuration, TimeUnit.MILLISECONDS);
        timeStartPoint = Clock.systemUTC().instant();
    }
    public long pausedTime;
    public SimpleBooleanProperty paused;
    public SimpleStringProperty timeProperty;
    private final Instant timeStartPoint;
    private final ScheduledExecutorService execService = Executors.newScheduledThreadPool(5);
    
    public int updateDuration;
    private void updateTimeProperty(){
        timeProperty.set(getSecondsPassedRound()+"");
    }
    public Instant getNow(){
        return Clock.systemUTC().instant();
    }
    public long getMiliPassed(Instant...inst){
        Instant currentTimePoint;
        if(inst.length==0){
            currentTimePoint = Clock.systemUTC().instant();
        }else{
            currentTimePoint = inst[0];
        }
        return currentTimePoint.toEpochMilli()-timeStartPoint.toEpochMilli();
    }
    public double getSecondsPassed(Instant...inst){
        Instant currentTimePoint;
        if(inst.length==0){
            currentTimePoint = Clock.systemUTC().instant();
        }else{
            currentTimePoint = inst[0];
        }
        return (double)(currentTimePoint.toEpochMilli()-timeStartPoint.toEpochMilli()-pausedTime + updateDuration)/1000;
    }
    public long getSecondsPassedRound(Instant...inst){
        return (long) Math.floor(getSecondsPassed(inst));
    }
    public void stopTimer(){
        this.timeProperty.set("Done in: "+(this.getSecondsPassed()));
        this.execService.shutdownNow();
    }
}
