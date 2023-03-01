package com.vanillage.raytraceantixray.data;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkBlocks {
    private final Reference<LevelChunk> chunk;
    private final long key;
    private final Map<BlockPos, Boolean> blocks;

    public ChunkBlocks(LevelChunk chunk, Map<BlockPos, Boolean> blocks) {
        this.chunk = new WeakReference<>(chunk);
        key = chunk.getPos().toLong();
        this.blocks = blocks;
    }

    public LevelChunk getChunk() {
        return chunk.get();
    }

    public long getKey() {
        return key;
    }

    public Map<BlockPos, Boolean> getBlocks() {
        return blocks;
    }
}
