package views.panels;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MonitorPanel extends JPanel {

    private final JTextArea monitorArea;

    public MonitorPanel() {
        setLayout(new BorderLayout());

        monitorArea = new JTextArea();
        monitorArea.setEditable(false);

        add(new JScrollPane(monitorArea), BorderLayout.CENTER);
    }

    public void appendMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        monitorArea.append(message + System.lineSeparator());
        monitorArea.setCaretPosition(monitorArea.getDocument().getLength());
    }

    public void setMessage(String message) {
        monitorArea.setText(message == null ? "" : message);
        monitorArea.setCaretPosition(monitorArea.getDocument().getLength());
    }

    public JTextArea getMonitorArea() {
        return monitorArea;
    }
}
