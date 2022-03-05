package com.vanillage.raytraceantixray;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.comphenix.protocol.ProtocolLibrary;
import com.destroystokyo.paper.antixray.ChunkPacketBlockControllerAntiXray.EngineMode;
import com.google.common.collect.MapMaker;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.listeners.PacketListener;
import com.vanillage.raytraceantixray.listeners.PlayerListener;
import com.vanillage.raytraceantixray.listeners.WorldListener;
import com.vanillage.raytraceantixray.tasks.ScheduledRayTraceRunnable;
import com.vanillage.raytraceantixray.tasks.UpdateBukkitRunnable;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RayTraceAntiXray extends JavaPlugin {
    private volatile boolean running = false;
    private final Map<ClientboundLevelChunkPacket, ChunkBlocks> packetChunkBlocksCache = new MapMaker().weakKeys().makeMap();
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    @Override
    public void onEnable() {
        // saveResource("README.txt", false);
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        // saveConfig();
        // Initialize stuff.
        running = true;
        executorService = Executors.newFixedThreadPool(Math.max(getConfig().getInt("settings.anti-xray.ray-trace-threads"), 1));
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new ScheduledRayTraceRunnable(this), 0L, Math.max(getConfig().getLong("settings.anti-xray.ms-per-ray-trace-tick"), 1L), TimeUnit.MILLISECONDS);
        new UpdateBukkitRunnable(this).runTaskTimer(this, 0L, Math.max(getConfig().getLong("settings.anti-xray.update-ticks"), 1L));
        // Register events.
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this));
        // registerCommands();
        getLogger().info(getDescription().getFullName() + " enabled");
    }

    @Override
    public void onDisable() {
        // unregisterCommands();
        // Cleanup stuff.
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        running = false;
        scheduledExecutorService.shutdownNow();

        try {
            scheduledExecutorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        executorService.shutdownNow();

        try {
            executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        packetChunkBlocksCache.clear();
        playerData.clear();
        getLogger().info(getDescription().getFullName() + " disabled");
    }

    /* public void onReload() {
        // Cleanup stuff.
        saveDefaultConfig();
        reloadConfig();
        getConfig().options().copyDefaults(true);
        // saveConfig();
        // Initialize stuff.
        getLogger().info(getDescription().getFullName() + " reloaded");
    } */

    public boolean isRunning() {
        return running;
    }

    public Map<ClientboundLevelChunkPacket, ChunkBlocks> getPacketChunkBlocksCache() {
        return packetChunkBlocksCache;
    }

    public Map<UUID, PlayerData> getPlayerData() {
        return playerData;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public boolean isEnabled(World world) {
        return ((CraftWorld) world).getHandle().paperConfig.antiXray && ((CraftWorld) world).getHandle().paperConfig.engineMode == EngineMode.HIDE && getConfig().getBoolean("world-settings." + world.getName() + ".anti-xray.ray-trace", getConfig().getBoolean("world-settings.default.anti-xray.ray-trace"));
    }

    public List<Location> getLocations(Entity entity, Location location) {
        if (((CraftWorld) location.getWorld()).getHandle().chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray && getConfig().getBoolean("world-settings." + location.getWorld().getName() + ".anti-xray.ray-trace-third-person", getConfig().getBoolean("world-settings.default.anti-xray.ray-trace-third-person"))) {
            Vector direction = location.getDirection();
            return Arrays.asList(location, move(entity, location, direction), move(entity, location, direction.multiply(-1.)).setDirection(direction));
        }

        return Collections.singletonList(location);
    }

    private Location move(Entity entity, Location location, Vector direction) {
        return location.clone().subtract(direction.clone().multiply(getMaxZoom(entity, location, direction, 4.)));
    }

    private double getMaxZoom(Entity entity, Location location, Vector direction, double maxZoom) {
        Vec3 position = new Vec3(location.getX(), location.getY(), location.getZ());

        for (int i = 0; i < 8; i++) {
            float edgeX = (float) ((i & 1) * 2 - 1);
            float edgeY = (float) ((i >> 1 & 1) * 2 - 1);
            float edgeZ = (float) ((i >> 2 & 1) * 2 - 1);
            edgeX = edgeX * 0.1f;
            edgeY = edgeY * 0.1f;
            edgeZ = edgeZ * 0.1f;
            Vec3 edge = position.add(edgeX, edgeY, edgeZ);
            Vec3 edgeMoved = new Vec3(position.x - direction.getX() * maxZoom + (double) edgeX + (double) edgeZ, position.y - direction.getY() * maxZoom + (double) edgeY, position.z - direction.getZ() * maxZoom + (double) edgeZ);
            BlockHitResult result = ((CraftWorld) location.getWorld()).getHandle().clip(new ClipContext(edge, edgeMoved, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, ((CraftEntity) entity).getHandle()));

            if (result.getType() != HitResult.Type.MISS) {
                double zoom = result.getLocation().distanceTo(position);

                if (zoom < maxZoom) {
                    maxZoom = zoom;
                }
            }
        }

        return maxZoom;
    }
}
