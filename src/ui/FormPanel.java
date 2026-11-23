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
        setOpaque(false);
    }

    void addField(String text, JComponent input) {
        addField(text, input, input);
    }

    void addField(String text, JComponent input, JComponent labeledInput) {
        JLabel label = SwingStyle.muted(text);
        label.setLabelFor(labeledInput);
        SwingStyle.field(input);

        GridBagConstraints position = new GridBagConstraints();
        position.gridy = row++;
        position.anchor = GridBagConstraints.LINE_START;
        position.insets = new Insets(3, 0, 3, 12);
        add(label, position);

        position.gridx = 1;
        position.insets = new Insets(3, 0, 3, 0);
        position.weightx = 1;
        position.fill = GridBagConstraints.HORIZONTAL;
        add(input, position);
    }
}
