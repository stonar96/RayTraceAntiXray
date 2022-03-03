package com.vanillage.raytraceantixray.tasks;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.antixray.ChunkPacketBlockController;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.util.BlockIterator;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.ChunkSection;
import net.minecraft.server.v1_16_R3.IBlockData;

public final class RayTraceRunnable implements Runnable, Callable<Object> {
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
    private final RayTraceAntiXray plugin;
    private final PlayerData playerData;

    public RayTraceRunnable(RayTraceAntiXray plugin, PlayerData playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    @Override
    public Object call() throws Exception {
        run();
        return null;
    }

    @Override
    public void run() {
        List<? extends Location> locations = playerData.getLocations();
        Location playerLocation = locations.get(0);
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) playerLocation.getWorld()).getHandle().chunkPacketBlockController;

        if (!(chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray)) {
            return;
        }

        boolean[] solidGlobal = ((ChunkPacketBlockControllerAntiXray) chunkPacketBlockController).solidGlobal;
        double rayTraceDistance = Math.max(plugin.getConfig().getDouble("world-settings." + playerLocation.getWorld().getName() + ".anti-xray.ray-trace-distance", plugin.getConfig().getDouble("world-settings.default.anti-xray.ray-trace-distance")), 0.);
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

            for (Entry<ChunkCoordIntPair, ChunkBlocks> chunkEntry : playerData.getChunks().entrySet()) {
                ChunkBlocks chunkBlocks = chunkEntry.getValue();
                Chunk chunk = chunkBlocks.getChunk();

                if (chunk == null) {
                    playerData.getChunks().remove(chunkEntry.getKey(), chunkBlocks);
                    continue;
                }

                if (chunk.locX < chunkXMin || chunk.locX > chunkXMax || chunk.locZ < chunkZMin || chunk.locZ > chunkZMax) {
                    continue;
                }

                Iterator<? extends BlockPosition> iterator = chunkBlocks.getBlocks().iterator();

                while (iterator.hasNext()) {
                    BlockPosition block = iterator.next();
                    block = new BlockPosition((chunk.locX << 4) + block.getX(), block.getY(), (chunk.locZ << 4) + block.getZ());
                    Vector blockCenter = new Vector(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
                    Vector difference = vector.clone().subtract(blockCenter);

                    if (difference.lengthSquared() > rayTraceDistanceSquared || difference.dot(direction) > 0.) {
                        continue;
                    }

                    Iterator<BlockPosition> blockIterator = new BlockIterator(blockCenter, vector);
                    boolean update = true;

                    while (blockIterator.hasNext()) {
                        BlockPosition rayBlock = blockIterator.next();
                        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(rayBlock);
                        ChunkBlocks rayChunkBlocks = playerData.getChunks().get(chunkCoordIntPair);

                        if (rayChunkBlocks == null) {
                            update = false;
                            break;
                        }

                        Chunk rayChunk = rayChunkBlocks.getChunk();

                        if (rayChunk == null) {
                            playerData.getChunks().remove(chunkCoordIntPair, rayChunkBlocks);
                            update = false;
                            break;
                        }

                        int sectionY = rayBlock.getY() >> 4;

                        if (sectionY < 0 || sectionY > 15) {
                            continue;
                        }

                        ChunkSection section = rayChunk.getSections()[sectionY];

                        if (section == null) {
                            continue;
                        }

                        IBlockData blockData;

                        // synchronized (section.getBlocks()) {
                        //     try {
                        //         section.getBlocks().a();
                                blockData = section.getType(rayBlock.getX() & 15, rayBlock.getY() & 15, rayBlock.getZ() & 15);
                        //     } finally {
                        //         section.getBlocks().b();
                        //     }
                        // }

                        if (solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)] && checkSurroundingBlocks(block.getX(), block.getY(), block.getZ(), rayBlock.getX(), rayBlock.getY(), rayBlock.getZ(), difference, section, chunkCoordIntPair.x, sectionY, chunkCoordIntPair.z, playerData, solidGlobal)) {
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
    }

    private IBlockData getBlockData(int x, int y, int z, ChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ, PlayerData playerData) {
        int chunkX = x >> 4;
        int sectionY = y >> 4;
        int chunkZ = z >> 4;

        if (expectedChunkX != chunkX || expectedSectionY != sectionY || expectedChunkZ != chunkZ) {
            ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(chunkX, chunkZ);
            ChunkBlocks chunkBlocks = playerData.getChunks().get(chunkCoordIntPair);

            if (chunkBlocks == null) {
                return null;
            }

            Chunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                playerData.getChunks().remove(chunkCoordIntPair, chunkBlocks);
                return null;
            }

            if (sectionY < 0 || sectionY > 15) {
                return Blocks.AIR.getBlockData();
            }

            expectedSection = chunk.getSections()[sectionY];

            if (expectedSection == null) {
                return Blocks.AIR.getBlockData();
            }
        }

        IBlockData blockData;

        // synchronized (expectedSection.getBlocks()) {
        //     try {
        //         expectedSection.getBlocks().a();
                blockData = expectedSection.getType(x & 15, y & 15, z & 15);
        //     } finally {
        //         expectedSection.getBlocks().b();
        //     }
        // }

        return blockData;
    }

    private boolean checkSurroundingBlocks(int blockX, int blockY, int blockZ, int rayBlockX, int rayBlockY, int rayBlockZ, Vector direction, ChunkSection expectedSection, int expectedChunkX, int expectedSectionY, int expectedChunkZ, PlayerData playerData, boolean[] solidGlobal) {
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
            IBlockData blockData = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionY, expectedChunkZ, playerData);

            if (blockData == null) {
                return true;
            }

            if (solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)]) {
                continue;
            }

            increase.accept(ref);

            if (ref[0] == blockX && ref[1] == blockY && ref[2] == blockZ) {
                return false;
            }

            blockData = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionY, expectedChunkZ, playerData);

            if (blockData == null) {
                return true;
            }

            if (!solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)]) {
                return false;
            }

            decrease.accept(ref);
        }

        return true;
    }

    private static interface IntArrayConsumer extends Consumer<int[]> {

    }
}
