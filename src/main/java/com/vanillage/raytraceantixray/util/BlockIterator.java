package com.vanillage.raytraceantixray.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.util.Vector;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.MathHelper;

public final class BlockIterator implements Iterator<BlockPosition> {
    private final Vector start;
    private final Vector direction;
    private final double distanceSquared;
    private final int xDirection;
    private final int yDirection;
    private final int zDirection;
    private int xFace;
    private int yFace;
    private int zFace;
    private double xFaceY;
    private double xFaceZ;
    private double yFaceZ;
    private double yFaceX;
    private double zFaceX;
    private double zFaceY;
    private double xFaceDistanceSquared;
    private double yFaceDistanceSquared;
    private double zFaceDistanceSquared;
    private BlockPosition next;

    public BlockIterator(Vector start, Vector end) {
        this.start = start.clone();
        Vector direction = end.clone().subtract(start);
        distanceSquared = direction.lengthSquared();
        this.direction = direction.normalize();
        xDirection = direction.getX() < 0. ? -1 : 1;
        yDirection = direction.getY() < 0. ? -1 : 1;
        zDirection = direction.getZ() < 0. ? -1 : 1;
        setup();
    }

    public BlockIterator(Vector start, Vector direction, double distance) {
        this.start = start.clone();
        this.direction = direction.clone().normalize();
        distanceSquared = distance * distance;
        xDirection = direction.getX() < 0. ? -1 : 1;
        yDirection = direction.getY() < 0. ? -1 : 1;
        zDirection = direction.getZ() < 0. ? -1 : 1;
        setup();
    }

    private void setup() {
        xFace = start.getBlockX() + ((xDirection + 1) / 2);
        yFace = start.getBlockY() + ((yDirection + 1) / 2);
        zFace = start.getBlockZ() + ((zDirection + 1) / 2);
        intersectX();
        intersectY();
        intersectZ();
        calculateNext();
    }

    private void intersectX() {
        double dx = xFace - start.getX();
        double factor = dx / direction.getX();
        double dy = factor * direction.getY();
        double dz = factor * direction.getZ();
        xFaceY = dy + start.getY();
        xFaceZ = dz + start.getZ();
        xFaceDistanceSquared = dx * dx + dy * dy + dz * dz;
    }

    private void intersectY() {
        double dy = yFace - start.getY();
        double factor = dy / direction.getY();
        double dz = factor * direction.getZ();
        double dx = factor * direction.getX();
        yFaceZ = dz + start.getZ();
        yFaceX = dx + start.getX();
        yFaceDistanceSquared = dx * dx + dy * dy + dz * dz;
    }

    private void intersectZ() {
        double dz = zFace - start.getZ();
        double factor = dz / direction.getZ();
        double dx = factor * direction.getX();
        double dy = factor * direction.getY();
        zFaceX = dx + start.getX();
        zFaceY = dy + start.getY();
        zFaceDistanceSquared = dx * dx + dy * dy + dz * dz;
    }

    private void calculateNext() {
        if (xFaceDistanceSquared < yFaceDistanceSquared) {
            if (zFaceDistanceSquared < xFaceDistanceSquared) {
                if (zFaceDistanceSquared <= distanceSquared) {
                    next = new BlockPosition(MathHelper.floor(zFaceX), MathHelper.floor(zFaceY), zFace + (zDirection - 1) / 2);
                    zFace += zDirection;
                    intersectZ();
                } else {
                    next = null;
                }
            } else {
                if (xFaceDistanceSquared <= distanceSquared) {
                    next = new BlockPosition(xFace + (xDirection - 1) / 2, MathHelper.floor(xFaceY), MathHelper.floor(xFaceZ));
                    xFace += xDirection;
                    intersectX();
                } else {
                    next = null;
                }
            }
        } else if (yFaceDistanceSquared < zFaceDistanceSquared) {
            if (yFaceDistanceSquared <= distanceSquared) {
                next = new BlockPosition(MathHelper.floor(yFaceX), yFace + (yDirection - 1) / 2, MathHelper.floor(yFaceZ));
                yFace += yDirection;
                intersectY();
            } else {
                next = null;
            }
        } else {
            if (zFaceDistanceSquared <= distanceSquared) {
                next = new BlockPosition(MathHelper.floor(zFaceX), MathHelper.floor(zFaceY), zFace + (zDirection - 1) / 2);
                zFace += zDirection;
                intersectZ();
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
    public BlockPosition next() {
        BlockPosition next = this.next;

        if (next == null) {
            throw new NoSuchElementException();
        }

        calculateNext();
        return next;
    }
}
