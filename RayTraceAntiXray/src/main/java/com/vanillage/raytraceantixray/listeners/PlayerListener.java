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
        rayTraceAntiXray.addPlayer(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        rayTraceAntiXray.removePlayer(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = rayTraceAntiXray.getPlayerData().get(player.getUniqueId());

        if (!rayTraceAntiXray.validatePlayerData(player, playerData, "onPlayerMove")) {
            return;
        }

        Location to = event.getTo();

        if (to.getWorld() == playerData.getLocations()[0].getWorld()) {
            VectorialLocation location = new VectorialLocation(to);
            Vector vector = location.getVector();
            vector.setY(vector.getY() + player.getEyeHeight());
            playerData.setLocations(RayTraceAntiXray.getLocations(player, location));
        }
    }
}
