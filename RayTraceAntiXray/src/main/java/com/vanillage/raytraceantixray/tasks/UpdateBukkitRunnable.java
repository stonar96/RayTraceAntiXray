package com.vanillage.raytraceantixray.tasks;

import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.Result;

public final class UpdateBukkitRunnable extends BukkitRunnable {
    private final RayTraceAntiXray plugin;

    public UpdateBukkitRunnable(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            PlayerData playerData = plugin.getPlayerData().get(p.getUniqueId());
            World world = playerData.getLocations()[0].getWorld();

            if (!p.getWorld().equals(world)) {
                return;
            }

            Queue<Result> results = playerData.getResults();

            for (Result result = results.poll(); result != null; result = results.poll()) {
                int x = result.getX();
                int z = result.getZ();

                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                int y = result.getY();
                BlockData blockData;

                if (result.isVisible()) {
                    blockData = world.getBlockData(x, y, z);
                } else if (world.getEnvironment() == Environment.NETHER) {
                    blockData = Material.NETHERRACK.createBlockData();
                } else if (world.getEnvironment() == Environment.THE_END) {
                    blockData = Material.END_STONE.createBlockData();
                } else if (y < 0) {
                    blockData = Material.DEEPSLATE.createBlockData();
                } else {
                    blockData = Material.STONE.createBlockData();
                }

                p.sendBlockChange(new Location(world, x, y, z), blockData);
            }
        });
    }
}
