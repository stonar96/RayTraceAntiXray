package com.vanillage.raytraceantixray.data;

import java.lang.ref.WeakReference;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkBlocks {
    private final WeakReference<LevelChunk> chunk;
    private final Map<? extends BlockPos, Boolean> blocks;

    public ChunkBlocks(LevelChunk chunk, Map<? extends BlockPos, Boolean> blocks) {
        this.chunk = new WeakReference<>(chunk);
        this.blocks = blocks;
    }

    public LevelChunk getChunk() {
        return chunk.get();
    }

    public Map<? extends BlockPos, Boolean> getBlocks() {
        return blocks;
    }
}
