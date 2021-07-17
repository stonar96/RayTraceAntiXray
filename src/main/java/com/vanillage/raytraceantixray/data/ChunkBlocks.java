package com.vanillage.raytraceantixray.data;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;

public final class ChunkBlocks {
    private final Chunk chunk;
    private final Iterable<? extends BlockPosition> blocks;

    public ChunkBlocks(Chunk chunk, Iterable<? extends BlockPosition> blocks) {
        this.chunk = chunk;
        this.blocks = blocks;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public Iterable<? extends BlockPosition> getBlocks() {
        return blocks;
    }
}
