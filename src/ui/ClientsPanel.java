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
import model.Client;

public class ClientsPanel extends JPanel {
    private final DataStore dataStore;

    private final JTextField txtName = new JTextField(18);
    private final JTextField txtEmail = new JTextField(18);
    private final JTextField txtPhone = new JTextField(18);

    private final JButton btnAddClient = new JButton("Add Client");
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");

    private final DefaultListModel<Client> clientListModel = new DefaultListModel<>();
    private final JList<Client> clientList = new JList<>(clientListModel);

    public ClientsPanel(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore can't be null");
        }
        this.dataStore = dataStore;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                populateClientForm();
            }
        });

        add(createFormPanel(), BorderLayout.NORTH);
        add(new JScrollPane(clientList), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refreshClientList();
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
        btnSearch.addActionListener(event -> searchClients());
        btnUpdate.addActionListener(event -> updateClient());
        btnDelete.addActionListener(event -> deleteClient());

        buttonPanel.add(btnAddClient);
        buttonPanel.add(btnSearch);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);

        return buttonPanel;
    }

    private void addClient() {
        String name = txtName.getText();
        String email = txtEmail.getText();
        String phone = txtPhone.getText();

        Client client;
        try {
            client = new Client(name, email, phone);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Invalid Client", JOptionPane.ERROR_MESSAGE);
            return;
        }

        dataStore.addClient(client);
        refreshClientList();
        clearClientForm();
    }

    private void searchClients() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            refreshClientList();
            return;
        }

        clientListModel.clear();
        for (int i = 0; i < dataStore.getClientCount(); i++) {
            Client client = dataStore.getClient(i);
            if (client.getName().equalsIgnoreCase(name)) {
                clientListModel.addElement(client);
            }
        }

        if (clientListModel.getSize() == 0) {
            JOptionPane.showMessageDialog(this, "No clients found.", "Search Clients", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void populateClientForm() {
        Client selectedClient = clientList.getSelectedValue();
        if (selectedClient != null) {
            txtName.setText(selectedClient.getName());
            txtEmail.setText(selectedClient.getEmail());
            txtPhone.setText(selectedClient.getPhone());
        }
    }

    private void updateClient() {
        Client selectedClient = clientList.getSelectedValue();
        if (selectedClient == null) {
            JOptionPane.showMessageDialog(this, "Select a client to update.", "Update Client", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String name = txtName.getText();
        String email = txtEmail.getText();
        String phone = txtPhone.getText();

        Client validatedClient;
        try {
            validatedClient = new Client(name, email, phone);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Invalid Client", JOptionPane.ERROR_MESSAGE);
            return;
        }

        selectedClient.setName(validatedClient.getName());
        selectedClient.setEmail(validatedClient.getEmail());
        selectedClient.setPhone(validatedClient.getPhone());
        refreshClientList();
    }

    private void deleteClient() {
        Client selectedClient = clientList.getSelectedValue();
        if (selectedClient == null) {
            JOptionPane.showMessageDialog(this, "Select a client to delete.", "Delete Client", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!dataStore.removeClient(selectedClient)) {
            JOptionPane.showMessageDialog(this, "The selected client could not be found.", "Delete Client", JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshClientList();
        clearClientForm();
    }

    private void clearClientForm() {
        txtName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
    }

    private void refreshClientList() {
        clientListModel.clear();
        for (int i = 0; i < dataStore.getClientCount(); i++) {
            clientListModel.addElement(dataStore.getClient(i));
        }
    }

    // TEMPORARY standalone preview. Remove when MainFrame integration is complete.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Clients Panel - Temporary Preview");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            DataStore dataStore = new DataStore();
            frame.add(new ClientsPanel(dataStore));
            frame.setSize(540, 360);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
