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
                () -> assertEquals(47, region.maxZ));
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
}
