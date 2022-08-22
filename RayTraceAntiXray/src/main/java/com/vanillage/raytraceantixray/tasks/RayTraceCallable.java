package com.vanillage.raytraceantixray.tasks;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
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
    private static final IntArrayConsumer[] CENTER_TO_X_TORUS = new IntArrayConsumer[] { INCREASE_Y, INCREASE_Z, DECREASE_Y, DECREASE_Y, DECREASE_Z, DECREASE_Z, INCREASE_Y, INCREASE_Y };
    private static final IntArrayConsumer[] CENTER_TO_Y_TORUS = new IntArrayConsumer[] { INCREASE_Z, INCREASE_X, DECREASE_Z, DECREASE_Z, DECREASE_X, DECREASE_X, INCREASE_Z, INCREASE_Z };
    private static final IntArrayConsumer[] CENTER_TO_Z_TORUS = new IntArrayConsumer[] { INCREASE_X, INCREASE_Y, DECREASE_X, DECREASE_X, DECREASE_Y, DECREASE_Y, INCREASE_X, INCREASE_X };
    private final int[] ref = new int[3];
    private final PlayerData playerData;

    public RayTraceCallable(PlayerData playerData) {
        this.playerData = playerData;
    }

    @Override
    public Void call() {
        List<? extends Location> locations = playerData.getLocations();
        Location playerLocation = locations.get(0);
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) playerLocation.getWorld()).getHandle().chunkPacketBlockController;

        if (!(chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray)) {
            return null;
        }

        ChunkPacketBlockControllerAntiXray chunkPacketBlockControllerAntiXray = (ChunkPacketBlockControllerAntiXray) chunkPacketBlockController;
        boolean[] solidGlobal = chunkPacketBlockControllerAntiXray.solidGlobal;
        double rayTraceDistance = chunkPacketBlockControllerAntiXray.rayTraceDistance;
        Location temp = playerLocation.clone();
        temp.setX(playerLocation.getX() - rayTraceDistance);
        temp.setZ(playerLocation.getZ() - rayTraceDistance);
        int chunkXMin = temp.getBlockX() >> 4;
        int chunkZMin = temp.getBlockZ() >> 4;
        temp.setX(playerLocation.getX() + rayTraceDistance);
        temp.setZ(playerLocation.getZ() + rayTraceDistance);
        int chunkXMax = temp.getBlockX() >> 4;
        int chunkZMax = temp.getBlockZ() >> 4;
        double rayTraceDistanceSquared = rayTraceDistance * rayTraceDistance;

        for (Location location : locations) {
            Vector vector = location.toVector();
            Vector direction = location.getDirection();

            for (Entry<ChunkPos, ChunkBlocks> chunkEntry : playerData.getChunks().entrySet()) {
                ChunkBlocks chunkBlocks = chunkEntry.getValue();
                LevelChunk chunk = chunkBlocks.getChunk();

                if (chunk == null) {
                    playerData.getChunks().remove(chunkEntry.getKey(), chunkBlocks);
                    continue;
                }

                if (chunk.locX < chunkXMin || chunk.locX > chunkXMax || chunk.locZ < chunkZMin || chunk.locZ > chunkZMax) {
                    continue;
                }

                Iterator<? extends BlockPos> iterator = chunkBlocks.getBlocks().iterator();

                while (iterator.hasNext()) {
                    BlockPos block = iterator.next();
                    block = new BlockPos((chunk.locX << 4) + block.getX(), block.getY(), (chunk.locZ << 4) + block.getZ());
                    Vector blockCenter = new Vector(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
                    Vector difference = vector.clone().subtract(blockCenter);

                    if (difference.lengthSquared() > rayTraceDistanceSquared || difference.dot(direction) > 0.) {
                        continue;
                    }

                    Iterator<BlockPos> blockIterator = new BlockIterator(blockCenter, vector);
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

                        if (solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(blockState)] && checkSurroundingBlocks(block.getX(), block.getY(), block.getZ(), rayBlock.getX(), rayBlock.getY(), rayBlock.getZ(), difference, section, chunkPos.x, sectionY, chunkPos.z, playerData, solidGlobal)) {
                            update = false;
                            break;
                        }
                    }

                    if (update) {
                        playerData.getResult().add(block);
                        iterator.remove();
                    }
                }
            }
        }

        return null;
    }

    private BlockState getBlockData(int x, int y, int z, LevelChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ, PlayerData playerData) {
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

    private boolean checkSurroundingBlocks(int blockX, int blockY, int blockZ, int rayBlockX, int rayBlockY, int rayBlockZ, Vector direction, LevelChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ, PlayerData playerData, boolean[] solidGlobal) {
        IntArrayConsumer[] centerToTorus;
        IntArrayConsumer increase;
        IntArrayConsumer decrease;
        double absDirectionX = Math.abs(direction.getX());
        double absDirectionY = Math.abs(direction.getY());
        double absDirectionZ = Math.abs(direction.getZ());

        if (absDirectionX > absDirectionY) {
            if (absDirectionZ > absDirectionX) {
                centerToTorus = CENTER_TO_Z_TORUS;

                if (direction.getZ() > 0) {
                    increase = DECREASE_Z;
                    decrease = INCREASE_Z;
                } else {
                    increase = INCREASE_Z;
                    decrease = DECREASE_Z;
                }
            } else {
                centerToTorus = CENTER_TO_X_TORUS;

                if (direction.getX() > 0) {
                    increase = DECREASE_X;
                    decrease = INCREASE_X;
                } else {
                    increase = INCREASE_X;
                    decrease = DECREASE_X;
                }
            }
        } else if (absDirectionY > absDirectionZ) {
            centerToTorus = CENTER_TO_Y_TORUS;

            if (direction.getY() > 0) {
                increase = DECREASE_Y;
                decrease = INCREASE_Y;
            } else {
                increase = INCREASE_Y;
                decrease = DECREASE_Y;
            }
        } else {
            centerToTorus = CENTER_TO_Z_TORUS;

            if (direction.getZ() > 0) {
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

        for (int step = 0; step < centerToTorus.length; step++) {
            centerToTorus[step].accept(ref);
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

    private static interface IntArrayConsumer extends Consumer<int[]> {

    }
}
