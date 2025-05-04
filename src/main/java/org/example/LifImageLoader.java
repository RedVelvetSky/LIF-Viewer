package org.example;

import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.LIFReader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for loading image data from Leica LIF files using Bio-Formats.
 * <p>
 * This class wraps Bio-Formats readers to decode individual planes
 * as {@link BufferedImage} instances, which are returned as a list of
 * {@link LifPlane} records. Each plane is labeled with its position in the
 * LIF hierarchy (Series, Channel, Z, and optionally T).
 *
 * <p>Designed to be used by {@link GalleryFrame} for UI display of LIF contents.
 */

final class LifImageLoader {

    /**
     * Loads all image planes from a given LIF file and returns them as labeled {@link LifPlane} objects.
     * <p>
     * Internally uses {@link BufferedImageReader} and {@link FormatReader} from Bio-Formats
     * to decode AWT images and extract ZCT coordinates (Z-slice, Channel, Timepoint).
     *
     * @param lifFile The LIF file to read
     * @return A list of LifPlane objects, each containing a label and the decoded image
     * @throws IOException     If the file cannot be read from disk
     * @throws FormatException If the file cannot be parsed as a valid LIF file
     */

    static List<LifPlane> load(File lifFile) throws IOException, FormatException {
        List<LifPlane> out = new ArrayList<>();

        // Wrap the LIFReader so we can call openImage(i)
        BufferedImageReader bir = new BufferedImageReader(new LIFReader());
        bir.setId(lifFile.getAbsolutePath());

        // Cast to FormatReader to get the coordinate methods
        FormatReader base = (FormatReader) bir.getReader();

        for (int s = 0; s < bir.getSeriesCount(); s++) {
            bir.setSeries(s);
            base.setSeries(s);

            int sizeT = bir.getSizeT();
            int planeCount = bir.getImageCount();

            for (int i = 0; i < planeCount; i++) {
                // render AWT image
                BufferedImage img = bir.openImage(i);

                // OPTION A: single call to getZCTCoords
                int[] zct = base.getZCTCoords(i);
                int z = zct[0];
                int c = zct[1];
                int t = zct[2];

                // OPTION B: three modulo calls instead
                // int z = base.getModuloZ(i);
                // int c = base.getModuloC(i);
                // int t = base.getModuloT(i);

                String label = String.format("S%d-C%d-Z%d%s",
                        s, c, z,
                        (sizeT > 1 ? "-T" + t : ""));
                out.add(new LifPlane(label, img));
            }
        }

        bir.close();
        return out;
    }
}
