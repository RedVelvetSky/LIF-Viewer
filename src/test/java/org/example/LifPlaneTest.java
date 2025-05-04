package org.example;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link LifPlane} class.
 *
 * These tests verify correct behavior of the constructor, field immutability,
 * and the {@code toString()} override.
 */
public class LifPlaneTest {

    /**
     * Verifies that {@link LifPlane#toString()} returns the label string
     * provided at construction. This is important for identification and logging.
     */
    @Test
    void testToStringReturnsLabel() {
        BufferedImage img = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        LifPlane lp = new LifPlane("S0-C1-Z5", img);
        assertEquals("S0-C1-Z5", lp.toString());
    }

    /**
     * Tests that the {@code label} and {@code image} fields of {@link LifPlane}
     * are correctly assigned and remain unchanged (i.e., immutable from the caller's perspective).
     */
    @Test
    void testImmutableFields() {
        BufferedImage img = new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
        LifPlane lp = new LifPlane("lbl", img);
        assertSame(img, lp.image);
        assertEquals("lbl", lp.label);
    }
}
