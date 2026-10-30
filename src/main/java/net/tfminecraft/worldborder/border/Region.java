package net.tfminecraft.worldborder.border;

public final class Region {

    /** No lethal band: everything past the edge only takes the regular damage. */
    public static final int NO_LETHAL_DEPTH = -1;
    /** No outer limit: the damage (and lethal) band reaches out forever. */
    public static final int ENDLESS_BUFFER = 0;

    public final String world;
    public final int minX;
    public final int maxX;
    public final int minZ;
    public final int maxZ;
    /** Blocks past the edge where the lethal band starts, or {@link #NO_LETHAL_DEPTH}. */
    public final int lethalDepth;
    /** Blocks past the edge where the buffer ends and nothing happens, or {@link #ENDLESS_BUFFER}. */
    public final int bufferWidth;

    public Region(String world, int minX, int maxX, int minZ, int maxZ) {
        this(world, minX, maxX, minZ, maxZ, NO_LETHAL_DEPTH, ENDLESS_BUFFER);
    }

    public Region(String world, int minX, int maxX, int minZ, int maxZ, int lethalDepth, int bufferWidth) {
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.lethalDepth = lethalDepth;
        this.bufferWidth = bufferWidth;
    }

    public enum Zone {
        SAFE,
        WARNING,
        OUTSIDE,
        LETHAL,
        BEYOND
    }

    public Zone zoneAt(double x, double z, int graceInset) {
        double dist = Math.min(
                Math.min(x - minX, maxX - x),
                Math.min(z - minZ, maxZ - z));
        if (dist < 0) {
            return outsideZone(-dist);
        }
        if (dist <= graceInset) {
            return Zone.WARNING;
        }
        return Zone.SAFE;
    }

    /**
     * Band for a player {@code depth} blocks past the edge, measured along the furthest
     * axis so the bands are square like the border itself.
     */
    private Zone outsideZone(double depth) {
        if (bufferWidth > ENDLESS_BUFFER && depth > bufferWidth) {
            return Zone.BEYOND;
        }
        if (lethalDepth >= 0 && depth >= lethalDepth) {
            return Zone.LETHAL;
        }
        return Zone.OUTSIDE;
    }

    /**
     * Width of the lethal band in blocks, {@code 0} when there is none,
     * or {@link Double#POSITIVE_INFINITY} when it never ends.
     */
    public double lethalBandWidth() {
        if (lethalDepth < 0) {
            return 0;
        }
        if (bufferWidth <= ENDLESS_BUFFER) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(0, bufferWidth - lethalDepth);
    }
}
