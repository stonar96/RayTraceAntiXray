package com.vanillage.raytraceantixray.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.util.Vector;

import net.minecraft.core.BlockPos;

// Amanatides, J., & Woo, A. A Fast Voxel Traversal Algorithm for Ray Tracing. http://www.cse.yorku.ca/~amana/research/grid.pdf.
public final class BlockIteratorOriginal implements Iterator<BlockPos> {
    private int x;
    private int y;
    private int z;
    private final int stepX;
    private final int stepY;
    private final int stepZ;
    private final double tMax;
    private double tMaxX;
    private double tMaxY;
    private double tMaxZ;
    private final double tDeltaX;
    private final double tDeltaY;
    private final double tDeltaZ;
    private BlockPos next;

    public BlockIteratorOriginal(Vector start, Vector end) {
        x = start.getBlockX();
        y = start.getBlockY();
        z = start.getBlockZ();
        Vector direction = end.clone().subtract(start);
        tMax = direction.length();
        // direction.normalize();
        direction.setX(direction.getX() / tMax);
        direction.setY(direction.getY() / tMax);
        direction.setZ(direction.getZ() / tMax);
        stepX = direction.getX() < 0. ? -1 : 1;
        stepY = direction.getY() < 0. ? -1 : 1;
        stepZ = direction.getZ() < 0. ? -1 : 1;
        tMaxX = direction.getX() == 0. ? Double.POSITIVE_INFINITY : (x + (stepX + 1) / 2 - start.getX()) / direction.getX();
        tMaxY = direction.getY() == 0. ? Double.POSITIVE_INFINITY : (y + (stepY + 1) / 2 - start.getY()) / direction.getY();
        tMaxZ = direction.getZ() == 0. ? Double.POSITIVE_INFINITY : (z + (stepZ + 1) / 2 - start.getZ()) / direction.getZ();
        tDeltaX = 1. / Math.abs(direction.getX());
        tDeltaY = 1. / Math.abs(direction.getY());
        tDeltaZ = 1. / Math.abs(direction.getZ());
        // next = new BlockPos(x, y, z);
        calculateNext(); // This implementation doesn't include the start block. Use comment above if needed.
    }

    public BlockIteratorOriginal(Vector start, Vector direction, double distance) {
        x = start.getBlockX();
        y = start.getBlockY();
        z = start.getBlockZ();
        direction = direction.clone().multiply(Math.signum(distance)).normalize();
        tMax = Math.abs(distance);
        stepX = direction.getX() < 0. ? -1 : 1;
        stepY = direction.getY() < 0. ? -1 : 1;
        stepZ = direction.getZ() < 0. ? -1 : 1;
        tMaxX = direction.getX() == 0. ? Double.POSITIVE_INFINITY : (x + (stepX + 1) / 2 - start.getX()) / direction.getX();
        tMaxY = direction.getY() == 0. ? Double.POSITIVE_INFINITY : (y + (stepY + 1) / 2 - start.getY()) / direction.getY();
        tMaxZ = direction.getZ() == 0. ? Double.POSITIVE_INFINITY : (z + (stepZ + 1) / 2 - start.getZ()) / direction.getZ();
        tDeltaX = 1. / Math.abs(direction.getX());
        tDeltaY = 1. / Math.abs(direction.getY());
        tDeltaZ = 1. / Math.abs(direction.getZ());
        // next = new BlockPos(x, y, z);
        calculateNext(); // This implementation doesn't include the start block. Use comment above if needed.
    }

    private void calculateNext() {
        if (tMaxX < tMaxY) {
            if (tMaxZ < tMaxX) {
                if (tMaxZ <= tMax) {
                    z += stepZ;
                    next = new BlockPos(x, y, z);
                    tMaxZ += tDeltaZ;
                } else {
                    next = null;
                }
            } else {
                if (tMaxX <= tMax) {
                    if (tMaxZ == tMaxX) {
                        z += stepZ;
                        tMaxZ += tDeltaZ;
                    }

                    x += stepX;
                    next = new BlockPos(x, y, z);
                    tMaxX += tDeltaX;
                } else {
                    next = null;
                }
            }
        } else if (tMaxY < tMaxZ) {
            if (tMaxY <= tMax) {
                if (tMaxX == tMaxY) {
                    x += stepX;
                    tMaxX += tDeltaX;
                }

                y += stepY;
                next = new BlockPos(x, y, z);
                tMaxY += tDeltaY;
            } else {
                next = null;
            }
        } else {
            if (tMaxZ <= tMax) {
                if (tMaxX == tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                }

                if (tMaxY == tMaxZ) {
                    y += stepY;
                    tMaxY += tDeltaY;
                }

                z += stepZ;
                next = new BlockPos(x, y, z);
                tMaxZ += tDeltaZ;
            } else {
                next = null;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public BlockPos next() {
        BlockPos next = this.next;

        if (next == null) {
            throw new NoSuchElementException();
        }

        calculateNext();
        return next;
    }
}
