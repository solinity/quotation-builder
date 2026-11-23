import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import storage.DataStore;
import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DataStore store = new DataStore();
                new MainFrame(store).setVisible(true);
            } catch (IOException error) {
                JOptionPane.showMessageDialog(null, error.getMessage(),
                        "Could not open quotations", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
