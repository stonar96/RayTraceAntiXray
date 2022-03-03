package com.vanillage.raytraceantixray.data;

import java.lang.ref.WeakReference;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;

public final class ChunkBlocks {
    private final WeakReference<Chunk> chunk;
    private final Iterable<? extends BlockPosition> blocks;

    public ChunkBlocks(Chunk chunk, Iterable<? extends BlockPosition> blocks) {
        this.chunk = new WeakReference<>(chunk);
        this.blocks = blocks;
    }

    public Chunk getChunk() {
        return chunk.get();
    }

    public Iterable<? extends BlockPosition> getBlocks() {
        return blocks;
    }
}
