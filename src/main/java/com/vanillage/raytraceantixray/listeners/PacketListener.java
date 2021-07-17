package com.vanillage.raytraceantixray.listeners;

import org.bukkit.Location;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;

import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;

public final class PacketListener extends PacketAdapter {
    private final RayTraceAntiXray plugin;

    public PacketListener(RayTraceAntiXray plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.UNLOAD_CHUNK);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {
            PlayerData playerData = plugin.getPlayerData().get(event.getPlayer().getUniqueId());
            ChunkBlocks chunkBlocks = plugin.getPacketChunkBlocksCache().remove(event.getPacket().getHandle());

            if (chunkBlocks == null) {
                Location location = event.getPlayer().getEyeLocation();

                if (!location.getWorld().equals(playerData.getLocations().get(0).getWorld())) {
                    playerData = new PlayerData(plugin.getLocations(event.getPlayer(), location));
                    plugin.getPlayerData().put(event.getPlayer().getUniqueId(), playerData);
                }

                // Not enabled in this world.
                return;
            }

            if (!chunkBlocks.getChunk().getWorld().getWorld().equals(playerData.getLocations().get(0).getWorld())) {
                Location location = event.getPlayer().getEyeLocation();

                if (!chunkBlocks.getChunk().getWorld().getWorld().equals(location.getWorld())) {
                    // (Chunk) packets can be delayed.
                    // If the worlds don't match, the player is already in another world.
                    // The packet can be ignored.
                    return;
                }

                playerData = new PlayerData(plugin.getLocations(event.getPlayer(), location));
                plugin.getPlayerData().put(event.getPlayer().getUniqueId(), playerData);
            }

            playerData.getChunks().put(chunkBlocks.getChunk().getPos(), chunkBlocks);
        } else if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            StructureModifier<Integer> integers = event.getPacket().getIntegers();
            plugin.getPlayerData().get(event.getPlayer().getUniqueId()).getChunks().remove(new ChunkCoordIntPair(integers.read(0), integers.read(1)));
        }
    }
}
