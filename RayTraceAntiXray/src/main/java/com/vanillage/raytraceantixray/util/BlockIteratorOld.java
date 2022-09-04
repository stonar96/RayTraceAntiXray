package com.vanillage.raytraceantixray.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.util.Vector;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class BlockIteratorOld implements Iterator<BlockPos> {
    private final Vector start;
    private final Vector direction;
    private final double distanceSquared;
    private final int xDirection;
    private final int yDirection;
    private final int zDirection;
    private int xFace;
    private int yFace;
    private int zFace;
    private double xFaceDy;
    private double xFaceDz;
    private double yFaceDz;
    private double yFaceDx;
    private double zFaceDx;
    private double zFaceDy;
    private double xFaceDistanceSquared;
    private double yFaceDistanceSquared;
    private double zFaceDistanceSquared;
    private BlockPos next;

    public BlockIteratorOld(Vector start, Vector end) {
        this.start = start.clone();
        Vector direction = end.clone().subtract(start);
        distanceSquared = direction.lengthSquared();
        this.direction = direction.normalize();
        xDirection = (int) Math.copySign(1., direction.getX());
        yDirection = (int) Math.copySign(1., direction.getY());
        zDirection = (int) Math.copySign(1., direction.getZ());
        setup();
    }

    public BlockIteratorOld(Vector start, Vector direction, double distance) {
        this.start = start.clone();
        this.direction = direction.clone().multiply(Math.signum(distance)).normalize();
        distanceSquared = distance * distance;
        xDirection = (int) Math.copySign(1., direction.getX());
        yDirection = (int) Math.copySign(1., direction.getY());
        zDirection = (int) Math.copySign(1., direction.getZ());
        setup();
    }

    private void setup() {
        xFace = start.getBlockX() + (xDirection + 1) / 2;
        yFace = start.getBlockY() + (yDirection + 1) / 2;
        zFace = start.getBlockZ() + (zDirection + 1) / 2;
        intersectX();
        intersectY();
        intersectZ();
        calculateNext();
    }

    private void intersectX() {
        double dx = xFace - start.getX();
        double factor = (direction.getX() == 0. && !Double.isNaN(dx) ? Math.copySign(1., dx) : dx) / direction.getX();
        xFaceDy = (direction.getY() == 0. ? Math.signum(factor) : factor) * direction.getY();
        xFaceDz = (direction.getZ() == 0. ? Math.signum(factor) : factor) * direction.getZ();
        xFaceDistanceSquared = dx * dx + xFaceDy * xFaceDy + xFaceDz * xFaceDz;
    }

    private void intersectY() {
        double dy = yFace - start.getY();
        double factor = (direction.getY() == 0. && !Double.isNaN(dy) ? Math.copySign(1., dy) : dy) / direction.getY();
        yFaceDz = (direction.getZ() == 0. ? Math.signum(factor) : factor) * direction.getZ();
        yFaceDx = (direction.getX() == 0. ? Math.signum(factor) : factor) * direction.getX();
        yFaceDistanceSquared = yFaceDx * yFaceDx + dy * dy + yFaceDz * yFaceDz;
    }

    private void intersectZ() {
        double dz = zFace - start.getZ();
        double factor = (direction.getZ() == 0. && !Double.isNaN(dz) ? Math.copySign(1., dz) : dz) / direction.getZ();
        zFaceDx = (direction.getX() == 0. ? Math.signum(factor) : factor) * direction.getX();
        zFaceDy = (direction.getY() == 0. ? Math.signum(factor) : factor) * direction.getY();
        zFaceDistanceSquared = zFaceDx * zFaceDx + zFaceDy * zFaceDy + dz * dz;
    }

    private void calculateNext() {
        if (xFaceDistanceSquared < yFaceDistanceSquared) {
            if (zFaceDistanceSquared < xFaceDistanceSquared) {
                if (zFaceDistanceSquared <= distanceSquared) {
                    next = new BlockPos(Mth.floor(zFaceDx + start.getX()), Mth.floor(zFaceDy + start.getY()), zFace + (zDirection - 1) / 2);
                    zFace += zDirection;
                    intersectZ();
                } else {
                    next = null;
                }
            } else {
                if (xFaceDistanceSquared <= distanceSquared) {
                    next = new BlockPos(xFace + (xDirection - 1) / 2, Mth.floor(xFaceDy + start.getY()), Mth.floor(xFaceDz + start.getZ()));

                    if (zFaceDistanceSquared == xFaceDistanceSquared) {
                        zFace += zDirection;
                        intersectZ();
                    }

                    xFace += xDirection;
                    intersectX();
                } else {
                    next = null;
                }
            }
        } else if (yFaceDistanceSquared < zFaceDistanceSquared) {
            if (yFaceDistanceSquared <= distanceSquared) {
                next = new BlockPos(Mth.floor(yFaceDx + start.getX()), yFace + (yDirection - 1) / 2, Mth.floor(yFaceDz + start.getZ()));

                if (xFaceDistanceSquared == yFaceDistanceSquared) {
                    xFace += xDirection;
                    intersectX();
                }

                yFace += yDirection;
                intersectY();
            } else {
                next = null;
            }
        } else {
            if (zFaceDistanceSquared <= distanceSquared) {
                next = new BlockPos(Mth.floor(zFaceDx + start.getX()), Mth.floor(zFaceDy + start.getY()), zFace + (zDirection - 1) / 2);

                if (xFaceDistanceSquared == zFaceDistanceSquared) {
                    xFace += xDirection;
                    intersectX();
                }

                if (yFaceDistanceSquared == zFaceDistanceSquared) {
                    yFace += yDirection;
                    intersectY();
                }

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
    public BlockPos next() {
        BlockPos next = this.next;

        if (next == null) {
            throw new NoSuchElementException();
        }

        calculateNext();
        return next;
    }
}
