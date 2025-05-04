package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Custom Swing cell renderer for displaying {@link File} objects in a {@link JList}.
 * <p>
 * This renderer overrides the default behavior to show only the file name
 * (not the full path), which improves readability in file selection UIs.
 */

class FileListCellRenderer extends DefaultListCellRenderer {
    /**
     * Configures the rendering of a single list cell.
     * <p>
     * If the item is a {@link File}, only its name (not full path) is displayed.
     *
     * @param list         The list that is asking the renderer to draw
     * @param value        The value to assign to the cell
     * @param index        The cell's index
     * @param isSelected   True if the specified cell is selected
     * @param cellHasFocus True if the specified cell has the focus
     * @return The component used to render the cell
     */

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof File) setText(((File) value).getName());
        return this;
    }
}