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
            plugin.getExecutorService().invokeAll(plugin.getPlayerData().values().stream().map(p -> new RayTraceRunnable(plugin, p)).collect(Collectors.toList()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}
