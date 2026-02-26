package panels.roles;

import models.User;
import models.Vehicle;
import services.CloudLogService;
import services.VehicleService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OwnerPanel extends JPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final User currentUser;
    private final VehicleService vehicleService;
    private final CloudLogService cloudLogService;

    private final JTextField ownerIdField;
    private final JTextField vehicleInfoField;
    private final JTextField residencyField;
    private final JLabel countLabel;
    private final JLabel totalEarningsLabel;

    private final DefaultTableModel vehiclesModel;
    private final JTable vehiclesTable;

    public OwnerPanel(User currentUser, VehicleService vehicleService, CloudLogService cloudLogService) {
        this.currentUser = currentUser;
        this.vehicleService = vehicleService;
        this.cloudLogService = cloudLogService;
        setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Vehicle Owner Registration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addLabel(formPanel, "Vehicle Owner ID:", gbc, row);
        ownerIdField = addField(formPanel, gbc, row++);
        ownerIdField.setText(currentUser.getUserId());
        ownerIdField.setEditable(false);

        addLabel(formPanel, "Vehicle Information:", gbc, row);
        vehicleInfoField = addField(formPanel, gbc, row++);

        addLabel(formPanel, "Approx Residency Time (hrs):", gbc, row);
        residencyField = addField(formPanel, gbc, row++);

        JButton registerButton = new JButton("Register Vehicle");
        JButton refreshButton = new JButton("Refresh Vehicles");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        formPanel.add(registerButton, gbc);
        gbc.gridx = 1;
        formPanel.add(refreshButton, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        countLabel = new JLabel("Vehicles registered: 0 / " + VehicleService.MAX_VEHICLES_PER_OWNER);
        formPanel.add(countLabel, gbc);

        row++;
        gbc.gridy = row;
        totalEarningsLabel = new JLabel("Total payout earned: $0.00");
        formPanel.add(totalEarningsLabel, gbc);

        registerButton.addActionListener(e -> registerVehicle());
        refreshButton.addActionListener(e -> refreshVehicles());

        vehiclesModel = new DefaultTableModel(
                new Object[]{"Vehicle ID", "Vehicle Info", "Residency (hrs)", "Total Earned ($)", "Jobs Worked", "Jobs In Progress"},
                0) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        vehiclesTable = new JTable(vehiclesModel);
        vehiclesTable.setFillsViewportHeight(true);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("My Vehicles"));
        tablePanel.add(new JScrollPane(vehiclesTable), BorderLayout.CENTER);

        add(formPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);

        refreshVehicles();
    }

    private void registerVehicle() {
        String ownerId = ownerIdField.getText().trim();
        String vehicleInfo = vehicleInfoField.getText().trim();
        String residencyText = residencyField.getText().trim();

        if (ownerId.isEmpty() || vehicleInfo.isEmpty() || residencyText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Owner ID, vehicle info, and residency are required.");
            return;
        }

        int residencyHours;
        try {
            residencyHours = Integer.parseInt(residencyText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Residency time must be a whole number.");
            return;
        }

        if (residencyHours <= 0) {
            JOptionPane.showMessageDialog(this, "Residency time must be positive.");
            return;
        }

        try {
            Vehicle vehicle = vehicleService.registerVehicle(ownerId, vehicleInfo, residencyHours);
            cloudLogService.append(String.format(
                    "[%s] ROLE: VEHICLE_OWNER | OWNER_ID: %s | VEHICLE_ID: %s | VEHICLE_INFO: %s | RESIDENCY_HRS: %d | PAYOUT_EARNED: %s | STATUS: REGISTERED",
                    LocalDateTime.now().format(TS_FMT),
                    ownerId,
                    vehicle.getVehicleId(),
                    safe(vehicleInfo),
                    residencyHours,
                    vehicle.getTotalEarnings().toPlainString()));

            clearForm();
            refreshVehicles();
            JOptionPane.showMessageDialog(this, "Vehicle registered.");
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Vehicle registered, but audit log write failed.");
            refreshVehicles();
        }
    }

    private void refreshVehicles() {
        vehiclesModel.setRowCount(0);
        List<Vehicle> vehicles = vehicleService.getVehiclesForOwner(currentUser.getUserId());
        for (Vehicle vehicle : vehicles) {
            vehiclesModel.addRow(new Object[]{
                    vehicle.getVehicleId(),
                    vehicle.getVehicleInfo(),
                    vehicle.getResidencyHours(),
                    vehicle.getTotalEarnings().toPlainString(),
                    vehicle.getJobsWorked(),
                    vehicle.getJobsInProgress()
            });
        }

        countLabel.setText("Vehicles registered: " + vehicles.size() + " / " + VehicleService.MAX_VEHICLES_PER_OWNER);
        BigDecimal totalOwnerEarnings = vehicleService.totalEarningsForOwner(currentUser.getUserId());
        totalEarningsLabel.setText("Total payout earned: $" + totalOwnerEarnings.toPlainString());
    }

    private void clearForm() {
        vehicleInfoField.setText("");
        residencyField.setText("");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void addLabel(JPanel panel, String text, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(text), gbc);
    }

    private JTextField addField(JPanel panel, GridBagConstraints gbc, int row) {
        JTextField field = new JTextField(24);
        gbc.gridx = 1;
        gbc.gridy = row;
        panel.add(field, gbc);
        return field;
    }
}
