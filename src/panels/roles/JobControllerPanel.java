package panels.roles;

import models.Job;
import models.Vehicle;
import services.CloudLogService;
import services.JobService;
import models.User;
import services.VehicleService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class JobControllerPanel extends JPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final User currentUser;
    private final JobService jobService;
    private final VehicleService vehicleService;
    private final CloudLogService cloudLogService;

    private final DefaultTableModel jobsModel;
    private final JTable jobsTable;
    private final JTextArea detailsArea;
    private final JComboBox<String> vehicleSelector;

    public JobControllerPanel(User currentUser, JobService jobService, VehicleService vehicleService, CloudLogService cloudLogService) {
        this.currentUser = currentUser;
        this.jobService = jobService;
        this.vehicleService = vehicleService;
        this.cloudLogService = cloudLogService;

        setLayout(new BorderLayout(8, 8));

        jobsModel = new DefaultTableModel(
                new Object[]{"Queue Rank", "Job ID", "Assigned Vehicle", "Submitter ID", "Description", "Deadline", "Status", "Interrupts", "Failures", "Reports"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jobsTable = new JTable(jobsModel);
        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedDetails();
            }
        });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Job Controller Queue Management"));
        JButton upButton = new JButton("Move Up");
        JButton downButton = new JButton("Move Down");
        vehicleSelector = new JComboBox<>();
        JButton assignButton = new JButton("Attach To Vehicle");
        JButton refreshButton = new JButton("Refresh");
        topBar.add(upButton);
        topBar.add(downButton);
        topBar.add(new JLabel("Vehicle:"));
        topBar.add(vehicleSelector);
        topBar.add(assignButton);
        topBar.add(refreshButton);

        upButton.addActionListener(e -> moveSelected(true));
        downButton.addActionListener(e -> moveSelected(false));
        assignButton.addActionListener(e -> assignSelectedJobToVehicle());
        refreshButton.addActionListener(e -> refreshJobs());

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createTitledBorder("All Jobs Across Vehicles"));
        center.add(new JScrollPane(jobsTable), BorderLayout.CENTER);

        detailsArea = new JTextArea(6, 40);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Selected Job Details"));
        center.add(detailsScroll, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        refreshJobs();
    }

    private void assignSelectedJobToVehicle() {
        int row = jobsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }

        String selectedVehicle = (String) vehicleSelector.getSelectedItem();
        String vehicleId = parseVehicleId(selectedVehicle);
        if (vehicleId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a vehicle.");
            return;
        }

        String jobId = String.valueOf(jobsModel.getValueAt(row, 1));
        if (!jobService.assignJobToVehicle(jobId, vehicleId)) {
            JOptionPane.showMessageDialog(this, "Could not attach job to vehicle.");
            return;
        }

        writeAudit(String.format(
                "[%s] ROLE: JOB_CONTROLLER | CONTROLLER_ID: %s | JOB_ID: %s | ACTION: ATTACH_TO_VEHICLE | VEHICLE_ID: %s",
                LocalDateTime.now().format(TS_FMT),
                currentUser.getUserId(),
                jobId,
                vehicleId));

        refreshJobs();
        selectByJobId(jobId);
    }

    private void moveSelected(boolean up) {
        int row = jobsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }

        String jobId = String.valueOf(jobsModel.getValueAt(row, 1));
        boolean moved = up ? jobService.moveJobUp(jobId) : jobService.moveJobDown(jobId);
        if (!moved) {
            JOptionPane.showMessageDialog(this, up ? "Job is already at the top." : "Job is already at the bottom.");
            return;
        }

        writeAudit(String.format(
                "[%s] ROLE: JOB_CONTROLLER | CONTROLLER_ID: %s | JOB_ID: %s | ACTION: %s",
                LocalDateTime.now().format(TS_FMT),
                currentUser.getUserId(),
                jobId,
                up ? "MOVE_UP" : "MOVE_DOWN"));

        refreshJobs();
        selectByJobId(jobId);
    }

    private void refreshJobs() {
        refreshVehicleSelector();
        jobsModel.setRowCount(0);
        List<Job> jobs = jobService.getQueueOrderedJobs();
        for (Job job : jobs) {
            jobsModel.addRow(new Object[]{
                    job.getPriorityRank(),
                    job.getJobId(),
                    emptyIfBlank(job.getAssignedVehicleId()),
                    job.getSubmitterUserId(),
                    job.getDescription(),
                    job.getDeadline(),
                    job.getStatus().name(),
                    job.getInterrupts(),
                    job.getFailures(),
                    job.getReportLines().size()
            });
        }
        showSelectedDetails();
    }

    private void selectByJobId(String jobId) {
        for (int i = 0; i < jobsModel.getRowCount(); i++) {
            String rowId = String.valueOf(jobsModel.getValueAt(i, 1));
            if (jobId.equalsIgnoreCase(rowId)) {
                jobsTable.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private void showSelectedDetails() {
        int row = jobsTable.getSelectedRow();
        if (row < 0) {
            detailsArea.setText("");
            return;
        }

        String jobId = String.valueOf(jobsModel.getValueAt(row, 1));
        Job job = jobService.findById(jobId).orElse(null);
        if (job == null) {
            detailsArea.setText("");
            return;
        }

        String text = "Job ID: " + job.getJobId() + System.lineSeparator()
                + "Submitter: " + job.getSubmitterUserId() + System.lineSeparator()
                + "Status: " + job.getStatus().name() + System.lineSeparator()
                + "Queue Rank: " + job.getPriorityRank() + System.lineSeparator()
                + "Assigned Vehicle: " + emptyIfBlank(job.getAssignedVehicleId()) + System.lineSeparator()
                + "Deadline: " + job.getDeadline() + System.lineSeparator()
                + "Cost: " + job.getCostForJob().toPlainString() + System.lineSeparator()
                + "Interrupts: " + job.getInterrupts() + System.lineSeparator()
                + "Failures: " + job.getFailures() + System.lineSeparator()
                + "Reports:" + System.lineSeparator()
                + String.join(System.lineSeparator(), job.getReportLines());
        detailsArea.setText(text);
    }

    private void refreshVehicleSelector() {
        String current = (String) vehicleSelector.getSelectedItem();
        vehicleSelector.removeAllItems();
        for (Vehicle vehicle : vehicleService.getAllVehicles()) {
            vehicleSelector.addItem(vehicle.getVehicleId() + " - " + vehicle.getVehicleInfo());
        }
        if (current != null) {
            vehicleSelector.setSelectedItem(current);
        }
    }

    private String parseVehicleId(String selectedValue) {
        if (selectedValue == null || selectedValue.trim().isEmpty()) {
            return "";
        }
        int sep = selectedValue.indexOf(" - ");
        if (sep < 0) {
            return selectedValue.trim();
        }
        return selectedValue.substring(0, sep).trim();
    }

    private String emptyIfBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "(unassigned)";
        }
        return value.trim();
    }

    private void writeAudit(String entry) {
        try {
            cloudLogService.append(entry);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not write to audit log.");
        }
    }
}
