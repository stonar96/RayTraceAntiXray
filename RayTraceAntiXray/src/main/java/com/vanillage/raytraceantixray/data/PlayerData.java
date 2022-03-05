package com.vanillage.raytraceantixray.data;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Location;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class PlayerData {
    private final Map<ChunkPos, ChunkBlocks> chunks = new ConcurrentHashMap<>();
    private volatile List<? extends Location> locations;
    private final Queue<BlockPos> result = new ConcurrentLinkedQueue<>();

    public PlayerData(List<? extends Location> locations) {
        this.locations = locations;
    }

    public Map<ChunkPos, ChunkBlocks> getChunks() {
        return chunks;
    }

    public List<? extends Location> getLocations() {
        return locations;
    }

    public void setLocations(List<? extends Location> locations) {
        this.locations = locations;
    }

    public Queue<BlockPos> getResult() {
        return result;
    }
}
