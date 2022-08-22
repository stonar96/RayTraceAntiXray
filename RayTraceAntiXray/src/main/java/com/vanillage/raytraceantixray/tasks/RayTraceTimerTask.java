package com.vanillage.raytraceantixray.tasks;

import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import com.vanillage.raytraceantixray.RayTraceAntiXray;

public final class RayTraceTimerTask extends TimerTask {
    private final RayTraceAntiXray plugin;

    public RayTraceTimerTask(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean timings = plugin.isTimings();
        long start = timings ? System.currentTimeMillis() : 0L;

        try {
            plugin.getExecutorService().invokeAll(plugin.getPlayerData().values().stream().map(p -> new RayTraceCallable(p)).collect(Collectors.toList()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException e) {

        }

        if (timings) {
            long stop = System.currentTimeMillis();
            plugin.getLogger().info((stop - start) + "ms per ray trace tick.");
        }
    }
}
