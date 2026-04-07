package views.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

public class AdminPanel extends JPanel {

    private final JLabel serverStatusLabel;
    private final DefaultTableModel pendingRequestsModel;
    private final JTable pendingRequestsTable;
    private final JTextArea requestDetailsArea;
    private final JButton acceptButton;
    private final JButton rejectButton;
    private final JButton refreshButton;

    public AdminPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serverStatusLabel = new JLabel("Server Status: Running");
        topPanel.add(serverStatusLabel);

        refreshButton = new JButton("Refresh");
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);

        pendingRequestsModel = new DefaultTableModel(
            new Object[]{"Request ID", "Role", "Timestamp", "Status"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        pendingRequestsTable = new JTable(pendingRequestsModel);
        add(new JScrollPane(pendingRequestsTable), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        requestDetailsArea = new JTextArea();
        requestDetailsArea.setEditable(false);
        rightPanel.add(new JScrollPane(requestDetailsArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        acceptButton = new JButton("Accept");
        rejectButton = new JButton("Reject");
        buttonPanel.add(acceptButton);
        buttonPanel.add(rejectButton);

        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);
    }

    public void addPendingRequestRow(Object[] rowData) {
        pendingRequestsModel.addRow(rowData);
    }

    public void clearPendingRequests() {
        pendingRequestsModel.setRowCount(0);
    }

    public JTable getPendingRequestsTable() {
        return pendingRequestsTable;
    }

    public JTextArea getRequestDetailsArea() {
        return requestDetailsArea;
    }

    public JButton getAcceptButton() {
        return acceptButton;
    }

    public JButton getRejectButton() {
        return rejectButton;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }

    public void setServerStatus(String statusText) {
        serverStatusLabel.setText(statusText);
    }
}
