package com.vanillage.raytraceantixray.listeners;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.LongWrapper;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.tasks.RayTraceCallable;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PacketListener extends PacketAdapter {
    private final RayTraceAntiXray plugin;

    public PacketListener(RayTraceAntiXray plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.UNLOAD_CHUNK, PacketType.Play.Server.RESPAWN);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketType packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.MAP_CHUNK) {
            // A player data instance is always bound to a world and defines what is to be calculated.
            // Apart from the join and quit event, this is the only place that defines the world of a player by renewing the player data instance.
            // In principle, we could (additionally) renew the player data instance anywhere else if we detect a world change (e.g. move event, changed world event, ...).
            // However, Anti-Xray specifies what is to be calculated via the chunk packet.
            // Since Anti-Xray is async, chunk packets can be delayed.
            // (The packet order of all packets is still being preserved and consistent.)
            // This could for example lead to the following order of events:
            // (1) Chunk packet event of world A.
            // (2) Changed world event from world A to B.
            // (3) Chunk packet event of world A.
            // (4) Chunk packet event of world B.
            // This would lead to unnecessarily renewing the player data instance multiple times in the worst case.
            // Therefore, we only renew the player data instance here.
            // For similar reasons, we also handle chunk unloads via packet events below.
            // Everywhere else we have to check if the player's world still matches the world of the player data instance before we use it.
            // (See for example the move event.)
            // Get the result from Anti-Xray for the current chunk packet.
            // We can't remove the entry because the same chunk packet can be sent to multiple players.
            // The garbage collector will remove the entry later since we're using a weak key map.
            ChunkBlocks chunkBlocks = plugin.getPacketChunkBlocksCache().get(event.getPacket().getHandle());

            if (chunkBlocks == null) {
                // RayTraceAntiXray is probably not enabled in this world (or other plugins bypass Anti-Xray).
                // We can't determine the world from the chunk packet in this case.
                // Thus we use the player's current (more up to date) world instead.
                Player player = event.getPlayer();
                Location location = player.getEyeLocation();
                ConcurrentMap<UUID, PlayerData> playerDataMap = plugin.getPlayerData();
                UUID uniqueId = player.getUniqueId();

                if (!location.getWorld().equals(playerDataMap.get(uniqueId).getLocations()[0].getWorld())) {
                    // Detected a world change.
                    // In the event order listing above, this corresponds to (4) when RayTraceAntiXray is disabled in world B.
                    // The player's current world is world B since (2).
                    PlayerData playerData = new PlayerData(RayTraceAntiXray.getLocations(player, new VectorialLocation(location)));
                    playerData.setCallable(new RayTraceCallable(plugin, playerData));
                    playerDataMap.put(uniqueId, playerData);
                }

                return;
            }

            // Get chunk from weak reference.
            LevelChunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                // The chunk has already been unloaded and garbage collected.
                // A chunk unload packet will probably follow.
                // We can ignore this chunk packet.
                return;
            }

            CraftWorld world = chunk.getLevel().getWorld();
            ConcurrentMap<UUID, PlayerData> playerDataMap = plugin.getPlayerData();
            Player player = event.getPlayer();
            UUID uniqueId = player.getUniqueId();
            PlayerData playerData = playerDataMap.get(uniqueId);

            if (!world.equals(playerData.getLocations()[0].getWorld())) {
                // Detected a world change.
                // We need the player's current location to construct a new player data instance.
                Location location = player.getEyeLocation();

                if (!world.equals(location.getWorld())) {
                    // The player has changed the world again since this chunk packet was sent.
                    // (As described above, packets can be delayed.)
                    // Example event order for this case:
                    // (1) Chunk packet event of world A.
                    // (2) Changed world event from world A to B.
                    // (3) Changed world event from world B to C.
                    // (4) Chunk packet event of world B.
                    // (5) Chunk packet event of world C.
                    // The previous chunk packet was from world A in (1).
                    // The current chunk packet is from world B in (4) but the player is already in world C.
                    // We can ignore this chunk packet and wait until we get a chunk packet from world C in (5).
                    return;
                }

                // Renew the player data instance.
                playerData = new PlayerData(RayTraceAntiXray.getLocations(player, new VectorialLocation(location)));
                playerData.setCallable(new RayTraceCallable(plugin, playerData));
                playerDataMap.put(uniqueId, playerData);
            }

            // We need to copy the chunk blocks because the same chunk packet could have been sent to multiple players.
            chunkBlocks = new ChunkBlocks(chunk, new HashMap<>(chunkBlocks.getBlocks()));
            playerData.getChunks().put(chunkBlocks.getKey(), chunkBlocks);
        } else if (packetType == PacketType.Play.Server.UNLOAD_CHUNK) {
            // Note that chunk unload packets aren't sent on world change and on respawn.
            // World changes are already handled above.
            // Technically removing chunks isn't necessary since we're using a weak reference to the chunk.
            ChunkCoordIntPair chunkCoordIntPair = event.getPacket().getChunkCoordIntPairs().read(0);
            plugin.getPlayerData().get(event.getPlayer().getUniqueId()).getChunks().remove(new LongWrapper(ChunkPos.asLong(chunkCoordIntPair.getChunkX(), chunkCoordIntPair.getChunkZ())));
        } else if (packetType == PacketType.Play.Server.RESPAWN) {
            // As with world changes, chunk unload packets aren't sent on respawn.
            // All required chunks are (re)sent afterwards.
            // Thus we clear the chunks.
            // Technically this isn't necessary since we're using a weak reference to the chunk.
            // If respawning involves a world change, it will be handled in the next chunk packet event.
            plugin.getPlayerData().get(event.getPlayer().getUniqueId()).getChunks().clear();
        }
    }
}
