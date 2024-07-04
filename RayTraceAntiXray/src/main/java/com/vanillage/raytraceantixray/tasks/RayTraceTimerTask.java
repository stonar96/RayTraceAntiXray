package com.vanillage.raytraceantixray.tasks;

import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

import com.vanillage.raytraceantixray.RayTraceAntiXray;

public final class RayTraceTimerTask extends TimerTask {
    private final RayTraceAntiXray rayTraceAntiXray;

    public RayTraceTimerTask(RayTraceAntiXray rayTraceAntiXray) {
        this.rayTraceAntiXray = rayTraceAntiXray;
    }

    @Override
    public void run() {
        boolean timingsEnabled = rayTraceAntiXray.isTimingsEnabled();
        long start = timingsEnabled ? System.currentTimeMillis() : 0L;

        try {
            rayTraceAntiXray.getExecutorService().invokeAll(rayTraceAntiXray.getPlayerData().values());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException e) {

        }

        if (timingsEnabled) {
            long stop = System.currentTimeMillis();
            rayTraceAntiXray.getPlugin().getLogger().info((stop - start) + "ms per ray trace tick.");
        }
    }
}
