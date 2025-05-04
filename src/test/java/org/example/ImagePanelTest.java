package org.example;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for private static image utility methods and zoom behavior
 * in the {@link ImagePanel} class.
 *
 * This test class uses Java Reflection to access and test private static methods,
 * such as {@code toARGB}, {@code computeMIP}, and {@code blendChannels}.
 */
public class ImagePanelTest {

    /**
     * Tests the {@code toARGB} method to ensure it correctly converts a
     * {@link BufferedImage} of type {@code TYPE_BYTE_INDEXED} to {@code TYPE_INT_ARGB}.
     *
     * The test creates a small indexed image, invokes the private static
     * {@code toARGB} method, and asserts that the pixel colors are preserved.
     */
    @Test
    void testToARGBConvertsIndexed() throws Exception {
        // create a 2×2 indexed image
        BufferedImage indexed = new BufferedImage(
                2, 2, BufferedImage.TYPE_BYTE_INDEXED
        );
        indexed.setRGB(0,0, 0xFF0000FF);  // blue in palette
        indexed.setRGB(1,1, 0xFFFF0000);  // red in palette

        // invoke private static toARGB via reflection
        Method m = ImagePanel.class.getDeclaredMethod("toARGB", BufferedImage.class);
        m.setAccessible(true);
        BufferedImage argb = (BufferedImage) m.invoke(null, indexed);

        assertEquals(BufferedImage.TYPE_INT_ARGB, argb.getType());
        // pixel values should survive
        assertEquals(0xFF0000FF, argb.getRGB(0,0));
        assertEquals(0xFFFF0000, argb.getRGB(1,1));
    }

    /**
     * Tests the {@code computeMIP} method to verify that it correctly performs
     * maximum intensity projection across two ARGB images.
     *
     * The expected result is a new image where each pixel contains the
     * maximum channel value across the input images.
     */
    @Test
    void testComputeMIP() throws Exception {
        // two 2×1 images: one all (10,20,30), another all (5,25,15)
        BufferedImage a = new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);
        BufferedImage b = new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);
        int colorA = (0xFF<<24)|(10<<16)|(20<<8)|30;
        int colorB = (0xFF<<24)|(5<<16)|(25<<8)|15;
        for (int x=0; x<2; x++){
            a.setRGB(x,0,colorA);
            b.setRGB(x,0,colorB);
        }

        Method mipM = ImagePanel.class.getDeclaredMethod(
                "computeMIP", List.class
        );
        mipM.setAccessible(true);
        @SuppressWarnings("unchecked")
        BufferedImage mip = (BufferedImage)mipM.invoke(
                null, List.of(a,b)
        );

        // max per-channel → (10,25,30)
        int expected = (0xFF<<24)|(10<<16)|(25<<8)|30;
        assertEquals(expected, mip.getRGB(0,0));
        assertEquals(expected, mip.getRGB(1,0));
    }

    /**
     * Tests the {@code blendChannels} method to ensure it performs correct
     * per-channel max grayscale blending. Each input image corresponds to a single color channel.
     */
    @Test
    void testBlendChannelsMaxGray() throws Exception {
        // create two 1×1 grayscale ARGB images:
        BufferedImage c0 = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        BufferedImage c1 = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        // set pixel: c0 has blue=50, c1 has green=80
        c0.setRGB(0,0, (0xFF<<24)|(0<<16)|(0<<8)|50);
        c1.setRGB(0,0, (0xFF<<24)|(0<<16)|(80<<8)|0);

        Method blendM = ImagePanel.class.getDeclaredMethod(
                "blendChannels", List.class
        );
        blendM.setAccessible(true);
        @SuppressWarnings("unchecked")
        BufferedImage comp = (BufferedImage)blendM.invoke(
                null, List.of(c0, c1)
        );

        int pix = comp.getRGB(0,0);
        int r = (pix>>16)&0xFF;
        int g = (pix>>8)&0xFF;
        int b = pix&0xFF;
        // channel0 → red = max(0,0,50)=50; channel1 → green = max(0,80,0)=80
        assertEquals(50, r);
        assertEquals(80, g);
        assertEquals(0, b);
    }

    /**
     * Verifies the public zoom API of {@link ImagePanel}, ensuring
     * that zoom can be set and retrieved precisely.
     */
    @Test
    void testPanelZoomApi() {
        ImagePanel panel = new ImagePanel();
        panel.setZoom(2.5);
        assertEquals(2.5, panel.getZoom(), 1e-6);
    }
}
