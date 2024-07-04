package com.vanillage.raytraceantixray.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.tasks.RayTraceCallable;
import com.vanillage.raytraceantixray.tasks.UpdateBukkitRunnable;

public final class PlayerListener implements Listener {
    private final RayTraceAntiXray rayTraceAntiXray;

    public PlayerListener(RayTraceAntiXray rayTraceAntiXray) {
        this.rayTraceAntiXray = rayTraceAntiXray;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!rayTraceAntiXray.validatePlayer(player)) {
            return;
        }

        PlayerData playerData = new PlayerData(RayTraceAntiXray.getLocations(player, new VectorialLocation(player.getEyeLocation())));
        playerData.setCallable(new RayTraceCallable(rayTraceAntiXray, playerData));
        rayTraceAntiXray.getPlayerData().put(player.getUniqueId(), playerData);

        if (rayTraceAntiXray.isFolia()) {
            player.getScheduler().runAtFixedRate(rayTraceAntiXray.getPlugin(), new UpdateBukkitRunnable(rayTraceAntiXray, player), null, 1L, rayTraceAntiXray.getUpdateTicks());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        rayTraceAntiXray.getPlayerData().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = rayTraceAntiXray.getPlayerData().get(player.getUniqueId());

        if (!rayTraceAntiXray.validatePlayerData(player, playerData, "onPlayerMove")) {
            return;
        }

        Location to = event.getTo();

        if (to.getWorld().equals(playerData.getLocations()[0].getWorld())) {
            VectorialLocation location = new VectorialLocation(to);
            Vector vector = location.getVector();
            vector.setY(vector.getY() + player.getEyeHeight());
            playerData.setLocations(RayTraceAntiXray.getLocations(player, location));
        }
    }
}
