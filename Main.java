import javax.swing.SwingUtilities;
import storage.DataStore;
import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame(new DataStore()).setVisible(true));
    }
}
