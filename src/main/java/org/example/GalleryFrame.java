package org.example;

import com.formdev.flatlaf.FlatLightLaf;
import loci.common.services.ServiceFactory;
import loci.formats.in.LIFReader;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;
import ome.xml.meta.OMEXMLMetadata;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.ImagePanel.blendChannels;
import static org.example.ImagePanel.computeMIP;

/**
 * GalleryFrame is the main GUI window for LIF image gallery.
 * <p>
 * This application is designed as a semester project for Programming in Java Language.
 * It is done for browsing and interacting with microscopy datasets,
 * especially Leica LIF files. It provides a tree-based view of the image hierarchy,
 * support for brightness, contrast, and zoom controls, and functionality to blend
 * multi-channel images or view metadata.
 * <p>
 * Features:
 * <ul>
 *   <li>Support for loading LIF files, individual images, or image folders</li>
 *   <li>Tree-based navigation of series, channels, and planes</li>
 *   <li>Adjustable image parameters: brightness, contrast, zoom, Z-slice</li>
 *   <li>Maximum Intensity Projection (MIP) rendering</li>
 *   <li>Multi-channel blending with live preview</li>
 *   <li>OME-XML metadata inspection</li>
 *   <li>Saving images in PNG or TIFF format</li>
 * </ul>
 * <p>
 * Dependencies:
 * <ul>
 *   <li>Bio-Formats (for LIF parsing and OME metadata)</li>
 *   <li>FlatLaf (for modern UI appearance)</li>
 * </ul>
 */


public class GalleryFrame extends JFrame {

    private final DefaultTreeModel treeModel;
    private final JTree imageTree;
    private final ImagePanel imagePanel = new ImagePanel();
    private final Map<String, BufferedImage> channelBaseMap = new LinkedHashMap<>();
    private final JSlider brightnessSlider = new JSlider(50, 400, 100);
    private final JSlider contrastSlider = new JSlider(-100, 100, 0);
    private final JSlider zoomSlider = new JSlider(0, 400, 100);
    private final JSlider zSlider = new JSlider(0, 0, 0);  // min = max = 0 ⇒ disabled
    private boolean mipEnabled = false;
    private File lastOpened = null;

    /**
     * Constructs a new GalleryFrame window, initializing all UI components,
     * menus, sliders, listeners, and appearance settings.
     * <p>
     * Loads the FlatLaf theme, configures the tree, sets default zoom,
     * and prepares interactive elements.
     */

    public GalleryFrame() {
        super("Image / LIF Gallery");

        // 1) Install FlatLaf
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ignored) {
        }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 2) Empty tree root
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No file loaded");
        treeModel = new DefaultTreeModel(root);
        imageTree = new JTree(treeModel);
        imageTree.setRootVisible(false);
        imageTree.getSelectionModel()
                .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // 3) Custom icons in the tree
        imageTree.setCellRenderer(new IconTreeCellRenderer());

        initMenuBar();
        initViewMenu();          // <— hook in our MIP toggle
        initToolBar();
        initUI();
        initListeners();

        // initial zoom = 1.0×
        imagePanel.setZoom(zoomSlider.getValue() / 100.0);
    }

//    /**
//     * Blends up to three grayscale channels into an RGB composite.
//     * <p>
//     * The first three channels are mapped to Red, Green, and Blue.
//     * Each pixel’s intensity is taken as the maximum of its R/G/B values
//     * to simulate grayscale strength in color.
//     *
//     * @param chans A list of grayscale BufferedImages (1–3 items)
//     * @return A single ARGB BufferedImage representing the RGB blend
//     */
//
//    private static BufferedImage blendChannels(List<BufferedImage> chans) {
//        int w = chans.get(0).getWidth();
//        int h = chans.get(0).getHeight();
//        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//
//        for (int y = 0; y < h; y++) {
//            for (int x = 0; x < w; x++) {
//                int r = 0, g = 0, b = 0, a = 0;
//
//                for (int i = 0; i < chans.size() && i < 3; i++) {
//                    int px = chans.get(i).getRGB(x, y);
//                    int ai = (px >>> 24) & 0xFF;
//                    int ri = (px >>> 16) & 0xFF;
//                    int gi = (px >>> 8) & 0xFF;
//                    int bi = (px) & 0xFF;
//
//                    // true intensity: max of the three channels
//                    int gray = Math.max(Math.max(ri, gi), bi);
//
//                    if (i == 0) r = gray;
//                    else if (i == 1) g = gray;
//                    else b = gray;
//
//                    // keep the highest alpha
//                    a = Math.max(a, ai);
//                }
//
//                int pix = (a << 24) | (r << 16) | (g << 8) | b;
//                out.setRGB(x, y, pix);
//            }
//        }
//        return out;
//    }

//    /**
//     * Computes a Maximum Intensity Projection (MIP) from a list of grayscale images.
//     * <p>
//     * For each pixel, the highest intensity across the stack is selected for each channel (ARGB).
//     * This method is used to flatten Z-stacks into a single representative image.
//     *
//     * @param imgs A list of images representing Z-stack slices
//     * @return A new BufferedImage with maximum per-pixel intensities
//     */
//
//    private static BufferedImage computeMIP(List<BufferedImage> imgs) {
//        int w = imgs.get(0).getWidth(), h = imgs.get(0).getHeight();
//        BufferedImage mip = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//        for (int y = 0; y < h; y++) {
//            for (int x = 0; x < w; x++) {
//                int maxA = 0, maxR = 0, maxG = 0, maxB = 0;
//                for (BufferedImage bi : imgs) {
//                    int argb = bi.getRGB(x, y);
//                    int a = (argb >>> 24) & 0xFF,
//                            r = (argb >>> 16) & 0xFF,
//                            g = (argb >>> 8) & 0xFF,
//                            b = (argb) & 0xFF;
//                    if (a > maxA) maxA = a;
//                    if (r > maxR) maxR = r;
//                    if (g > maxG) maxG = g;
//                    if (b > maxB) maxB = b;
//                }
//                int pix = (maxA << 24) | (maxR << 16) | (maxG << 8) | maxB;
//                mip.setRGB(x, y, pix);
//            }
//        }
//        return mip;
//    }

    /**
     * Initializes the application menu bar with file-related actions:
     * Open, Save As, and Exit. Keyboard shortcuts are also configured.
     */

    private void initMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");

        JMenuItem saveAs = new JMenuItem("Save As…");
        saveAs.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
        saveAs.addActionListener(e -> saveCurrentImage());
        file.add(saveAs);

        JMenuItem open = new JMenuItem("Open…");
        open.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        open.addActionListener(e -> choosePath());

        JMenuItem exit = new JMenuItem("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
        exit.addActionListener(e -> dispose());

        file.add(open);
        file.addSeparator();
        file.add(exit);
        bar.add(file);
        setJMenuBar(bar);
    }

    /**
     * Creates and configures the toolbar with controls for:
     * <ul>
     *   <li>Opening files/folders</li>
     *   <li>Fitting image to window</li>
     *   <li>Resetting zoom</li>
     *   <li>Launching the blend dialog</li>
     *   <li>Displaying image metadata</li>
     * </ul>
     * <p>
     * Toolbar icons are loaded from resource paths.
     */

    private void initToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton openBtn = makeToolButton("icons/open (1).png", "Open (Ctrl+O)",
                e -> choosePath());
        JButton fitBtn = makeToolButton("icons/fit (1).png", "Fit to Window",
                e -> {
                    imagePanel.fitToWindow();
                    zoomSlider.setValue((int) (imagePanel.getZoom() * 100));
                });
        JButton resetBtn = makeToolButton("icons/reset (1).png", "Reset Zoom",
                e -> {
                    zoomSlider.setValue(100);
                    imagePanel.setZoom(1.0);
                });

        toolbar.add(openBtn);
        toolbar.addSeparator();
        toolbar.add(fitBtn);
        toolbar.add(resetBtn);

        JButton blendBtn = new JButton("Blend");
        blendBtn.setToolTipText("Blend channels in the selected series");
        blendBtn.addActionListener(e -> showBlendDialog());
        toolbar.addSeparator();
        toolbar.add(blendBtn);

        JButton metaBtn = new JButton("Metadata");
        metaBtn.setToolTipText("Show image metadata");
        metaBtn.addActionListener(e -> {
            try {
                showMetadataDialog();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        toolbar.addSeparator();
        toolbar.add(metaBtn);


        getContentPane().add(toolbar, BorderLayout.NORTH);
    }

    /**
     * Opens a dialog displaying detailed metadata for the currently loaded LIF file.
     * <p>
     * Uses the Bio-Formats OMEXML service to extract OME-compliant metadata
     * such as image dimensions, series count, and physical pixel sizes.
     *
     * @throws IOException If reading the file fails or metadata cannot be extracted
     */

    private void showMetadataDialog() throws IOException {
        if (lastOpened == null || !lastOpened.getName().toLowerCase().endsWith(".lif")) {
            JOptionPane.showMessageDialog(this,
                    "No LIF file loaded or metadata unavailable.",
                    "Metadata", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder info = new StringBuilder();
        LIFReader reader = new LIFReader();
        OMEXMLMetadata meta = null;
        try {
            // Initialize Bio-Formats OME metadata store
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            meta = service.createOMEXMLMetadata();

            reader.setMetadataStore((MetadataStore) meta);
            reader.setId(lastOpened.getAbsolutePath());

            int seriesCount = reader.getSeriesCount();
            info.append("File: ").append(lastOpened.getName()).append("\n")
                    .append("Series count: ").append(seriesCount).append("\n\n");

            for (int s = 0; s < seriesCount; s++) {
                reader.setSeries(s);
                int sizeX = reader.getSizeX();
                int sizeY = reader.getSizeY();
                int sizeZ = reader.getSizeZ();
                int sizeC = reader.getSizeC();
                int sizeT = reader.getSizeT();

                info.append("Series ").append(s).append(":\n")
                        .append("  Dimensions: ")
                        .append(sizeX).append(" × ")
                        .append(sizeY).append(" × ")
                        .append(sizeZ).append(" (Z), ")
                        .append(sizeC).append(" (C), ")
                        .append(sizeT).append(" (T)\n");

                // physical pixel size, if present
                Double px = meta.getPixelsPhysicalSizeX(s) != null
                        ? meta.getPixelsPhysicalSizeX(s).value().doubleValue() : null;
                Double py = meta.getPixelsPhysicalSizeY(s) != null
                        ? meta.getPixelsPhysicalSizeY(s).value().doubleValue() : null;
                if (px != null && py != null) {
                    info.append(String.format("  Physical size: %.3f µm × %.3f µm\n\n", px, py));
                } else {
                    info.append("\n");
                }
            }
        } catch (Exception ex) {
            info.append("Error reading metadata:\n")
                    .append(ex.getMessage());
        } finally {
            reader.close();
        }

        // show in scrollable text area
        JTextArea ta = new JTextArea(info.toString());
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(ta);
        scroll.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(this, scroll,
                "Metadata for " + lastOpened.getName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Opens a dialog allowing the user to blend multiple channels from a selected image series.
     * <p>
     * The dialog includes:
     * <ul>
     *   <li>Checkboxes for selecting channels</li>
     *   <li>A brightness slider for preview adjustment</li>
     *   <li>A live preview area</li>
     * </ul>
     * When the user confirms, the selected channels are blended and shown in the main panel.
     */

    private void showBlendDialog() {
        // 1) Determine the selected “Series” node
        TreePath sel = imageTree.getSelectionPath();
        DefaultMutableTreeNode seriesNode = null;
        if (sel != null) {
            TreeNode n = (TreeNode) sel.getLastPathComponent();
            while (n != null) {
                if (n.toString().startsWith("Series ")) {
                    seriesNode = (DefaultMutableTreeNode) n;
                    break;
                }
                n = n.getParent();
            }
        }
        if (seriesNode == null) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            if (root.getChildCount() > 0)
                seriesNode = (DefaultMutableTreeNode) root.getChildAt(0);
        }
        if (seriesNode == null) {
            JOptionPane.showMessageDialog(this,
                    "No series available to blend.",
                    "Blend", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2) Collect per-channel base images (MIP or single slice)
        LinkedHashMap<String, BufferedImage> channelMap = new LinkedHashMap<>();
        for (Enumeration<?> ec = seriesNode.children(); ec.hasMoreElements(); ) {
            DefaultMutableTreeNode chanNode = (DefaultMutableTreeNode) ec.nextElement();
            String chanLabel = chanNode.getUserObject().toString();

            List<BufferedImage> imgs = new ArrayList<>();
            for (Enumeration<?> ei = chanNode.children(); ei.hasMoreElements(); ) {
                Object uo = ((DefaultMutableTreeNode) ei.nextElement()).getUserObject();
                if (uo instanceof LifPlane)
                    imgs.add(((LifPlane) uo).image);
            }
            BufferedImage base = (imgs.size() > 1)
                    ? computeMIP(imgs)
                    : ImagePanel.toARGB(imgs.get(0));
            channelMap.put(chanLabel, base);
        }
        if (channelMap.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Selected series has no channels.",
                    "Blend", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3) Build dialog
        JDialog dlg = new JDialog(this, "Blend Channels in " + seriesNode, true);
        dlg.setLayout(new BorderLayout(8, 8));
        dlg.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 3a) Checkboxes on the left
        JPanel boxPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        LinkedHashMap<String, JCheckBox> checks = new LinkedHashMap<>();
        channelMap.forEach((label, img) -> {
            JCheckBox cb = new JCheckBox(label, true);
            checks.put(label, cb);
            boxPanel.add(cb);
        });
        dlg.add(new JScrollPane(boxPanel), BorderLayout.WEST);

        // 3b) Brightness slider + preview on the right
        JSlider brightSlider = new JSlider(0, 400, 100);
        brightSlider.setMajorTickSpacing(50);
        brightSlider.setMinorTickSpacing(10);
        brightSlider.setPaintTicks(true);
        brightSlider.setPaintLabels(true);
        brightSlider.setBorder(BorderFactory.createTitledBorder("Preview Brightness (%)"));

        JLabel preview = new JLabel();
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setPreferredSize(new Dimension(300, 300));

        JPanel previewPanel = new JPanel(new BorderLayout(4, 4));
        previewPanel.add(brightSlider, BorderLayout.NORTH);
        previewPanel.add(preview, BorderLayout.CENTER);
        dlg.add(previewPanel, BorderLayout.CENTER);

        // 3c) OK/Cancel buttons
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        JPanel btnPanel = new JPanel();
        btnPanel.add(ok);
        btnPanel.add(cancel);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        // 4) Live‐update helper
        Runnable updatePreview = () -> {
            // 4a) blend checked channels
            List<BufferedImage> toBlend = checks.entrySet().stream()
                    .filter(e -> e.getValue().isSelected())
                    .map(e -> channelMap.get(e.getKey()))
                    .collect(Collectors.toList());
            if (toBlend.isEmpty()) {
                preview.setIcon(null);
                return;
            }
            BufferedImage comp = blendChannels(toBlend);

            // 4b) apply brightness
            float bfac = brightSlider.getValue() / 100f;
            RescaleOp op = new RescaleOp(bfac, 0, null);
            BufferedImage bright = op.filter(comp, null);

            // 4c) scale to fixed preview size
            Dimension ps = preview.getPreferredSize();
            Image scaled = bright.getScaledInstance(ps.width, ps.height, Image.SCALE_SMOOTH);
            preview.setIcon(new ImageIcon(scaled));
        };

        // 5) Wire listeners
        checks.values().forEach(cb -> cb.addItemListener(e -> updatePreview.run()));
        brightSlider.addChangeListener(e -> updatePreview.run());
        updatePreview.run();

        ok.addActionListener(e -> {
            // on OK, blend & send composite (without brightness op)
            List<BufferedImage> toBlend = checks.entrySet().stream()
                    .filter(en -> en.getValue().isSelected())
                    .map(en -> channelMap.get(en.getKey()))
                    .collect(Collectors.toList());
            if (!toBlend.isEmpty()) {
                BufferedImage finalComp = blendChannels(toBlend);
                imagePanel.setImage(finalComp);
            }
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());

        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    /**
     * Creates a JButton with a given icon and action, falling back to text if icon loading fails.
     *
     * @param resourcePath Path to icon resource (relative to classpath)
     * @param tooltip      Tooltip text shown on hover
     * @param al           Action listener for the button
     * @return A configured JButton instance
     */

    private JButton makeToolButton(String resourcePath, String tooltip, ActionListener al) {
        JButton btn = new JButton();
        btn.setToolTipText(tooltip);
        btn.addActionListener(al);

        // try loading the icon from /icons/...
        try (InputStream in = getClass().getResourceAsStream("/" + resourcePath)) {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                btn.setIcon(new ImageIcon(img));
            } else {
                // resource missing → fall back to text
                btn.setText(tooltip);
            }
        } catch (IOException e) {
            // I/O error reading the PNG → fall back to text
            btn.setText(tooltip);
        }

        return btn;
    }

    /**
     * Initializes the 'View' menu, including a toggle for enabling/disabling
     * Maximum Intensity Projection (MIP) mode.
     * <p>
     * Reloads the current file with or without MIP when toggled.
     */

    private void initViewMenu() {
        JMenu view = new JMenu("View");
        JCheckBoxMenuItem mipItem = new JCheckBoxMenuItem("Show MIP");
        mipItem.addActionListener(e -> {
            mipEnabled = mipItem.isSelected();
            // re-load whatever was open to rebuild the tree
            if (lastOpened != null) {
                if (lastOpened.isDirectory()) loadDirectory(lastOpened);
                else loadFile(lastOpened);
            }
        });
        view.add(mipItem);
        getJMenuBar().add(view);
    }

    /**
     * Wraps a slider with a descriptive label in a consistent horizontal layout.
     *
     * @param text   Label text to display on the left
     * @param slider Slider component to be labeled
     * @return JPanel containing the label and slider
     */

    private JPanel makeLabeledSlider(String text, JSlider slider) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(text), BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        return row;
    }

    /**
     * Constructs and arranges the main user interface layout, including:
     * <ul>
     *   <li>The image tree panel on the left</li>
     *   <li>Control sliders (brightness, contrast, zoom, Z-slice)</li>
     *   <li>The image display panel</li>
     * </ul>
     */

    private void initUI() {
        JScrollPane treeScroll = new JScrollPane(imageTree);
        treeScroll.setPreferredSize(new Dimension(240, 0));

        // 4 rows, one per slider
        JPanel controls = new JPanel(new GridLayout(4, 1, 6, 6));
        controls.add(makeLabeledSlider("Brightness", brightnessSlider));
        controls.add(makeLabeledSlider("Contrast", contrastSlider));
        controls.add(makeLabeledSlider("Zoom", zoomSlider));
        controls.add(makeLabeledSlider("Z-slice", zSlider));

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.add(controls, BorderLayout.NORTH);
        right.add(imagePanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                treeScroll, right
        );
        split.setDividerLocation(240);

        getContentPane().add(split, BorderLayout.CENTER);
    }

    /**
     * Updates the Z-slice slider to reflect the number of available slices
     * in the current image stack.
     * <p>
     * Enables the slider only if multiple Z-planes are available.
     */

    private void prepareZSlider() {
        int n = imagePanel.getSliceCount();
        zSlider.setMinimum(0);
        zSlider.setMaximum(Math.max(0, n - 1));
        zSlider.setValue(0);
        zSlider.setEnabled(n > 1);
    }

    /**
     * Attaches event listeners to all interactive UI components:
     * <ul>
     *   <li>Tree selection: handles what image(s) to load</li>
     *   <li>Sliders: brightness, contrast, zoom, Z-slice</li>
     * </ul>
     * <p>
     * These listeners coordinate image updates and viewer behavior.
     */

    private void initListeners() {
        TreeSelectionListener tsl = e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    imageTree.getLastSelectedPathComponent();
            if (node == null) return;

            Object uo = node.getUserObject();

            try {
                // 1) If it’s a Channel node → load the WHOLE stack
                if (uo instanceof String && ((String) uo).startsWith("Channel")) {
                    List<BufferedImage> planes = new ArrayList<>();
                    Enumeration<?> kids = node.children();
                    while (kids.hasMoreElements()) {
                        Object kid = ((DefaultMutableTreeNode) kids.nextElement()).getUserObject();
                        if (kid instanceof LifPlane) {
                            planes.add(((LifPlane) kid).image);
                        }
                    }
                    imagePanel.setStack(planes);
                }
                // 2) If it’s a directory → load all files as a stack
                else if (uo instanceof File && ((File) uo).isDirectory()) {
                    List<BufferedImage> imgs = new ArrayList<>();
                    for (File f : ImageLoader.loadImages((File) uo)) {
                        try {
                            imgs.add(ImageIO.read(f));
                        } catch (IOException ex) { /* skip */ }
                    }
                    imagePanel.setStack(imgs);
                }
                // 3) If it’s a plane under a Channel → just move the slice
                else if (uo instanceof LifPlane) {
                    DefaultMutableTreeNode chanNode =
                            (DefaultMutableTreeNode) node.getParent();
                    int idx = 0;
                    for (Enumeration<?> en = chanNode.children(); en.hasMoreElements(); idx++) {
                        DefaultMutableTreeNode kid =
                                (DefaultMutableTreeNode) en.nextElement();
                        if (kid == node) {
                            imagePanel.setSlice(idx);
                            break;
                        }
                    }
                }
                // 4) A standalone file → single image
                else if (uo instanceof File) {
                    imagePanel.setImage((File) uo);
                }

                prepareZSlider();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(GalleryFrame.this,
                        "Error loading image(s):\n" + ex.getMessage(),
                        "Load error", JOptionPane.ERROR_MESSAGE);
            }
        };

        imageTree.addTreeSelectionListener(tsl);

        brightnessSlider.addChangeListener(e ->
                imagePanel.setBrightness(brightnessSlider.getValue() / 100f));
        contrastSlider.addChangeListener(e ->
                imagePanel.setContrast(contrastSlider.getValue()));
        zoomSlider.addChangeListener(e ->
                imagePanel.setZoom(zoomSlider.getValue() / 100.0));
        zSlider.addChangeListener(e ->
                imagePanel.setSlice(zSlider.getValue()));
    }

    /**
     * Opens a file chooser dialog to let the user select a file or directory.
     * <p>
     * Automatically determines the type (LIF, image, or folder) and dispatches
     * to the appropriate loader method.
     */

    private void choosePath() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        // Only show LIF & common image types
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "LIF / Images", "lif", "png", "jpg", "jpeg", "bmp", "gif");
        fc.addChoosableFileFilter(filter);
        fc.setFileFilter(filter);

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            lastOpened = f;

            String name = f.getName().toLowerCase();
            if (f.isDirectory()) {
                loadDirectory(f);
            } else if (name.endsWith(".lif")) {
                loadFile(f);
            } else if (name.matches(".*\\.(png|jpe?g|bmp|gif)$")) {
                loadImageFile(f);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Unsupported file type: " + f.getName(),
                        "Open", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Loads a single image file (e.g. PNG, JPG) and sets it as the current image.
     *
     * @param imgFile The file to load
     */

    private void loadImageFile(File imgFile) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(imgFile.getName());
        // add the File itself so your existing listener will call setImage(...)
        root.add(new DefaultMutableTreeNode(imgFile));
        refreshTree(root);
        prepareZSlider();
    }

    /**
     * Loads all supported image files (PNG, JPG, etc.) from the given directory,
     * and builds a stack view in the tree panel.
     *
     * @param dir Directory containing images
     */

    private void loadDirectory(File dir) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(dir.getName());
        List<File> imgs = ImageLoader.loadImages(dir);
        for (File img : imgs) {
            root.add(new DefaultMutableTreeNode(img));
        }
        refreshTree(root);

        prepareZSlider();
    }

    /**
     * Loads and parses a Leica LIF microscopy file.
     * <p>
     * Organizes the file contents into a hierarchy of Series → Channel → Planes,
     * and optionally generates MIPs for each channel.
     *
     * @param f The LIF file to load
     */

    private void loadFile(File f) {
        // remember for “View → Show MIP” reload and for blending
        lastOpened = f;
        channelBaseMap.clear();

        try {
            // root of the tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(f.getName());

            // 1) Load all planes from the LIF file
            List<LifPlane> planes = LifImageLoader.load(f);

            // 2) Group into series → channel
            Map<Integer, DefaultMutableTreeNode> seriesMap = new LinkedHashMap<>();
            for (LifPlane lp : planes) {
                String[] parts = lp.label.split("[-:]");
                int s = Integer.parseInt(parts[0].substring(1));
                int c = Integer.parseInt(parts[1].substring(1));

                DefaultMutableTreeNode seriesNode =
                        seriesMap.computeIfAbsent(s, key -> {
                            DefaultMutableTreeNode n = new DefaultMutableTreeNode("Series " + key);
                            root.add(n);
                            return n;
                        });

                String chanLabel = "Channel " + c;
                DefaultMutableTreeNode chanNode = null;
                for (Enumeration<?> e = seriesNode.children(); e.hasMoreElements(); ) {
                    DefaultMutableTreeNode kid = (DefaultMutableTreeNode) e.nextElement();
                    if (chanLabel.equals(kid.getUserObject())) {
                        chanNode = kid;
                        break;
                    }
                }
                if (chanNode == null) {
                    chanNode = new DefaultMutableTreeNode(chanLabel);
                    seriesNode.add(chanNode);
                }
                chanNode.add(new DefaultMutableTreeNode(lp));
            }

            // 3) For each channel, compute its “base” image (MIP if needed), store it,
            //    and optionally append a MIP leaf
            for (DefaultMutableTreeNode seriesNode : seriesMap.values()) {
                for (Enumeration<?> ec = seriesNode.children(); ec.hasMoreElements(); ) {
                    DefaultMutableTreeNode chanNode = (DefaultMutableTreeNode) ec.nextElement();
                    String chanLabel = chanNode.getUserObject().toString();

                    // collect all z‐planes for this channel
                    List<BufferedImage> imgs = new ArrayList<>();
                    for (Enumeration<?> ei = chanNode.children(); ei.hasMoreElements(); ) {
                        Object uo = ((DefaultMutableTreeNode) ei.nextElement()).getUserObject();
                        if (uo instanceof LifPlane) {
                            imgs.add(((LifPlane) uo).image);
                        }
                    }

                    // compute base: MIP if >1 slice, else the single plane
                    BufferedImage base = (imgs.size() > 1)
                            ? computeMIP(imgs)
                            : ImagePanel.toARGB(imgs.get(0));

                    // keep for later blending
                    channelBaseMap.put(chanLabel, base);

                    // if MIP-view is on and we have a true stack, add a leaf
                    if (mipEnabled && imgs.size() > 1) {
                        BufferedImage mip = computeMIP(imgs);
                        chanNode.add(new DefaultMutableTreeNode(
                                new LifPlane(chanLabel + "-MIP", mip)
                        ));
                    }
                }
            }

            // 4) Refresh the JTree and enable the Z-slider appropriately
            refreshTree(root);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Cannot read file:\n" + ex.getMessage(),
                    "Load error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens a save dialog to export the currently displayed image to disk.
     * <p>
     * Supports PNG and TIFF formats. Applies brightness/contrast/zoom adjustments
     * before saving.
     */

    private void saveCurrentImage() {
        // grab the already‐adjusted image
        BufferedImage img = imagePanel.getAdjustedImage();
        if (img == null) {
            JOptionPane.showMessageDialog(this,
                    "No image to save.",
                    "Save As", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Image As");
        FileNameExtensionFilter pngFilter =
                new FileNameExtensionFilter("PNG Image (*.png)", "png");
        FileNameExtensionFilter tiffFilter =
                new FileNameExtensionFilter("TIFF Image (*.tif, *.tiff)", "tif", "tiff");
        fc.addChoosableFileFilter(pngFilter);
        fc.addChoosableFileFilter(tiffFilter);
        fc.setFileFilter(pngFilter);

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            String ext = fc.getFileFilter() == tiffFilter ? "tif" : "png";
            if (!out.getName().toLowerCase().endsWith("." + ext)) {
                out = new File(out.getAbsolutePath() + "." + ext);
            }
            try {
                ImageIO.write(img, ext, out);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save:\n" + ex.getMessage(),
                        "Save As", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Traverses the tree model to locate the first channel node.
     *
     * @param root Root node to start searching from
     * @return First "Channel X" node found, or null if none
     */

    private DefaultMutableTreeNode findFirstChannel(DefaultMutableTreeNode root) {
        Enumeration<?> seriesKids = root.children();
        while (seriesKids.hasMoreElements()) {
            DefaultMutableTreeNode series = (DefaultMutableTreeNode) seriesKids.nextElement();
            Enumeration<?> chanKids = series.children();
            while (chanKids.hasMoreElements()) {
                DefaultMutableTreeNode chan = (DefaultMutableTreeNode) chanKids.nextElement();
                Object uo = chan.getUserObject();
                if (uo instanceof String && ((String) uo).startsWith("Channel")) {
                    return chan;
                }
            }
        }
        return null;
    }

    /**
     * Replaces the JTree's root node, expands all visible paths,
     * and selects the first usable image node (preferably a channel).
     *
     * @param root The new root node to apply to the tree
     */

    private void refreshTree(DefaultMutableTreeNode root) {
        treeModel.setRoot(root);

        // expand all rows
        for (int i = 0; i < imageTree.getRowCount(); i++) {
            imageTree.expandRow(i);
        }

        // find & select the first “Channel …” node
        DefaultMutableTreeNode firstChannel = findFirstChannel(root);
        if (firstChannel != null) {
            imageTree.setSelectionPath(new TreePath(firstChannel.getPath()));
        }
        // fallback to first leaf
        else {
            DefaultMutableTreeNode leaf = root.getFirstLeaf();
            if (leaf != null) {
                imageTree.setSelectionPath(new TreePath(leaf.getPath()));
            }
        }

        prepareZSlider();
    }

    /**
     * Custom TreeCellRenderer that assigns icons based on node type:
     * <ul>
     *   <li>Series and Channel nodes get folder-style icons</li>
     *   <li>Image plane nodes (e.g., LifPlane) get leaf icons</li>
     * </ul>
     */

    private static class IconTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Icon seriesIcon = UIManager.getIcon("Tree.closedIcon");
        private final Icon channelIcon = UIManager.getIcon("Tree.closedIcon");
        private final Icon planeIcon = UIManager.getIcon("Tree.leafIcon");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded, boolean leaf, int row, boolean focus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object user = node.getUserObject();
            if (user instanceof LifPlane || (leaf && user instanceof File)) {
                setIcon(planeIcon);
            } else {
                setIcon(expanded ? seriesIcon : channelIcon);
            }
            return this;
        }
    }
}
