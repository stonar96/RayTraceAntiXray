package com.vanillage.raytraceantixray.tasks;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.antixray.ChunkPacketBlockController;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.util.BlockIterator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;

public final class RayTraceCallable implements Callable<Void> {
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
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_POS_Y_POS = new IntArrayConsumer[] { INCREASE_X, INCREASE_Y, DECREASE_X };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_POS_Y_NEG = new IntArrayConsumer[] { INCREASE_X, DECREASE_Y, DECREASE_X };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_POS = new IntArrayConsumer[] { DECREASE_X, INCREASE_Y, INCREASE_X };
    private static final IntArrayConsumer[] NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_NEG = new IntArrayConsumer[] { DECREASE_X, DECREASE_Y, INCREASE_X };
    private final int[] ref = new int[3];
    private final PlayerData playerData;
    private final BlockIterator blockIterator = new BlockIterator(new Vector(), new Vector());

    public RayTraceCallable(PlayerData playerData) {
        this.playerData = playerData;
    }

    @Override
    public Void call() {
        try {
            rayTrace();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

        return null;
    }

    private void rayTrace() {
        List<? extends Location> locations = playerData.getLocations();
        Location playerLocation = locations.get(0);
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) playerLocation.getWorld()).getHandle().chunkPacketBlockController;

        if (!(chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray)) {
            return;
        }

        ChunkPacketBlockControllerAntiXray chunkPacketBlockControllerAntiXray = (ChunkPacketBlockControllerAntiXray) chunkPacketBlockController;
        boolean[] solidGlobal = chunkPacketBlockControllerAntiXray.solidGlobal;
        double rayTraceDistance = chunkPacketBlockControllerAntiXray.rayTraceDistance;
        Vector playerVector = playerLocation.toVector();
        playerVector.setX(playerLocation.getX() - rayTraceDistance);
        playerVector.setZ(playerLocation.getZ() - rayTraceDistance);
        int chunkXMin = playerVector.getBlockX() >> 4;
        int chunkZMin = playerVector.getBlockZ() >> 4;
        playerVector.setX(playerLocation.getX() + rayTraceDistance);
        playerVector.setZ(playerLocation.getZ() + rayTraceDistance);
        int chunkXMax = playerVector.getBlockX() >> 4;
        int chunkZMax = playerVector.getBlockZ() >> 4;
        playerVector.setX(playerLocation.getX());
        playerVector.setZ(playerLocation.getZ());
        double rayTraceDistanceSquared = rayTraceDistance * rayTraceDistance;

        for (Entry<ChunkPos, ChunkBlocks> chunkEntry : playerData.getChunks().entrySet()) {
            ChunkBlocks chunkBlocks = chunkEntry.getValue();
            LevelChunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                playerData.getChunks().remove(chunkEntry.getKey(), chunkBlocks);
                continue;
            }

            if (chunk.getPos().x < chunkXMin || chunk.getPos().x > chunkXMax || chunk.getPos().z < chunkZMin || chunk.getPos().z > chunkZMax) {
                continue;
            }

            Iterator<? extends BlockPos> iterator = chunkBlocks.getBlocks().iterator();

            while (iterator.hasNext()) {
                BlockPos block = iterator.next();
                Vector blockCenter = new Vector(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);

                if (!(playerVector.distanceSquared(blockCenter) <= rayTraceDistanceSquared)) {
                    continue;
                }

                for (Location location : locations) {
                    Vector vector = location.toVector();
                    Vector difference = vector.clone().subtract(blockCenter);

                    // Actually, we should check all 8 block corners here instead of the block center.
                    if (!(difference.dot(location.getDirection()) <= 0.)) {
                        continue;
                    }

                    Iterator<BlockPos> blockIterator = this.blockIterator.initialize(blockCenter, vector);
                    boolean update = true;

                    while (blockIterator.hasNext()) {
                        BlockPos rayBlock = blockIterator.next();
                        ChunkPos chunkPos = new ChunkPos(rayBlock);
                        ChunkBlocks rayChunkBlocks = playerData.getChunks().get(chunkPos);

                        if (rayChunkBlocks == null) {
                            update = false;
                            break;
                        }

                        LevelChunk rayChunk = rayChunkBlocks.getChunk();

                        if (rayChunk == null) {
                            playerData.getChunks().remove(chunkPos, rayChunkBlocks);
                            update = false;
                            break;
                        }

                        int sectionY = rayBlock.getY() >> 4;

                        if (sectionY < rayChunk.getMinSection() || sectionY > rayChunk.getMaxSection() - 1) {
                            continue;
                        }

                        LevelChunkSection section = rayChunk.getSections()[sectionY - rayChunk.getMinSection()];

                        if (section == null || section.hasOnlyAir()) { // Sections aren't null anymore.
                            continue;
                        }

                        BlockState blockState;

                        // synchronized (section.getStates()) {
                        //     try {
                        //         section.getStates().acquire();
                                try {
                                    blockState = section.getBlockState(rayBlock.getX() & 15, rayBlock.getY() & 15, rayBlock.getZ() & 15);
                                } catch (MissingPaletteEntryException e) {
                                    blockState = Blocks.AIR.defaultBlockState();
                                }
                        //     } finally {
                        //         section.getStates().release();
                        //     }
                        // }

                        if (solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(blockState)] && checkNearbyBlocks(block.getX(), block.getY(), block.getZ(), rayBlock.getX(), rayBlock.getY(), rayBlock.getZ(), difference, section, chunkPos.x, sectionY, chunkPos.z, playerData, solidGlobal)) {
                            update = false;
                            break;
                        }
                    }

                    if (update) {
                        playerData.getResult().add(block);
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    private boolean checkNearbyBlocks(int blockX, int blockY, int blockZ, int rayBlockX, int rayBlockY, int rayBlockZ, Vector difference, LevelChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ, PlayerData playerData, boolean[] solidGlobal) {
        IntArrayConsumer[] nearbyBlocks;
        IntArrayConsumer increase;
        IntArrayConsumer decrease;
        double absDifferenceX = Math.abs(difference.getX());
        double absDifferenceY = Math.abs(difference.getY());
        double absDifferenceZ = Math.abs(difference.getZ());
        Vector rayBlockDifference = new Vector(rayBlockX - blockX, rayBlockY - blockY, rayBlockZ - blockZ);

        if (absDifferenceX > absDifferenceY) {
            if (absDifferenceZ > absDifferenceX) {
                intersectZ(rayBlockDifference, difference.getZ()).subtract(difference);

                if (rayBlockDifference.getX() > 0.) {
                    if (rayBlockDifference.getY() > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_POS;
                    }
                } else {
                    if (rayBlockDifference.getY() > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_POS;
                    }
                }

                if (difference.getZ() > 0.) {
                    increase = DECREASE_Z;
                    decrease = INCREASE_Z;
                } else {
                    increase = INCREASE_Z;
                    decrease = DECREASE_Z;
                }
            } else {
                intersectX(rayBlockDifference, difference.getX()).subtract(difference);

                if (rayBlockDifference.getY() > 0.) {
                    if (rayBlockDifference.getZ() > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_NEG_Z_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_NEG_Z_POS;
                    }
                } else {
                    if (rayBlockDifference.getZ() > 0.) {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_POS_Z_NEG;
                    } else {
                        nearbyBlocks = NEARBY_BLOCKS_X_PLANE_Y_POS_Z_POS;
                    }
                }

                if (difference.getX() > 0.) {
                    increase = DECREASE_X;
                    decrease = INCREASE_X;
                } else {
                    increase = INCREASE_X;
                    decrease = DECREASE_X;
                }
            }
        } else if (absDifferenceY > absDifferenceZ) {
            intersectY(rayBlockDifference, difference.getY()).subtract(difference);

            if (rayBlockDifference.getZ() > 0.) {
                if (rayBlockDifference.getX() > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_NEG_X_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_NEG_X_POS;
                }
            } else {
                if (rayBlockDifference.getX() > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_POS_X_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Y_PLANE_Z_POS_X_POS;
                }
            }

            if (difference.getY() > 0.) {
                increase = DECREASE_Y;
                decrease = INCREASE_Y;
            } else {
                increase = INCREASE_Y;
                decrease = DECREASE_Y;
            }
        } else {
            intersectZ(rayBlockDifference, difference.getZ()).subtract(difference);

            if (rayBlockDifference.getX() > 0.) {
                if (rayBlockDifference.getY() > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_NEG_Y_POS;
                }
            } else {
                if (rayBlockDifference.getY() > 0.) {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_NEG;
                } else {
                    nearbyBlocks = NEARBY_BLOCKS_Z_PLANE_X_POS_Y_POS;
                }
            }

            if (difference.getZ() > 0.) {
                increase = DECREASE_Z;
                decrease = INCREASE_Z;
            } else {
                increase = INCREASE_Z;
                decrease = DECREASE_Z;
            }
        }

        // int[] ref = { rayBlockX, rayBlockY, rayBlockZ };
        ref[0] = rayBlockX;
        ref[1] = rayBlockY;
        ref[2] = rayBlockZ;

        for (int step = 0; step < nearbyBlocks.length; step++) {
            nearbyBlocks[step].accept(ref);
            BlockState blockState = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionY, expectedChunkZ, playerData);

            if (blockState == null) {
                return true;
            }

            if (solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(blockState)]) {
                continue;
            }

            increase.accept(ref);

            if (ref[0] == blockX && ref[1] == blockY && ref[2] == blockZ) {
                return false;
            }

            blockState = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionY, expectedChunkZ, playerData);

            if (blockState == null) {
                return true;
            }

            if (!solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(blockState)]) {
                return false;
            }

            decrease.accept(ref);
        }

        return true;
    }

    private static Vector intersectX(Vector vector, double x) {
        return intersectX(vector, x, vector);
    }

    private static Vector intersectX(Vector vector, double x, Vector result) {
        double factor = (vector.getX() == 0. && !Double.isNaN(x) ? Math.copySign(1., x) : x) / vector.getX();
        result.setY((vector.getY() == 0. ? Math.signum(factor) : factor) * vector.getY());
        result.setZ((vector.getZ() == 0. ? Math.signum(factor) : factor) * vector.getZ());
        result.setX(x);
        return result;
    }

    private static Vector intersectY(Vector vector, double y) {
        return intersectY(vector, y, vector);
    }

    private static Vector intersectY(Vector vector, double y, Vector result) {
        double factor = (vector.getY() == 0. && !Double.isNaN(y) ? Math.copySign(1., y) : y) / vector.getY();
        result.setZ((vector.getZ() == 0. ? Math.signum(factor) : factor) * vector.getZ());
        result.setX((vector.getX() == 0. ? Math.signum(factor) : factor) * vector.getX());
        result.setY(y);
        return result;
    }

    private static Vector intersectZ(Vector vector, double z) {
        return intersectZ(vector, z, vector);
    }

    private static Vector intersectZ(Vector vector, double z, Vector result) {
        double factor = (vector.getZ() == 0. && !Double.isNaN(z) ? Math.copySign(1., z) : z) / vector.getZ();
        result.setX((vector.getX() == 0. ? Math.signum(factor) : factor) * vector.getX());
        result.setY((vector.getY() == 0. ? Math.signum(factor) : factor) * vector.getY());
        result.setZ(z);
        return result;
    }

    private static BlockState getBlockData(int x, int y, int z, LevelChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ, PlayerData playerData) {
        int chunkX = x >> 4;
        int sectionY = y >> 4;
        int chunkZ = z >> 4;

        if (expectedChunkX != chunkX || expectedSectionY != sectionY || expectedChunkZ != chunkZ) {
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            ChunkBlocks chunkBlocks = playerData.getChunks().get(chunkPos);

            if (chunkBlocks == null) {
                return null;
            }

            LevelChunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                playerData.getChunks().remove(chunkPos, chunkBlocks);
                return null;
            }

            if (sectionY < chunk.getMinSection() || sectionY > chunk.getMaxSection() - 1) {
                return Blocks.AIR.defaultBlockState();
            }

            expectedSection = chunk.getSections()[sectionY - chunk.getMinSection()];

            if (expectedSection == null || expectedSection.hasOnlyAir()) { // Sections aren't null anymore.
                return Blocks.AIR.defaultBlockState();
            }
        }

        BlockState blockState;

        // synchronized (expectedSection.getStates()) {
        //     try {
        //         expectedSection.getStates().acquire();
                try {
                    blockState = expectedSection.getBlockState(x & 15, y & 15, z & 15);
                } catch (MissingPaletteEntryException e) {
                    blockState = Blocks.AIR.defaultBlockState();
                }
        //     } finally {
        //         expectedSection.getStates().release();
        //     }
        // }

        return blockState;
    }

    private static interface IntArrayConsumer extends Consumer<int[]> {

    }
}
