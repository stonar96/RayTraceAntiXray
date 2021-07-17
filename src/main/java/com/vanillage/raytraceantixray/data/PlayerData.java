package com.vanillage.raytraceantixray.data;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Location;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;

public final class PlayerData {
    private final Map<ChunkCoordIntPair, ChunkBlocks> chunks = new ConcurrentHashMap<>();
    private volatile List<? extends Location> locations;
    private final Queue<BlockPosition> result = new ConcurrentLinkedQueue<>();

    public PlayerData(List<? extends Location> locations) {
        this.locations = locations;
    }

    public Map<ChunkCoordIntPair, ChunkBlocks> getChunks() {
        return chunks;
    }

    public List<? extends Location> getLocations() {
        return locations;
    }

    public void setLocations(List<? extends Location> locations) {
        this.locations = locations;
    }

    public Queue<BlockPosition> getResult() {
        return result;
    }
}
