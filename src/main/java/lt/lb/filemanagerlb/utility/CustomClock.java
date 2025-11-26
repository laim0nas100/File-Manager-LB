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
        pausedTime = 0;
        timeProperty = new SimpleStringProperty();
        paused = new SimpleBooleanProperty(false);
        scheduled = exe.scheduleAtFixedRate(() -> {
            if (paused.get()) {
                pausedTime += clockTickRateMS;
            } else {
                FX.runAndWait(this::updateTimeProperty);
            }

        }, 0, clockTickRateMS, TimeUnit.MILLISECONDS);
        timeStartPoint = getNow();
    }
    public long pausedTime;
    public SimpleBooleanProperty paused;
    public SimpleStringProperty timeProperty;
    private final Instant timeStartPoint;
    private final ScheduledFuture scheduled;
    private boolean done = false;

    public static final int clockTickRateMS = 200;

    private void updateTimeProperty() {
        timeProperty.set(getSecondsPassedRound() + "");
    }

    public static Instant getNow() {
        return Clock.systemUTC().instant();
    }

    public double getSecondsPassed(Instant inst) {
        return (double) (inst.toEpochMilli() - timeStartPoint.toEpochMilli() - pausedTime + clockTickRateMS) / 1000;
    }
    
    
    public long getSecondsPassedRound(Instant inst){
        return (long) Math.floor(getSecondsPassed(inst));
    }

    public long getSecondsPassedRound() {
        return getSecondsPassedRound(getNow());
    }

    public void stopTimer(boolean update) {
        if (done) {
            return;
        }
        done = true;
        scheduled.cancel(true);
        if (update) {
            FX.runAndWait(() -> {
                timeProperty.set("Done in: " + (getSecondsPassed(getNow())));
            });

        }
    }
}
