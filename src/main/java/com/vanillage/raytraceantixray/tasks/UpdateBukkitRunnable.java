package com.vanillage.raytraceantixray.tasks;

import java.util.Queue;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;

import com.destroystokyo.paper.antixray.ChunkPacketBlockController;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.PlayerData;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.World;

public final class UpdateBukkitRunnable extends BukkitRunnable {
    private final RayTraceAntiXray plugin;

    public UpdateBukkitRunnable(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Entry<UUID, PlayerData> entry : plugin.getPlayerData().entrySet()) {
            PlayerData playerData = entry.getValue();
            World world = ((CraftWorld) playerData.getLocation().getWorld()).getHandle();
            ChunkPacketBlockController chunkPacketBlockController = world.chunkPacketBlockController;

            if (chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray) {
                Queue<BlockPosition> result = playerData.getResult();

                for (BlockPosition block = result.poll(); block != null; block = result.poll()) {
                    ((ChunkPacketBlockControllerAntiXray) chunkPacketBlockController).updateBlock(world, block);
                }
            }
        }
    }
}
