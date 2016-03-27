/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.time.Clock;
import java.time.Instant;

/**
 *
 * @author Laimonas Beniušis
 */
public class CustomClock {
    protected CustomClock(){
        }
        private static final CustomClock instance = new CustomClock();
        private static final Instant timeStartPoint = Clock.systemUTC().instant();
        public Instant getNow(){
            return Clock.systemUTC().instant();
        }
        public  long getMiliPassed(Instant...inst){
            Instant currentTimePoint;
            if(inst.length==0){
                currentTimePoint = Clock.systemUTC().instant();
            }else{
                currentTimePoint = inst[0];
            }
            return currentTimePoint.toEpochMilli()-timeStartPoint.toEpochMilli();
        }
        public  double getSecondsPassed(Instant...inst){
            Instant currentTimePoint;
            if(inst.length==0){
                currentTimePoint = Clock.systemUTC().instant();
            }else{
                currentTimePoint = inst[0];
            }
            return (double)(currentTimePoint.toEpochMilli()-timeStartPoint.toEpochMilli())/1000;
        }
        public  double getSecondsPassedRound(Instant...inst){
            Instant currentTimePoint;
            if(inst.length==0){
                currentTimePoint = Clock.systemUTC().instant();
            }else{
                currentTimePoint = inst[0];
            }
            return (currentTimePoint.getEpochSecond()-timeStartPoint.getEpochSecond());
        }
        public static CustomClock getClock(){
            return instance;
        }
}
