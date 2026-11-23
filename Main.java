import javax.swing.SwingUtilities;
import storage.DataStore;
import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DataStore dataStore = new DataStore();
            MainFrame frame = new MainFrame(dataStore);
            frame.setVisible(true);
        });
    }
}