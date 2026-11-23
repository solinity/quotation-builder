package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Offering;
import storage.DataStore;

public class OfferingsPanel extends JPanel {
    private final DataStore store;
    private final DefaultListModel<Offering> results = new DefaultListModel<>();
    private final JList<Offering> list = new JList<>(results);
    private final JTextField search = new JTextField();
    private final JTextArea detail = MainFrame.detailArea();
    private final JButton edit = new JButton("Edit");
    private final JButton delete = new JButton("Delete");

    public OfferingsPanel(DataStore store) {
        super(new BorderLayout());
        this.store = store;

        JButton find = new JButton("Search");
        JButton clear = new JButton("Clear");
        JPanel serviceList = MainFrame.listPanel("Service list", list, search, find, clear);

        JButton add = MainFrame.primaryButton("New service");
        JPanel actions = MainFrame.actionRow();
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        add(MainFrame.workspace("Services", "Set the rates you use most often. All rates are in USD.",
                serviceList, "Service details", detail, actions));

        find.addActionListener(event -> refresh(list.getSelectedValue()));
        search.addActionListener(event -> refresh(list.getSelectedValue()));
        clear.addActionListener(event -> {
            search.setText("");
            refresh(list.getSelectedValue());
        });
        list.addListSelectionListener(event -> showOffering());
        add.addActionListener(event -> editOffering(null));
        edit.addActionListener(event -> editOffering(list.getSelectedValue()));
        delete.addActionListener(event -> deleteOffering());

        refresh(null);
    }

    private void refresh(Offering selection) {
        results.clear();
        for (Offering offering : store.getOfferings()) {
            if (MainFrame.matches(offering.getName() + " " + offering.getUnit(), search.getText())) {
                results.addElement(offering);
            }
        }
        list.setSelectedValue(selection, true);
        showOffering();
    }

    private void showOffering() {
        Offering offering = list.getSelectedValue();
        edit.setEnabled(offering != null);
        delete.setEnabled(offering != null);

        if (offering == null) {
            if (results.isEmpty() && !search.getText().trim().isEmpty()) {
                detail.setText("No services match your search.");
            } else if (results.isEmpty()) {
                detail.setText("Add a service, such as a design project or an hour of tutoring.");
            } else {
                detail.setText("Select a service to see its rate.");
            }
        } else {
            detail.setText(offering.getName() + "\n\nRate: "
                    + MainFrame.money(offering.getRate()) + " per " + offering.getUnit());
        }
        detail.setCaretPosition(0);
    }

    private void editOffering(Offering existing) {
        JTextField name = new JTextField(22);
        String[] units = {"project", "hour", "item"};
        JComboBox<String> unit = new JComboBox<>(units);
        JTextField rate = new JTextField(22);
        if (existing != null) {
            name.setText(existing.getName());
            unit.setSelectedItem(existing.getUnit());
            rate.setText(MainFrame.rateInput(existing.getRate()));
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Service name"));
        form.add(name);
        form.add(new JLabel("Billing unit"));
        form.add(unit);
        form.add(new JLabel("Rate in USD"));
        form.add(rate);

        String title = existing == null ? "New service" : "Edit service";
        while (JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                long cents = Offering.readRate(rate.getText());
                Offering offering = new Offering(name.getText(), (String) unit.getSelectedItem(), cents);
                ArrayList<Offering> offerings = store.getOfferings();
                if (existing == null) {
                    offerings.add(offering);
                } else {
                    offerings.set(offerings.indexOf(existing), offering);
                }
                store.saveOfferings(offerings);
                search.setText("");
                refresh(offering);
                return;
            } catch (NumberFormatException error) {
                JOptionPane.showMessageDialog(this, "Enter the rate as a number, for example 45.50.",
                        title, JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException | IOException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteOffering() {
        Offering offering = list.getSelectedValue();
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + offering.getName() + " from the service list?",
                "Delete service", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                ArrayList<Offering> offerings = store.getOfferings();
                offerings.remove(offering);
                store.saveOfferings(offerings);
                refresh(null);
            } catch (IOException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(),
                        "Delete service", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
