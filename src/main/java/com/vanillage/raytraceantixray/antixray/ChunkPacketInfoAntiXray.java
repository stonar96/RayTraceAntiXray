package com.vanillage.raytraceantixray.antixray;

import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_16_R3.IBlockData;

import com.destroystokyo.paper.antixray.ChunkPacketInfo;

import net.minecraft.server.v1_16_R3.Chunk;

public final class ChunkPacketInfoAntiXray extends ChunkPacketInfo<IBlockData> implements Runnable {

    private Chunk[] nearbyChunks;
    private final ChunkPacketBlockControllerAntiXray chunkPacketBlockControllerAntiXray;

    public ChunkPacketInfoAntiXray(PacketPlayOutMapChunk packetPlayOutMapChunk, Chunk chunk, int chunkSectionSelector,
                                   ChunkPacketBlockControllerAntiXray chunkPacketBlockControllerAntiXray) {
        super(packetPlayOutMapChunk, chunk, chunkSectionSelector);
        this.chunkPacketBlockControllerAntiXray = chunkPacketBlockControllerAntiXray;
    }

    public Chunk[] getNearbyChunks() {
        return nearbyChunks;
    }

    public void setNearbyChunks(Chunk... nearbyChunks) {
        this.nearbyChunks = nearbyChunks;
    }

    @Override
    public void run() {
        chunkPacketBlockControllerAntiXray.obfuscate(this);
    }
}
