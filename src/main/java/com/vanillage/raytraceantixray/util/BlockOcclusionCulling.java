package com.vanillage.raytraceantixray.util;

import java.util.function.Consumer;

public final class BlockOcclusionCulling {
    private static final IntArrayConsumer INCREASE_X = c -> c[0]++;
    private static final IntArrayConsumer DECREASE_X = c -> c[0]--;
    private static final IntArrayConsumer INCREASE_Y = c -> c[1]++;
    private static final IntArrayConsumer DECREASE_Y = c -> c[1]--;
    private static final IntArrayConsumer INCREASE_Z = c -> c[2]++;
    private static final IntArrayConsumer DECREASE_Z = c -> c[2]--;
    private static final IntArrayConsumer[] NEARBY_BLOCKS_X_PLANE_Y_POS_Z_POS = new IntArrayConsumer[] { INCREASE_Y, INCREASE_Z, DECREASE_Y };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_X_PLANE_Y_POS_Z_NEG = new IntArrayConsumer[] { INCREASE_Y, DECREASE_Z, DECREASE_Y };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_X_PLANE_Y_NEG_Z_POS = new IntArrayConsumer[] { DECREASE_Y, INCREASE_Z, INCREASE_Y };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_X_PLANE_Y_NEG_Z_NEG = new IntArrayConsumer[] { DECREASE_Y, DECREASE_Z, INCREASE_Y };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Y_PLANE_Z_POS_X_POS = new IntArrayConsumer[] { INCREASE_Z, INCREASE_X, DECREASE_Z };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Y_PLANE_Z_POS_X_NEG = new IntArrayConsumer[] { INCREASE_Z, DECREASE_X, DECREASE_Z };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Y_PLANE_Z_NEG_X_POS = new IntArrayConsumer[] { DECREASE_Z, INCREASE_X, INCREASE_Z };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Y_PLANE_Z_NEG_X_NEG = new IntArrayConsumer[] { DECREASE_Z, DECREASE_X, INCREASE_Z };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_POS_Y_POS = new IntArrayConsumer[] { INCREASE_Y, INCREASE_X, DECREASE_Y /* INCREASE_X, INCREASE_Y, DECREASE_X */ };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_POS_Y_NEG = new IntArrayConsumer[] { DECREASE_Y, INCREASE_X, INCREASE_Y /* INCREASE_X, DECREASE_Y, DECREASE_X */ };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_POS = new IntArrayConsumer[] { INCREASE_Y, DECREASE_X, DECREASE_Y /* DECREASE_X, INCREASE_Y, INCREASE_X */ };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_NEG = new IntArrayConsumer[] { DECREASE_Y, DECREASE_X, INCREASE_Y /* DECREASE_X, DECREASE_Y, INCREASE_X */ };
    private final BlockIteratorFactory blockIteratorFactory;
    private final BlockOcclusionGetter blockOcclusionGetter;
    private final boolean frustumCullingEnabled;

    public BlockOcclusionCulling(BlockIteratorFactory blockIteratorFactory, BlockOcclusionGetter blockOcclusionGetter, boolean frustumCullingEnabled) {
        this.blockIteratorFactory = blockIteratorFactory;
        this.blockOcclusionGetter = blockOcclusionGetter;
        this.frustumCullingEnabled = frustumCullingEnabled;
    }

    public boolean isVisible(int x, int y, int z, double vectorX, double vectorY, double vectorZ, double directionX, double directionY, double directionZ) {
        double centerX = x + 0.5;
        double centerY = y + 0.5;
        double centerZ = z + 0.5;
        double differenceX = vectorX - centerX;
        double differenceY = vectorY - centerY;
        double differenceZ = vectorZ - centerZ;
        double distanceSquared = differenceX * differenceX + differenceY * differenceY + differenceZ * differenceZ;
        return isVisible(x, y, z, centerX, centerY, centerZ, differenceX, differenceY, differenceZ, distanceSquared, directionX, directionY, directionZ);
    }

    public boolean isVisible(int x, int y, int z, double centerX, double centerY, double centerZ, double differenceX, double differenceY, double differenceZ, double distanceSquared, double directionX, double directionY, double directionZ) {
        if (frustumCullingEnabled && (differenceX - directionX) * directionX + (differenceY - directionY) * directionY + (differenceZ - directionZ) * directionZ > 0.) { // Should actually be (difference - Math.sqrt(3.) * direction / 2.) * direction.
            return false;
        }

        double distance = Math.sqrt(distanceSquared);
        double fixedDistance = distance == 0. ? Double.NaN : distance;
        BlockIterator blockIterator = blockIteratorFactory.getBlockIterator(x, y, z, centerX, centerY, centerZ, differenceX / fixedDistance, differenceY / fixedDistance, differenceZ / fixedDistance, distance);
        int[] ray;

        while ((ray = blockIterator.calculateNext()) != null) {
            int rayX = ray[0];
            int rayY = ray[1];
            int rayZ = ray[2];

            if (blockOcclusionGetter.isOccludingRay(rayX, rayY, rayZ) && checkNearbyBlocks(x, y, z, ray, rayX, rayY, rayZ, differenceX, differenceY, differenceZ)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkNearbyBlocks(int x, int y, int z, int[] ray, int rayX, int rayY, int rayZ, double differenceX, double differenceY, double differenceZ) {
        IntArrayConsumer[] nearbyBlocks;
        IntArrayConsumer increase;
        IntArrayConsumer decrease;
        double absDifferenceX = Math.abs(differenceX);
        double absDifferenceY = Math.abs(differenceY);
        double absDifferenceZ = Math.abs(differenceZ);
        double rayDifferenceX = rayX - x;
        double rayDifferenceY = rayY - y;
        double rayDifferenceZ = rayZ - z;

        if (absDifferenceX > absDifferenceY) {
            if (absDifferenceZ > absDifferenceX) {
                double factor = divide(differenceZ, rayDifferenceZ);
                rayDifferenceX = multiply(factor, rayDifferenceX) - differenceX;
                rayDifferenceY = multiply(factor, rayDifferenceY) - differenceY;

                if (rayDifferenceX > 0.) {
                    if (rayDifferenceY > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_POS;
                    }
                } else {
                    if (rayDifferenceY > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_POS;
                    }
                }

                if (differenceZ > 0.) {
                    increase = DECREASE_Z;
                    decrease = INCREASE_Z;
                } else {
                    increase = INCREASE_Z;
                    decrease = DECREASE_Z;
                }
            } else {
                double factor = divide(differenceX, rayDifferenceX);
                rayDifferenceY = multiply(factor, rayDifferenceY) - differenceY;
                rayDifferenceZ = multiply(factor, rayDifferenceZ) - differenceZ;

                if (rayDifferenceY > 0.) {
                    if (rayDifferenceZ > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_NEG_Z_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_NEG_Z_POS;
                    }
                } else {
                    if (rayDifferenceZ > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_POS_Z_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_POS_Z_POS;
                    }
                }

                if (differenceX > 0.) {
                    increase = DECREASE_X;
                    decrease = INCREASE_X;
                } else {
                    increase = INCREASE_X;
                    decrease = DECREASE_X;
                }
            }
        } else if (absDifferenceY > absDifferenceZ) {
            double factor = divide(differenceY, rayDifferenceY);
            rayDifferenceZ = multiply(factor, rayDifferenceZ) - differenceZ;
            rayDifferenceX = multiply(factor, rayDifferenceX) - differenceX;

            if (rayDifferenceZ > 0.) {
                if (rayDifferenceX > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_NEG_X_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_NEG_X_POS;
                }
            } else {
                if (rayDifferenceX > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_POS_X_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_POS_X_POS;
                }
            }

            if (differenceY > 0.) {
                increase = DECREASE_Y;
                decrease = INCREASE_Y;
            } else {
                increase = INCREASE_Y;
                decrease = DECREASE_Y;
            }
        } else {
            double factor = divide(differenceZ, rayDifferenceZ);
            rayDifferenceX = multiply(factor, rayDifferenceX) - differenceX;
            rayDifferenceY = multiply(factor, rayDifferenceY) - differenceY;

            if (rayDifferenceX > 0.) {
                if (rayDifferenceY > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_POS;
                }
            } else {
                if (rayDifferenceY > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_POS;
                }
            }

            if (differenceZ > 0.) {
                increase = DECREASE_Z;
                decrease = INCREASE_Z;
            } else {
                increase = INCREASE_Z;
                decrease = DECREASE_Z;
            }
        }

        for (int step = 0; step < nearbyBlocks.length; step++) {
            nearbyBlocks[step].accept(ray);

            if (blockOcclusionGetter.isOccludingNearby(ray[0], ray[1], ray[2])) {
                continue;
            }

            increase.accept(ray);
            rayX = ray[0];
            rayY = ray[1];
            rayZ = ray[2];

            if (rayX == x && rayY == y && rayZ == z || !blockOcclusionGetter.isOccludingNearby(rayX, rayY, rayZ)) {
                return false;
            }

            decrease.accept(ray);
        }

        return true;
    }

    private static double divide(double dividend, double divisor) {
        return (divisor == 0. && !Double.isNaN(dividend) ? Math.copySign(1., dividend) : dividend) / divisor;
    }

    private static double multiply(double factor1, double factor2) {
        return (factor2 == 0. ? Math.signum(factor1) : factor1) * factor2;
    }

    @FunctionalInterface
    private interface IntArrayConsumer extends Consumer<int[]> {

    }

    @FunctionalInterface
    public interface BlockIteratorFactory {
        BlockIterator getBlockIterator(int x, int y, int z, double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance);
    }

    @FunctionalInterface
    public interface BlockOcclusionGetter {
        boolean isOccluding(int x, int y, int z);

        default boolean isOccludingRay(int x, int y, int z) {
            return isOccluding(x, y, z);
        }

        default boolean isOccludingNearby(int x, int y, int z) {
            return isOccluding(x, y, z);
        }
    }
}
