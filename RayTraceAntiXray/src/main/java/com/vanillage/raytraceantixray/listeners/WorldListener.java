package com.vanillage.raytraceantixray.listeners;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public final class WorldListener implements Listener {
    private final RayTraceAntiXray plugin;

    public WorldListener(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldInit(WorldInitEvent event) {
        if (plugin.isEnabled(event.getWorld())) {
            plugin.getThirdPersonByWorldId().put(event.getWorld().getUID(), plugin.getConfig().getBoolean("world-settings.default.anti-xray.ray-trace-third-person"));
            if (plugin.getConfig().contains("world-settings." + event.getWorld().getName() + ".anti-xray.ray-trace-third-person")) {
                plugin.getThirdPersonByWorldId().put(event.getWorld().getUID(), plugin.getConfig().getBoolean("world-settings." + event.getWorld().getName() + ".anti-xray.ray-trace-third-person"));
            }

            List<String> toTrace = plugin.getConfig().getList("world-settings." + event.getWorld().getName() + ".anti-xray.ray-trace-blocks", plugin.getConfig().getList("world-settings.default.anti-xray.ray-trace-blocks")).stream().filter(o -> o != null).map(String::valueOf).collect(Collectors.toList());

            try {
                Field chunkPacketBlockController = Level.class.getDeclaredField("chunkPacketBlockController");
                chunkPacketBlockController.setAccessible(true);
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                Unsafe unsafe = (Unsafe) theUnsafe.get(null);
                unsafe.putObject(((CraftWorld) event.getWorld()).getHandle(), unsafe.objectFieldOffset(chunkPacketBlockController), new ChunkPacketBlockControllerAntiXray(plugin, plugin.getConfig().getInt("world-settings." + event.getWorld().getName() + ".anti-xray.max-ray-trace-block-count-per-chunk", plugin.getConfig().getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk")), toTrace.isEmpty() ? null : toTrace, ((CraftWorld) event.getWorld()).getHandle(), MinecraftServer.getServer().executor));
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        plugin.getThirdPersonByWorldId().remove(e.getWorld().getUID());
    }

}
