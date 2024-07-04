package com.vanillage.raytraceantixray;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Throwables;

public final class RayTraceAntiXrayPlugin extends JavaPlugin {
    private volatile RayTraceAntiXray rayTraceAntiXray;

    @Override
    public /* synchronized */ void onEnable() {
        enable(false);
    }

    @Override
    public /* synchronized */ void onDisable() {
        rayTraceAntiXray.disable();
        rayTraceAntiXray = null;
        getLogger().info(getPluginMeta().getDisplayName() + " disabled");
    }

    // Avoid synchronization and reload on the main (global region) thread.
    // Synchronization can also potentially cause deadlocks.
    public /* synchronized */ void enable(boolean reload) {
        Throwable throwable = null;

        try {
            try {
                if (reload) {
                    rayTraceAntiXray.disable();
                } else if (!new File(getDataFolder(), "README.txt").exists()) {
                    saveResource("README.txt", false);
                }

                saveDefaultConfig();

                // if (reload) { // Reload config anyway.
                    reloadConfig();
                // }

                FileConfiguration config = getConfig();
                config.options().copyDefaults(true);
                // Add defaults.
                // saveConfig();
                rayTraceAntiXray = new RayTraceAntiXray(this, config);
                rayTraceAntiXray.enable();
                getLogger().info(getPluginMeta().getDisplayName() + (reload ? " reloaded" : " enabled"));
            } catch (Throwable t) {
                throwable = t;
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (Throwable t) {
            if (throwable == null) {
                throwable = t;
            } else {
                throwable.addSuppressed(t);
            }
        } finally {
            if (throwable != null) {
                Throwables.throwIfUnchecked(throwable);
                throw new RuntimeException(throwable);
            }
        }
    }

    public RayTraceAntiXray getRayTraceAntiXray() {
        return rayTraceAntiXray;
    }
}
