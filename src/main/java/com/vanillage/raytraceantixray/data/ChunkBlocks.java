package com.vanillage.raytraceantixray.data;

import java.util.Collection;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;

public final class ChunkBlocks {
    private final Chunk chunk;
    private final Collection<BlockPosition> blocks;

    public ChunkBlocks(Chunk chunk, Collection<BlockPosition> blocks) {
        this.chunk = chunk;
        this.blocks = blocks;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public Collection<BlockPosition> getBlocks() {
        return blocks;
    }
}
