package com.vanillage.raytraceantixray.data;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class VectorialLocation {
    private final Reference<World> world;
    private final Vector vector;
    private final Vector direction;

    public VectorialLocation(World world, Vector vector, Vector direction) {
        this.world = new WeakReference<>(world);
        this.vector = vector;
        this.direction = direction;
    }

    public VectorialLocation(VectorialLocation location) {
        world = location.world;
        vector = location.getVector().clone();
        direction = location.getDirection().clone();
    }

    public VectorialLocation(Location location) {
        world = new WeakReference<>(location.getWorld());
        vector = location.toVector();
        direction = location.getDirection();
    }

    public World getWorld() {
        return world.get();
    }

    public Vector getVector() {
        return vector;
    }

    public Vector getDirection() {
        return direction;
    }
}
