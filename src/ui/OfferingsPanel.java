package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import model.Offering;
import storage.DataStore;

public class OfferingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;
    private final JTextField nameField = new JTextField(18);
    private final JTextArea descriptionField = new JTextArea(3, 22);
    private final JTextField priceField = new JTextField(18);
    private final JTextField searchField = new JTextField(18);
    private final JLabel listStatus = new JLabel();
    private final JLabel editorTitle = SwingStyle.heading("New offering");
    private final JLabel selectionStatus = SwingStyle.muted("Name and unit price are required.");
    private final JButton add = new JButton("Add Offering");
    private final JButton update = new JButton("Update");
    private final JButton delete = new JButton("Delete");
    private final DefaultListModel<Offering> offeringModel = new DefaultListModel<>();
    private final JList<Offering> offeringList = new JList<>(offeringModel);
    private String activeSearch = "";
    private boolean refreshingList;

    public OfferingsPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        SwingStyle.page(this);
        SwingStyle.muted(listStatus);
        offeringList.setCellRenderer(new RecordRenderer(dataStore));
        offeringList.addListSelectionListener(event -> {
            if (!refreshingList && !event.getValueIsAdjusting()) {
                populateForm();
            }
        });

        add(createEditor(), BorderLayout.WEST);
        add(createListPanel(), BorderLayout.CENTER);
        populateForm();
        refreshOfferingList();
    }

    private JPanel createEditor() {
        FormPanel form = new FormPanel();
        SwingStyle.field(descriptionField);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        descriptionField.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        JScrollPane description = new JScrollPane(descriptionField);
        description.setBorder(BorderFactory.createLineBorder(SwingStyle.LINE));
        description.setPreferredSize(new Dimension(165, 80));
        description.setMinimumSize(new Dimension(75, 64));
        form.addField("Name", nameField);
        form.addField("Description (optional)", description, descriptionField);
        form.addField("Unit price", priceField);

        JButton clear = new JButton("Clear Form");
        SwingStyle.button(add, true);
        SwingStyle.button(update, true);
        SwingStyle.button(delete, false);
        SwingStyle.button(clear, false);
        add.addActionListener(event -> addOffering());
        update.addActionListener(event -> updateOffering());
        delete.addActionListener(event -> deleteOffering());
        clear.addActionListener(event -> clearForm());
        JPanel details = new JPanel(new BorderLayout(0, 10));
        details.setOpaque(false);
        details.add(selectionStatus, BorderLayout.NORTH);
        details.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(0, 8));
        actions.setOpaque(false);
        actions.add(SwingStyle.muted("Unit price × quantity or hours on a quote."), BorderLayout.NORTH);
        actions.add(SwingStyle.row(add, update, delete, clear), BorderLayout.SOUTH);
        details.add(actions, BorderLayout.SOUTH);
        JPanel editor = SwingStyle.card();
        editor.add(editorTitle, BorderLayout.NORTH);
        editor.add(details, BorderLayout.CENTER);
        editor.setPreferredSize(new Dimension(415, editor.getPreferredSize().height));
        JPanel editorColumn = new JPanel(new BorderLayout());
        editorColumn.setOpaque(false);
        editorColumn.add(editor, BorderLayout.NORTH);
        return editorColumn;
    }

    private JPanel createListPanel() {
        JButton search = new JButton("Search");
        JButton showAll = new JButton("Show All");
        search.addActionListener(event -> searchOfferings());
        searchField.addActionListener(event -> searchOfferings());
        showAll.addActionListener(event -> showAllOfferings());

        JPanel searchBar = SwingStyle.searchBar("Search by name", searchField, search, showAll);
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(SwingStyle.heading("Service catalog"), BorderLayout.NORTH);
        header.add(searchBar, BorderLayout.CENTER);
        JPanel listPanel = SwingStyle.card();
        listPanel.add(header, BorderLayout.NORTH);
        listPanel.add(SwingStyle.list(offeringList, "Registered offerings", 55, 5), BorderLayout.CENTER);
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
        } catch (IllegalArgumentException | IllegalStateException error) {
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
            dataStore.updateOffering(selected, readOfferingForm());
            showAllOfferings();
            offeringList.setSelectedValue(selected, true);
            populateForm();
        } catch (IllegalArgumentException | IllegalStateException error) {
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
            if (dataStore.isOfferingReferenced(selected)) {
                throw new IllegalArgumentException("This offering is used by a saved quotation.");
            }
            if (JOptionPane.showConfirmDialog(this, "Delete " + selected.getName() + "?",
                    "Delete offering", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            if (!dataStore.removeOffering(selected)) {
                throw new IllegalArgumentException("The selected offering could not be found.");
            }
            refreshOfferingList();
            clearForm();
        } catch (IllegalArgumentException | IllegalStateException error) {
            showError(error.getMessage());
        }
    }

    private void populateForm() {
        Offering selected = offeringList.getSelectedValue();
        add.setEnabled(selected == null);
        update.setEnabled(selected != null);
        delete.setEnabled(selected != null);
        editorTitle.setText(selected == null ? "New offering" : "Edit offering");
        selectionStatus.setText(selected == null ? "Name and unit price are required."
                : "Offering #" + dataStore.getOfferingNumber(selected) + " selected");
        if (selected != null) {
            nameField.setText(selected.getName());
            descriptionField.setText(selected.getDescription());
            priceField.setText(Double.toString(selected.getUnitPrice()));
        } else {
            nameField.setText("");
            descriptionField.setText("");
            priceField.setText("");
        }
    }

    private void clearForm() {
        offeringList.clearSelection();
        populateForm();
    }

    private void searchOfferings() {
        activeSearch = searchField.getText().trim();
        refreshOfferingList();
    }

    private void showAllOfferings() {
        searchField.setText("");
        searchOfferings();
    }

    void refreshOfferingList() {
        Offering selected = offeringList.getSelectedValue();
        refreshingList = true;
        try {
            offeringModel.clear();
            for (Offering offering : dataStore.searchOfferings(activeSearch)) {
                offeringModel.addElement(offering);
            }
            offeringList.setSelectedValue(selected, true);
        } finally {
            refreshingList = false;
        }
        if (offeringList.getSelectedValue() != selected) {
            populateForm();
        }
        listStatus.setText(offeringModel.isEmpty()
                ? (activeSearch.isEmpty() ? "No offerings yet. Add your first service or product." : "No offerings match your search.")
                : offeringModel.size() + (offeringModel.size() == 1 ? " offering shown" : " offerings shown"));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Offering", JOptionPane.ERROR_MESSAGE);
    }
}
