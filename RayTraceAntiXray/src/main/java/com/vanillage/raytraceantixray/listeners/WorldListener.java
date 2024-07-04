package com.vanillage.raytraceantixray.listeners;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class WorldListener implements Listener {
    private final RayTraceAntiXray rayTraceAntiXray;

    public WorldListener(RayTraceAntiXray rayTraceAntiXray) {
        this.rayTraceAntiXray = rayTraceAntiXray;
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();

        if (rayTraceAntiXray.isEnabled(world)) {
            Configuration config = rayTraceAntiXray.getConfig();
            String worldName = world.getName();
            boolean rayTraceThirdPerson = config.getBoolean("world-settings." + worldName + ".anti-xray.ray-trace-third-person", config.getBoolean("world-settings.default.anti-xray.ray-trace-third-person"));
            double rayTraceDistance = Math.max(config.getDouble("world-settings." + worldName + ".anti-xray.ray-trace-distance", config.getDouble("world-settings.default.anti-xray.ray-trace-distance")), 0.);
            boolean rehideBlocks = config.getBoolean("world-settings." + worldName + ".anti-xray.rehide-blocks", config.getBoolean("world-settings.default.anti-xray.rehide-blocks"));
            double rehideDistance = Math.max(config.getDouble("world-settings." + worldName + ".anti-xray.rehide-distance", config.getDouble("world-settings.default.anti-xray.rehide-distance")), 0.);
            int maxRayTraceBlockCountPerChunk = Math.max(config.getInt("world-settings." + worldName + ".anti-xray.max-ray-trace-block-count-per-chunk", config.getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk")), 0);
            List<String> rayTraceBlocks = config.getList("world-settings." + worldName + ".anti-xray.ray-trace-blocks", config.getList("world-settings.default.anti-xray.ray-trace-blocks")).stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
            ServerLevel serverLevel = ((CraftWorld) world).getHandle();
            ChunkPacketBlockControllerAntiXray controller = new ChunkPacketBlockControllerAntiXray(rayTraceAntiXray, serverLevel.chunkPacketBlockController, rayTraceThirdPerson, rayTraceDistance, rehideBlocks, rehideDistance, maxRayTraceBlockCountPerChunk, rayTraceBlocks.isEmpty() ? null : rayTraceBlocks, serverLevel, MinecraftServer.getServer().executor);

            try {
                Field field = Level.class.getDeclaredField("chunkPacketBlockController");
                field.setAccessible(true);
                field.set(serverLevel, controller);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
