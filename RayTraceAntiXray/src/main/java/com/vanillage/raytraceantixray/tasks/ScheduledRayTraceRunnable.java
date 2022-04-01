package com.vanillage.raytraceantixray.tasks;

import java.util.stream.Collectors;

import com.vanillage.raytraceantixray.RayTraceAntiXray;

public final class ScheduledRayTraceRunnable implements Runnable {
    private final RayTraceAntiXray plugin;

    public ScheduledRayTraceRunnable(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            boolean timings = plugin.isTimings();
            long start = timings ? System.currentTimeMillis() : 0L;
            plugin.getExecutorService().invokeAll(plugin.getPlayerData().values().stream().map(p -> new RayTraceRunnable(plugin, p)).collect(Collectors.toList()));

            if (timings) {
                long stop = System.currentTimeMillis();
                plugin.getLogger().info((stop - start) + "ms per ray trace tick.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}
