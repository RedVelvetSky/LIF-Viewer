# LIF Viewer

LIF Viewer is a desktop Java application made mostly for quick browsing and visualizing of microscopy images, with special support for Leica LIF files. It includes metadata inspection, image blending, zoom and brightness controls, and some export functionality to common formats.

I thought it would be nice to make something lightweight if you don't want to install the whole Fiji ImageJ distribution.

---

## 📦 Features

### LIF File Support and Metadata

- Visualizes Leica `.lif` files in a tree view (Series → Channels → Planes)
- Extracts metadata: image dimensions, Z/C/T counts, physical pixel size
- Supports Maximum Intensity Projection (MIP) and Z-stack browsing

### Multi-Channel Image Blending

- Click `Blend` button, select grayscale channels, and blend them together
- Adjust brightness for a better live preview
- The Composite result is shown in the main panel immediately

### Standard Image and Batch Folder Support

- Supports common formats: PNG, JPG, JPEG, BMP, GIF
- Load entire folders as Z-stacks
- Tree navigation supports image preview and single/stack selection

### Modernish UI (Swing + FlatLaf)

- Tree-based navigation with selection handlers
- Sliders for brightness, contrast, zoom, and Z-slice control
- Toolbar and menu bar for quick access
- Keyboard shortcuts:
    - `Ctrl+O` – Open
    - `Ctrl+S` – Save As
    - `Ctrl+Q` – Quit

### Image Saving

- Save the current view (with all adjustments) as PNG or TIFF
- Export available via menu or shortcut

---

## 🧑‍💻 How to Use

### Opening Files

- Use **File → Open** or the toolbar
- Supports:
    - `.lif`
    - `.png`, `.jpg`, `.jpeg`, `.bmp`, `.gif`

### Navigating the Image Tree

- `.lif` files: Series → Channels → Planes (z-stacks)

### Adjusting Display

- Use sliders on the right:
    - Brightness
    - Contrast
    - Zoom
    - Z-slice (if available in `.lif`)

### Blending Channels

1. Select a series from the tree
2. Click **Blend** on the toolbar
3. Choose channels and adjust brightness if needed
4. Press OK to generate a composite image

### Viewing Metadata

- Click **Metadata** on the toolbar after loading `.lif` file
- Opens a scrollable dialog showing some essential series metadata

### Saving Images

- Use **File → Save As** or `Ctrl+S`
- Choose PNG or TIFF
- Adjusted image is exported

### Enabling MIP View

- Use **View → Show MIP** to toggle
- App adds MIP nodes into the tree

---

## 🛠️ Setup & Build from source

### Prerequisites

- Java Development Kit (JDK) 21
- Apache Maven 3.6+
- Internet connection (for dependencies)

### Project Structure

```
src/main/java # Application source
src/test/java # Unit tests
src/main/javadoc/ # Javadoc overview
lib/bioformats_package.jar # Manually installed Bio-Formats library
```


### 📦 Installing Bio-Formats Manually

Due to Maven issues, install the Bio-Formats JAR manually:

```bash
mvn install:install-file \
  -Dfile=lib/bioformats_package.jar \
  -DgroupId=org.openmicroscopy \
  -DartifactId=bioformats_package \
  -Dversion=6.14.1 \
  -Dpackaging=jar
```

This registers the JAR locally so it can be used like a standard Maven dependency.

### 🔧 Build

```bash
mvn clean compile
```

### 🚀 Run the App

```bash
mvn clean compile exec:java
```

### ✅ Run Unit Tests

```bash
mvn clean test
```

Test reports are in: `target/surefire-reports/`

### 📚 Generate Javadoc

```bash
mvn javadoc:javadoc
```

Open in browser: `target/site/apidocs/index.html`

### ☕ Java Configuration

Project targets Java 21. Your pom.xml should include:

```bash
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>
```

Make sure your local JDK version is correct:

```bash
java -version
```

### 📬 Contact

For issues or suggestions, contact the developer (me) somehow or open an issue in the repository.

**License:** [MIT](LICENSE)