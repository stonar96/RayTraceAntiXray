package com.vanillage.raytraceantixray.listeners;

import java.lang.reflect.Field;

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
            try {
                Field chunkPacketBlockController = World.class.getDeclaredField("chunkPacketBlockController");
                chunkPacketBlockController.setAccessible(true);
                chunkPacketBlockController.set(((CraftWorld) event.getWorld()).getHandle(), new ChunkPacketBlockControllerAntiXray(plugin, ((CraftWorld) event.getWorld()).getHandle(), MinecraftServer.getServer().executorService));
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
