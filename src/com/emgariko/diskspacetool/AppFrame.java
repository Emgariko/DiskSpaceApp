package com.emgariko.diskspacetool;

import com.emgariko.diskspacetool.model.CustomTableCellRenderer;
import com.emgariko.diskspacetool.model.DirectoryTableModel;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

public class AppFrame extends JFrame implements ActionListener {

    // UI
    private JPanel mainPanel;
    private JTextField dirTextField;
    private JButton toParentButton;
    private JTable table;

    // State
    private Path dir;
    private DirectoryTableModel model;

    private TableRowSorter<DirectoryTableModel> sorter;

    private static final int INITIAL_WIDTH = 600;
    private static final int INITIAL_HEIGHT = 400;
    private static final String TITLE = "Disk Space Tool";
    private static final String TO_PARENT_BUTTON_TEXT = "<";
    private static final int SCROLL_PANE_BORDER_THICKNESS = 5;

    private void init() {
        setTitle(TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        setContentPane(mainPanel);

        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
    }

    private void tryGoToParentDirectory() {
        var parent = dir.getParent();
        if (parent == null) { return; }

        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(parent, BasicFileAttributes.class);
        } catch (IOException ex) {
            // cannot open directory
            return;
        }

        if (attrs.isDirectory()) {
            updateStateAndShow(parent);
        }
    }

    // toParent button handler
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == toParentButton) {
            tryGoToParentDirectory();
        }
    }

    private void textFieldEnterPressedHandler(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            dirTextField.setEnabled(false);
            String text = dirTextField.getText();

            try {
                Path newDir = Path.of(text).normalize();

                if (!newDir.isAbsolute()) {
                    dirTextField.setText(dir.toString());
                    return;
                }

                BasicFileAttributes attrs = Files.readAttributes(newDir, BasicFileAttributes.class);
                if (attrs.isDirectory()) {
                    // Note: newDir could potentially represent the same path as before.
                    // In such cases, we avoid calling updateStateAndShow to prevent unnecessary updates.
                    // However, if newDir has undergone changes during the .normalize() process,
                    // then the textField should reflect the normalized path.
                    dirTextField.setText(newDir.toString());
                    if (dir.equals(newDir)) { return; }
                    updateStateAndShow(newDir);
                } else {
                    dirTextField.setText(dir.toString());
                }
            } catch (InvalidPathException | IOException ex) {
                // invalid path or cannot open directory
                dirTextField.setText(dir.toString());
            }
        }
    }

    private void dirTextFieldMouseClickHandler() {
        dirTextField.setEnabled(true);
        dirTextField.requestFocus();
    }

    private void addToolPanel() {
        // TODO: could be replaced with JToolBar
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.X_AXIS));
        mainPanel.add(toolPanel);

        toParentButton = new JButton(TO_PARENT_BUTTON_TEXT);
        toParentButton.addActionListener(this);
        toolPanel.add(toParentButton);

        dirTextField = new JTextField();
        dirTextField.setEnabled(false);
        dirTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                textFieldEnterPressedHandler(e);
            }
        });
        dirTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dirTextFieldMouseClickHandler();
            }
        });

        toolPanel.add(dirTextField);
        toolPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, toolPanel.getMinimumSize().height));
    }

    private void tryGoToSelectedDirectoryHandler(int row, int column) {
        if (row >= 0 && column >= 0) {
            int index = table.convertRowIndexToModel(row);
            Path clickedItem = (Path) model.getValueAt(index, DirectoryTableModel.NAME_COLUMN);
            if (model.isDirectory(index)) {
                updateStateAndShow(clickedItem);
            }
        }
    }

    private void tableMouseClickHandler(MouseEvent e) {
        if (e.getClickCount() == 2) {
            Point point = e.getPoint();
            int row = table.rowAtPoint(point);
            int column = table.columnAtPoint(point);
            tryGoToSelectedDirectoryHandler(row, column);
        }
    }

    private void tableKeyPressedHandler(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            tryGoToSelectedDirectoryHandler(table.getSelectedRow(), table.getSelectedColumn());
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            tryGoToParentDirectory();
        }
    }

    private void addTable() {
        table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, SCROLL_PANE_BORDER_THICKNESS));
        mainPanel.add(scrollPane);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tableMouseClickHandler(e);
            }

        });

        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                tableKeyPressedHandler(e);
            }
        });

        sorter = new TableRowSorter<>();
        table.setRowSorter(sorter);
    }



    public AppFrame() {
        init();

        addToolPanel();
        addTable();

        updateStateAndShow(Paths.get(System.getProperty("user.home")));
        setVisible(true);
    }

    private void updateStateAndShow(Path newDir) {
        if (model != null) {
            model.cancelTasks();
        }

        try {
            DirectoryTableModel newModel = new DirectoryTableModel(newDir, table);
            model = newModel;
            dir = newDir;
        } catch (IOException e) { // IOException occurred while opening directory, do not change model and dir
            System.err.format("IOException occurred while opening directory: \"%s\"\n", e.getMessage());
            if (dir == null) { // first time opening app, model is null as well
                JOptionPane.showMessageDialog(this, e.getMessage() +
                        ". Home directory isn't accessible", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            dirTextField.setText(dir.toString());

            return;
        }

        dirTextField.setText(dir.toString());
        table.setModel(model);

        sorter.setModel(model);
        sorter.setComparator(1, (Comparator<Long>) Long::compareTo);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private static void createUIComponents() {
        new AppFrame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppFrame::createUIComponents);
    }
}
