package com.vanillage.raytraceantixray.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

// Amanatides, J., & Woo, A. A Fast Voxel Traversal Algorithm for Ray Tracing. http://www.cse.yorku.ca/~amana/research/grid.pdf.
public final class BlockIterator implements Iterator<int[]> {
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
    private int[] ref = new int[3]; // This implementation always returns ref or refSwap to avoid garbage. Can easily be changed if needed.
    private int[] refSwap = new int[3];
    private int[] next;

    public BlockIterator(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        initialize(startX, startY, startZ, endX, endY, endZ);
    }

    public BlockIterator(int x, int y, int z, double startX, double startY, double startZ, double endX, double endY, double endZ) {
        initialize(x, y, z, startX, startY, startZ, endX, endY, endZ);
    }

    public BlockIterator(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance) {
        initialize(startX, startY, startZ, directionX, directionY, directionZ, distance);
    }

    public BlockIterator(int x, int y, int z, double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance) {
        initialize(x, y, z, startX, startY, startZ, directionX, directionY, directionZ, distance);
    }

    public BlockIterator(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance, boolean normalized) {
        if (normalized) {
            initializeNormalized(startX, startY, startZ, directionX, directionY, directionZ, distance);
        } else {
            initialize(startX, startY, startZ, directionX, directionY, directionZ, distance);
        }
    }

    public BlockIterator(int x, int y, int z, double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance, boolean normalized) {
        if (normalized) {
            initializeNormalized(x, y, z, startX, startY, startZ, directionX, directionY, directionZ, distance);
        } else {
            initialize(x, y, z, startX, startY, startZ, directionX, directionY, directionZ, distance);
        }
    }

    public BlockIterator initialize(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        return initialize(floor(startX), floor(startY), floor(startZ), startX, startY, startZ, endX, endY, endZ);
    }

    public BlockIterator initialize(int x, int y, int z, double startX, double startY, double startZ, double endX, double endY, double endZ) {
        double directionX = endX - startX;
        double directionY = endY - startY;
        double directionZ = endZ - startZ;
        double distance = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        double fixedDistance = distance == 0. ? Double.NaN : distance;
        directionX /= fixedDistance;
        directionY /= fixedDistance;
        directionZ /= fixedDistance;
        return initializeNormalized(x, y, z, startX, startY, startZ, directionX, directionY, directionZ, distance);
    }

    public BlockIterator initialize(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance) {
        return initialize(floor(startX), floor(startY), floor(startZ), startX, startY, startZ, directionX, directionY, directionZ, distance);
    }

    public BlockIterator initialize(int x, int y, int z, double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance) {
        double signum = Math.signum(distance);
        directionX *= signum;
        directionY *= signum;
        directionZ *= signum;
        double length = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);

        if (length == 0.) {
            length = Double.NaN;
        }

        directionX /= length;
        directionY /= length;
        directionZ /= length;
        return initializeNormalized(x, y, z, startX, startY, startZ, directionX, directionY, directionZ, Math.abs(distance));
    }

    public BlockIterator initializeNormalized(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance) {
        return initializeNormalized(floor(startX), floor(startY), floor(startZ), startX, startY, startZ, directionX, directionY, directionZ, Math.abs(distance));
    }

    public BlockIterator initializeNormalized(int x, int y, int z, double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance) {
        this.x = x;
        this.y = y;
        this.z = z;
        stepX = directionX < 0. ? -1 : 1;
        stepY = directionY < 0. ? -1 : 1;
        stepZ = directionZ < 0. ? -1 : 1;
        tMax = distance;
        tMaxX = directionX == 0. ? Double.POSITIVE_INFINITY : (x + (stepX + 1) / 2 - startX) / directionX;
        tMaxY = directionY == 0. ? Double.POSITIVE_INFINITY : (y + (stepY + 1) / 2 - startY) / directionY;
        tMaxZ = directionZ == 0. ? Double.POSITIVE_INFINITY : (z + (stepZ + 1) / 2 - startZ) / directionZ;
        tDeltaX = 1. / Math.abs(directionX);
        tDeltaY = 1. / Math.abs(directionY);
        tDeltaZ = 1. / Math.abs(directionZ);
        next = ref;
        ref[0] = x;
        ref[1] = y;
        ref[2] = z;
        return this;
    }

    public int[] calculateNext() {
        if (tMaxX < tMaxY) {
            if (tMaxZ < tMaxX) {
                if (tMaxZ <= tMax) {
                    z += stepZ;
                    // next = new int[] { x, y, z };
                    ref[0] = x;
                    ref[1] = y;
                    ref[2] = z;
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
                    // next = new int[] { x, y, z };
                    ref[0] = x;
                    ref[1] = y;
                    ref[2] = z;
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
                // next = new int[] { x, y, z };
                ref[0] = x;
                ref[1] = y;
                ref[2] = z;
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
                // next = new int[] { x, y, z };
                ref[0] = x;
                ref[1] = y;
                ref[2] = z;
                tMaxZ += tDeltaZ;
            } else {
                next = null;
            }
        }

        return next;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public int[] next() {
        int[] next = this.next;

        if (next == null) {
            throw new NoSuchElementException();
        }

        int[] temp = ref;
        ref = refSwap;
        refSwap = temp;
        this.next = ref;
        calculateNext();
        return next;
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }
}
