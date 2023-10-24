package com.emgariko.diskspacetool.model;

import com.emgariko.diskspacetool.BackgroundTask;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DirectoryTableModel extends AbstractTableModel {

    private static class DirectoryData {

        private void calcSizes(DirectoryTableModel model) {
            for (int i = 0; i < content.size(); i++) {
                isDirectory.add(false);
                sizes.add(null);

                Path path = content.get(i);
                BasicFileAttributes attrs;
                try {
                    attrs = Files.readAttributes(path, BasicFileAttributes.class);
                } catch (IOException e) {
                    // IOException occurred, show error instead of size
                    sizes.set(i, -1L);
                    continue;
                }

                if (attrs.isDirectory()) {
                    BackgroundTask task = new BackgroundTask(model, path, i);
                    tasks.add(task);
                    task.execute();
                    isDirectory.set(i, true);
                } else if (attrs.isRegularFile()) {
                    sizes.set(i, attrs.size());
                }
            }
        }

        private final List<Path> content;
        private final ArrayList<Long> sizes;
        private final ArrayList<BackgroundTask> tasks = new ArrayList<>();
        private final ArrayList<Boolean> isDirectory;
        public final int rowsCount;

        public DirectoryData(List<Path> content, DirectoryTableModel model) {
            this.content = content;
            this.sizes = new ArrayList<>(content.size());
            this.rowsCount = content.size();
            this.isDirectory = new ArrayList<>(content.size());
            calcSizes(model);
        }

        public Path get(int index) {
            return content.get(index);
        }

        public Long getSize(int index) {
            return sizes.get(index);
        }

        public void setSize(int index, Long size) {
            sizes.set(index, size);
        }

        public void cancelTasks() {
            for (BackgroundTask task : tasks) {
                task.cancel(true);
            }
        }
    }

    public final static Integer NAME_COLUMN = 0;
    public final static Integer SIZE_COLUMN = 1;

    // TODO: better somehow place the gap before the first column name
    private final String[] columnNames = {"       Name", "Size"};
    private final DirectoryData directoryData;
    private final JTable table;

    public DirectoryTableModel(Path directory, JTable table) throws IOException {
        this.table = table;
        try (Stream<Path> childrenStream = Files.list(directory)) {
            List<Path> directoryFiles = childrenStream.toList();
            this.directoryData = new DirectoryData(directoryFiles, this);
        }
    }

    public boolean isDirectory(int rowIndex) {
        return directoryData.isDirectory.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return directoryData.rowsCount;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == NAME_COLUMN) {
            return directoryData.get(rowIndex);
        } else if (columnIndex == SIZE_COLUMN) {
            return directoryData.getSize(rowIndex);
        }

        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == SIZE_COLUMN) {
            directoryData.setSize(rowIndex, (Long) aValue);
            int[] selectedRows = table.getSelectedRows();

            fireTableDataChanged();

            table.clearSelection();
            for (int row : selectedRows) {
                if (row < table.getRowCount()) {
                    table.addRowSelectionInterval(row, row);
                }
            }
        }
    }

    public void cancelTasks() {
        directoryData.cancelTasks();
    }
}
