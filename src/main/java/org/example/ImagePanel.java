package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A custom Swing {@link JPanel} for displaying and interacting with microscopy images.
 * <p>
 * This panel supports:
 * <ul>
 *   <li>Rendering 2D images or Z-stacks of images</li>
 *   <li>Brightness and contrast adjustments in real-time</li>
 *   <li>Zooming with mouse wheel and fitting to window</li>
 *   <li>Panning via mouse drag</li>
 *   <li>Switching between Z-slices</li>
 * </ul>
 * <p>
 * The panel maintains both the original and adjusted versions of the current image.
 * It is intended to be used inside the {@link GalleryFrame} class for image display.
 */

public class ImagePanel extends JPanel {

    private BufferedImage original;    // the image we actually adjust & draw
    private BufferedImage adjusted;    // after brightness / contrast
    private float brightness = 1.0f;
    private float contrast = 0.0f;
    private double zoom = 1.0;

    // panning state
    private double offsetX = 0, offsetY = 0;
    private int lastDragX, lastDragY;

    // Z-stack
    private List<BufferedImage> stack = Collections.emptyList();
    private int slice = 0;

    /**
     * Constructs the image panel with interactive listeners for:
     * <ul>
     *   <li>Mouse drag-based panning</li>
     *   <li>Mouse wheel-based zooming</li>
     * </ul>
     * Initializes state for brightness, contrast, and zoom.
     */

    public ImagePanel() {
        // pan-on-drag
        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastDragX = e.getX();
                lastDragY = e.getY();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }

            public void mouseDragged(MouseEvent e) {
                offsetX += e.getX() - lastDragX;
                offsetY += e.getY() - lastDragY;
                lastDragX = e.getX();
                lastDragY = e.getY();
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // wheel zoom
        addMouseWheelListener(e -> {
            if (original == null) return;
            double old = zoom;
            zoom = Math.max(0.1, Math.min(zoom * (1 - e.getPreciseWheelRotation() * 0.1), 10.0));
            // keep mouse-point fixed:
            double mx = e.getX(), my = e.getY();
            offsetX = mx - (mx - offsetX) * (zoom / old);
            offsetY = my - (my - offsetY) * (zoom / old);
            repaint();
        });
    }

    /**
     * Converts an indexed or palette-based image to ARGB format.
     * <p>
     * If the input image is already in a supported format (non-indexed),
     * it is returned unchanged.
     *
     * @param in The input image
     * @return An ARGB-formatted copy of the input image
     */

    static BufferedImage toARGB(BufferedImage in) {
        if (in.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
            return in;
        }
        BufferedImage out = new BufferedImage(
                in.getWidth(), in.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = out.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();
        return out;
    }

    /**
     * Returns the original (unadjusted) image currently displayed.
     *
     * @return the raw image as a {@link BufferedImage}, or null if none is loaded
     */

    public BufferedImage getCurrentImage() {
        return original;
    }

    /**
     * Returns the currently displayed image with brightness and contrast adjustments applied.
     *
     * @return the adjusted image as a {@link BufferedImage}, or null if none is loaded
     */

    public BufferedImage getAdjustedImage() {
        return adjusted;
    }

    /**
     * Loads an image from a file and displays it.
     * Converts the image to ARGB format if needed, resets pan and zoom.
     *
     * @param file the image file to load
     */

    public void setImage(File file) {
        try {
            BufferedImage raw = ImageIO.read(file);
            setImage(raw);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sets a single image for display in the panel.
     * Converts the image to ARGB format if needed and resets pan/zoom.
     *
     * @param img the image to display
     */

    public void setImage(BufferedImage img) {
        // convert indexed→ARGB:
        this.original = toARGB(img);
        this.offsetX = this.offsetY = 0;
        applyAdjustments();
        repaint();
    }

    /**
     * Loads a Z-stack of images and displays the first slice (index 0).
     * <p>
     * All images are converted to ARGB format for uniform processing.
     *
     * @param imgs List of images representing Z-stack slices
     */

    public void setStack(List<BufferedImage> imgs) {
        // convert all to ARGB up front:
        this.stack = imgs.stream()
                .map(ImagePanel::toARGB)
                .collect(Collectors.toList());
        this.slice = 0;
        if (!stack.isEmpty()) {
            setImage(stack.get(0));
        } else {
            original = adjusted = null;
            repaint();
        }
    }

    /**
     * Switches to a specific Z-slice in the stack and displays it.
     *
     * @param z Zero-based index of the slice to display
     */

    public void setSlice(int z) {
        if (z >= 0 && z < stack.size()) {
            this.slice = z;
            setImage(stack.get(z));
        }
    }

    /**
     * Returns the number of slices in the current image stack.
     *
     * @return the size of the Z-stack
     */

    public int getSliceCount() {
        return stack.size();
    }

    /**
     * @param b Brightness factor (1.0 = no change, &lt;1.0 = darker, &gt;1.0 = brighter)
     */

    public void setBrightness(float b) {
        this.brightness = b;
        applyAdjustments();
        repaint();
    }

    /**
     * Adjusts the contrast offset for the image.
     *
     * @param c Contrast adjustment (0.0 = no change, positive/negative for shifts)
     */

    public void setContrast(float c) {
        this.contrast = c;
        applyAdjustments();
        repaint();
    }

    /**
     * Gets the current zoom factor used for rendering the image.
     *
     * @return the current zoom level
     */

    public double getZoom() {
        return zoom;
    }

    /**
     * Sets the zoom level for image display and resets pan to center.
     *
     * @param z New zoom factor (e.g., 1.0 = 100%, 0.5 = 50%)
     */

    public void setZoom(double z) {
        this.zoom = z;
        this.offsetX = this.offsetY = 0;
        repaint();
    }

    /**
     * Automatically adjusts the zoom level so the image fits within the panel bounds.
     * Also resets the pan to (0,0).
     */

    public void fitToWindow() {
        if (adjusted == null) return;
        double sx = getWidth() / (double) adjusted.getWidth();
        double sy = getHeight() / (double) adjusted.getHeight();
        zoom = Math.min(sx, sy);
        offsetX = offsetY = 0;
        repaint();
    }

    /**
     * Internal helper method to apply brightness and contrast adjustments
     * to the current image using a {@link RescaleOp}.
     * <p>
     * Updates the {@code adjusted} image buffer used for rendering.
     */
    private void applyAdjustments() {
        if (original == null) return;
        RescaleOp op = new RescaleOp(brightness, contrast, null);
        adjusted = op.filter(original, null);
    }

    /**
     * Paints the current adjusted image onto the panel with zoom and pan applied.
     * <p>
     * Called automatically by the Swing framework when the panel needs to be redrawn.
     *
     * @param g The graphics context used for painting
     */

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (adjusted == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = (int) (adjusted.getWidth() * zoom);
        int h = (int) (adjusted.getHeight() * zoom);
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;

        g2.translate(offsetX, offsetY);
        g2.drawImage(adjusted, x, y, w, h, null);
    }

    /**
     * Computes a Maximum Intensity Projection (MIP) from a list of grayscale images.
     * <p>
     * For each pixel, the highest intensity across the stack is selected for each channel (ARGB).
     * This method is used to flatten Z-stacks into a single representative image.
     *
     * @param imgs A list of images representing Z-stack slices
     * @return A new BufferedImage with maximum per-pixel intensities
     */

    public static BufferedImage computeMIP(List<BufferedImage> imgs) {
        int w = imgs.get(0).getWidth(), h = imgs.get(0).getHeight();
        BufferedImage mip = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int maxA = 0, maxR = 0, maxG = 0, maxB = 0;
                for (BufferedImage bi : imgs) {
                    int argb = bi.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF,
                            r = (argb >>> 16) & 0xFF,
                            g = (argb >>> 8) & 0xFF,
                            b = (argb) & 0xFF;
                    if (a > maxA) maxA = a;
                    if (r > maxR) maxR = r;
                    if (g > maxG) maxG = g;
                    if (b > maxB) maxB = b;
                }
                int pix = (maxA << 24) | (maxR << 16) | (maxG << 8) | maxB;
                mip.setRGB(x, y, pix);
            }
        }
        return mip;
    }

    /**
     * Blends up to three grayscale channels into an RGB composite.
     * <p>
     * The first three channels are mapped to Red, Green, and Blue.
     * Each pixel’s intensity is taken as the maximum of its R/G/B values
     * to simulate grayscale strength in color.
     *
     * @param chans A list of grayscale BufferedImages (1–3 items)
     * @return A single ARGB BufferedImage representing the RGB blend
     */

    public static BufferedImage blendChannels(List<BufferedImage> chans) {
        int w = chans.get(0).getWidth();
        int h = chans.get(0).getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0, g = 0, b = 0, a = 0;

                for (int i = 0; i < chans.size() && i < 3; i++) {
                    int px = chans.get(i).getRGB(x, y);
                    int ai = (px >>> 24) & 0xFF;
                    int ri = (px >>> 16) & 0xFF;
                    int gi = (px >>> 8) & 0xFF;
                    int bi = (px) & 0xFF;

                    // true intensity: max of the three channels
                    int gray = Math.max(Math.max(ri, gi), bi);

                    if (i == 0) r = gray;
                    else if (i == 1) g = gray;
                    else b = gray;

                    // keep the highest alpha
                    a = Math.max(a, ai);
                }

                int pix = (a << 24) | (r << 16) | (g << 8) | b;
                out.setRGB(x, y, pix);
            }
        }
        return out;
    }
}