package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import model.Client;
import storage.DataStore;

public class ClientsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;

    private final JTextField txtName = new JTextField(32);
    private final JTextField txtEmail = new JTextField(32);
    private final JTextField txtPhone = new JTextField(32);
    private final JTextField txtSearch = new JTextField(18);
    private final JLabel listStatus = new JLabel();
    private String activeSearch = "";

    private final JButton btnAddClient = new JButton("Add Client");
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnShowAll = new JButton("Show All");
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
        clientList.setCellRenderer(new RecordRenderer(dataStore));
        clientList.getAccessibleContext().setAccessibleName("Registered clients");
        clientList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                populateClientForm();
            }
        });

        JPanel editor = new JPanel(new BorderLayout(0, 5));
        editor.setBorder(BorderFactory.createTitledBorder("Client details"));
        editor.add(createFormPanel(), BorderLayout.WEST);
        editor.add(createButtonPanel(), BorderLayout.SOUTH);

        add(editor, BorderLayout.NORTH);
        add(createListPanel(), BorderLayout.CENTER);
        refreshClientList();
    }

    private JPanel createFormPanel() {
        FormPanel formPanel = new FormPanel();
        formPanel.addField("Name", txtName);
        formPanel.addField("Email (optional)", txtEmail);
        formPanel.addField("Phone (optional)", txtPhone);
        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton clear = new JButton("Clear Form");

        btnAddClient.addActionListener(event -> addClient());
        btnUpdate.addActionListener(event -> updateClient());
        btnDelete.addActionListener(event -> deleteClient());
        clear.addActionListener(event -> clearClientForm());

        buttonPanel.add(btnAddClient);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(clear);

        return buttonPanel;
    }

    private JPanel createListPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel searchLabel = new JLabel("Search by name");
        searchLabel.setLabelFor(txtSearch);
        searchPanel.add(searchLabel);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);
        btnSearch.addActionListener(event -> searchClients());
        txtSearch.addActionListener(event -> searchClients());
        btnShowAll.addActionListener(event -> showAllClients());

        JPanel listPanel = new JPanel(new BorderLayout(0, 5));
        listPanel.setBorder(BorderFactory.createTitledBorder("Registered clients"));
        listPanel.add(searchPanel, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(clientList), BorderLayout.CENTER);
        listPanel.add(listStatus, BorderLayout.SOUTH);
        return listPanel;
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
        showAllClients();
        clearClientForm();
    }

    private void searchClients() {
        activeSearch = txtSearch.getText().trim();
        refreshClientList();
    }

    private void showAllClients() {
        txtSearch.setText("");
        searchClients();
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
        showAllClients();
        clientList.setSelectedValue(selectedClient, true);
    }

    private void deleteClient() {
        Client selectedClient = clientList.getSelectedValue();
        if (selectedClient == null) {
            JOptionPane.showMessageDialog(this, "Select a client to delete.", "Delete Client", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            if (!dataStore.removeClient(selectedClient)) {
                throw new IllegalArgumentException("The selected client could not be found.");
            }
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Delete Client", JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshClientList();
        clearClientForm();
    }

    private void clearClientForm() {
        clientList.clearSelection();
        txtName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
    }

    void resetForSession() {
        clearClientForm();
        showAllClients();
    }

    void refreshClientList() {
        Client selected = clientList.getSelectedValue();
        clientListModel.clear();
        for (Client client : dataStore.searchClients(activeSearch)) {
            clientListModel.addElement(client);
        }
        clientList.setSelectedValue(selected, true);
        listStatus.setText(clientListModel.isEmpty() ? "No clients found." : clientListModel.size() + " shown");
    }
}
