package org.example;

import java.awt.image.BufferedImage;

/**
 * Immutable data record representing a single plane (Z-slice) of an image
 * loaded from a Leica LIF file.
 * <p>
 * Each instance contains both:
 * <ul>
 *   <li>A label indicating its position in the series/channel/Z-stack hierarchy (e.g., "S0-C1-Z5")</li>
 *   <li>The decoded {@link BufferedImage} corresponding to that plane</li>
 * </ul>
 * Used primarily to populate the image hierarchy tree (JTree) and display images
 * in the UI.
 */

public class LifPlane {
    public final String label;           // e.g.  "S0-C1-Z5"
    public final BufferedImage image;    // decoded plane

    /**
     * Constructs a new {@code LifPlane} with the given label and image.
     *
     * @param label A unique string identifier for this plane (e.g., "S0-C1-Z5")
     * @param image The actual decoded image data as a {@link BufferedImage}
     */

    public LifPlane(String label, BufferedImage image) {
        this.label = label;
        this.image = image;
    }

    @Override
    public String toString() {
        return label;
    }
}
