package panels.roles;

import models.Job;
import models.User;
import services.JobService;
import services.CloudLogService;

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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientPanel extends JPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DEADLINE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final User currentUser;
    private final JobService jobService;
    private final CloudLogService cloudLogService;

    private final JTextField clientIdField;
    private final JTextField durationField;
    private final JTextField deadlineField;
    private final JTextField costField;
    private final JTextField descriptionField;
    private final JLabel jobCountValue;
    private final JLabel interruptStatValue;
    private final JLabel failureStatValue;

    private final DefaultTableModel jobsModel;
    private final JTable jobsTable;

    public ClientPanel(User currentUser, JobService jobService, CloudLogService cloudLogService) {
        this.currentUser = currentUser;
        this.jobService = jobService;
        this.cloudLogService = cloudLogService;
        setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addLabel(formPanel, "Client ID:", gbc, row);
        clientIdField = addField(formPanel, gbc, row++);
        clientIdField.setText(currentUser.getUserId());
        clientIdField.setEditable(false);

        addLabel(formPanel, "Approx Job Duration (hrs):", gbc, row);
        durationField = addField(formPanel, gbc, row++);

        addLabel(formPanel, "Job Deadline (yyyy-MM-dd HH:mm):", gbc, row);
        deadlineField = addField(formPanel, gbc, row++);

        addLabel(formPanel, "Cost for Job ($):", gbc, row);
        costField = addField(formPanel, gbc, row++);

        addLabel(formPanel, "Job Description:", gbc, row);
        descriptionField = addField(formPanel, gbc, row++);

        JButton submitButton = new JButton("Add Job");
        JButton refreshButton = new JButton("Refresh Jobs");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        formPanel.add(submitButton, gbc);
        gbc.gridx = 1;
        formPanel.add(refreshButton, gbc);

        jobsModel = new DefaultTableModel(
                new Object[]{"Job ID", "Description", "Duration (hrs)", "Deadline", "Cost", "Interrupts", "Failures", "Status"},
                0) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        jobsTable = new JTable(jobsModel);
        jobsTable.setFillsViewportHeight(true);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("My Job Listings"));
        tablePanel.add(new JScrollPane(jobsTable), BorderLayout.CENTER);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Job Stats"));
        jobCountValue = new JLabel("Jobs: 0");
        interruptStatValue = new JLabel("Interrupts: 0");
        failureStatValue = new JLabel("Failures: 0");
        statsPanel.add(jobCountValue);
        statsPanel.add(interruptStatValue);
        statsPanel.add(failureStatValue);
        tablePanel.add(statsPanel, BorderLayout.SOUTH);

        add(formPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);

        submitButton.addActionListener(e -> submit());
        refreshButton.addActionListener(e -> refreshJobsTable());
        refreshJobsTable();
    }

    private void submit() {
        String clientId = clientIdField.getText().trim();
        String duration = durationField.getText().trim();
        String deadline = deadlineField.getText().trim();
        String cost = costField.getText().trim();
        String description = descriptionField.getText().trim();

        if (clientId.isEmpty() || duration.isEmpty() || deadline.isEmpty() || cost.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Client ID, duration, deadline, cost, and description are required.");
            return;
        }

        int durationHours;
        BigDecimal jobCost;
        LocalDateTime jobDeadline;

        try {
            durationHours = Integer.parseInt(duration);
            jobCost = new BigDecimal(cost);
            jobDeadline = parseDeadline(deadline);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Duration must be a whole number and cost must be numeric.");
            return;
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        if (durationHours <= 0 || jobCost.signum() <= 0) {
            JOptionPane.showMessageDialog(this, "Duration and cost must be positive.");
            return;
        }

        if (jobDeadline.isBefore(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "Deadline must be a present or future timestamp.");
            return;
        }

        Job job = jobService.createClientJob(
                clientId,
                description,
                durationHours,
                jobDeadline,
                jobCost);

        String entry = String.format(
                "[%s] ROLE: CLIENT | CLIENT_ID: %s | JOB_ID: %s | APPROX_JOB_DURATION: %d | JOB_DEADLINE: %s | COST_FOR_JOB: %s | JOB_DESCRIPTION: %s | INTERRUPTS: %d | FAILURES: %d | STATUS: %s",
                LocalDateTime.now().format(TS_FMT),
                clientId,
                job.getJobId(),
                durationHours,
                jobDeadline.format(DEADLINE_FMT),
                jobCost.toPlainString(),
                safe(description),
                job.getInterrupts(),
                job.getFailures(),
                job.getStatus().name());

        try {
            cloudLogService.append(entry);
            clearForm();
            refreshJobsTable();
            JOptionPane.showMessageDialog(this, "Job created successfully.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Job was created, but writing to audit log failed.");
        }
    }

    private void refreshJobsTable() {
        jobsModel.setRowCount(0);

        List<Job> jobs = jobService.getJobsForClient(currentUser.getUserId());
        int totalInterrupts = 0;
        int totalFailures = 0;
        for (Job job : jobs) {
            totalInterrupts += job.getInterrupts();
            totalFailures += job.getFailures();
            jobsModel.addRow(new Object[]{
                    job.getJobId(),
                    job.getDescription(),
                    job.getApproxDurationHours(),
                    job.getDeadline().format(DEADLINE_FMT),
                    job.getCostForJob().toPlainString(),
                    job.getInterrupts(),
                    job.getFailures(),
                    job.getStatus().name()
            });
        }

        jobCountValue.setText("Jobs: " + jobs.size());
        interruptStatValue.setText("Interrupts: " + totalInterrupts);
        failureStatValue.setText("Failures: " + totalFailures);
    }

    private void clearForm() {
        durationField.setText("");
        deadlineField.setText("");
        costField.setText("");
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
