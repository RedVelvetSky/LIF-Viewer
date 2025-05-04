package org.example;

import javax.swing.*;

/**
 * Entry point for the .LIF image viewer application.
 * <p>
 * This class initializes the Swing event dispatch thread (EDT) and launches
 * the main {@link GalleryFrame} UI. All GUI operations are scheduled on the EDT
 * to ensure thread safety and proper rendering behavior.
 */

public class Main {
    /**
     * Launches the application by creating and displaying the {@link GalleryFrame}.
     * <p>
     * The method delegates the UI initialization to the Swing event dispatch thread
     * using {@link SwingUtilities#invokeLater}, which is the recommended approach for
     * starting Swing-based GUIs.
     *
     * @param args Command-line arguments (unused)
     */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GalleryFrame().setVisible(true));
    }
}