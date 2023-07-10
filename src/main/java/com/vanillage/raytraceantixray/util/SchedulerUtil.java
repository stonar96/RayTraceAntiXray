package com.vanillage.raytraceantixray.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;


public class SchedulerUtil {
    private static Boolean isFolia = checkFolia();

    public static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Class.forName("io.papermc.paper.threadedregions.RegionizedServerInitEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void runTaskTimerEntity(Plugin plugin, Entity entity, Runnable runnable, long delay, long period) {
        if (isFolia) {
            entity.getScheduler().runAtFixedRate(plugin, task -> runnable.run(), null, delay == 0 ? 1 : delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
    }
}
