package lt.lb.filemanagerlb.utility;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import lt.lb.commons.javafx.FX;

/**
 *
 * @author Laimonas Beniušis
 */
public class CustomClock {

    public CustomClock(ScheduledExecutorService exe) {
        updateDuration = 500;
        pausedTime = 0;
        timeProperty = new SimpleStringProperty();
        paused = new SimpleBooleanProperty(false);
        scheduled = exe.scheduleAtFixedRate(() -> {
            FX.submit(() -> {

                updateTimeProperty();
                if (paused.get()) {
                    pausedTime += updateDuration;
                }
            });
        }, 0, updateDuration, TimeUnit.MILLISECONDS);
        timeStartPoint = getNow();
    }
    public long pausedTime;
    public SimpleBooleanProperty paused;
    public SimpleStringProperty timeProperty;
    private final Instant timeStartPoint;
    private final ScheduledFuture scheduled;
    private boolean done = false;

    public int updateDuration;

    private void updateTimeProperty() {
        timeProperty.set(getSecondsPassedRound() + "");
    }

    public static Instant getNow() {
        return Clock.systemUTC().instant();
    }

    public long getMiliPassed(Instant... inst) {
        Instant currentTimePoint;
        if (inst.length == 0) {
            currentTimePoint = getNow();
        } else {
            currentTimePoint = inst[0];
        }
        return currentTimePoint.toEpochMilli() - timeStartPoint.toEpochMilli();
    }

    public double getSecondsPassed(Instant... inst) {
        Instant currentTimePoint;
        if (inst.length == 0) {
            currentTimePoint = Clock.systemUTC().instant();
        } else {
            currentTimePoint = inst[0];
        }
        return (double) (currentTimePoint.toEpochMilli() - timeStartPoint.toEpochMilli() - pausedTime + updateDuration) / 1000;
    }

    public long getSecondsPassedRound(Instant... inst) {
        return (long) Math.floor(getSecondsPassed(inst));
    }

    public void stopTimer() {
        if (done) {
            return;
        }
        done = true;
        this.timeProperty.set("Done in: " + (this.getSecondsPassed()));
        this.scheduled.cancel(true);
    }
}
