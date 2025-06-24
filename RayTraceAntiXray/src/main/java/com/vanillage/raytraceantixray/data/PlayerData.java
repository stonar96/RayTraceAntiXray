package com.vanillage.raytraceantixray.data;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class PlayerData implements Callable<Object> {
    private volatile VectorialLocation[] locations;
    private ScheduledTask scheduledTask;
    private final ConcurrentMap<LongWrapper, ChunkBlocks> chunks = new ConcurrentHashMap<>();
    private final Queue<Result> results = new ConcurrentLinkedQueue<>();
    private Callable<?> callable;

    public PlayerData(VectorialLocation[] locations, ScheduledTask scheduledTask) {
        this.locations = locations;
        this.scheduledTask = scheduledTask;
    }

    public VectorialLocation[] getLocations() {
        return locations;
    }

    public void setLocations(VectorialLocation[] locations) {
        this.locations = locations;
    }

    public ScheduledTask getScheduledTask() {
        return scheduledTask;
    }

    public void setScheduledTask(ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
    }

    public ConcurrentMap<LongWrapper, ChunkBlocks> getChunks() {
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
