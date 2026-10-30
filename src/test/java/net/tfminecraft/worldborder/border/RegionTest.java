package net.tfminecraft.worldborder.border;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegionTest {

    private final Region region = new Region("overworld", -12, 23, -31, 47);

    @Test
    void retainsWorldAndAsymmetricBounds() {
        assertAll(
                () -> assertEquals("overworld", region.world),
                () -> assertEquals(-12, region.minX),
                () -> assertEquals(23, region.maxX),
                () -> assertEquals(-31, region.minZ),
                () -> assertEquals(47, region.maxZ),
                () -> assertEquals(Region.NO_LETHAL_DEPTH, region.lethalDepth),
                () -> assertEquals(Region.ENDLESS_BUFFER, region.bufferWidth));
    }

    @ParameterizedTest(name = "({0}, {1}), inset {2}: {3}")
    @CsvSource({
            "-12.01, 0, 3, OUTSIDE",
            "23.01, 0, 3, OUTSIDE",
            "0, -31.01, 3, OUTSIDE",
            "0, 47.01, 3, OUTSIDE",
            "-12.01, 47.01, 3, OUTSIDE",
            "-12, 0, 3, WARNING",
            "23, 0, 3, WARNING",
            "0, -31, 3, WARNING",
            "0, 47, 3, WARNING",
            "-12, -31, 3, WARNING",
            "23, 47, 3, WARNING",
            "-9, 0, 3, WARNING",
            "20, 0, 3, WARNING",
            "0, -28, 3, WARNING",
            "0, 44, 3, WARNING",
            "-8.99, 0, 3, SAFE",
            "19.99, 0, 3, SAFE",
            "0, -27.99, 3, SAFE",
            "0, 43.99, 3, SAFE",
            "0, 0, 3, SAFE",
            "-12, 0, 0, WARNING",
            "-11.99, 0, 0, SAFE",
            "0, 0, 100, WARNING",
            "24, 0, 100, OUTSIDE"
    })
    void classifiesAllEdgesAndInclusiveWarningInset(double x, double z, int inset, Region.Zone expected) {
        assertEquals(expected, region.zoneAt(x, z, inset));
    }

    @Test
    void zeroWidthRegionWarnsOnTheBorderAndDamagesOutside() {
        Region point = new Region("point", 7, 7, -4, -4);
        assertEquals(Region.Zone.WARNING, point.zoneAt(7, -4, 0));
        assertEquals(Region.Zone.OUTSIDE, point.zoneAt(7.01, -4, 0));
        assertEquals(Region.Zone.OUTSIDE, point.zoneAt(7, -4.01, 0));
    }

    @Test
    void supportsIntegerExtremeBoundsWithoutOverflow() {
        Region enormous = new Region("large", Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(Region.Zone.SAFE, enormous.zoneAt(0, 0, 20));
        assertEquals(Region.Zone.WARNING, enormous.zoneAt(Integer.MAX_VALUE, 0, 20));
        assertEquals(Region.Zone.OUTSIDE, enormous.zoneAt((double) Integer.MAX_VALUE + 1, 0, 20));
    }

    @ParameterizedTest(name = "({0}, {1}): {2}")
    @CsvSource({
            "0, 0, SAFE",
            "-12, 0, WARNING",
            "-12.01, 0, OUTSIDE",
            "-21.99, 0, OUTSIDE",
            "-22, 0, LETHAL",
            "33, 0, LETHAL",
            "0, 57, LETHAL",
            "-62, 0, LETHAL",
            "-62.01, 0, BEYOND",
            "73.01, 0, BEYOND",
            "0, -81.01, BEYOND",
            "0, 97.01, BEYOND",
            "-40, 97.01, BEYOND",
            "-30, 60, LETHAL",
            "-13, 48, OUTSIDE"
    })
    void bufferBandsAreMeasuredAlongTheFurthestAxis(double x, double z, Region.Zone expected) {
        Region buffered = new Region("overworld", -12, 23, -31, 47, 10, 50);
        assertEquals(expected, buffered.zoneAt(x, z, 3));
    }

    @Test
    void lethalDepthWithoutBufferWidthKillsForever() {
        Region endless = new Region("overworld", 0, 10, 0, 10, 0, Region.ENDLESS_BUFFER);
        assertEquals(Region.Zone.WARNING, endless.zoneAt(10, 5, 0));
        assertEquals(Region.Zone.LETHAL, endless.zoneAt(10.01, 5, 0));
        assertEquals(Region.Zone.LETHAL, endless.zoneAt(1_000_000, 5, 0));
        assertEquals(Double.POSITIVE_INFINITY, endless.lethalBandWidth());
    }

    @Test
    void bufferWidthWithoutLethalDepthOnlyDamagesUpToTheLimit() {
        Region damaging = new Region("overworld", 0, 10, 0, 10, Region.NO_LETHAL_DEPTH, 100);
        assertEquals(Region.Zone.OUTSIDE, damaging.zoneAt(110, 5, 0));
        assertEquals(Region.Zone.BEYOND, damaging.zoneAt(110.01, 5, 0));
        assertEquals(0, damaging.lethalBandWidth());
    }

    @Test
    void lethalBandWidthIsTheGapBetweenDepthAndBufferAndNeverNegative() {
        assertEquals(900, new Region("w", 0, 10, 0, 10, 100, 1000).lethalBandWidth());
        assertEquals(0, new Region("w", 0, 10, 0, 10, 100, 100).lethalBandWidth());
        Region inverted = new Region("w", 0, 10, 0, 10, 200, 100);
        assertEquals(0, inverted.lethalBandWidth());
        assertEquals(Region.Zone.OUTSIDE, inverted.zoneAt(110, 5, 0));
        assertEquals(Region.Zone.BEYOND, inverted.zoneAt(110.01, 5, 0));
    }
}
