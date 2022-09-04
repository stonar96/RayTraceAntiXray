package com.vanillage.raytraceantixray.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.tasks.RayTraceCallable;

public final class PlayerListener implements Listener {
    private final RayTraceAntiXray plugin;

    public PlayerListener(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerData playerData = new PlayerData(plugin.getLocations(event.getPlayer(), event.getPlayer().getEyeLocation()));
        playerData.setCallable(new RayTraceCallable(playerData));
        plugin.getPlayerData().put(event.getPlayer().getUniqueId(), playerData);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerData().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        PlayerData playerData = plugin.getPlayerData().get(event.getPlayer().getUniqueId());
        Location location = event.getTo();

        if (location.getWorld().equals(playerData.getLocations().get(0).getWorld())) {
            location = location.clone();
            location.setY(location.getY() + event.getPlayer().getEyeHeight());
            playerData.setLocations(plugin.getLocations(event.getPlayer(), location));
        }
    }
}
