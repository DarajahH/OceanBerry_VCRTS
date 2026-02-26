package panels.roles;

import models.Job;
import models.JobStatus;
import models.User;
import services.CloudLogService;
import services.JobService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class JobOwnerPanel extends JPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DEADLINE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final User currentUser;
    private final JobService jobService;
    private final CloudLogService cloudLogService;

    private JTextField ownerIdField;
    private JTextField durationField;
    private JTextField deadlineField;
    private JTextField submissionCostField;
    private JTextField descriptionField;

    private JTextField selectedJobIdField;
    private JComboBox<JobStatus> statusCombo;
    private JTextField reportLineField;
    private final JTextArea reportsArea;

    private final DefaultTableModel jobsModel;
    private final JTable jobsTable;

    public JobOwnerPanel(User currentUser, JobService jobService, CloudLogService cloudLogService) {
        this.currentUser = currentUser;
        this.jobService = jobService;
        this.cloudLogService = cloudLogService;

        setLayout(new BorderLayout(8, 8));

        JPanel northPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        northPanel.add(createSubmitPanel());
        northPanel.add(createManagePanel());

        jobsModel = new DefaultTableModel(
                new Object[]{"Job ID", "Description", "Duration (hrs)", "Deadline", "Cost", "Interrupts", "Failures", "Status", "Reports"},
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
                syncSelectionFromTable();
            }
        });

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("My Submitted Jobs"));
        tablePanel.add(new JScrollPane(jobsTable), BorderLayout.CENTER);

        reportsArea = new JTextArea(6, 40);
        reportsArea.setEditable(false);
        reportsArea.setLineWrap(true);
        reportsArea.setWrapStyleWord(true);
        JScrollPane reportsScroll = new JScrollPane(reportsArea);
        reportsScroll.setBorder(BorderFactory.createTitledBorder("Selected Job Reports"));
        tablePanel.add(reportsScroll, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);

        refreshJobsTable();
    }

    private JPanel createSubmitPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Submit Job"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addLabel(panel, "Job Owner ID:", gbc, row);
        ownerIdField = addField(panel, gbc, row++);
        ownerIdField.setText(currentUser.getUserId());
        ownerIdField.setEditable(false);

        addLabel(panel, "Approx Job Duration (hrs):", gbc, row);
        durationField = addField(panel, gbc, row++);

        addLabel(panel, "Job Deadline (yyyy-MM-dd HH:mm):", gbc, row);
        deadlineField = addField(panel, gbc, row++);

        addLabel(panel, "Cost for Submission ($):", gbc, row);
        submissionCostField = addField(panel, gbc, row++);

        addLabel(panel, "Job Description:", gbc, row);
        descriptionField = addField(panel, gbc, row++);

        JButton submitButton = new JButton("Submit Job");
        JButton refreshButton = new JButton("Refresh");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(submitButton, gbc);
        gbc.gridx = 1;
        panel.add(refreshButton, gbc);

        submitButton.addActionListener(e -> submitJob());
        refreshButton.addActionListener(e -> refreshJobsTable());

        return panel;
    }

    private JPanel createManagePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Track Progress & Reports"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addLabel(panel, "Selected Job ID:", gbc, row);
        selectedJobIdField = addField(panel, gbc, row++);
        selectedJobIdField.setEditable(false);

        addLabel(panel, "Status:", gbc, row);
        statusCombo = new JComboBox<>(JobStatus.values());
        gbc.gridx = 1;
        gbc.gridy = row++;
        panel.add(statusCombo, gbc);

        addLabel(panel, "Report Line:", gbc, row);
        reportLineField = addField(panel, gbc, row++);

        JButton updateStatusButton = new JButton("Update Status");
        JButton addReportButton = new JButton("Add Report");
        JButton addInterruptButton = new JButton("+ Interrupt");
        JButton addFailureButton = new JButton("+ Failure");

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(updateStatusButton, gbc);
        gbc.gridx = 1;
        panel.add(addReportButton, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(addInterruptButton, gbc);
        gbc.gridx = 1;
        panel.add(addFailureButton, gbc);

        updateStatusButton.addActionListener(e -> updateStatus());
        addReportButton.addActionListener(e -> addReportLine());
        addInterruptButton.addActionListener(e -> incrementInterrupts());
        addFailureButton.addActionListener(e -> incrementFailures());

        return panel;
    }

    private void submitJob() {
        String ownerId = ownerIdField.getText().trim();
        String durationText = durationField.getText().trim();
        String deadlineText = deadlineField.getText().trim();
        String costText = submissionCostField.getText().trim();
        String description = descriptionField.getText().trim();

        if (ownerId.isEmpty() || durationText.isEmpty() || deadlineText.isEmpty() || costText.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All submit-job fields are required.");
            return;
        }

        int durationHours;
        BigDecimal submissionCost;
        LocalDateTime deadline;
        try {
            durationHours = Integer.parseInt(durationText);
            submissionCost = new BigDecimal(costText);
            deadline = parseDeadline(deadlineText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Duration must be a whole number and cost must be numeric.");
            return;
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        if (durationHours <= 0 || submissionCost.signum() <= 0) {
            JOptionPane.showMessageDialog(this, "Duration and cost must be positive.");
            return;
        }
        if (deadline.isBefore(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "Deadline must be a present or future timestamp.");
            return;
        }

        Job job = jobService.createJobOwnerSubmission(
                ownerId,
                description,
                durationHours,
                deadline,
                submissionCost);

        writeAudit(String.format(
                "[%s] ROLE: JOB_OWNER | JOB_OWNER_ID: %s | JOB_ID: %s | SUBMISSION_TIMESTAMP: %s | COST_FOR_SUBMISSION: %s | JOB_DESCRIPTION: %s | STATUS: %s",
                LocalDateTime.now().format(TS_FMT),
                ownerId,
                job.getJobId(),
                job.getCreatedAt().format(TS_FMT),
                submissionCost.toPlainString(),
                safe(description),
                job.getStatus().name()));

        clearSubmitForm();
        refreshJobsTable();
        JOptionPane.showMessageDialog(this, "Job submission saved.");
    }

    private void updateStatus() {
        Optional<Job> selected = selectedJob();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }

        JobStatus newStatus = (JobStatus) statusCombo.getSelectedItem();
        if (newStatus == null) {
            JOptionPane.showMessageDialog(this, "Select a status.");
            return;
        }

        jobService.updateStatus(selected.get().getJobId(), newStatus);
        writeAudit(String.format(
                "[%s] ROLE: JOB_OWNER | JOB_OWNER_ID: %s | JOB_ID: %s | ACTION: UPDATE_STATUS | STATUS: %s",
                LocalDateTime.now().format(TS_FMT),
                currentUser.getUserId(),
                selected.get().getJobId(),
                newStatus.name()));

        refreshJobsTable();
    }

    private void addReportLine() {
        Optional<Job> selected = selectedJob();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }

        String reportLine = reportLineField.getText().trim();
        if (reportLine.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter report text.");
            return;
        }

        jobService.addReportLine(selected.get().getJobId(), reportLine);
        writeAudit(String.format(
                "[%s] ROLE: JOB_OWNER | JOB_OWNER_ID: %s | JOB_ID: %s | ACTION: ADD_REPORT | REPORT_LINE: %s",
                LocalDateTime.now().format(TS_FMT),
                currentUser.getUserId(),
                selected.get().getJobId(),
                safe(reportLine)));

        reportLineField.setText("");
        refreshJobsTable();
    }

    private void incrementInterrupts() {
        Optional<Job> selected = selectedJob();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }

        jobService.incrementInterrupts(selected.get().getJobId());
        writeAudit(String.format(
                "[%s] ROLE: JOB_OWNER | JOB_OWNER_ID: %s | JOB_ID: %s | ACTION: INCREMENT_INTERRUPT",
                LocalDateTime.now().format(TS_FMT),
                currentUser.getUserId(),
                selected.get().getJobId()));

        refreshJobsTable();
    }

    private void incrementFailures() {
        Optional<Job> selected = selectedJob();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a job first.");
            return;
        }

        jobService.incrementFailures(selected.get().getJobId());
        writeAudit(String.format(
                "[%s] ROLE: JOB_OWNER | JOB_OWNER_ID: %s | JOB_ID: %s | ACTION: INCREMENT_FAILURE",
                LocalDateTime.now().format(TS_FMT),
                currentUser.getUserId(),
                selected.get().getJobId()));

        refreshJobsTable();
    }

    private void refreshJobsTable() {
        String selectedId = selectedJobIdField.getText().trim();

        jobsModel.setRowCount(0);
        List<Job> jobs = jobService.getJobsForSubmitter(currentUser.getUserId());
        for (Job job : jobs) {
            jobsModel.addRow(new Object[]{
                    job.getJobId(),
                    job.getDescription(),
                    job.getApproxDurationHours(),
                    job.getDeadline().format(DEADLINE_FMT),
                    job.getCostForJob().toPlainString(),
                    job.getInterrupts(),
                    job.getFailures(),
                    job.getStatus().name(),
                    job.getReportLines().size()
            });
        }

        restoreSelection(selectedId);
        syncSelectionFromTable();
    }

    private void restoreSelection(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            return;
        }
        for (int i = 0; i < jobsModel.getRowCount(); i++) {
            String rowId = String.valueOf(jobsModel.getValueAt(i, 0));
            if (jobId.equalsIgnoreCase(rowId)) {
                jobsTable.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private void syncSelectionFromTable() {
        int row = jobsTable.getSelectedRow();
        if (row < 0) {
            selectedJobIdField.setText("");
            reportsArea.setText("");
            return;
        }

        String jobId = String.valueOf(jobsModel.getValueAt(row, 0));
        selectedJobIdField.setText(jobId);

        Optional<Job> maybeJob = jobService.findById(jobId);
        if (maybeJob.isPresent()) {
            Job job = maybeJob.get();
            statusCombo.setSelectedItem(job.getStatus());
            reportsArea.setText(String.join(System.lineSeparator(), job.getReportLines()));
        } else {
            reportsArea.setText("");
        }
    }

    private Optional<Job> selectedJob() {
        String jobId = selectedJobIdField.getText().trim();
        if (jobId.isEmpty()) {
            return Optional.empty();
        }
        return jobService.findById(jobId);
    }

    private void clearSubmitForm() {
        durationField.setText("");
        deadlineField.setText("");
        submissionCostField.setText("");
        descriptionField.setText("");
    }

    private LocalDateTime parseDeadline(String value) {
        try {
            return LocalDateTime.parse(value, DEADLINE_FMT);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ignoredAgain) {
                throw new IllegalArgumentException("Deadline format must be yyyy-MM-dd HH:mm or ISO yyyy-MM-ddTHH:mm:ss.");
            }
        }
    }

    private void writeAudit(String entry) {
        try {
            cloudLogService.append(entry);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not write to audit log.");
        }
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
        JTextField field = new JTextField(22);
        gbc.gridx = 1;
        gbc.gridy = row;
        panel.add(field, gbc);
        return field;
    }
}
