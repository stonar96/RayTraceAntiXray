package com.vanillage.raytraceantixray.data;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

public final class PlayerData implements Callable<Object> {
    private volatile VectorialLocation[] locations;
    private final NonBlockingHashMapLong<ChunkBlocks> chunks = new NonBlockingHashMapLong<>();
    private final Queue<Result> results = new ConcurrentLinkedQueue<>();
    private Callable<?> callable;

    public PlayerData(VectorialLocation[] locations) {
        this.locations = locations;
    }

    public VectorialLocation[] getLocations() {
        return locations;
    }

    public void setLocations(VectorialLocation[] locations) {
        this.locations = locations;
    }

    public NonBlockingHashMapLong<ChunkBlocks> getChunks() {
        return chunks;
    }

    public Queue<Result> getResults() {
        return results;
    }

    public Callable<?> getCallable() {
        return callable;
    }

    public void setCallable(Callable<?> callable) {
        this.callable = callable;
    }

    @Override
    public Object call() throws Exception {
        return callable.call();
    }
}
