import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import storage.DataStore;
import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Path file = Path.of("data", "quotations.txt");
            try {
                MainFrame frame = new MainFrame(DataStore.open(file));
                frame.setVisible(true);
            } catch (IOException | SecurityException error) {
                MainFrame frame = new MainFrame(new DataStore(), "Temporary session   |   Records kept in memory");
                frame.setVisible(true);
                JOptionPane.showMessageDialog(frame, "Could not load saved records from this file.\n"
                        + file.toAbsolutePath() + "\n\n"
                        + "This is a temporary session. Records added here will clear when you close the window.",
                        "Could not open data file", JOptionPane.WARNING_MESSAGE);
            }
        });
    }
}
