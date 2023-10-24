package com.emgariko.diskspacetool.model;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.nio.file.Path;

public class CustomTableCellRenderer extends DefaultTableCellRenderer {

    private static final Color WHITE = Color.WHITE;
    private static final Color LIGHT_GRAY = new Color(230, 230, 230);
    private static final String RESOURCES_PATH = "resources/";
    private static final String FOLDER_ICON = "folder.png";
    private static final String FILE_ICON = "file.png";

    private static final String IN_PROGRESS_STRING = "--";
    private static final String ERROR_STRING = "ERROR";

    private static final String BYTES_STRING = "B";
    private static final String KILOBYTES_STRING = "KB";
    private static final String MEGABYTES_STRING = "MB";
    private static final String GIGABYTES_STRING = "GB";
    private static final String TERABYTES_STRING = "TB";

    private static ImageIcon getIcon(String name) {
        return new ImageIcon(RESOURCES_PATH + name);
    }


    private static String formatSize(double size, String unit) {
        return String.format("%.2f %s", size, unit);
    }

    private static String formatFileSize(long fileSizeInBytes) {
        final long KILOBYTE = 1024;
        final long MEGABYTE = 1024 * KILOBYTE;
        final long GIGABYTE = 1024 * MEGABYTE;
        final long TERABYTE = 1024 * GIGABYTE;

        if (fileSizeInBytes < KILOBYTE) {
            return formatSize(fileSizeInBytes, BYTES_STRING);
        } else if (fileSizeInBytes < MEGABYTE) {
            return formatSize((double) fileSizeInBytes / KILOBYTE, KILOBYTES_STRING);
        } else if (fileSizeInBytes < GIGABYTE) {
            return formatSize((double) fileSizeInBytes / MEGABYTE, MEGABYTES_STRING);
        } else if (fileSizeInBytes < TERABYTE) {
            return formatSize((double) fileSizeInBytes / GIGABYTE, GIGABYTES_STRING);
        } else {
            return formatSize((double) fileSizeInBytes / TERABYTE, TERABYTES_STRING);
        }
    }

    private void handleSizeValue(Long size) {
        if (size == null) {
            setText(IN_PROGRESS_STRING);
        } else if (size == -1L) {
            // TODO: show error icon
            setText(ERROR_STRING);
        } else {
            setText(formatFileSize(size));
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
            Color backgroundColor = (row % 2 == 0) ? WHITE : LIGHT_GRAY;
            c.setBackground(backgroundColor);
        }

        if (column == DirectoryTableModel.NAME_COLUMN) {
            Path path = (Path) value;
            DirectoryTableModel model = (DirectoryTableModel) table.getModel();

            setIcon(getIcon(model.isDirectory(table.convertRowIndexToModel(row)) ? FOLDER_ICON : FILE_ICON));
            setText(path.getName(path.getNameCount() - 1).toString());
        } else if (column == DirectoryTableModel.SIZE_COLUMN) {
            setIcon(null);
            Long size = (Long) value;
            handleSizeValue(size);
        }

        return c;
    }
}
