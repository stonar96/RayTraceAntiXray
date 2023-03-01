package com.vanillage.raytraceantixray.data;

public final class Result {
    private final int x;
    private final int y;
    private final int z;
    private final boolean visible;

    public Result(int x, int y, int z, boolean visible) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.visible = visible;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isVisible() {
        return visible;
    }
}
