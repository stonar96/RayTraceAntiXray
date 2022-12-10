package com.vanillage.raytraceantixray.listeners;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import sun.misc.Unsafe;

public final class WorldListener implements Listener {
    private final RayTraceAntiXray plugin;

    public WorldListener(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        if (plugin.isEnabled(event.getWorld())) {
            boolean rayTraceThirdPerson = plugin.getConfig().getBoolean("world-settings." + event.getWorld().getName() + ".anti-xray.ray-trace-third-person", plugin.getConfig().getBoolean("world-settings.default.anti-xray.ray-trace-third-person"));
            double rayTraceDistance = Math.max(plugin.getConfig().getDouble("world-settings." + event.getWorld().getName() + ".anti-xray.ray-trace-distance", plugin.getConfig().getDouble("world-settings.default.anti-xray.ray-trace-distance")), 0.);
            int maxRayTraceBlockCountPerChunk = Math.max(plugin.getConfig().getInt("world-settings." + event.getWorld().getName() + ".anti-xray.max-ray-trace-block-count-per-chunk", plugin.getConfig().getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk")), 0);
            List<String> rayTraceBlocks = plugin.getConfig().getList("world-settings." + event.getWorld().getName() + ".anti-xray.ray-trace-blocks", plugin.getConfig().getList("world-settings.default.anti-xray.ray-trace-blocks")).stream().filter(o -> o != null).map(String::valueOf).collect(Collectors.toList());

            try {
                Field chunkPacketBlockController = Level.class.getDeclaredField("chunkPacketBlockController");
                chunkPacketBlockController.setAccessible(true);
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                Unsafe unsafe = (Unsafe) theUnsafe.get(null);
                unsafe.putObject(((CraftWorld) event.getWorld()).getHandle(), unsafe.objectFieldOffset(chunkPacketBlockController), new ChunkPacketBlockControllerAntiXray(plugin, rayTraceThirdPerson, rayTraceDistance, maxRayTraceBlockCountPerChunk, rayTraceBlocks.isEmpty() ? null : rayTraceBlocks, ((CraftWorld) event.getWorld()).getHandle(), MinecraftServer.getServer().executor));
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
