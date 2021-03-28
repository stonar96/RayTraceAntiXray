package com.vanillage.raytraceantixray;

import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.destroystokyo.paper.antixray.ChunkPacketBlockControllerAntiXray.EngineMode;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.listeners.PacketListener;
import com.vanillage.raytraceantixray.listeners.PlayerListener;
import com.vanillage.raytraceantixray.listeners.WorldListener;
import com.vanillage.raytraceantixray.tasks.RayTraceTimerTask;
import com.vanillage.raytraceantixray.tasks.UpdateBukkitRunnable;

import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;

public final class RayTraceAntiXray extends JavaPlugin {
    private volatile boolean running = false;
    private final Map<PacketPlayOutMapChunk, ChunkBlocks> packetChunkBlocksCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private Timer timer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        // saveConfig();
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this));
        running = true;
        timer = new Timer(true);
        timer.schedule(new RayTraceTimerTask(this), 0L, Math.max(getConfig().getLong("settings.anti-xray.ms-per-ray-trace-tick"), 1L));
        new UpdateBukkitRunnable(this).runTaskTimer(this, 0L, Math.max(getConfig().getLong("settings.anti-xray.update-ticks"), 1L));
        getLogger().info(getDescription().getFullName() + " enabled");
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        running = false;
        timer.cancel();
        packetChunkBlocksCache.clear();
        playerData.clear();
        getLogger().info(getDescription().getFullName() + " disabled");
    }

    public boolean isRunning() {
        return running;
    }

    public Map<PacketPlayOutMapChunk, ChunkBlocks> getPacketChunkBlocksCache() {
        return packetChunkBlocksCache;
    }

    public Map<UUID, PlayerData> getPlayerData() {
        return playerData;
    }

    public boolean isEnabled(World world) {
        return ((CraftWorld) world).getHandle().paperConfig.antiXray && ((CraftWorld) world).getHandle().paperConfig.engineMode == EngineMode.HIDE && getConfig().getBoolean("world-settings." + world.getName() + ".anti-xray.ray-trace", getConfig().getBoolean("world-settings.default.anti-xray.ray-trace"));
    }
}
