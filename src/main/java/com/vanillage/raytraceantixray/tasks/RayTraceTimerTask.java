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
    private static final IntArrayConsumer[] centerToXTorus = new IntArrayConsumer[] { c -> c[1]++, c -> {
        c[1]++;
        c[2]++;
    }, c -> c[2]++, c -> {
        c[1]--;
        c[2]++;
    }, c -> c[1]--, c -> {
        c[1]--;
        c[2]--;
    }, c -> c[2]--, c -> {
        c[1]++;
        c[2]--;
    } };
    private static final IntArrayConsumer increaseX = c -> c[0]++;
    private static final IntArrayConsumer decreaseX = c -> c[0]--;
    private static final IntArrayConsumer[] centerToYTorus = new IntArrayConsumer[] { c -> c[2]++, c -> {
        c[2]++;
        c[0]++;
    }, c -> c[0]++, c -> {
        c[2]--;
        c[0]++;
    }, c -> c[2]--, c -> {
        c[2]--;
        c[0]--;
    }, c -> c[0]--, c -> {
        c[2]++;
        c[0]--;
    } };
    private static final IntArrayConsumer increaseY = c -> c[1]++;
    private static final IntArrayConsumer decreaseY = c -> c[1]--;
    private static final IntArrayConsumer[] centerToZTorus = new IntArrayConsumer[] { c -> c[0]++, c -> {
        c[0]++;
        c[1]++;
    }, c -> c[1]++, c -> {
        c[0]--;
        c[1]++;
    }, c -> c[0]--, c -> {
        c[0]--;
        c[1]--;
    }, c -> c[1]--, c -> {
        c[0]++;
        c[1]--;
    } };
    private static final IntArrayConsumer increaseZ = c -> c[2]++;
    private static final IntArrayConsumer decreaseZ = c -> c[2]--;
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

                        if (solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)] && closesTorus(rayBlock.getX(), rayBlock.getY(), rayBlock.getZ(), difference, section, chunkCoordIntPair.x, sectionIndex, chunkCoordIntPair.z, playerData, solidGlobal)) {
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

    private boolean closesTorus(int x, int y, int z, Vector direction, ChunkSection expectedSection, int expectedChunkX, int expectedSectionIndex, int expectedChunkZ, PlayerData playerData, boolean[] solidGlobal) {
        IntArrayConsumer[] centerToTorus;
        IntArrayConsumer increase;
        IntArrayConsumer decrease;
        double absDirectionX = Math.abs(direction.getX());
        double absDirectionY = Math.abs(direction.getY());
        double absDirectionZ = Math.abs(direction.getZ());

        if (absDirectionX > absDirectionY) {
            if (absDirectionZ > absDirectionX) {
                centerToTorus = centerToZTorus;
                increase = increaseZ;
                decrease = decreaseZ;
            } else {
                centerToTorus = centerToXTorus;
                increase = increaseX;
                decrease = decreaseX;
            }
        } else if (absDirectionY > absDirectionZ) {
            centerToTorus = centerToYTorus;
            increase = increaseY;
            decrease = decreaseY;
        } else {
            centerToTorus = centerToZTorus;
            increase = increaseZ;
            decrease = decreaseZ;
        }

        return closesTorus(x, y, z, ref, 0, 0, 0, centerToTorus, increase, decrease, expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData, solidGlobal);
    }

    private boolean closesTorus(int x, int y, int z, int[] ref, int step, int initialLayer, int layer, IntArrayConsumer[] centerToTorus, IntArrayConsumer increase, IntArrayConsumer decrease, ChunkSection expectedSection, int expectedChunkX, int expectedSectionIndex, int expectedChunkZ, PlayerData playerData, boolean[] solidGlobal) {
        ref[0] = x;
        ref[1] = y;
        ref[2] = z;
        centerToTorus[step].accept(ref);
        IBlockData blockData = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData);

        if (blockData == null) {
            return true;
        }

        if (solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)]) {
            return step == 7 || closesTorus(x, y, z, ref, step + 1, step == 0 ? 0 : initialLayer, 0, centerToTorus, increase, decrease, expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData, solidGlobal);
        }

        if ((layer == 0 || layer == -1) && (step != 7 || initialLayer == 0 || initialLayer == -1)) {
            decrease.accept(ref);
            blockData = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData);

            if (blockData == null) {
                return true;
            }

            if (solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)]) {
                if (step == 7) {
                    return true;
                }

                if (closesTorus(x, y, z, ref, step + 1, step == 0 ? -1 : initialLayer, -1, centerToTorus, increase, decrease, expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData, solidGlobal)) {
                    return true;
                } else {
                    ref[0] = x;
                    ref[1] = y;
                    ref[2] = z;
                    centerToTorus[step].accept(ref);
                }
            } else {
                increase.accept(ref);
            }
        }

        if ((layer == 0 || layer == 1) && (step != 7 || initialLayer == 0 || initialLayer == 1)) {
            increase.accept(ref);
            blockData = getBlockData(ref[0], ref[1], ref[2], expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData);
            return blockData == null || solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)] && (step == 7 || closesTorus(x, y, z, ref, step + 1, step == 0 ? 1 : initialLayer, 1, centerToTorus, increase, decrease, expectedSection, expectedChunkX, expectedSectionIndex, expectedChunkZ, playerData, solidGlobal));
        }

        return false;
    }

    private static interface IntArrayConsumer extends Consumer<int[]> {

    }
}
