package com.vanillage.raytraceantixray.data;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Location;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;

public final class PlayerData {
    private final Map<ChunkCoordIntPair, ChunkBlocks> chunks = new ConcurrentHashMap<>();
    private volatile Location location = null;
    private final Queue<BlockPosition> result = new ConcurrentLinkedQueue<>();

    public PlayerData(Location location) {
        this.location = location;
    }

    public Map<ChunkCoordIntPair, ChunkBlocks> getChunks() {
        return chunks;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Queue<BlockPosition> getResult() {
        return result;
    }
}
