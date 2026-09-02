package net.tfminecraft.worldborder.border;

public final class Region {

    public final String world;
    public final int minX;
    public final int maxX;
    public final int minZ;
    public final int maxZ;

    public Region(String world, int minX, int maxX, int minZ, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public enum Zone {
        SAFE,
        WARNING,
        OUTSIDE
    }

    public Zone zoneAt(double x, double z, int graceInset) {
        double dist = Math.min(
                Math.min(x - minX, maxX - x),
                Math.min(z - minZ, maxZ - z));
        if (dist < 0) {
            return Zone.OUTSIDE;
        }
        if (dist <= graceInset) {
            return Zone.WARNING;
        }
        return Zone.SAFE;
    }
}
