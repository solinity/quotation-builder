package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import model.Client;
import storage.DataStore;

public class ClientsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;

    private final JTextField txtName = new JTextField(18);
    private final JTextField txtEmail = new JTextField(18);
    private final JTextField txtPhone = new JTextField(18);
    private final JTextField txtSearch = new JTextField(18);
    private final JLabel listStatus = new JLabel();
    private final JLabel editorTitle = SwingStyle.heading("New client");
    private final JLabel selectionStatus = SwingStyle.muted("Name is required.");
    private String activeSearch = "";
    private boolean refreshingList;

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

        SwingStyle.page(this);
        SwingStyle.muted(listStatus);

        clientList.setCellRenderer(new RecordRenderer(dataStore));
        clientList.addListSelectionListener(event -> {
            if (!refreshingList && !event.getValueIsAdjusting()) {
                populateClientForm();
            }
        });

        JPanel details = new JPanel(new BorderLayout(0, 10));
        details.setOpaque(false);
        details.add(selectionStatus, BorderLayout.NORTH);
        details.add(createFormPanel(), BorderLayout.CENTER);
        details.add(createButtonPanel(), BorderLayout.SOUTH);
        JPanel editor = SwingStyle.card();
        editor.add(editorTitle, BorderLayout.NORTH);
        editor.add(details, BorderLayout.CENTER);
        editor.setPreferredSize(new Dimension(400, editor.getPreferredSize().height));
        JPanel editorColumn = new JPanel(new BorderLayout());
        editorColumn.setOpaque(false);
        editorColumn.add(editor, BorderLayout.NORTH);

        add(editorColumn, BorderLayout.WEST);
        add(createListPanel(), BorderLayout.CENTER);
        populateClientForm();
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
        JButton clear = new JButton("Clear Form");
        SwingStyle.button(btnAddClient, true);
        SwingStyle.button(btnUpdate, true);
        SwingStyle.button(btnDelete, false);
        SwingStyle.button(clear, false);

        btnAddClient.addActionListener(event -> addClient());
        btnUpdate.addActionListener(event -> updateClient());
        btnDelete.addActionListener(event -> deleteClient());
        clear.addActionListener(event -> clearClientForm());

        return SwingStyle.row(btnAddClient, btnUpdate, btnDelete, clear);
    }

    private JPanel createListPanel() {
        JPanel searchPanel = SwingStyle.searchBar("Search by name", txtSearch, btnSearch, btnShowAll);
        btnSearch.addActionListener(event -> searchClients());
        txtSearch.addActionListener(event -> searchClients());
        btnShowAll.addActionListener(event -> showAllClients());

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(SwingStyle.heading("Registered clients"), BorderLayout.NORTH);
        header.add(searchPanel, BorderLayout.CENTER);
        JPanel listPanel = SwingStyle.card();
        listPanel.add(header, BorderLayout.NORTH);
        listPanel.add(SwingStyle.list(clientList, "Registered clients", 55, 5), BorderLayout.CENTER);
        listPanel.add(listStatus, BorderLayout.SOUTH);
        return listPanel;
    }

    private void addClient() {
        String name = txtName.getText();
        String email = txtEmail.getText();
        String phone = txtPhone.getText();

        try {
            dataStore.addClient(new Client(name, email, phone));
            showAllClients();
            clearClientForm();
        } catch (IllegalArgumentException | IllegalStateException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Add Client", JOptionPane.ERROR_MESSAGE);
        }
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
        boolean selected = selectedClient != null;
        btnAddClient.setEnabled(!selected);
        btnUpdate.setEnabled(selected);
        btnDelete.setEnabled(selected);
        editorTitle.setText(selected ? "Edit client" : "New client");
        selectionStatus.setText(selected ? "Client #" + dataStore.getClientNumber(selectedClient) + " selected"
                : "Name is required.");
        if (selectedClient != null) {
            txtName.setText(selectedClient.getName());
            txtEmail.setText(selectedClient.getEmail());
            txtPhone.setText(selectedClient.getPhone());
        } else {
            txtName.setText("");
            txtEmail.setText("");
            txtPhone.setText("");
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

        try {
            dataStore.updateClient(selectedClient, new Client(name, email, phone));
            showAllClients();
            clientList.setSelectedValue(selectedClient, true);
            populateClientForm();
        } catch (IllegalArgumentException | IllegalStateException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Update Client", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteClient() {
        Client selectedClient = clientList.getSelectedValue();
        if (selectedClient == null) {
            JOptionPane.showMessageDialog(this, "Select a client to delete.", "Delete Client", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            if (dataStore.isClientReferenced(selectedClient)) {
                throw new IllegalArgumentException("This client is used by a saved quotation.");
            }
            if (JOptionPane.showConfirmDialog(this, "Delete " + selectedClient.getName() + "?",
                    "Delete client", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            if (!dataStore.removeClient(selectedClient)) {
                throw new IllegalArgumentException("The selected client could not be found.");
            }
        } catch (IllegalArgumentException | IllegalStateException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Delete Client", JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshClientList();
        clearClientForm();
    }

    private void clearClientForm() {
        clientList.clearSelection();
        populateClientForm();
    }

    void refreshClientList() {
        Client selected = clientList.getSelectedValue();
        refreshingList = true;
        try {
            clientListModel.clear();
            for (Client client : dataStore.searchClients(activeSearch)) {
                clientListModel.addElement(client);
            }
            clientList.setSelectedValue(selected, true);
        } finally {
            refreshingList = false;
        }
        if (clientList.getSelectedValue() != selected) {
            populateClientForm();
        }
        listStatus.setText(clientListModel.isEmpty()
                ? (activeSearch.isEmpty() ? "No clients yet. Add your first client using the form." : "No clients match your search.")
                : clientListModel.size() + (clientListModel.size() == 1 ? " client shown" : " clients shown"));
    }
}
