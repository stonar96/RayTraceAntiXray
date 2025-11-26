package com.vanillage.raytraceantixray.tasks;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.Vector;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.LongWrapper;
import com.vanillage.raytraceantixray.data.MutableLongWrapper;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.Result;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.util.BlockIterator;
import com.vanillage.raytraceantixray.util.BlockOcclusionCulling;
import com.vanillage.raytraceantixray.util.BlockOcclusionCulling.BlockOcclusionGetter;

import io.papermc.paper.antixray.ChunkPacketBlockController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.chunk.PaletteResize;

public final class RayTraceCallable implements Callable<Void> {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private final RayTraceAntiXray plugin;
    private final PlayerData playerData;
    private final CachedSectionBlockOcclusionGetter cachedSectionBlockOcclusionGetter;
    private final BlockOcclusionCulling blockOcclusionCulling;
    private final Collection<ChunkBlocks> chunks;
    private final double rayTraceDistance;
    private final double rayTraceDistanceSquared;
    private final boolean rehideBlocks;
    private final double rehideDistanceSquared;

    public RayTraceCallable(RayTraceAntiXray plugin, PlayerData playerData) {
        this.plugin = plugin;
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) playerData.getLocations()[0].getWorld()).getHandle().chunkPacketBlockController;

        if (!(chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray)) {
            this.playerData = null;
            cachedSectionBlockOcclusionGetter = null;
            blockOcclusionCulling = null;
            chunks = null;
            rayTraceDistance = 0.;
            rayTraceDistanceSquared = 0.;
            rehideBlocks = false;
            rehideDistanceSquared = 0.;
            return;
        }

        this.playerData = playerData;
        MutableLongWrapper mutableLongWrapper = new MutableLongWrapper(0L);
        ConcurrentMap<LongWrapper, ChunkBlocks> chunks = playerData.getChunks();
        ChunkPacketBlockControllerAntiXray chunkPacketBlockControllerAntiXray = (ChunkPacketBlockControllerAntiXray) chunkPacketBlockController;
        boolean[] solidGlobal = chunkPacketBlockControllerAntiXray.solidGlobal;
        cachedSectionBlockOcclusionGetter = new CachedSectionBlockOcclusionGetter() {
            private static final boolean UNLOADED_OCCLUDING = true;
            private LevelChunk chunk;
            private LevelChunkSection section;
            private int chunkX;
            private int sectionY;
            private int chunkZ;

            @Override
            public boolean isOccluding(int x, int y, int z) {
                int chunkX = x >> 4;
                int chunkZ = z >> 4;

                if (this.chunkX != chunkX || this.chunkZ != chunkZ) {
                    mutableLongWrapper.setValue(ChunkPos.asLong(chunkX, chunkZ));
                    ChunkBlocks chunkBlocks = chunks.get(mutableLongWrapper);

                    if (chunkBlocks == null) {
                        return UNLOADED_OCCLUDING;
                    }

                    LevelChunk chunk = chunkBlocks.getChunk();

                    if (chunk == null) {
                        return UNLOADED_OCCLUDING;
                    }

                    int sectionY = y >> 4;
                    int minSectionY = chunk.getMinSectionY();

                    if (sectionY < minSectionY || sectionY >= chunk.getMaxSectionY()) {
                        return false;
                    }

                    LevelChunkSection section = chunk.getSections()[sectionY - minSectionY];
                    return section != null && !section.hasOnlyAir() && solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(getBlockState(section, x, y, z), PaletteResize.noResizeExpected())]; // Sections aren't null anymore. Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                }

                int sectionY = y >> 4;

                if (this.sectionY != sectionY) {
                    if (chunk == null) {
                        return UNLOADED_OCCLUDING;
                    }

                    int minSectionY = chunk.getMinSectionY();

                    if (sectionY < minSectionY || sectionY >= chunk.getMaxSectionY()) {
                        return false;
                    }

                    LevelChunkSection section = chunk.getSections()[sectionY - minSectionY];
                    return section != null && !section.hasOnlyAir() && solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(getBlockState(section, x, y, z), PaletteResize.noResizeExpected())]; // Sections aren't null anymore. Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                }

                if (section == null) {
                    return chunk == null && UNLOADED_OCCLUDING;
                }

                return solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(getBlockState(section, x, y, z), PaletteResize.noResizeExpected())];
            }

            @Override
            public boolean isOccludingRay(int x, int y, int z) {
                int chunkX = x >> 4;
                int sectionY = y >> 4;
                int chunkZ = z >> 4;

                if (this.chunkX != chunkX || this.chunkZ != chunkZ) {
                    this.chunkX = chunkX;
                    this.sectionY = sectionY;
                    this.chunkZ = chunkZ;
                    mutableLongWrapper.setValue(ChunkPos.asLong(chunkX, chunkZ));
                    ChunkBlocks chunkBlocks = chunks.get(mutableLongWrapper);

                    if (chunkBlocks == null) {
                        chunk = null;
                        section = null;
                        return UNLOADED_OCCLUDING;
                    }

                    chunk = chunkBlocks.getChunk();

                    if (chunk == null) {
                        section = null;
                        return UNLOADED_OCCLUDING;
                    }

                    int minSectionY = chunk.getMinSectionY();

                    if (sectionY < minSectionY || sectionY >= chunk.getMaxSectionY()) {
                        section = null;
                        return false;
                    }

                    section = chunk.getSections()[sectionY - minSectionY];

                    if (section == null) { // Sections aren't null anymore.
                        return false;
                    }

                    if (section.hasOnlyAir()) { // Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                        section = null;
                        return false;
                    }

                    return solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(getBlockState(section, x, y, z), PaletteResize.noResizeExpected())];
                }

                if (this.sectionY != sectionY) {
                    this.sectionY = sectionY;

                    if (chunk == null) {
                        // section = null;
                        return UNLOADED_OCCLUDING;
                    }

                    int minSectionY = chunk.getMinSectionY();

                    if (sectionY < minSectionY || sectionY >= chunk.getMaxSectionY()) {
                        section = null;
                        return false;
                    }

                    section = chunk.getSections()[sectionY - minSectionY];

                    if (section == null) { // Sections aren't null anymore.
                        return false;
                    }

                    if (section.hasOnlyAir()) { // Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
                        section = null;
                        return false;
                    }

                    return solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(getBlockState(section, x, y, z), PaletteResize.noResizeExpected())];
                }

                if (section == null) {
                    return chunk == null && UNLOADED_OCCLUDING;
                }

                return solidGlobal[ChunkPacketBlockControllerAntiXray.GLOBAL_BLOCKSTATE_PALETTE.idFor(getBlockState(section, x, y, z), PaletteResize.noResizeExpected())];
            }

            @Override
            public void initializeCache(LevelChunk chunk, int chunkX, int sectionY, int chunkZ) {
                this.chunk = chunk;
                section = chunk.getSections()[sectionY - chunk.getMinSectionY()];
                this.chunkX = chunkX;
                this.sectionY = sectionY;
                this.chunkZ = chunkZ;
            }

            @Override
            public void clearCache() {
                chunk = null;
                section = null;
            }
        };
        blockOcclusionCulling = new BlockOcclusionCulling(new BlockIterator(0., 0., 0., 0., 0., 0.)::initializeNormalized, cachedSectionBlockOcclusionGetter, true);
        this.chunks = chunks.values();
        rayTraceDistance = chunkPacketBlockControllerAntiXray.rayTraceDistance;
        rayTraceDistanceSquared = rayTraceDistance * rayTraceDistance;
        rehideBlocks = chunkPacketBlockControllerAntiXray.rehideBlocks;
        double rehideDistance = chunkPacketBlockControllerAntiXray.rehideDistance;
        rehideDistanceSquared = rehideDistance * rehideDistance;
    }

    @Override
    public Void call() {
        try {
            rayTrace();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "An error occured on the RayTraceAntiXray tick thread", t);
            throw t;
        }

        return null;
    }

    private void rayTrace() {
        if (blockOcclusionCulling == null) {
            return;
        }

        ConcurrentMap<LongWrapper, ChunkBlocks> chunks = playerData.getChunks();
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
        Queue<Result> results = playerData.getResults();

        for (ChunkBlocks chunkBlocks : this.chunks) {
            LevelChunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                chunks.remove(chunkBlocks.getKey(), chunkBlocks);
                continue;
            }

            ChunkPos chunkPos = chunk.getPos();
            int chunkX = chunkPos.x;

            if (chunkX < chunkXMin || chunkX > chunkXMax) {
                continue;
            }

            int chunkZ = chunkPos.z;

            if (chunkZ < chunkZMin || chunkZ > chunkZMax) {
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

                if (distanceSquared < rehideDistanceSquared) {
                    int sectionY = y >> 4;

                    for (int i = 0; i < locations.length; i++) {
                        VectorialLocation location = locations[i];
                        Vector direction = location.getDirection();
                        double directionX = direction.getX();
                        double directionY = direction.getY();
                        double directionZ = direction.getZ();
                        cachedSectionBlockOcclusionGetter.initializeCache(chunk, chunkX, sectionY, chunkZ);

                        if (i == 0) {
                            if (blockOcclusionCulling.isVisible(x, y, z, centerX, centerY, centerZ, differenceX, differenceY, differenceZ, distanceSquared, directionX, directionY, directionZ)) {
                                visible = true;
                                break;
                            }
                        } else {
                            Vector vector = location.getVector();
                            double vectorDifferenceX = vector.getX() - centerX;
                            double vectorDifferenceY = vector.getY() - centerY;
                            double vectorDifferenceZ = vector.getZ() - centerZ;

                            if (blockOcclusionCulling.isVisible(x, y, z, centerX, centerY, centerZ, vectorDifferenceX, vectorDifferenceY, vectorDifferenceZ, vectorDifferenceX * vectorDifferenceX + vectorDifferenceY * vectorDifferenceY + vectorDifferenceZ * vectorDifferenceZ, directionX, directionY, directionZ)) {
                                visible = true;
                                break;
                            }
                        }
                    }
                }

                boolean hidden = blockHidden.getValue();

                if (visible) {
                    if (hidden) {
                        results.add(new Result(chunkBlocks, block, true));

                        if (rehideBlocks) {
                            blockHidden.setValue(false);
                        } else {
                            iterator.remove();
                        }
                    }
                } else if (!hidden) {
                    results.add(new Result(chunkBlocks, block, false));
                    blockHidden.setValue(true);
                }
            }
        }

        cachedSectionBlockOcclusionGetter.clearCache();
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

    private interface CachedSectionBlockOcclusionGetter extends BlockOcclusionGetter {
        void initializeCache(LevelChunk chunk, int chunkX, int sectionY, int chunkZ);

        void clearCache();
    }
}
