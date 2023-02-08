package com.vanillage.raytraceantixray.data;

import net.minecraft.core.BlockPos;

public final class Result {
    private final BlockPos block;
    private final boolean visible;

    public Result(BlockPos block, boolean visible) {
        this.block = block;
        this.visible = visible;
    }

    public BlockPos getBlock() {
        return block;
    }

    public boolean isVisible() {
        return visible;
    }
}
