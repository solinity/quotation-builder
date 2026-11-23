package ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

class FormPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private int row;

    FormPanel() {
        super(new GridBagLayout());
    }

    JLabel addField(String text, JComponent input) {
        JLabel label = new JLabel(text);
        label.setLabelFor(input);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        int topGap = row == 0 ? 0 : 6;
        constraints.insets = new Insets(topGap, 0, 0, 16);
        add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(topGap, 0, 0, 0);
        add(input, constraints);
        row++;
        return label;
    }
}
