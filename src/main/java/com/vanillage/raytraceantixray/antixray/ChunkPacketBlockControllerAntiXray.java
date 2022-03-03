package com.vanillage.raytraceantixray.antixray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.PlayerInteractManager;
import net.minecraft.server.v1_16_R3.WorldServer;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.World;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkEmpty;
import net.minecraft.server.v1_16_R3.ChunkSection;
import net.minecraft.server.v1_16_R3.DataPalette;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import org.bukkit.Bukkit;
import org.bukkit.World.Environment;

import com.destroystokyo.paper.PaperWorldConfig;
import com.destroystokyo.paper.antixray.ChunkPacketBlockController;
import com.destroystokyo.paper.antixray.ChunkPacketBlockControllerAntiXray.EngineMode;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.destroystokyo.paper.antixray.ChunkPacketInfo;
import com.destroystokyo.paper.antixray.DataBitsReader;
import com.destroystokyo.paper.antixray.DataBitsWriter;

public final class ChunkPacketBlockControllerAntiXray extends ChunkPacketBlockController {

    private final RayTraceAntiXray plugin;
    private final Executor executor;
    private final EngineMode engineMode;
    private final int maxChunkSectionIndex;
    private final int updateRadius;
    private final boolean usePermission;
    private final int maxRayTraceBlockCountPerChunk;
    private final IBlockData[] predefinedBlockData;
    private final IBlockData[] predefinedBlockDataFull;
    private final IBlockData[] predefinedBlockDataStone;
    private final IBlockData[] predefinedBlockDataNetherrack;
    private final IBlockData[] predefinedBlockDataEndStone;
    private final int[] predefinedBlockDataBitsGlobal;
    private final int[] predefinedBlockDataBitsStoneGlobal;
    private final int[] predefinedBlockDataBitsNetherrackGlobal;
    private final int[] predefinedBlockDataBitsEndStoneGlobal;
    public final boolean[] solidGlobal = new boolean[Block.REGISTRY_ID.size()];
    private final boolean[] obfuscateGlobal = new boolean[Block.REGISTRY_ID.size()];
    private final boolean[] traceGlobal;
    private final ChunkSection[] emptyNearbyChunkSections = {Chunk.EMPTY_CHUNK_SECTION, Chunk.EMPTY_CHUNK_SECTION, Chunk.EMPTY_CHUNK_SECTION, Chunk.EMPTY_CHUNK_SECTION};
    private final int maxBlockYUpdatePosition;

    public ChunkPacketBlockControllerAntiXray(RayTraceAntiXray plugin, int maxRayTraceBlockCountPerChunk, Iterable<? extends String> toTrace, World world, Executor executor) {
        this.plugin = plugin;
        PaperWorldConfig paperWorldConfig = world.paperConfig;
        engineMode = paperWorldConfig.engineMode;
        maxChunkSectionIndex = paperWorldConfig.maxChunkSectionIndex;
        updateRadius = paperWorldConfig.updateRadius;
        usePermission = paperWorldConfig.usePermission;
        this.maxRayTraceBlockCountPerChunk = maxRayTraceBlockCountPerChunk;

        this.executor = executor;

        List<String> toObfuscate;

        if (engineMode == EngineMode.HIDE) {
            toObfuscate = paperWorldConfig.hiddenBlocks;
            predefinedBlockData = null;
            predefinedBlockDataFull = null;
            predefinedBlockDataStone = new IBlockData[] {Blocks.STONE.getBlockData()};
            predefinedBlockDataNetherrack = new IBlockData[] {Blocks.NETHERRACK.getBlockData()};
            predefinedBlockDataEndStone = new IBlockData[] {Blocks.END_STONE.getBlockData()};
            predefinedBlockDataBitsGlobal = null;
            predefinedBlockDataBitsStoneGlobal = new int[] {ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(Blocks.STONE.getBlockData())};
            predefinedBlockDataBitsNetherrackGlobal = new int[] {ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(Blocks.NETHERRACK.getBlockData())};
            predefinedBlockDataBitsEndStoneGlobal = new int[] {ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(Blocks.END_STONE.getBlockData())};
        } else {
            toObfuscate = new ArrayList<>(paperWorldConfig.replacementBlocks);
            List<IBlockData> predefinedBlockDataList = new LinkedList<IBlockData>();

            for (String id : paperWorldConfig.hiddenBlocks) {
                Block block = IRegistry.BLOCK.getOptional(new MinecraftKey(id)).orElse(null);

                if (block != null && !block.isTileEntity()) {
                    toObfuscate.add(id);
                    predefinedBlockDataList.add(block.getBlockData());
                }
            }

            // The doc of the LinkedHashSet(Collection<? extends E> c) constructor doesn't specify that the insertion order is the predictable iteration order of the specified Collection, although it is in the implementation
            Set<IBlockData> predefinedBlockDataSet = new LinkedHashSet<IBlockData>();
            // Therefore addAll(Collection<? extends E> c) is used, which guarantees this order in the doc
            predefinedBlockDataSet.addAll(predefinedBlockDataList);
            predefinedBlockData = predefinedBlockDataSet.size() == 0 ? new IBlockData[] {Blocks.DIAMOND_ORE.getBlockData()} : predefinedBlockDataSet.toArray(new IBlockData[0]);
            predefinedBlockDataFull = predefinedBlockDataSet.size() == 0 ? new IBlockData[] {Blocks.DIAMOND_ORE.getBlockData()} : predefinedBlockDataList.toArray(new IBlockData[0]);
            predefinedBlockDataStone = null;
            predefinedBlockDataNetherrack = null;
            predefinedBlockDataEndStone = null;
            predefinedBlockDataBitsGlobal = new int[predefinedBlockDataFull.length];

            for (int i = 0; i < predefinedBlockDataFull.length; i++) {
                predefinedBlockDataBitsGlobal[i] = ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(predefinedBlockDataFull[i]);
            }

            predefinedBlockDataBitsStoneGlobal = null;
            predefinedBlockDataBitsNetherrackGlobal = null;
            predefinedBlockDataBitsEndStoneGlobal = null;
        }

        for (String id : toObfuscate) {
            Block block = IRegistry.BLOCK.getOptional(new MinecraftKey(id)).orElse(null);

            // Don't obfuscate air because air causes unnecessary block updates and causes block updates to fail in the void
            if (block != null && !block.getBlockData().isAir()) {
                // Replace all block states of a specified block
                // No OBFHELPER for nms.BlockStateList#a() due to too many decompile errors
                // The OBFHELPER should be getBlockDataList()
                for (IBlockData blockData : block.getStates().a()) {
                    obfuscateGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)] = true;
                }
            }
        }

        if (toTrace == null) {
            traceGlobal = obfuscateGlobal;
        } else {
            traceGlobal = new boolean[Block.REGISTRY_ID.size()];

            for (String id : toTrace) {
                Block block = IRegistry.BLOCK.getOptional(new MinecraftKey(id)).orElse(null);

                // Don't obfuscate air because air causes unnecessary block updates and causes block updates to fail in the void
                if (block != null && !block.getBlockData().isAir()) {
                    // Replace all block states of a specified block
                    // No OBFHELPER for nms.BlockStateList#a() due to too many decompile errors
                    // The OBFHELPER should be getBlockDataList()
                    for (IBlockData blockData : block.getStates().a()) {
                        traceGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)] = true;
                        obfuscateGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)] = true;
                    }
                }
            }
        }

        ChunkEmpty emptyChunk = new ChunkEmpty(world, new ChunkCoordIntPair(0, 0));
        BlockPosition zeroPos = new BlockPosition(0, 0, 0);

        for (int i = 0; i < solidGlobal.length; i++) {
            IBlockData blockData = ChunkSection.GLOBAL_PALETTE.getObject(i);

            if (blockData != null) {
                solidGlobal[i] = blockData.isOccluding(emptyChunk, zeroPos)
                    && blockData.getBlock() != Blocks.SPAWNER && blockData.getBlock() != Blocks.BARRIER && blockData.getBlock() != Blocks.SHULKER_BOX && blockData.getBlock() != Blocks.SLIME_BLOCK || paperWorldConfig.lavaObscures && blockData == Blocks.LAVA.getBlockData();
                // Comparing blockData == Blocks.LAVA.getBlockData() instead of blockData.getBlock() == Blocks.LAVA ensures that only "stationary lava" is used
                // shulker box checks TE.
            }
        }

        this.maxBlockYUpdatePosition = (maxChunkSectionIndex + 1) * 16 + updateRadius - 1;
    }

    private int getPredefinedBlockDataFullLength() {
        return engineMode == EngineMode.HIDE ? 1 : predefinedBlockDataFull.length;
    }

    @Override
    public IBlockData[] getPredefinedBlockData(World world, IChunkAccess chunk, ChunkSection chunkSection, boolean initializeBlocks) {
        // Return the block data which should be added to the data palettes so that they can be used for the obfuscation
        if (chunkSection.getYPosition() >> 4 <= maxChunkSectionIndex) {
            switch (engineMode) {
                case HIDE:
                    switch (world.getWorld().getEnvironment()) {
                        case NETHER:
                            return predefinedBlockDataNetherrack;
                        case THE_END:
                            return predefinedBlockDataEndStone;
                        default:
                            return predefinedBlockDataStone;
                    }
                default:
                    return predefinedBlockData;
            }
        }

        return null;
    }

    @Override
    public boolean shouldModify(EntityPlayer entityPlayer, Chunk chunk, int chunkSectionSelector) {
        return !usePermission || !entityPlayer.getBukkitEntity().hasPermission("paper.antixray.bypass");
    }

    @Override
    public ChunkPacketInfoAntiXray getChunkPacketInfo(PacketPlayOutMapChunk packetPlayOutMapChunk, Chunk chunk, int chunkSectionSelector) {
        // Return a new instance to collect data and objects in the right state while creating the chunk packet for thread safe access later
        // Note: As of 1.14 this has to be moved later due to the chunk system.
        ChunkPacketInfoAntiXray chunkPacketInfoAntiXray = new ChunkPacketInfoAntiXray(packetPlayOutMapChunk, chunk, chunkSectionSelector, this);
        return chunkPacketInfoAntiXray;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void modifyBlocks(PacketPlayOutMapChunk packetPlayOutMapChunk, ChunkPacketInfo<IBlockData> chunkPacketInfo) {
        if (chunkPacketInfo == null) {
            packetPlayOutMapChunk.setReady(true);
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            // plugins?
            MinecraftServer.getServer().scheduleOnMain(() -> {
                this.modifyBlocks(packetPlayOutMapChunk, chunkPacketInfo);
            });
            return;
        }

        Chunk chunk = chunkPacketInfo.getChunk();
        int x = chunk.getPos().x;
        int z = chunk.getPos().z;
        WorldServer world = (WorldServer)chunk.world;
        ((ChunkPacketInfoAntiXray) chunkPacketInfo).setNearbyChunks(
            (Chunk) world.getChunkIfLoadedImmediately(x - 1, z),
            (Chunk) world.getChunkIfLoadedImmediately(x + 1, z),
            (Chunk) world.getChunkIfLoadedImmediately(x, z - 1),
            (Chunk) world.getChunkIfLoadedImmediately(x, z + 1));

        executor.execute((ChunkPacketInfoAntiXray) chunkPacketInfo);
    }

    // Actually these fields should be variables inside the obfuscate method but in sync mode or with SingleThreadExecutor in async mode it's okay (even without ThreadLocal)
    // If an ExecutorService with multiple threads is used, ThreadLocal must be used here
    private final ThreadLocal<int[]> predefinedBlockDataBits = ThreadLocal.withInitial(() -> new int[getPredefinedBlockDataFullLength()]);
    private static final ThreadLocal<boolean[]> solid = ThreadLocal.withInitial(() -> new boolean[Block.REGISTRY_ID.size()]);
    private static final ThreadLocal<boolean[]> obfuscate = ThreadLocal.withInitial(() -> new boolean[Block.REGISTRY_ID.size()]);
    private static final ThreadLocal<boolean[]> trace = ThreadLocal.withInitial(() -> new boolean[Block.REGISTRY_ID.size()]);
    // These boolean arrays represent chunk layers, true means don't obfuscate, false means obfuscate
    private static final ThreadLocal<boolean[][]> current = ThreadLocal.withInitial(() -> new boolean[16][16]);
    private static final ThreadLocal<boolean[][]> next = ThreadLocal.withInitial(() -> new boolean[16][16]);
    private static final ThreadLocal<boolean[][]> nextNext = ThreadLocal.withInitial(() -> new boolean[16][16]);
    private static final ThreadLocal<boolean[][]> traceCache = ThreadLocal.withInitial(() -> new boolean[16][16]);

    public void obfuscate(ChunkPacketInfoAntiXray chunkPacketInfoAntiXray) {
        int[] predefinedBlockDataBits = this.predefinedBlockDataBits.get();
        boolean[] solid = ChunkPacketBlockControllerAntiXray.solid.get();
        boolean[] obfuscate = ChunkPacketBlockControllerAntiXray.obfuscate.get();
        boolean[] trace = traceGlobal == obfuscateGlobal ? obfuscate : ChunkPacketBlockControllerAntiXray.trace.get();
        boolean[][] current = ChunkPacketBlockControllerAntiXray.current.get();
        boolean[][] next = ChunkPacketBlockControllerAntiXray.next.get();
        boolean[][] nextNext = ChunkPacketBlockControllerAntiXray.nextNext.get();
        boolean[][] traceCache = ChunkPacketBlockControllerAntiXray.traceCache.get();
        // dataBitsReader, dataBitsWriter and nearbyChunkSections could also be reused (with ThreadLocal if necessary) but it's not worth it
        DataBitsReader dataBitsReader = new DataBitsReader();
        DataBitsWriter dataBitsWriter = new DataBitsWriter();
        ChunkSection[] nearbyChunkSections = new ChunkSection[4];
        boolean[] solidTemp = null;
        boolean[] obfuscateTemp = null;
        boolean[] traceTemp = null;
        dataBitsReader.setDataBits(chunkPacketInfoAntiXray.getData());
        dataBitsWriter.setDataBits(chunkPacketInfoAntiXray.getData());
        int numberOfBlocks = predefinedBlockDataBits.length;
        // Keep the lambda expressions as simple as possible. They are used very frequently.
        IntSupplier random = numberOfBlocks == 1 ? (() -> 0) : new IntSupplier() {
            private int state;

            {
                while ((state = ThreadLocalRandom.current().nextInt()) == 0);
            }

            @Override
            public int getAsInt() {
                // https://en.wikipedia.org/wiki/Xorshift
                state ^= state << 13;
                state ^= state >>> 17;
                state ^= state << 5;
                // https://www.pcg-random.org/posts/bounded-rands.html
                return (int) ((Integer.toUnsignedLong(state) * numberOfBlocks) >>> 32);
            }
        };
        Collection<BlockPosition> blocks = new HashSet<>();

        for (int chunkSectionIndex = 0; chunkSectionIndex <= maxChunkSectionIndex; chunkSectionIndex++) {
            if (chunkPacketInfoAntiXray.isWritten(chunkSectionIndex) && chunkPacketInfoAntiXray.getPredefinedObjects(chunkSectionIndex) != null) {
                int[] predefinedBlockDataBitsTemp;

                if (chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex) == ChunkSection.GLOBAL_PALETTE) {
                    predefinedBlockDataBitsTemp = engineMode == EngineMode.HIDE ? chunkPacketInfoAntiXray.getChunk().world.getWorld().getEnvironment() == Environment.NETHER ? predefinedBlockDataBitsNetherrackGlobal : chunkPacketInfoAntiXray.getChunk().world.getWorld().getEnvironment() == Environment.THE_END ? predefinedBlockDataBitsEndStoneGlobal : predefinedBlockDataBitsStoneGlobal : predefinedBlockDataBitsGlobal;
                } else {
                    // If it's this.predefinedBlockData, use this.predefinedBlockDataFull instead
                    IBlockData[] predefinedBlockDataFull = chunkPacketInfoAntiXray.getPredefinedObjects(chunkSectionIndex) == predefinedBlockData ? this.predefinedBlockDataFull : chunkPacketInfoAntiXray.getPredefinedObjects(chunkSectionIndex);
                    predefinedBlockDataBitsTemp = predefinedBlockDataBits;

                    for (int i = 0; i < predefinedBlockDataBitsTemp.length; i++) {
                        predefinedBlockDataBitsTemp[i] = chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex).getOrCreateIdFor(predefinedBlockDataFull[i]);
                    }
                }

                dataBitsWriter.setIndex(chunkPacketInfoAntiXray.getDataBitsIndex(chunkSectionIndex));

                // Check if the chunk section below was not obfuscated
                if (chunkSectionIndex == 0 || !chunkPacketInfoAntiXray.isWritten(chunkSectionIndex - 1) || chunkPacketInfoAntiXray.getPredefinedObjects(chunkSectionIndex - 1) == null) {
                    // If so, initialize some stuff
                    dataBitsReader.setBitsPerObject(chunkPacketInfoAntiXray.getBitsPerObject(chunkSectionIndex));
                    dataBitsReader.setIndex(chunkPacketInfoAntiXray.getDataBitsIndex(chunkSectionIndex));
                    solidTemp = readDataPalette(chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex), solid, solidGlobal);
                    obfuscateTemp = readDataPalette(chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex), obfuscate, obfuscateGlobal);
                    traceTemp = trace == obfuscate ? obfuscateTemp : readDataPalette(chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex), trace, traceGlobal);
                    // Read the blocks of the upper layer of the chunk section below if it exists
                    ChunkSection belowChunkSection = null;
                    boolean skipFirstLayer = chunkSectionIndex == 0 || (belowChunkSection = chunkPacketInfoAntiXray.getChunk().getSections()[chunkSectionIndex - 1]) == Chunk.EMPTY_CHUNK_SECTION;

                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            current[z][x] = true;
                            next[z][x] = skipFirstLayer || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(belowChunkSection.getType(x, 15, z))];
                            traceCache[z][x] = false;
                        }
                    }

                    // Abuse the obfuscateLayer method to read the blocks of the first layer of the current chunk section
                    dataBitsWriter.setBitsPerObject(0);
                    obfuscateLayer(chunkSectionIndex, -1, dataBitsReader, dataBitsWriter, solidTemp, obfuscateTemp, traceTemp, predefinedBlockDataBitsTemp, current, next, nextNext, traceCache, emptyNearbyChunkSections, random, blocks);
                }

                dataBitsWriter.setBitsPerObject(chunkPacketInfoAntiXray.getBitsPerObject(chunkSectionIndex));
                nearbyChunkSections[0] = chunkPacketInfoAntiXray.getNearbyChunks()[0] == null ? Chunk.EMPTY_CHUNK_SECTION : chunkPacketInfoAntiXray.getNearbyChunks()[0].getSections()[chunkSectionIndex];
                nearbyChunkSections[1] = chunkPacketInfoAntiXray.getNearbyChunks()[1] == null ? Chunk.EMPTY_CHUNK_SECTION : chunkPacketInfoAntiXray.getNearbyChunks()[1].getSections()[chunkSectionIndex];
                nearbyChunkSections[2] = chunkPacketInfoAntiXray.getNearbyChunks()[2] == null ? Chunk.EMPTY_CHUNK_SECTION : chunkPacketInfoAntiXray.getNearbyChunks()[2].getSections()[chunkSectionIndex];
                nearbyChunkSections[3] = chunkPacketInfoAntiXray.getNearbyChunks()[3] == null ? Chunk.EMPTY_CHUNK_SECTION : chunkPacketInfoAntiXray.getNearbyChunks()[3].getSections()[chunkSectionIndex];

                // Obfuscate all layers of the current chunk section except the upper one
                for (int y = 0; y < 15; y++) {
                    boolean[][] temp = current;
                    current = next;
                    next = nextNext;
                    nextNext = temp;
                    obfuscateLayer(chunkSectionIndex, y, dataBitsReader, dataBitsWriter, solidTemp, obfuscateTemp, traceTemp, predefinedBlockDataBitsTemp, current, next, nextNext, traceCache, nearbyChunkSections, random, blocks);
                }

                // Check if the chunk section above doesn't need obfuscation
                if (chunkSectionIndex == maxChunkSectionIndex || !chunkPacketInfoAntiXray.isWritten(chunkSectionIndex + 1) || chunkPacketInfoAntiXray.getPredefinedObjects(chunkSectionIndex + 1) == null) {
                    // If so, obfuscate the upper layer of the current chunk section by reading blocks of the first layer from the chunk section above if it exists
                    ChunkSection aboveChunkSection = chunkSectionIndex == 15 ? Chunk.EMPTY_CHUNK_SECTION : chunkPacketInfoAntiXray.getChunk().getSections()[chunkSectionIndex + 1];
                    boolean[][] temp = current;
                    current = next;
                    next = nextNext;
                    nextNext = temp;

                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            if (aboveChunkSection == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(aboveChunkSection.getType(x, 0, z))]) {
                                current[z][x] = true;
                            }
                        }
                    }

                    // There is nothing to read anymore
                    dataBitsReader.setBitsPerObject(0);
                    solid[0] = true;
                    obfuscateLayer(chunkSectionIndex, 15, dataBitsReader, dataBitsWriter, solid, obfuscateTemp, traceTemp, predefinedBlockDataBitsTemp, current, next, nextNext, traceCache, nearbyChunkSections, random, blocks);
                } else {
                    // If not, initialize the reader and other stuff for the chunk section above to obfuscate the upper layer of the current chunk section
                    dataBitsReader.setBitsPerObject(chunkPacketInfoAntiXray.getBitsPerObject(chunkSectionIndex + 1));
                    dataBitsReader.setIndex(chunkPacketInfoAntiXray.getDataBitsIndex(chunkSectionIndex + 1));
                    solidTemp = readDataPalette(chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex + 1), solid, solidGlobal);
                    obfuscateTemp = readDataPalette(chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex + 1), obfuscate, obfuscateGlobal);
                    traceTemp = trace == obfuscate ? obfuscateTemp : readDataPalette(chunkPacketInfoAntiXray.getDataPalette(chunkSectionIndex + 1), trace, traceGlobal);
                    boolean[][] temp = current;
                    current = next;
                    next = nextNext;
                    nextNext = temp;
                    obfuscateLayer(chunkSectionIndex, 15, dataBitsReader, dataBitsWriter, solidTemp, obfuscateTemp, traceTemp, predefinedBlockDataBitsTemp, current, next, nextNext, traceCache, nearbyChunkSections, random, blocks);
                }

                dataBitsWriter.finish();
            }
        }

        if (plugin.isRunning()) {
            plugin.getPacketChunkBlocksCache().put(chunkPacketInfoAntiXray.getPacketPlayOutMapChunk(), new ChunkBlocks(chunkPacketInfoAntiXray.getChunk(), blocks));
        }

        chunkPacketInfoAntiXray.getPacketPlayOutMapChunk().setReady(true);
    }

    private void obfuscateLayer(int chunkSectionIndex, int y, DataBitsReader dataBitsReader, DataBitsWriter dataBitsWriter, boolean[] solid, boolean[] obfuscate, boolean[] trace, int[] predefinedBlockDataBits, boolean[][] current, boolean[][] next, boolean[][] nextNext, boolean[][] traceCache, ChunkSection[] nearbyChunkSections, IntSupplier random, Collection<? super BlockPosition> blocks) {
        int realY = (chunkSectionIndex << 4) + y;
        // First block of first line
        int dataBits = dataBitsReader.read();

        if (nextNext[0][0] = !solid[dataBits]) {
            if (traceCache[0][0] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                blocks.add(new BlockPosition(0, realY, 0));
            } else {
                dataBitsWriter.skip();
            }

            next[0][1] = true;
            next[1][0] = true;
        } else {
            if (nearbyChunkSections[2] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[2].getType(0, y, 15))] || nearbyChunkSections[0] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[0].getType(15, y, 0))] || current[0][0]) {
                if (traceCache[0][0] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(0, realY, 0));
                } else {
                    dataBitsWriter.skip();
                }
            } else {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
            }
        }

        if (trace[dataBits]) {
            traceCache[0][0] = true;
        } else {
            traceCache[0][0] = false;

            if (!obfuscate[dataBits]) {
                next[0][0] = true;
            }
        }

        // First line
        for (int x = 1; x < 15; x++) {
            dataBits = dataBitsReader.read();

            if (nextNext[0][x] = !solid[dataBits]) {
                if (traceCache[0][x] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(x, realY, 0));
                } else {
                    dataBitsWriter.skip();
                }

                next[0][x - 1] = true;
                next[0][x + 1] = true;
                next[1][x] = true;
            } else {
                if (nearbyChunkSections[2] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[2].getType(x, y, 15))] || current[0][x]) {
                    if (traceCache[0][x] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                        dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                        blocks.add(new BlockPosition(x, realY, 0));
                    } else {
                        dataBitsWriter.skip();
                    }
                } else {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
                }
            }

            if (trace[dataBits]) {
                traceCache[0][x] = true;
            } else {
                traceCache[0][x] = false;

                if (!obfuscate[dataBits]) {
                    next[0][x] = true;
                }
            }
        }

        // Last block of first line
        dataBits = dataBitsReader.read();

        if (nextNext[0][15] = !solid[dataBits]) {
            if (traceCache[0][15] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                blocks.add(new BlockPosition(15, realY, 0));
            } else {
                dataBitsWriter.skip();
            }

            next[0][14] = true;
            next[1][15] = true;
        } else {
            if (nearbyChunkSections[2] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[2].getType(15, y, 15))] || nearbyChunkSections[1] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[1].getType(0, y, 0))] || current[0][15]) {
                if (traceCache[0][15] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(15, realY, 0));
                } else {
                    dataBitsWriter.skip();
                }
            } else {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
            }
        }

        if (trace[dataBits]) {
            traceCache[0][15] = true;
        } else {
            traceCache[0][15] = false;

            if (!obfuscate[dataBits]) {
                next[0][15] = true;
            }
        }

        // All inner lines
        for (int z = 1; z < 15; z++) {
            // First block
            dataBits = dataBitsReader.read();

            if (nextNext[z][0] = !solid[dataBits]) {
                if (traceCache[z][0] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(0, realY, z));
                } else {
                    dataBitsWriter.skip();
                }

                next[z][1] = true;
                next[z - 1][0] = true;
                next[z + 1][0] = true;
            } else {
                if (nearbyChunkSections[0] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[0].getType(15, y, z))] || current[z][0]) {
                    if (traceCache[z][0] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                        dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                        blocks.add(new BlockPosition(0, realY, z));
                    } else {
                        dataBitsWriter.skip();
                    }
                } else {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
                }
            }

            if (trace[dataBits]) {
                traceCache[z][0] = true;
            } else {
                traceCache[z][0] = false;

                if (!obfuscate[dataBits]) {
                    next[z][0] = true;
                }
            }

            // All inner blocks
            for (int x = 1; x < 15; x++) {
                dataBits = dataBitsReader.read();

                if (nextNext[z][x] = !solid[dataBits]) {
                    if (traceCache[z][x] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                        dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                        blocks.add(new BlockPosition(x, realY, z));
                    } else {
                        dataBitsWriter.skip();
                    }

                    next[z][x - 1] = true;
                    next[z][x + 1] = true;
                    next[z - 1][x] = true;
                    next[z + 1][x] = true;
                } else {
                    if (current[z][x]) {
                        if (traceCache[z][x] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                            dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                            blocks.add(new BlockPosition(x, realY, z));
                        } else {
                            dataBitsWriter.skip();
                        }
                    } else {
                        dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
                    }
                }

                if (trace[dataBits]) {
                    traceCache[z][x] = true;
                } else {
                    traceCache[z][x] = false;

                    if (!obfuscate[dataBits]) {
                        next[z][x] = true;
                    }
                }
            }

            // Last block
            dataBits = dataBitsReader.read();

            if (nextNext[z][15] = !solid[dataBits]) {
                if (traceCache[z][15] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(15, realY, z));
                } else {
                    dataBitsWriter.skip();
                }

                next[z][14] = true;
                next[z - 1][15] = true;
                next[z + 1][15] = true;
            } else {
                if (nearbyChunkSections[1] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[1].getType(0, y, z))] || current[z][15]) {
                    if (traceCache[z][15] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                        dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                        blocks.add(new BlockPosition(15, realY, z));
                    } else {
                        dataBitsWriter.skip();
                    }
                } else {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
                }
            }

            if (trace[dataBits]) {
                traceCache[z][15] = true;
            } else {
                traceCache[z][15] = false;

                if (!obfuscate[dataBits]) {
                    next[z][15] = true;
                }
            }
        }

        // First block of last line
        dataBits = dataBitsReader.read();

        if (nextNext[15][0] = !solid[dataBits]) {
            if (traceCache[15][0] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                blocks.add(new BlockPosition(0, realY, 15));
            } else {
                dataBitsWriter.skip();
            }

            next[15][1] = true;
            next[14][0] = true;
        } else {
            if (nearbyChunkSections[3] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[3].getType(0, y, 0))] || nearbyChunkSections[0] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[0].getType(15, y, 15))] || current[15][0]) {
                if (traceCache[15][0] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(0, realY, 15));
                } else {
                    dataBitsWriter.skip();
                }
            } else {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
            }
        }

        if (trace[dataBits]) {
            traceCache[15][0] = true;
        } else {
            traceCache[15][0] = false;

            if (!obfuscate[dataBits]) {
                next[15][0] = true;
            }
        }

        // Last line
        for (int x = 1; x < 15; x++) {
            dataBits = dataBitsReader.read();

            if (nextNext[15][x] = !solid[dataBits]) {
                if (traceCache[15][x] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(x, realY, 15));
                } else {
                    dataBitsWriter.skip();
                }

                next[15][x - 1] = true;
                next[15][x + 1] = true;
                next[14][x] = true;
            } else {
                if (nearbyChunkSections[3] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[3].getType(x, y, 0))] || current[15][x]) {
                    if (traceCache[15][x] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                        dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                        blocks.add(new BlockPosition(x, realY, 15));
                    } else {
                        dataBitsWriter.skip();
                    }
                } else {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
                }
            }

            if (trace[dataBits]) {
                traceCache[15][x] = true;
            } else {
                traceCache[15][x] = false;

                if (!obfuscate[dataBits]) {
                    next[15][x] = true;
                }
            }
        }

        // Last block of last line
        dataBits = dataBitsReader.read();

        if (nextNext[15][15] = !solid[dataBits]) {
            if (traceCache[15][15] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                blocks.add(new BlockPosition(15, realY, 15));
            } else {
                dataBitsWriter.skip();
            }

            next[15][14] = true;
            next[14][15] = true;
        } else {
            if (nearbyChunkSections[3] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[3].getType(15, y, 0))] || nearbyChunkSections[1] == Chunk.EMPTY_CHUNK_SECTION || !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(nearbyChunkSections[1].getType(0, y, 15))] || current[15][15]) {
                if (traceCache[15][15] && blocks.size() < maxRayTraceBlockCountPerChunk) {
                    dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Exposed to air
                    blocks.add(new BlockPosition(15, realY, 15));
                } else {
                    dataBitsWriter.skip();
                }
            } else {
                dataBitsWriter.write(predefinedBlockDataBits[random.getAsInt()]); // Not exposed to air
            }
        }

        if (trace[dataBits]) {
            traceCache[15][15] = true;
        } else {
            traceCache[15][15] = false;

            if (!obfuscate[dataBits]) {
                next[15][15] = true;
            }
        }
    }

    private boolean[] readDataPalette(DataPalette<IBlockData> dataPalette, boolean[] temp, boolean[] global) {
        if (dataPalette == ChunkSection.GLOBAL_PALETTE) {
            return global;
        }

        IBlockData blockData;

        for (int i = 0; (blockData = dataPalette.getObject(i)) != null; i++) {
            temp[i] = global[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)];
        }

        return temp;
    }

    @Override
    public void onBlockChange(World world, BlockPosition blockPosition, IBlockData newBlockData, IBlockData oldBlockData, int flag) {
        if (oldBlockData != null && solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(oldBlockData)] && !solidGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(newBlockData)] && blockPosition.getY() <= maxBlockYUpdatePosition) {
            updateNearbyBlocks(world, blockPosition);
        }
    }

    @Override
    public void onPlayerLeftClickBlock(PlayerInteractManager playerInteractManager, BlockPosition blockPosition, EnumDirection enumDirection) {
        if (blockPosition.getY() <= maxBlockYUpdatePosition) {
            updateNearbyBlocks(playerInteractManager.world, blockPosition);
        }
    }

    private void updateNearbyBlocks(World world, BlockPosition blockPosition) {
        if (updateRadius >= 2) {
            BlockPosition temp = blockPosition.west();
            updateBlock(world, temp);
            updateBlock(world, temp.west());
            updateBlock(world, temp.down());
            updateBlock(world, temp.up());
            updateBlock(world, temp.north());
            updateBlock(world, temp.south());
            updateBlock(world, temp = blockPosition.east());
            updateBlock(world, temp.east());
            updateBlock(world, temp.down());
            updateBlock(world, temp.up());
            updateBlock(world, temp.north());
            updateBlock(world, temp.south());
            updateBlock(world, temp = blockPosition.down());
            updateBlock(world, temp.down());
            updateBlock(world, temp.north());
            updateBlock(world, temp.south());
            updateBlock(world, temp = blockPosition.up());
            updateBlock(world, temp.up());
            updateBlock(world, temp.north());
            updateBlock(world, temp.south());
            updateBlock(world, temp = blockPosition.north());
            updateBlock(world, temp.north());
            updateBlock(world, temp = blockPosition.south());
            updateBlock(world, temp.south());
        } else if (updateRadius == 1) {
            updateBlock(world, blockPosition.west());
            updateBlock(world, blockPosition.east());
            updateBlock(world, blockPosition.down());
            updateBlock(world, blockPosition.up());
            updateBlock(world, blockPosition.north());
            updateBlock(world, blockPosition.south());
        } else {
            // Do nothing if updateRadius <= 0 (test mode)
        }
    }

    public void updateBlock(World world, BlockPosition blockPosition) {
        IBlockData blockData = world.getTypeIfLoaded(blockPosition);

        if (blockData != null && obfuscateGlobal[ChunkSection.GLOBAL_PALETTE.getOrCreateIdFor(blockData)]) {
            // world.notify(blockPosition, blockData, blockData, 3);
            ((WorldServer)world).getChunkProvider().flagDirty(blockPosition); // We only need to re-send to client
        }
    }
}
