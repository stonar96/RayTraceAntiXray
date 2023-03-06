package com.vanillage.raytraceantixray.data;

import net.minecraft.core.BlockPos;

public final class Result {
    private final ChunkBlocks chunkBlocks;
    private final BlockPos block;
    private final boolean visible;

    public Result(ChunkBlocks chunkBlocks, BlockPos block, boolean visible) {
        this.chunkBlocks = chunkBlocks;
        this.block = block;
        this.visible = visible;
    }

    public ChunkBlocks getChunkBlocks() {
        return chunkBlocks;
    }

    public BlockPos getBlock() {
        return block;
    }

    public boolean isVisible() {
        return visible;
    }
}
