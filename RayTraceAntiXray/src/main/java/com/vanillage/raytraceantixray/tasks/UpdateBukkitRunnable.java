package com.vanillage.raytraceantixray.tasks;

import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.Result;

import net.minecraft.core.BlockPos;

public final class UpdateBukkitRunnable extends BukkitRunnable {
    private final RayTraceAntiXray plugin;

    public UpdateBukkitRunnable(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Entry<UUID, PlayerData> entry : plugin.getPlayerData().entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());

            if (player == null) {
                continue;
            }

            PlayerData playerData = entry.getValue();
            World world = playerData.getLocations().get(0).getWorld();

            if (!player.getWorld().equals(world)) {
                continue;
            }

            Queue<Result> resultQueue = playerData.getResultQueue();

            for (Result result = resultQueue.poll(); result != null; result = resultQueue.poll()) {
                BlockPos resultBlock = result.getBlock();
                Block block = world.getBlockAt(resultBlock.getX(), resultBlock.getY(), resultBlock.getZ());

                if (!block.getChunk().isLoaded()) {
                    continue;
                }

                BlockData blockData;

                if (result.isVisible()) {
                    blockData = block.getBlockData();
                } else if (world.getEnvironment() == Environment.NETHER) {
                    blockData = Material.NETHERRACK.createBlockData();
                } else if (world.getEnvironment() == Environment.THE_END) {
                    blockData = Material.END_STONE.createBlockData();
                } else if (block.getY() < 0) {
                    blockData = Material.DEEPSLATE.createBlockData();
                } else {
                    blockData = Material.STONE.createBlockData();
                }

                player.sendBlockChange(block.getLocation(), blockData);
            }
        }
    }
}
