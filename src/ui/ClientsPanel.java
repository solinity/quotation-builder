package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Client;
import storage.DataStore;

public class ClientsPanel extends JPanel {
    private final DataStore store;
    private final DefaultListModel<Client> results = new DefaultListModel<>();
    private final JList<Client> list = new JList<>(results);
    private final JTextField search = new JTextField();
    private final JTextArea detail = MainFrame.detailArea();
    private final JButton edit = new JButton("Edit");
    private final JButton delete = new JButton("Delete");

    public ClientsPanel(DataStore store) {
        super(new BorderLayout());
        this.store = store;

        JButton find = new JButton("Search");
        JButton clear = new JButton("Clear");
        JPanel clientList = MainFrame.listPanel("Client list", list, search, find, clear);

        JButton add = MainFrame.primaryButton("New client");
        JPanel actions = MainFrame.actionRow();
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        add(MainFrame.workspace("Clients", "Keep contact details ready for your next quote.",
                clientList, "Contact details", detail, actions));

        find.addActionListener(event -> refresh(list.getSelectedValue()));
        search.addActionListener(event -> refresh(list.getSelectedValue()));
        clear.addActionListener(event -> {
            search.setText("");
            refresh(list.getSelectedValue());
        });
        list.addListSelectionListener(event -> showClient());
        add.addActionListener(event -> editClient(null));
        edit.addActionListener(event -> editClient(list.getSelectedValue()));
        delete.addActionListener(event -> deleteClient());

        refresh(null);
    }

    private void refresh(Client selection) {
        results.clear();
        for (Client client : store.getClients()) {
            if (MainFrame.matches(client.getName() + " " + client.getEmail() + " " + client.getPhone(),
                    search.getText())) {
                results.addElement(client);
            }
        }
        list.setSelectedValue(selection, true);
        showClient();
    }

    private void showClient() {
        Client client = list.getSelectedValue();
        edit.setEnabled(client != null);
        delete.setEnabled(client != null);

        if (client == null) {
            if (results.isEmpty() && !search.getText().trim().isEmpty()) {
                detail.setText("No clients match your search.");
            } else if (results.isEmpty()) {
                detail.setText("Add a client to save their contact details.");
            } else {
                detail.setText("Select a client to see their contact details.");
            }
        } else {
            String text = client.getName();
            if (!client.getEmail().isEmpty()) {
                text += "\n\nEmail: " + client.getEmail();
            }
            if (!client.getPhone().isEmpty()) {
                text += "\nPhone: " + client.getPhone();
            }
            detail.setText(text);
        }
        detail.setCaretPosition(0);
    }

    private void editClient(Client existing) {
        JTextField name = new JTextField(22);
        JTextField email = new JTextField(22);
        JTextField phone = new JTextField(22);
        if (existing != null) {
            name.setText(existing.getName());
            email.setText(existing.getEmail());
            phone.setText(existing.getPhone());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Email (optional)"));
        form.add(email);
        form.add(new JLabel("Phone (optional)"));
        form.add(phone);

        String title = existing == null ? "New client" : "Edit client";
        while (JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                Client client = new Client(name.getText(), email.getText(), phone.getText());
                ArrayList<Client> clients = store.getClients();
                if (existing == null) {
                    clients.add(client);
                } else {
                    clients.set(clients.indexOf(existing), client);
                }
                store.saveClients(clients);
                search.setText("");
                refresh(client);
                return;
            } catch (IllegalArgumentException | IOException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteClient() {
        Client client = list.getSelectedValue();
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + client.getName() + " from the client list?",
                "Delete client", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                ArrayList<Client> clients = store.getClients();
                clients.remove(client);
                store.saveClients(clients);
                refresh(null);
            } catch (IOException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(),
                        "Delete client", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
