/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import LibraryLB.Threads.Sync.ConditionalWait;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import javafx.beans.property.SimpleDoubleProperty;

/**
 *
 * @author Lemmin
 */
public class ExtInputStream extends InputStream {
    public final SimpleDoubleProperty progress = new SimpleDoubleProperty(0);
    public final ConditionalWait waitingTool = new ConditionalWait();
    private InputStream stream;
    private final long bytesLength;
    private long bytesRead;
    public ExtInputStream(Path path, OpenOption... options) throws IOException {
        stream = Files.newInputStream(path, options);
        this.bytesLength = Files.size(path);
    }

    @Override
    public int read() throws IOException {
        waitingTool.conditionalWait();
        bytesRead+= 1;
        updateProgress(bytesRead,bytesLength);
        return stream.read();
    }
    @Override
    public int read(byte[] b) throws IOException{
        waitingTool.conditionalWait();
        bytesRead+= b.length;
        updateProgress(bytesRead,bytesLength);
        return stream.read(b);
    }
    @Override
    public int read(byte[] b, int off, int len) throws IOException{
        waitingTool.conditionalWait();
        bytesRead+= b.length;
        updateProgress(bytesRead,bytesLength);
        return stream.read(b, off, len);
    }

    private void updateProgress(long done, long total){
        done = Math.min(done, total);
        progress.set((double)done/total);
    }
    @Override
    public int available() throws IOException {
        return stream.available();
    }
    @Override
    public void close() throws IOException{
        stream.close();
    }
    @Override
    public void mark(int readlimit){
        stream.mark(readlimit);
    }
    @Override
    public boolean markSupported(){
        return stream.markSupported();
    }
    
    @Override
    public void reset() throws IOException{
        stream.reset();
    }
    @Override
    public long skip(long n) throws IOException{
        return stream.skip(n);
    }
    
    
    
    
    
}
