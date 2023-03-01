package com.vanillage.raytraceantixray.tasks;

import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

import com.vanillage.raytraceantixray.RayTraceAntiXray;

public final class RayTraceTimerTask extends TimerTask {
    private final RayTraceAntiXray plugin;

    public RayTraceTimerTask(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean timingsEnabled = plugin.isTimingsEnabled();
        long start = timingsEnabled ? System.currentTimeMillis() : 0L;

        try {
            plugin.getExecutorService().invokeAll(plugin.getPlayerData().values());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException e) {

        }

        if (timingsEnabled) {
            long stop = System.currentTimeMillis();
            plugin.getLogger().info((stop - start) + "ms per ray trace tick.");
        }
    }
}
