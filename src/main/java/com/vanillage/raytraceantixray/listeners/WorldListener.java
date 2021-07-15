package com.vanillage.raytraceantixray.listeners;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;

import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.World;

public final class WorldListener implements Listener {
    private final RayTraceAntiXray plugin;

    public WorldListener(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        if (plugin.isEnabled(event.getWorld())) {
            List<String> toTrace = plugin.getConfig().getList("world-settings." + event.getWorld().getName() + ".anti-xray.ray-trace-blocks", plugin.getConfig().getList("world-settings.default.anti-xray.ray-trace-blocks")).stream().filter(o -> o != null).map(String::valueOf).collect(Collectors.toList());

            try {
                Field chunkPacketBlockController = World.class.getDeclaredField("chunkPacketBlockController");
                chunkPacketBlockController.setAccessible(true);
                chunkPacketBlockController.set(((CraftWorld) event.getWorld()).getHandle(), new ChunkPacketBlockControllerAntiXray(plugin, plugin.getConfig().getInt("world-settings." + event.getWorld().getName() + ".anti-xray.max-ray-trace-block-count-per-chunk", plugin.getConfig().getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk")), toTrace.isEmpty() ? null : toTrace, ((CraftWorld) event.getWorld()).getHandle(), MinecraftServer.getServer().executorService));
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
