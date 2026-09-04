package lt.lb.filemanagerlb.vlc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import lt.lb.commons.Java;
import lt.lb.commons.threads.sync.Awaiter;
import lt.lb.commons.threads.sync.WaitTime;
import org.tinylog.Logger;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;

/**
 *
 * @author laim0nas100
 */
public class VLCMediaPlayerEventListener implements MediaPlayerEventListener {

    public static enum VLCPlayerEvents {
        error, mediaPlayerReady, playing, stopped, volume
    }

    public record VLCPlayerEvent(long time, VLCPlayerEvents event) {}

    protected ReentrantLock lock = new ReentrantLock();
    protected Condition con = lock.newCondition();
    protected List<VLCPlayerEvent> events = new ArrayList<>();
    protected Awaiter.AwaiterTime awaiter = Awaiter.fromLockCondition(() -> !events.isEmpty(), lock, con);

    public void clearEvents() {
        lock.lock();
        try {
            events.clear();
        } finally {
            lock.unlock();
        }

    }

    public VLCPlayerEvent getLastEvent() {
        return events.getLast();
    }

    public VLCPlayerEvent awaitLastEvent(WaitTime time) throws InterruptedException {
        if (awaiter.awaitBool(time)) {
            Logger.info("Awaited ok");
            return getLastEvent();
        }
        return null;
    }

    public boolean ready() throws InterruptedException {
        return awaitEvent(VLCPlayerEvents.mediaPlayerReady, VLCPlayerEvents.playing);
    }

    public boolean stopped() throws InterruptedException {
        return awaitEvent(VLCPlayerEvents.stopped);
    }

    public boolean volumeSet() throws InterruptedException {
        return awaitEvent(VLCPlayerEvents.volume);
    }

    public boolean awaitEvent(VLCPlayerEvents... types) throws InterruptedException {
        boolean awaitBool = awaiter.awaitBool(WaitTime.ofSeconds(1));// has events
        if (!awaitBool) {
            return false;
        }
        if (types.length == 0) { // non empty
            return true;
        }
        int lockedSize = events.size();

        // check last 2 events, because stopped also sets volume to 0.
        for (int i = lockedSize - 1; i >= 0 && i > lockedSize - 3; i--) {
            VLCPlayerEvent lastEvent = events.get(i);

            for (VLCPlayerEvents ev : types) {
                if (ev == lastEvent.event()) {
                    Logger.info("Awaited " + ev);
                    return true;
                }
            }
        }
        return false;
    }

    protected void addEvent(VLCPlayerEvents event) {
        VLCPlayerEvent vlcPlayerEvent = new VLCPlayerEvent(Java.getNanoTime(), event);
        lock.lock();
        try {
            events.add(vlcPlayerEvent);
            con.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {
    }

    @Override
    public void opening(MediaPlayer mediaPlayer) {
    }

    @Override
    public void buffering(MediaPlayer mediaPlayer, float newCache) {
    }

    @Override
    public void playing(MediaPlayer mediaPlayer) {
        addEvent(VLCPlayerEvents.playing);
    }

    @Override
    public void paused(MediaPlayer mediaPlayer) {
    }

    @Override
    public void stopped(MediaPlayer mediaPlayer) {
        addEvent(VLCPlayerEvents.stopped);
    }

    @Override
    public void forward(MediaPlayer mediaPlayer) {
    }

    @Override
    public void backward(MediaPlayer mediaPlayer) {
    }

    @Override
    public void finished(MediaPlayer mediaPlayer) {
    }

    @Override
    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
    }

    @Override
    public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {
    }

    @Override
    public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {
    }

    @Override
    public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {
    }

    @Override
    public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {
    }

    @Override
    public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
    }

    @Override
    public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
    }

    @Override
    public void scrambledChanged(MediaPlayer mediaPlayer, int newScrambled) {
    }

    @Override
    public void elementaryStreamAdded(MediaPlayer mediaPlayer, TrackType type, int id) {
    }

    @Override
    public void elementaryStreamDeleted(MediaPlayer mediaPlayer, TrackType type, int id) {
    }

    @Override
    public void elementaryStreamSelected(MediaPlayer mediaPlayer, TrackType type, int id) {
    }

    @Override
    public void corked(MediaPlayer mediaPlayer, boolean corked) {
    }

    @Override
    public void muted(MediaPlayer mediaPlayer, boolean muted) {
    }

    @Override
    public void volumeChanged(MediaPlayer mediaPlayer, float volume) {
        addEvent(VLCPlayerEvents.volume);
    }

    @Override
    public void audioDeviceChanged(MediaPlayer mediaPlayer, String audioDevice) {
    }

    @Override
    public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {
    }

    @Override
    public void error(MediaPlayer mediaPlayer) {
        addEvent(VLCPlayerEvents.error);
    }

    @Override
    public void mediaPlayerReady(MediaPlayer mediaPlayer) {
        addEvent(VLCPlayerEvents.mediaPlayerReady);
    }

}
