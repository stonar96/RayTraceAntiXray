package com.vanillage.raytraceantixray.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class SchedulerUtil {
    public static boolean folia = isFolia();
    private static Object globalRegionScheduler;
    private static Object regionScheduler;
    private static Method runAtFixedRate;
    private static Method executeMethod;

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Class.forName("io.papermc.paper.threadedregions.RegionizedServerInitEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static {
        // init reflect for folia
        if (folia) {
            try {
                String globalRegionSchedulerName = "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler";
                String regionSchedulerName = "io.papermc.paper.threadedregions.scheduler.RegionScheduler";
                Method getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
                Method getRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
                globalRegionScheduler = getGlobalRegionScheduler.invoke(Bukkit.class);
                regionScheduler = getRegionScheduler.invoke(Bukkit.class);
                executeMethod = Class.forName(regionSchedulerName).getMethod("execute", Plugin.class, World.class, int.class, int.class, Runnable.class);
                runAtFixedRate = Class.forName(globalRegionSchedulerName).getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException |
                     InvocationTargetException e) {
                e.printStackTrace();
                folia = false;
            }
        }
    }

    public static void runRegionTask(Plugin plugin, World world, int X, int Y, Runnable runnable) {
        if (folia) {
            try {
                executeMethod.invoke(regionScheduler, plugin, world, X, Y, runnable);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            runnable.run();
        }
    }

    // for all codes that use org.bukkit.scheduler.BukkitScheduler#runTaskLater
    public static void runTaskTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        if (folia) {
            try {
                runAtFixedRate.invoke(globalRegionScheduler, plugin, (Consumer<?>) task -> runnable.run(), delay != 0 ? delay : 1, period);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
    }
}
