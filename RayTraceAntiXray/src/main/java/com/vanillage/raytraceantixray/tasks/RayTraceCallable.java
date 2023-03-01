package com.vanillage.raytraceantixray.tasks;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.util.Vector;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import com.destroystokyo.paper.antixray.ChunkPacketBlockController;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.Result;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.util.BlockIterator;
import com.vanillage.raytraceantixray.util.BlockOcclusionCulling;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class RayTraceCallable implements Callable<Void> {
    private final PlayerData playerData;
    private final BlockOcclusionCulling blockOcclusionCulling;
    private final Collection<ChunkBlocks> chunks;
    private final double rayTraceDistance;
    private final double rayTraceDistanceSquared;
    private final boolean rehideBlocks;

    public RayTraceCallable(PlayerData playerData) {
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) playerData.getLocations()[0].getWorld()).getHandle().chunkPacketBlockController;

        if (!(chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray)) {
            this.playerData = null;
            blockOcclusionCulling = null;
            chunks = null;
            rayTraceDistance = 0.;
            rayTraceDistanceSquared = 0.;
            rehideBlocks = false;
            return;
        }

        this.playerData = playerData;
        NonBlockingHashMapLong<ChunkBlocks> chunks = playerData.getChunks();
        ChunkPacketBlockControllerAntiXray chunkPacketBlockControllerAntiXray = (ChunkPacketBlockControllerAntiXray) chunkPacketBlockController;
        boolean[] solidGlobal = chunkPacketBlockControllerAntiXray.solidGlobal;
        BlockIterator blockIterator = new BlockIterator(0., 0., 0., 0., 0., 0.);
        blockOcclusionCulling = new BlockOcclusionCulling(l -> {
            ChunkBlocks chunkBlocks = chunks.get(l);

            if (chunkBlocks == null) {
                return null;
            }

            return chunkBlocks.getChunk();
        }, b -> solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(b)], blockIterator::initializeNormalized, true, true);
        this.chunks = chunks.values();
        rayTraceDistance = chunkPacketBlockControllerAntiXray.rayTraceDistance;
        rayTraceDistanceSquared = rayTraceDistance * rayTraceDistance;
        rehideBlocks = chunkPacketBlockControllerAntiXray.rehideBlocks;
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
        if (blockOcclusionCulling == null) {
            return;
        }

        VectorialLocation[] locations = playerData.getLocations();
        Vector playerVector = locations[0].getVector();
        double playerX = playerVector.getX();
        double playerY = playerVector.getY();
        double playerZ = playerVector.getZ();
        playerVector.setX(playerX - rayTraceDistance);
        playerVector.setZ(playerZ - rayTraceDistance);
        int chunkXMin = playerVector.getBlockX() >> 4;
        int chunkZMin = playerVector.getBlockZ() >> 4;
        playerVector.setX(playerX + rayTraceDistance);
        playerVector.setZ(playerZ + rayTraceDistance);
        int chunkXMax = playerVector.getBlockX() >> 4;
        int chunkZMax = playerVector.getBlockZ() >> 4;
        playerVector.setX(playerX);
        playerVector.setZ(playerZ);

        for (ChunkBlocks chunkBlocks : chunks) {
            LevelChunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                playerData.getChunks().remove(chunkBlocks.getKey(), chunkBlocks);
                continue;
            }

            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;

            if (chunkX < chunkXMin || chunkX > chunkXMax || chunkZ < chunkZMin || chunkZ > chunkZMax) {
                continue;
            }

            Iterator<Entry<BlockPos, Boolean>> iterator = chunkBlocks.getBlocks().entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<BlockPos, Boolean> blockHidden = iterator.next();
                BlockPos block = blockHidden.getKey();
                int x = block.getX();
                int y = block.getY();
                int z = block.getZ();
                double centerX = x + 0.5;
                double centerY = y + 0.5;
                double centerZ = z + 0.5;
                double differenceX = playerX - centerX;
                double differenceY = playerY - centerY;
                double differenceZ = playerZ - centerZ;
                double distanceSquared = differenceX * differenceX + differenceY * differenceY + differenceZ * differenceZ;

                if (!(distanceSquared <= rayTraceDistanceSquared)) {
                    continue;
                }

                boolean visible = false;

                for (int i = 0; i < locations.length; i++) {
                    VectorialLocation location = locations[i];
                    Vector direction = location.getDirection();

                    if (i == 0) {
                        if (blockOcclusionCulling.isVisible(x, y, z, centerX, centerY, centerZ, differenceX, differenceY, differenceZ, distanceSquared, direction.getX(), direction.getY(), direction.getZ(), chunk, chunkX, chunkZ)) {
                            visible = true;
                            break;
                        }
                    } else {
                        Vector vector = location.getVector();
                        double vectorDifferenceX = vector.getX() - centerX;
                        double vectorDifferenceY = vector.getY() - centerY;
                        double vectorDifferenceZ = vector.getZ() - centerZ;
                        double vectorDistanceSquared = vectorDifferenceX * vectorDifferenceX + vectorDifferenceY * vectorDifferenceY + vectorDifferenceZ * vectorDifferenceZ;

                        if (blockOcclusionCulling.isVisible(x, y, z, centerX, centerY, centerZ, vectorDifferenceX, vectorDifferenceY, vectorDifferenceZ, vectorDistanceSquared, direction.getX(), direction.getY(), direction.getZ(), chunk, chunkX, chunkZ)) {
                            visible = true;
                            break;
                        }
                    }
                }

                if (visible) {
                    if (blockHidden.getValue()) {
                        playerData.getResults().add(new Result(x, y, z, true));

                        if (rehideBlocks) {
                            blockHidden.setValue(false);
                        } else {
                            iterator.remove();
                        }
                    }
                } else if (!blockHidden.getValue()) {
                    playerData.getResults().add(new Result(x, y, z, false));
                    blockHidden.setValue(true);
                }
            }
        }
    }
}
