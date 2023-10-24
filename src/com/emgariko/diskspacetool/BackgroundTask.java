package com.emgariko.diskspacetool;

import com.emgariko.diskspacetool.model.DirectoryTableModel;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class BackgroundTask extends SwingWorker<Long, Void> {

    public static class CancellableDirectorySizeCalculator extends SimpleFileVisitor<Path> {
        private final BackgroundTask task;

        CancellableDirectorySizeCalculator(BackgroundTask task) {
            this.task = task;
        }

        private long totalSize = 0;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (task.isCancelled()) {
                return FileVisitResult.TERMINATE;
            }
            totalSize += attrs.size();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ignored) {
            return FileVisitResult.CONTINUE;
        }

        public long getTotalSize() {
            return totalSize;
        }
    }

    private final DirectoryTableModel model;
    private final Path root;
    private final int index;

    public BackgroundTask(DirectoryTableModel model, Path root, int index) {
        this.model = model;
        this.root = root;
        this.index = index;
    }

    private long calsSize(Path path) {
        CancellableDirectorySizeCalculator calculator = new CancellableDirectorySizeCalculator(this);
        try {
            Files.walkFileTree(path, calculator); // it does not follow symbolic links
        } catch (IOException e) {
            return -1;
        }
        return calculator.getTotalSize();
    }

    @Override
    protected Long doInBackground() {
        return calsSize(root);
    }

    @Override
    protected void done() {
        try {
            var x = get();
            model.setValueAt(x, index, 1);
        } catch (ExecutionException | InterruptedException | CancellationException e) {
            // do nothing
        }
    }
}
