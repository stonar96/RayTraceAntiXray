package com.vanillage.raytraceantixray;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Throwables;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.commands.RayTraceAntiXrayTabExecutor;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.listeners.PacketListener;
import com.vanillage.raytraceantixray.listeners.PlayerListener;
import com.vanillage.raytraceantixray.listeners.WorldListener;
import com.vanillage.raytraceantixray.tasks.RayTraceCallable;
import com.vanillage.raytraceantixray.tasks.RayTraceTimerTask;
import com.vanillage.raytraceantixray.tasks.UpdateBukkitRunnable;

import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.configuration.WorldConfiguration.Anticheat.AntiXray;
import io.papermc.paper.configuration.type.EngineMode;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RayTraceAntiXray {
    private final RayTraceAntiXrayPlugin plugin;
    private final Configuration config;
    private final long updateTicks;
    private final boolean folia;

    {
        boolean folia = false;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {

        }

        this.folia = folia;
    }

    private final ConcurrentMap<ClientboundLevelChunkWithLightPacket, ChunkBlocks> packetChunkBlocksTransfer = new MapMaker().weakKeys().makeMap();
    private final ConcurrentMap<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private Timer timer;
    private volatile boolean running = false;
    private volatile boolean timingsEnabled = false;

    public RayTraceAntiXray(RayTraceAntiXrayPlugin plugin, Configuration config) {
        this.plugin = plugin;
        this.config = config;
        updateTicks = Math.max(config.getLong("settings.anti-xray.update-ticks"), 1L);
    }

    public void enable() {
        running = true;
        // Use a combination of a tick thread (timer) and a ray trace thread pool.
        // The timer schedules tasks (a task per player) to the thread pool and ensures a common and defined tick start and end time without overlap by waiting for the thread pool to finish all tasks.
        // A scheduled thread pool with a task per player would also be possible but then there's no common tick.
        executorService = Executors.newFixedThreadPool(Math.max(config.getInt("settings.anti-xray.ray-trace-threads"), 1), new ThreadFactoryBuilder().setThreadFactory(Executors.defaultThreadFactory()).setNameFormat("RayTraceAntiXray ray trace thread %d").setDaemon(true).build());
        // Use a timer instead of a single thread scheduled executor because there is no equivalent for the timer's schedule method.
        timer = new Timer("RayTraceAntiXray tick thread", true);
        timer.schedule(new RayTraceTimerTask(this), 0L, Math.max(config.getLong("settings.anti-xray.ms-per-ray-trace-tick"), 1L));

        if (!folia) {
            new UpdateBukkitRunnable(this).runTaskTimer(plugin, 0L, updateTicks);
        }

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        // Especially on Folia the order is important here.
        pluginManager.registerEvents(new WorldListener(this), plugin);
        // Handle missed world events.
        pluginManager.registerEvents(new PlayerListener(this), plugin);
        // Register player join event.
        // Handle missed player join events.
        // What if the player quit in the meantime?
        // Register player events.
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this));
        plugin.getCommand("raytraceantixray").setExecutor(new RayTraceAntiXrayTabExecutor(this));
        // Handle missed events.
    }

    public void disable() {
        // The server catches all throwables and may continue to run after disabling this instance.
        // We want to ensure as much as possible that everything is left behind in a clean and defined state.
        // So the goal is to at least attempt to execute all critical sections of code, regardless of what happens before.
        // Considering errors during error handling and JLS 11.1.3. Asynchronous Exceptions, throwables could potentially be thrown anywhere (even between blocks of code or statements?).
        // Thus the only way is to nest try-finally statements like this: try { try { } finally { } } finally { }
        // According to the bytecode of nested try-catch statements in JVMS 3.12, all nested try blocks are entered at the same time.
        // So we either reach the innermost try block, in which case all blocks will be at least attempt to be executed, or no block is entered at all (e.g. in case of a throwable being thrown before).
        // Both outcomes yield a defined state of this instance.
        // A more intuitive way would be to nest inside of the finally clause like this: try { } finally { try { } finally { } }
        // However, this doesn't provide the same guarantees as described above.
        // Additionally, we can add catch clauses to collect suppressed exceptions and rethrow in the last finally clause.
        Throwable throwable = null;

        try {
            try {
                try {
                    try {
                        try {
                            running = false;
                            plugin.getCommand("raytraceantixray").setExecutor(null);
                        } catch (Throwable t) {
                            throwable = t;
                        } finally {
                            ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
                            HandlerList.unregisterAll(plugin);
                            plugin.getServer().getScheduler().cancelTasks(plugin);
                        }
                    } catch (Throwable t) {
                        if (throwable == null) {
                            throwable = t;
                        } else {
                            throwable.addSuppressed(t);
                        }
                    } finally {
                        timer.cancel();
                        timer = null;
                    }
                } catch (Throwable t) {
                    if (throwable == null) {
                        throwable = t;
                    } else {
                        throwable.addSuppressed(t);
                    }
                } finally {
                    executorService.shutdownNow();

                    try {
                        executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    executorService = null;
                }
            } catch (Throwable t) {
                if (throwable == null) {
                    throwable = t;
                } else {
                    throwable.addSuppressed(t);
                }
            } finally {
                packetChunkBlocksTransfer.clear();
                playerData.clear();
                timingsEnabled = false;
            }
        } catch (Throwable t) {
            if (throwable == null) {
                throwable = t;
            } else {
                throwable.addSuppressed(t);
            }
        } finally {
            if (throwable != null) {
                Throwables.throwIfUnchecked(throwable);
                throw new RuntimeException(throwable);
            }
        }
    }

    public RayTraceAntiXrayPlugin getPlugin() {
        return plugin;
    }

    public Configuration getConfig() {
        return config;
    }

    public ConcurrentMap<ClientboundLevelChunkWithLightPacket, ChunkBlocks> getPacketChunkBlocksTransfer() {
        return packetChunkBlocksTransfer;
    }

    public ConcurrentMap<UUID, PlayerData> getPlayerData() {
        return playerData;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isTimingsEnabled() {
        return timingsEnabled;
    }

    public void setTimingsEnabled(boolean timingsEnabled) {
        this.timingsEnabled = timingsEnabled;
    }

    public boolean isEnabled(World world) {
        // Note that Paper Anti-Xray config changes are usually not applied when reloading the server.
        // However, for RayTraceAntiXray we implement this.
        // To do this safely, the following checks are required.
        // In particular, engine-mode: 1 must have already been active before.
        // Also note that max-block-height shouldn't be increased (see related logic in ChunkPacketBlockControllerAntiXray).
        if (!config.getBoolean("world-settings." + world.getName() + ".anti-xray.ray-trace", config.getBoolean("world-settings.default.anti-xray.ray-trace"))) {
            return false;
        }

        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        AntiXray antiXray = serverLevel.paperConfig().anticheat.antiXray;

        if (!antiXray.enabled || antiXray.engineMode != EngineMode.HIDE) {
            return false;
        }

        ChunkPacketBlockController chunkPacketBlockController = serverLevel.chunkPacketBlockController;

        if (chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray) {
            // Actually we shouldn't get here but it doesn't matter.
            return true;
        }

        if (!(chunkPacketBlockController instanceof io.papermc.paper.antixray.ChunkPacketBlockControllerAntiXray)) {
            return false;
        }

        try {
            Field field = io.papermc.paper.antixray.ChunkPacketBlockControllerAntiXray.class.getDeclaredField("engineMode");
            field.setAccessible(true);
            return field.get(chunkPacketBlockController) == EngineMode.HIDE;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public PlayerData addPlayer(Player player, boolean join) {
        if (!validatePlayer(player)) {
            return null;
        }

        VectorialLocation vectorialLocation = new VectorialLocation(player.getEyeLocation());
        PlayerData playerData = new PlayerData(!join && folia ? new VectorialLocation[] { vectorialLocation } : getLocations(player, vectorialLocation), null);
        playerData.setCallable(new RayTraceCallable(this, playerData));
        UUID uniqueId = player.getUniqueId();
        PlayerData previousPlayerData = this.playerData.putIfAbsent(uniqueId, playerData);

        if (previousPlayerData != null) {
            return previousPlayerData;
        }

        if (folia) {
            ScheduledTask scheduledTask = player.getScheduler().runAtFixedRate(plugin, new UpdateBukkitRunnable(this, player), () -> removePlayer(uniqueId, true), 1L, updateTicks);

            if (scheduledTask == null) {
                removePlayer(uniqueId, true);
                return null;
            }

            playerData.setScheduledTask(scheduledTask);
        }

        if (running) {
            return playerData;
        }

        removePlayer(uniqueId, false);
        return null;
    }

    public PlayerData renewPlayer(Player player, PlayerData playerData, Location location) {
        playerData = new PlayerData(getLocations(player, new VectorialLocation(location)), playerData.getScheduledTask());
        playerData.setCallable(new RayTraceCallable(this, playerData));
        UUID uniqueId = player.getUniqueId();

        if (this.playerData.replace(uniqueId, playerData) == null) {
            return null;
        }

        if (running) {
            return playerData;
        }

        removePlayer(uniqueId, true);
        return null;
    }

    public PlayerData removePlayer(UUID uniqueId, boolean quit) {
        PlayerData playerData = this.playerData.remove(uniqueId);

        if (!quit && playerData != null) {
            ScheduledTask scheduledTask = playerData.getScheduledTask();

            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        }

        return playerData;
    }

    public boolean validatePlayer(Player player) {
        return !player.hasMetadata("NPC");
    }

    public boolean validatePlayerData(Player player, PlayerData playerData, String methodName) {
        if (playerData != null) {
            return true;
        }

        if (!validatePlayer(player)) {
            return false;
        }

        if (running) {
            // We have no explanation.
            // Let the caller fail hard to print a stack trace.
            return true;
        }

        Logger logger = plugin.getLogger();
        logger.warning("The method " + methodName + " has been called for player " + player.getName() + " for a disabled instance");
        logger.warning("This can happen for a short time after the plugin has been disabled or reloaded");
        logger.warning("Otherwise, please restart your server and consider reporting the issue");
        // On Paper this should actually never happen.
        // Let the caller fail hard on Paper to print a stack trace.
        return !folia;
    }

    public static VectorialLocation[] getLocations(Entity entity, VectorialLocation location) {
        World world = location.getWorld();
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) world).getHandle().chunkPacketBlockController;

        if (chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray && ((ChunkPacketBlockControllerAntiXray) chunkPacketBlockController).rayTraceThirdPerson) {
            VectorialLocation thirdPersonFrontLocation = new VectorialLocation(location);
            thirdPersonFrontLocation.getDirection().multiply(-1.);
            return new VectorialLocation[] { location, move(entity, new VectorialLocation(world, location.getVector().clone(), location.getDirection())), move(entity, thirdPersonFrontLocation) };
        }

        return new VectorialLocation[] { location };
    }

    private static VectorialLocation move(Entity entity, VectorialLocation location) {
        location.getVector().subtract(location.getDirection().clone().multiply(getMaxZoom(entity, location, 4.)));
        return location;
    }

    private static double getMaxZoom(Entity entity, VectorialLocation location, double maxZoom) {
        Vector vector = location.getVector();
        Vec3 position = new Vec3(vector.getX(), vector.getY(), vector.getZ());
        double positionX = position.x;
        double positionY = position.y;
        double positionZ = position.z;
        Vector direction = location.getDirection();
        double directionX = direction.getX();
        double directionY = direction.getY();
        double directionZ = direction.getZ();
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();

        // Logic copied from Minecraft client.
        for (int i = 0; i < 8; i++) {
            float cornerX = (float) ((i & 1) * 2 - 1);
            float cornerY = (float) ((i >> 1 & 1) * 2 - 1);
            float cornerZ = (float) ((i >> 2 & 1) * 2 - 1);
            cornerX *= 0.1f;
            cornerY *= 0.1f;
            cornerZ *= 0.1f;
            Vec3 corner = position.add(cornerX, cornerY, cornerZ);
            Vec3 cornerMoved = new Vec3(positionX - directionX * maxZoom + (double) cornerX, positionY - directionY * maxZoom + (double) cornerY, positionZ - directionZ * maxZoom + (double) cornerZ);
            BlockHitResult result = serverLevel.clip(new ClipContext(corner, cornerMoved, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, handle));

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
