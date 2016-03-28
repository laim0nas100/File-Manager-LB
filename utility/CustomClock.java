/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.time.Clock;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Laimonas Beniušis
 */
public class CustomClock {
    public CustomClock(){
        timeProperty = new SimpleStringProperty();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(()->{
                    timeProperty.set(getSecondsPassedRound()+"");
                    //Log.writeln("Timer running");
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
        timeStartPoint = Clock.systemUTC().instant();
    }
    
    public SimpleStringProperty timeProperty;
    private final Timer timer;
    private final TimerTask timerTask;
    private final Instant timeStartPoint;
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
        return (double)(currentTimePoint.toEpochMilli()-timeStartPoint.toEpochMilli())/1000;
    }
    public long getSecondsPassedRound(Instant...inst){
        Instant currentTimePoint;
        if(inst.length==0){
            currentTimePoint = Clock.systemUTC().instant();
        }else{
            currentTimePoint = inst[0];
        }
        return (currentTimePoint.getEpochSecond()-timeStartPoint.getEpochSecond());
    }
    public void stopTimer(){
        this.timer.cancel();
    }
}
