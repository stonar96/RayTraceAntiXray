package com.vanillage.raytraceantixray.util;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Predicate;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;

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
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private final LongFunction<? extends LevelChunk> chunkGetter;
    private final Predicate<? super BlockState> blockOcclusionPredicate;
    private final BlockIteratorFactory blockIteratorFactory;
    private final boolean unloadedChunkOccluding;
    private final boolean frustumCullingEnabled;

    public BlockOcclusionCulling(LongFunction<? extends LevelChunk> chunkGetter, Predicate<? super BlockState> blockOcclusionPredicate, BlockIteratorFactory blockIteratorFactory, boolean unloadedChunkOccluding, boolean frustumCullingEnabled) {
        this.chunkGetter = chunkGetter;
        this.blockOcclusionPredicate = blockOcclusionPredicate;
        this.blockIteratorFactory = blockIteratorFactory;
        this.unloadedChunkOccluding = unloadedChunkOccluding;
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
        int chunkX = x >> 4;
        int chunkZ = y >> 4;
        return isVisible(x, y, z, centerX, centerY, centerZ, differenceX, differenceY, differenceZ, distanceSquared, directionX, directionY, directionZ, chunkGetter.apply(ChunkPos.asLong(chunkX, chunkZ)), chunkX, chunkZ);
    }

    public boolean isVisible(int x, int y, int z, double centerX, double centerY, double centerZ, double differenceX, double differenceY, double differenceZ, double distanceSquared, double directionX, double directionY, double directionZ, LevelChunk chunk, int chunkX, int chunkZ) {
        if (frustumCullingEnabled && !((differenceX - directionX) * directionX + (differenceY - directionY) * directionY + (differenceZ - directionZ) * directionZ <= 0.)) { // Should actually be (difference - Math.sqrt(3.) * direction / 2.) * direction.
            return false;
        }

        int sectionY = y >> 4;
        LevelChunkSection section = null;

        if (chunk != null) {
            section = chunk.getSections()[sectionY - chunk.getMinSection()];

            if (section != null && section.hasOnlyAir()) { // Sections aren't null anymore. Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                section = null;
            }
        } else if (unloadedChunkOccluding) {
            return false;
        }

        double distance = Math.sqrt(distanceSquared);
        Iterator<int[]> iterator = blockIteratorFactory.getBlockIterator(x, y, z, centerX, centerY, centerZ, differenceX / distance, differenceY / distance, differenceZ / distance, distance);

        while (iterator.hasNext()) {
            int[] ray = iterator.next();
            int rayX = ray[0];
            int rayY = ray[1];
            int rayZ = ray[2];
            int rayChunkX = rayX >> 4;
            int raySectionY = rayY >> 4;
            int rayChunkZ = rayZ >> 4;

            if (rayChunkX != chunkX || rayChunkZ != chunkZ) {
                chunkX = rayChunkX;
                sectionY = raySectionY;
                chunkZ = rayChunkZ;
                chunk = chunkGetter.apply(ChunkPos.asLong(chunkX, chunkZ));

                if (chunk == null) {
                    if (unloadedChunkOccluding) {
                        return false;
                    }

                    section = null;
                    continue;
                }

                if (sectionY < chunk.getMinSection() || sectionY > chunk.getMaxSection() - 1) {
                    section = null;
                    continue;
                }

                section = chunk.getSections()[sectionY - chunk.getMinSection()];

                if (section == null) { // Sections aren't null anymore.
                    continue;
                }

                if (section.hasOnlyAir()) { // Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                    section = null;
                    continue;
                }
            } else if (raySectionY != sectionY) {
                sectionY = raySectionY;

                if (chunk == null) {
                    // section = null;
                    continue;
                }

                if (sectionY < chunk.getMinSection() || sectionY > chunk.getMaxSection() - 1) {
                    section = null;
                    continue;
                }

                section = chunk.getSections()[sectionY - chunk.getMinSection()];

                if (section == null) { // Sections aren't null anymore.
                    continue;
                }

                if (section.hasOnlyAir()) { // Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                    section = null;
                    continue;
                }
            } else if (section == null) {
                continue;
            }

            if (blockOcclusionPredicate.test(getBlockState(section, rayX, rayY, rayZ)) && checkNearbyBlocks(x, y, z, ray, rayX, rayY, rayZ, differenceX, differenceY, differenceZ, chunk, section, chunkX, sectionY, chunkZ)) {
                return false;
            }
        }

        return true;
    }

    private static BlockState getBlockState(LevelChunkSection section, int x, int y, int z) {
        // synchronized (section.getStates()) {
        //     try {
        //         section.getStates().acquire();
                try {
                    return section.getBlockState(x & 15, y & 15, z & 15);
                } catch (MissingPaletteEntryException e) {
                    return AIR;
                }
        //     } finally {
        //         section.getStates().release();
        //     }
        // }
    }

    private boolean checkNearbyBlocks(int x, int y, int z, int[] ray, int rayX, int rayY, int rayZ, double differenceX, double differenceY, double differenceZ, LevelChunk expectedChunk, LevelChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ) {
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
            BlockState blockState = getBlockState(ray[0], ray[1], ray[2], expectedChunk, expectedSection, expectedChunkX, expectedSectionY, expectedChunkZ);

            if (blockState == null) {
                if (unloadedChunkOccluding) {
                    return true;
                }
            } else if (blockOcclusionPredicate.test(blockState)) {
                continue;
            }

            increase.accept(ray);

            if (ray[0] == x && ray[1] == y && ray[2] == z) {
                return false;
            }

            blockState = getBlockState(ray[0], ray[1], ray[2], expectedChunk, expectedSection, expectedChunkX, expectedSectionY, expectedChunkZ);

            if (blockState == null) {
                if (unloadedChunkOccluding) {
                    return true;
                }

                return false;
            }

            if (!blockOcclusionPredicate.test(blockState)) {
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

    private BlockState getBlockState(int x, int y, int z, LevelChunk expectedChunk, LevelChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ) {
        int chunkX = x >> 4;
        int sectionY = y >> 4;
        int chunkZ = z >> 4;

        if (chunkX != expectedChunkX || chunkZ != expectedChunkZ) {
            expectedChunk = chunkGetter.apply(ChunkPos.asLong(chunkX, chunkZ));

            if (expectedChunk == null) {
                return null;
            }

            if (sectionY < expectedChunk.getMinSection() || sectionY >= expectedChunk.getMaxSection()) {
                return AIR;
            }

            expectedSection = expectedChunk.getSections()[sectionY - expectedChunk.getMinSection()];

            if (expectedSection == null || expectedSection.hasOnlyAir()) { // Sections aren't null anymore. Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                return AIR;
            }
        } else if (sectionY != expectedSectionY) {
            if (sectionY < expectedChunk.getMinSection() || sectionY >= expectedChunk.getMaxSection()) {
                return AIR;
            }

            expectedSection = expectedChunk.getSections()[sectionY - expectedChunk.getMinSection()];

            if (expectedSection == null || expectedSection.hasOnlyAir()) { // Sections aren't null anymore. Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                return AIR;
            }
        }

        return getBlockState(expectedSection, x, y, z);
    }

    @FunctionalInterface
    private interface IntArrayConsumer extends Consumer<int[]> {

    }

    @FunctionalInterface
    public interface BlockIteratorFactory {
        Iterator<int[]> getBlockIterator(int x, int y, int z, double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double distance);
    }
}
