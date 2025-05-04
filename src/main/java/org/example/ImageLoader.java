package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for loading standard image files from a directory.
 * <p>
 * Supports filtering by common raster formats including:
 * <ul>
 *   <li>JPG / JPEG</li>
 *   <li>PNG</li>
 *   <li>BMP</li>
 *   <li>GIF</li>
 * </ul>
 * <p>
 * This loader is used by {@link GalleryFrame} to import folders containing image stacks.
 * Results are returned as a sorted list of {@link File} objects.
 */

class ImageLoader {
    private static final List<String> EXT = Arrays.asList("jpg", "jpeg", "png", "bmp", "gif");

    /**
     * Scans a directory and returns a sorted list of supported image files.
     * <p>
     * The method filters for files ending with extensions: jpg, jpeg, png, bmp, gif.
     * Only immediate children of the directory are considered (no recursion).
     *
     * @param dir The directory to scan
     * @return A sorted list of image files in the directory, or an empty list if none found
     */

    static List<File> loadImages(File dir) {
        List<File> res = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return res;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName().toLowerCase();
                for (String e : EXT)
                    if (name.endsWith("." + e)) {
                        res.add(f);
                        break;
                    }
            }
        }
        Collections.sort(res);
        return res;
    }
}