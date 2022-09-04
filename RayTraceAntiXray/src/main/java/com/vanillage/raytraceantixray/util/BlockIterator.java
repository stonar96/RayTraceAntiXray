package com.vanillage.raytraceantixray.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.util.Vector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;

// Amanatides, J., & Woo, A. A Fast Voxel Traversal Algorithm for Ray Tracing. http://www.cse.yorku.ca/~amana/research/grid.pdf.
public final class BlockIterator implements Iterator<BlockPos> {
    private int x;
    private int y;
    private int z;
    private int stepX;
    private int stepY;
    private int stepZ;
    private double tMax;
    private double tMaxX;
    private double tMaxY;
    private double tMaxZ;
    private double tDeltaX;
    private double tDeltaY;
    private double tDeltaZ;
    private final MutableBlockPos ref; // This implementation always returns ref to avoid garbage. Can easily be changed if needed.
    private BlockPos next;

    public BlockIterator(Vector start, Vector end) {
        ref = new MutableBlockPos();
        initialize(start, end);
    }

    public BlockIterator(Vector start, Vector direction, double distance) {
        ref = new MutableBlockPos();
        initialize(start, direction, distance);
    }

    public BlockIterator initialize(Vector start, Vector end) {
        // Vector direction = end.clone().subtract(start);
        // double distance = direction.length();
        // direction.normalize();
        // direction.setX(direction.getX() / distance);
        // direction.setY(direction.getY() / distance);
        // direction.setZ(direction.getZ() / distance);
        double directionX = end.getX() - start.getX();
        double directionY = end.getY() - start.getY();
        double directionZ = end.getZ() - start.getZ();
        double distance = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        directionX /= distance;
        directionY /= distance;
        directionZ /= distance;
        return initialize(start, directionX, directionY, directionZ, distance);
    }

    public BlockIterator initialize(Vector start, Vector direction, double distance) {
        // direction = direction.clone().multiply(Math.signum(distance)).normalize();
        // tMax = Math.abs(distance);
        return initialize(start, direction.getX(), direction.getY(), direction.getZ(), distance);
    }

    private BlockIterator initialize(Vector start, double directionX, double directionY, double directionZ, double distance) {
        x = start.getBlockX();
        y = start.getBlockY();
        z = start.getBlockZ();
        tMax = distance;
        stepX = directionX < 0. ? -1 : 1;
        stepY = directionY < 0. ? -1 : 1;
        stepZ = directionZ < 0. ? -1 : 1;
        tMaxX = directionX == 0. ? Double.POSITIVE_INFINITY : (x + (stepX + 1) / 2 - start.getX()) / directionX;
        tMaxY = directionY == 0. ? Double.POSITIVE_INFINITY : (y + (stepY + 1) / 2 - start.getY()) / directionY;
        tMaxZ = directionZ == 0. ? Double.POSITIVE_INFINITY : (z + (stepZ + 1) / 2 - start.getZ()) / directionZ;
        tDeltaX = 1. / Math.abs(directionX);
        tDeltaY = 1. / Math.abs(directionY);
        tDeltaZ = 1. / Math.abs(directionZ);
        next = ref;
        // ref.set(x, y, z);
        calculateNext(); // This implementation doesn't include the start block. Use comment above if needed.
        return this;
    }

    private void calculateNext() {
        if (tMaxX < tMaxY) {
            if (tMaxZ < tMaxX) {
                if (tMaxZ <= tMax) {
                    z += stepZ;
                    // next = new BlockPos(x, y, z);
                    ref.set(x, y, z);
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
                    // next = new BlockPos(x, y, z);
                    ref.set(x, y, z);
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
                // next = new BlockPos(x, y, z);
                ref.set(x, y, z);
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
                // next = new BlockPos(x, y, z);
                ref.set(x, y, z);
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
