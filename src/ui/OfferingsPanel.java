package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import model.Offering;
import storage.DataStore;

public class OfferingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;
    private final JTextField nameField = new JTextField(32);
    private final JTextArea descriptionField = new JTextArea(4, 32);
    private final JTextField priceField = new JTextField(32);
    private final JTextField searchField = new JTextField(18);
    private final JLabel listStatus = new JLabel();
    private final DefaultListModel<Offering> offeringModel = new DefaultListModel<>();
    private final JList<Offering> offeringList = new JList<>(offeringModel);
    private String activeSearch = "";

    public OfferingsPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        offeringList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        offeringList.setCellRenderer(new RecordRenderer(dataStore));
        offeringList.getAccessibleContext().setAccessibleName("Registered offerings");
        offeringList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                populateForm();
            }
        });

        add(createEditor(), BorderLayout.NORTH);
        add(createListPanel(), BorderLayout.CENTER);
        refreshOfferingList();
    }

    private JPanel createEditor() {
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        descriptionField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        descriptionField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        JScrollPane descriptionScroll = new JScrollPane(descriptionField);

        FormPanel form = new FormPanel();
        form.addField("Name", nameField);
        form.addField("Description (optional)", descriptionScroll).setLabelFor(descriptionField);
        form.addField("Unit price", priceField);

        JButton add = new JButton("Add Offering");
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("Clear Form");
        add.addActionListener(event -> addOffering());
        update.addActionListener(event -> updateOffering());
        delete.addActionListener(event -> deleteOffering());
        clear.addActionListener(event -> clearForm());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);

        JPanel editor = new JPanel(new BorderLayout(0, 5));
        editor.setBorder(BorderFactory.createTitledBorder("Offering details"));
        editor.add(form, BorderLayout.WEST);
        editor.add(buttons, BorderLayout.SOUTH);
        return editor;
    }

    private JPanel createListPanel() {
        JButton search = new JButton("Search");
        JButton showAll = new JButton("Show All");
        search.addActionListener(event -> searchOfferings());
        searchField.addActionListener(event -> searchOfferings());
        showAll.addActionListener(event -> showAllOfferings());

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel searchLabel = new JLabel("Search by name");
        searchLabel.setLabelFor(searchField);
        searchBar.add(searchLabel);
        searchBar.add(searchField);
        searchBar.add(search);
        searchBar.add(showAll);

        JPanel listPanel = new JPanel(new BorderLayout(0, 5));
        listPanel.setBorder(BorderFactory.createTitledBorder("Registered offerings"));
        listPanel.add(searchBar, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(offeringList), BorderLayout.CENTER);
        listPanel.add(listStatus, BorderLayout.SOUTH);
        return listPanel;
    }

    private Offering readOfferingForm() {
        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter a numerical unit price, such as 50.00.");
        }
        return new Offering(nameField.getText(), descriptionField.getText(), price);
    }

    private void addOffering() {
        try {
            dataStore.addOffering(readOfferingForm());
            showAllOfferings();
            clearForm();
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void updateOffering() {
        Offering selected = offeringList.getSelectedValue();
        if (selected == null) {
            showError("Select an offering to update.");
            return;
        }
        try {
            Offering changes = readOfferingForm();
            selected.setName(changes.getName());
            selected.setDescription(changes.getDescription());
            selected.setUnitPrice(changes.getUnitPrice());
            showAllOfferings();
            offeringList.setSelectedValue(selected, true);
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void deleteOffering() {
        Offering selected = offeringList.getSelectedValue();
        if (selected == null) {
            showError("Select an offering to delete.");
            return;
        }
        try {
            if (!dataStore.removeOffering(selected)) {
                throw new IllegalArgumentException("The selected offering could not be found.");
            }
            refreshOfferingList();
            clearForm();
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void populateForm() {
        Offering selected = offeringList.getSelectedValue();
        if (selected != null) {
            nameField.setText(selected.getName());
            descriptionField.setText(selected.getDescription());
            descriptionField.setCaretPosition(0);
            priceField.setText(Double.toString(selected.getUnitPrice()));
        }
    }

    private void clearForm() {
        offeringList.clearSelection();
        nameField.setText("");
        descriptionField.setText("");
        priceField.setText("");
    }

    private void searchOfferings() {
        activeSearch = searchField.getText().trim();
        refreshOfferingList();
    }

    private void showAllOfferings() {
        searchField.setText("");
        searchOfferings();
    }

    void resetForSession() {
        clearForm();
        showAllOfferings();
    }

    void refreshOfferingList() {
        Offering selected = offeringList.getSelectedValue();
        offeringModel.clear();
        for (Offering offering : dataStore.searchOfferings(activeSearch)) {
            offeringModel.addElement(offering);
        }
        offeringList.setSelectedValue(selected, true);
        listStatus.setText(offeringModel.isEmpty() ? "No offerings found." : offeringModel.size() + " shown");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Offering", JOptionPane.ERROR_MESSAGE);
    }
}
