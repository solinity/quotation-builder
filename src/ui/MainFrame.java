package ui;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import storage.DataStore;
import storage.SessionFile;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String APP_TITLE = "Freelance Quotation Builder";

    private final DataStore dataStore;
    private final ClientsPanel clients;
    private final OfferingsPanel offerings;
    private final QuotesPanel quotes;
    private Path sessionFile;

    public MainFrame(DataStore dataStore) {
        super(APP_TITLE);

        this.dataStore = dataStore;
        clients = new ClientsPanel(dataStore);
        offerings = new OfferingsPanel(dataStore);
        quotes = new QuotesPanel(dataStore);
        setJMenuBar(createMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Clients", clients);
        tabs.addTab("Offerings", offerings);
        tabs.addTab("Quotations", quotes);
        tabs.addChangeListener(event -> {
            if (tabs.getSelectedComponent() == quotes) {
                quotes.refreshFromStore();
            }
        });
        add(tabs);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(865, 555));
        setSize(1030, 660);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenu file = new JMenu("File");
        JMenuItem save = new JMenuItem("Save Session");
        save.addActionListener(event -> {
            if (sessionFile == null) {
                saveSessionAs();
            } else {
                saveSession(sessionFile);
            }
        });
        JMenuItem saveAs = new JMenuItem("Save Session As...");
        saveAs.addActionListener(event -> saveSessionAs());
        JMenuItem load = new JMenuItem("Load Session...");
        load.addActionListener(event -> {
            Path selected = chooseFile("Load Session", FileDialog.LOAD);
            if (selected != null) {
                loadSession(selected);
            }
        });
        file.add(save);
        file.add(saveAs);
        file.addSeparator();
        file.add(load);

        JMenu help = new JMenu("Help");
        JMenuItem location = new JMenuItem("Show Session File Location");
        location.addActionListener(event -> showSessionLocation());
        help.add(location);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(file);
        menuBar.add(help);
        return menuBar;
    }

    private Path chooseFile(String title, int mode) {
        FileDialog dialog = new FileDialog(this, title, mode);
        try {
            if (sessionFile != null) {
                dialog.setDirectory(sessionFile.getParent().toString());
            }
            if (mode == FileDialog.SAVE) {
                dialog.setFile(sessionFile == null ? "quotation-session" + SessionFile.EXTENSION
                        : sessionFile.getFileName().toString());
            }
            dialog.setVisible(true);
            File[] selected = dialog.getFiles();
            return selected.length == 0 ? null : selected[0].toPath().toAbsolutePath().normalize();
        } finally {
            dialog.dispose();
        }
    }

    private void saveSessionAs() {
        Path selected = chooseFile("Save Session As", FileDialog.SAVE);
        if (selected == null) {
            return;
        }
        if (!selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(SessionFile.EXTENSION)) {
            selected = selected.resolveSibling(selected.getFileName().toString() + SessionFile.EXTENSION);
            if (Files.exists(selected) && JOptionPane.showConfirmDialog(this,
                    "Replace the existing session file?\n" + selected, "Save Session",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
        }
        saveSession(selected);
    }

    private void saveSession(Path selected) {
        try {
            SessionFile.save(selected, dataStore);
            setSessionFile(selected);
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Could Not Save Session", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSession(Path selected) {
        try {
            DataStore loaded = SessionFile.load(selected);
            if ((dataStore.getClientCount() > 0 || dataStore.getOfferingCount() > 0 || dataStore.getQuoteCount() > 0)
                    && JOptionPane.showConfirmDialog(this,
                    "Load this file and replace the records and open forms in this window?", "Load Session",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            dataStore.replaceWith(loaded);
            clients.resetForSession();
            offerings.resetForSession();
            quotes.resetForSession();
            setSessionFile(selected);
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Could Not Load Session", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setSessionFile(Path selected) {
        sessionFile = selected;
        setTitle(APP_TITLE + " - " + selected.getFileName());
    }

    private void showSessionLocation() {
        Object message = "This session has no save file yet.";
        if (sessionFile != null) {
            JTextField location = new JTextField(sessionFile.toString(), 52);
            location.setEditable(false);
            location.setCaretPosition(0);
            message = new Object[] {"Current session save file", location};
        }
        JOptionPane.showMessageDialog(this, message, "Session File Location", JOptionPane.INFORMATION_MESSAGE);
    }
}
