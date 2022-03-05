package com.vanillage.raytraceantixray.data;

import java.lang.ref.WeakReference;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkBlocks {
    private final WeakReference<LevelChunk> chunk;
    private final Iterable<? extends BlockPos> blocks;

    public ChunkBlocks(LevelChunk chunk, Iterable<? extends BlockPos> blocks) {
        this.chunk = new WeakReference<>(chunk);
        this.blocks = blocks;
    }

    public LevelChunk getChunk() {
        return chunk.get();
    }

    public Iterable<? extends BlockPos> getBlocks() {
        return blocks;
    }
}
