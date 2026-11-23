package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

class FormPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JPanel labels = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JPanel inputs = new JPanel(new GridLayout(0, 1, 0, 6));

    FormPanel() {
        super(new BorderLayout(16, 0));
        add(labels, BorderLayout.WEST);
        add(inputs, BorderLayout.CENTER);
    }

    void addField(String text, JComponent input) {
        JLabel label = new JLabel(text);
        label.setLabelFor(input);
        labels.add(label);
        inputs.add(input);
    }
}
