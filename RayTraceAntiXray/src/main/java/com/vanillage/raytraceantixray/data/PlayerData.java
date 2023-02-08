package com.vanillage.raytraceantixray.data;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Location;

import net.minecraft.world.level.ChunkPos;

public final class PlayerData {
    private volatile List<? extends Location> locations;
    private final Map<ChunkPos, ChunkBlocks> chunks = new ConcurrentHashMap<>();
    private final Queue<Result> resultQueue = new ConcurrentLinkedQueue<>();
    private Callable<?> callable;

    public PlayerData(List<? extends Location> locations) {
        this.locations = locations;
    }

    public List<? extends Location> getLocations() {
        return locations;
    }

    public void setLocations(List<? extends Location> locations) {
        this.locations = locations;
    }

    public Map<ChunkPos, ChunkBlocks> getChunks() {
        return chunks;
    }

    public Queue<Result> getResultQueue() {
        return resultQueue;
    }

    public Callable<?> getCallable() {
        return callable;
    }

    public void setCallable(Callable<?> callable) {
        this.callable = callable;
    }
}
