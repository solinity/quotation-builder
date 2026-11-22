import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class ClientsPanel extends JPanel {
    private final JTextField txtName = new JTextField(18);
    private final JTextField txtEmail = new JTextField(18);
    private final JTextField txtPhone = new JTextField(18);

    private final JButton btnAddClient = new JButton("Add Client");
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");

    private final DefaultListModel<String> clientListModel = new DefaultListModel<>();
    private final JList<String> clientList = new JList<>(clientListModel);

    public ClientsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(createFormPanel(), BorderLayout.NORTH);
        add(new JScrollPane(clientList), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));

        formPanel.add(new JLabel("Client Name:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(txtEmail);
        formPanel.add(new JLabel("Phone:"));
        formPanel.add(txtPhone);

        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();

        btnAddClient.addActionListener(event -> addClient());
        btnSearch.setEnabled(false);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        buttonPanel.add(btnAddClient);
        buttonPanel.add(btnSearch);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);

        return buttonPanel;
    }

    private void addClient() {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a client name.", "Invalid Client", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!email.isEmpty() && !email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Email must contain @.", "Invalid Client", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String emailDisplay = email.isEmpty() ? "N/A" : email;
        String phoneDisplay = phone.isEmpty() ? "N/A" : phone;

        // Temporary: replace these formatted strings with Client objects backed by shared storage.
        clientListModel.addElement(name + " | Email: " + emailDisplay + " | Phone: " + phoneDisplay);

        txtName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
    }

    // TEMPORARY standalone preview. Remove when MainFrame integration is complete.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Clients Panel - Temporary Preview");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new ClientsPanel());
            frame.setSize(540, 360);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
