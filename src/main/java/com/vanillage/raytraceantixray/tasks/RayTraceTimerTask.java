package com.vanillage.raytraceantixray.tasks;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Map.Entry;
import java.util.TimerTask;

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
import net.minecraft.server.v1_16_R3.World;

public final class RayTraceTimerTask extends TimerTask {
    private static final IntArrayConsumer increaseX = c -> c[0]++;
    private static final IntArrayConsumer decreaseX = c -> c[0]--;
    private static final IntArrayConsumer increaseY = c -> c[1]++;
    private static final IntArrayConsumer decreaseY = c -> c[1]--;
    private static final IntArrayConsumer increaseZ = c -> c[2]++;
    private static final IntArrayConsumer decreaseZ = c -> c[2]--;
    private static final IntArrayConsumer[] centerToXTorus = new IntArrayConsumer[] { increaseY, increaseZ, decreaseY, decreaseY, decreaseZ, decreaseZ, increaseY, increaseY };
    private static final IntArrayConsumer[] centerToYTorus = new IntArrayConsumer[] { increaseZ, increaseX, decreaseZ, decreaseZ, decreaseX, decreaseX, increaseZ, increaseZ };
    private static final IntArrayConsumer[] centerToZTorus = new IntArrayConsumer[] { increaseX, increaseY, decreaseX, decreaseX, decreaseY, decreaseY, increaseX, increaseX };
    private final int[] ref = new int[3];
    private final RayTraceAntiXray plugin;

    public RayTraceTimerTask(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Entry<UUID, PlayerData> entry : plugin.getPlayerData().entrySet()) {
            PlayerData playerData = entry.getValue();
            Location location = playerData.getLocation();
            boolean[] solidGlobal = null;

            try {
                Field field = World.class.getDeclaredField("chunkPacketBlockController");
                field.setAccessible(true);
                ChunkPacketBlockController chunkPacketBlockController = (ChunkPacketBlockController) field.get(((CraftWorld) location.getWorld()).getHandle());

                if (chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray) {
                    solidGlobal = ((ChunkPacketBlockControllerAntiXray) chunkPacketBlockController).solidGlobal;
                } else {
                    continue;
                }
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }

            Vector vector = location.toVector();
            Vector direction = location.getDirection();
            double rayTraceDistance = Math.max(plugin.getConfig().getDouble("world-settings." + location.getWorld().getName() + ".anti-xray.ray-trace-distance", plugin.getConfig().getDouble("world-settings.default.anti-xray.ray-trace-distance")), 0.);
            Location temp = location.clone();
            temp.setX(location.getX() - rayTraceDistance);
            temp.setZ(location.getZ() - rayTraceDistance);
            int chunkXMin = temp.getBlockX() >> 4;
            int chunkZMin = temp.getBlockZ() >> 4;
            temp.setX(location.getX() + rayTraceDistance);
            temp.setZ(location.getZ() + rayTraceDistance);
            int chunkXMax = temp.getBlockX() >> 4;
            int chunkZMax = temp.getBlockZ() >> 4;
            double rayTraceDistanceSquared = rayTraceDistance * rayTraceDistance;

            for (Entry<ChunkCoordIntPair, ChunkBlocks> chunkEntry : playerData.getChunks().entrySet()) {
                ChunkBlocks chunkBlocks = chunkEntry.getValue();
                Chunk chunk = chunkBlocks.getChunk();

                if (chunk.locX < chunkXMin || chunk.locX > chunkXMax || chunk.locZ < chunkZMin || chunk.locZ > chunkZMax) {
                    continue;
                }

                Collection<BlockPosition> blocks = chunkBlocks.getBlocks();
                Iterator<BlockPosition> iterator = blocks.iterator();

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
                        int sectionIndex = rayBlock.getY() >> 4;

                        if (sectionIndex < 0 || sectionIndex > 15) {
                            continue;
                        }

                        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(rayBlock);
                        ChunkBlocks rayChunkBlocks = playerData.getChunks().get(chunkCoordIntPair);

                        if (rayChunkBlocks == null) {
                            update = false;
                            break;
                        }

                        ChunkSection section = rayChunkBlocks.getChunk().getSections()[sectionIndex];

                        if (section == null) {
                            continue;
                        }

                        IBlockData blockData;

                        synchronized (section.getBlocks()) {
                            blockData = section.getBlocks().a(rayBlock.getX() & 15, rayBlock.getY() & 15, rayBlock.getZ() & 15);
                        }

                        if (solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)] && checkSurroundingBlocks(block.getX(), block.getY(), block.getZ(), rayBlock.getX(), rayBlock.getY(), rayBlock.getZ(), difference, section, chunkCoordIntPair.x, sectionIndex, chunkCoordIntPair.z, playerData, solidGlobal)) {
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

    private IBlockData getBlockData(int x, int y, int z, ChunkSection expectedSection, int expectedChunkX, int expectedSectionIndex, int expectedChunkZ, PlayerData playerData) {
        int chunkX = x >> 4;
        int sectionIndex = y >> 4;
        int chunkZ = z >> 4;

        if (expectedChunkX != chunkX || expectedSectionIndex != sectionIndex || expectedChunkZ != chunkZ) {
            if (sectionIndex < 0 || sectionIndex > 15) {
                return Blocks.AIR.getBlockData();
            }

            ChunkBlocks chunkBlocks = playerData.getChunks().get(new ChunkCoordIntPair(chunkX, chunkZ));

            if (chunkBlocks == null) {
                return null;
            }

            expectedSection = chunkBlocks.getChunk().getSections()[sectionIndex];

            if (expectedSection == null) {
                return Blocks.AIR.getBlockData();
            }
        }

        IBlockData blockData;

        synchronized (expectedSection.getBlocks()) {
            blockData = expectedSection.getBlocks().a(x & 15, y & 15, z & 15);
        }

        return blockData;
    }

    private boolean checkSurroundingBlocks(int blockX, int blockY, int blockZ, int rayBlockX, int rayBlockY, int rayBlockZ, Vector direction, ChunkSection expectedSection, int expectedChunkX, int expectedSectionIndex, int expectedChunkZ, PlayerData playerData, boolean[] solidGlobal) {
        IntArrayConsumer[] centerToTorus;
        IntArrayConsumer increase;
        IntArrayConsumer decrease;
        double absDirectionX = Math.abs(direction.getX());
        double absDirectionY = Math.abs(direction.getY());
        double absDirectionZ = Math.abs(direction.getZ());

        if (absDirectionX > absDirectionY) {
            if (absDirectionZ > absDirectionX) {
                centerToTorus = centerToZTorus;

                if (direction.getZ() > 0) {
                    increase = decreaseZ;
                    decrease = increaseZ;
                } else {
                    increase = increaseZ;
                    decrease = decreaseZ;
                }
            } else {
                centerToTorus = centerToXTorus;

                if (direction.getX() > 0) {
                    increase = decreaseX;
                    decrease = increaseX;
                } else {
                    increase = increaseX;
                    decrease = decreaseX;
                }
            }
        } else if (absDirectionY > absDirectionZ) {
            centerToTorus = centerToYTorus;

            if (direction.getY() > 0) {
                increase = decreaseY;
                decrease = increaseY;
            } else {
                increase = increaseY;
                decrease = decreaseY;
            }
        } else {
            centerToTorus = centerToZTorus;

            if (direction.getZ() > 0) {
                increase = decreaseZ;
                decrease = increaseZ;
            } else {
                increase = increaseZ;
                decrease = decreaseZ;
            }
        }

        ref[0] = rayBlockX;
        ref[1] = rayBlockY;
        ref[2] = rayBlockZ;

        for (int step = 0; step < centerToTorus.length; step++) {
            centerToTorus[step].accept(ref);
            IBlockData blockData = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData);

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

            blockData = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData);

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
