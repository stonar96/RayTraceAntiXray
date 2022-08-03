package com.vanillage.raytraceantixray.tasks;

import java.util.Queue;

import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;

import com.destroystokyo.paper.antixray.ChunkPacketBlockController;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.PlayerData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class UpdateBukkitRunnable extends BukkitRunnable {
    private final RayTraceAntiXray plugin;

    public UpdateBukkitRunnable(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (PlayerData playerData : plugin.getPlayerData().values()) {
            Level level = ((CraftWorld) playerData.getLocations().get(0).getWorld()).getHandle();
            ChunkPacketBlockController chunkPacketBlockController = level.chunkPacketBlockController;

            if (chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray) {
                Queue<BlockPos> result = playerData.getResult();

                for (BlockPos block = result.poll(); block != null; block = result.poll()) {
                    ((ChunkPacketBlockControllerAntiXray) chunkPacketBlockController).updateBlock(level, block);
                }
            }
        }
    }
}
