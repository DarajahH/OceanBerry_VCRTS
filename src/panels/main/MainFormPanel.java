package panels.main;

import models.Permission;
import models.Role;
import models.User;
import services.CloudLogService;
import services.RolePermissionService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainFormPanel extends JPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CloudLogService cloudLogService;
    private final User currentUser;
    private final RolePermissionService rolePermissionService;
    private final Runnable onLogUpdated;

    private final JTextField idField;
    private final JTextField infoField;
    private final JTextField durationField;
    private final JTextField deadlineField;

    private final JLabel idLabel;
    private final JLabel infoLabel;
    private final JLabel durLabel;
    private final JLabel deadlineLabel;
    private final JLabel adminInstruction;

    private final JComboBox<String> roleSelector;
    private final JButton approveBtn;
    private final JButton rejectBtn;
    private final JButton submitBtn;

    public MainFormPanel(
            User currentUser,
            CloudLogService cloudLogService,
            RolePermissionService rolePermissionService,
            Runnable onLogUpdated) {
        this.currentUser = currentUser;
        this.cloudLogService = cloudLogService;
        this.rolePermissionService = rolePermissionService;
        this.onLogUpdated = onLogUpdated;

        setOpaque(false);
        setLayout(null);

        int lx = 40;
        int fx = 230;
        int sy = 40;
        int sp = 55;

        addStyledLabel("Switch User Role:", lx, sy);
        roleSelector = new JComboBox<>(new String[]{"VEHICLE_OWNER", "JOB_SUBMITTER", "JOB_CONTROLLER", "SYSTEM_ADMIN"});
        roleSelector.setBounds(fx, sy, 240, 35);
        add(roleSelector);

        adminInstruction = new JLabel("<html><i>Admin Mode: Decision power active.</i></html>");
        adminInstruction.setForeground(Color.CYAN);
        adminInstruction.setBounds(fx, sy + 35, 260, 20);
        adminInstruction.setVisible(false);
        add(adminInstruction);

        idLabel = addStyledLabel("Owner ID:", lx, sy + sp);
        idField = new JTextField();
        idField.setBounds(fx, sy + sp, 240, 30);
        add(idField);

        infoLabel = addStyledLabel("Vehicle Info:", lx, sy + (sp * 2));
        infoField = new JTextField();
        infoField.setBounds(fx, sy + (sp * 2), 240, 30);
        add(infoField);

        durLabel = addStyledLabel("Residency (Hrs):", lx, sy + (sp * 3));
        durationField = new JTextField();
        durationField.setBounds(fx, sy + (sp * 3), 240, 30);
        ((AbstractDocument) durationField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        add(durationField);

        deadlineLabel = addStyledLabel("Job Deadline:", lx, sy + (sp * 4));
        deadlineField = new JTextField();
        deadlineField.setBounds(fx, sy + (sp * 4), 240, 30);
        ((AbstractDocument) deadlineField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        deadlineField.setVisible(false);
        add(deadlineField);

        approveBtn = new JButton("APPROVE REQUEST");
        approveBtn.setBounds(fx, sy + (sp * 5), 240, 35);
        approveBtn.setBackground(new Color(52, 199, 89));
        approveBtn.setVisible(false);
        add(approveBtn);

        rejectBtn = new JButton("REJECT REQUEST");
        rejectBtn.setBounds(fx, sy + (sp * 5) + 40, 240, 35);
        rejectBtn.setBackground(new Color(255, 59, 48));
        rejectBtn.setVisible(false);
        add(rejectBtn);

        submitBtn = new JButton("Submit to Cloud");
        submitBtn.setBounds(fx, sy + (sp * 6), 240, 45);
        add(submitBtn);

        roleSelector.addActionListener(e -> updateUI((String) roleSelector.getSelectedItem()));
        submitBtn.addActionListener(e -> handleAction("SUBMITTED"));
        approveBtn.addActionListener(e -> handleAction("APPROVED"));
        rejectBtn.addActionListener(e -> handleAction("REJECTED"));

        configureRoleOptionsForUser();
        updateUI((String) roleSelector.getSelectedItem());
    }

    private void handleAction(String status) {
        String role = (String) roleSelector.getSelectedItem();
        if (!isActionAllowedForCurrentUser(role, status)) {
            JOptionPane.showMessageDialog(this, "Permission denied for " + currentUser.getRole() + ".");
            return;
        }

        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String dur = durationField.getText().trim();
        String deadline = "JOB_SUBMITTER".equals(role) ? deadlineField.getText().trim() : "N/A";

        if (id.isEmpty() || info.isEmpty() || dur.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter all information.");
            return;
        }

        String ts = LocalDateTime.now().format(TS_FMT);
        String entry = String.format("[%s] ROLE: %s | ID: %s | INFO: %s | DUR: %s | DEADLINE: %s | STATUS: %s", ts, role, id, info, dur, deadline, status);

        try {
            cloudLogService.append(entry);
            clearInputs();
            onLogUpdated.run();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving to cloud.");
        }
    }

    private void updateUI(String role) {
        boolean isAdmin = "SYSTEM_ADMIN".equals(role);
        boolean isSubmitter = "JOB_SUBMITTER".equals(role);
        boolean isController = "JOB_CONTROLLER".equals(role);

        idLabel.setText(isSubmitter ? "Submitter ID:" : (isController ? "Controller ID:" : (isAdmin ? "Target ID:" : "Vehicle Owner ID:")));
        infoLabel.setText(isSubmitter ? "Job Description:" : (isController ? "Queue Action:" : (isAdmin ? "Review Info:" : "Vehicle Information:")));
        durLabel.setText(isSubmitter ? "Approx Duration (Hrs):" : "Residency (Hrs):");
        deadlineLabel.setText("Job Deadline:");

        deadlineLabel.setVisible(isSubmitter);
        deadlineField.setVisible(isSubmitter);
        adminInstruction.setVisible(isAdmin);
        approveBtn.setVisible(isAdmin || isController);
        rejectBtn.setVisible(isAdmin || isController);
        submitBtn.setVisible(!isAdmin && !isController);
        submitBtn.setText(isSubmitter ? "Submit Job" : "Submit to Cloud");
    }

    private void clearInputs() {
        idField.setText("");
        infoField.setText("");
        durationField.setText("");
        deadlineField.setText("");
    }

    private JLabel addStyledLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));
        label.setBounds(x, y, 180, 30);
        add(label);
        return label;
    }

    private void configureRoleOptionsForUser() {
        roleSelector.removeAllItems();
        Role role = currentUser.getRole();
        if (role == Role.SYSTEM_ADMIN) {
            roleSelector.addItem(Role.VEHICLE_OWNER.name());
            roleSelector.addItem(Role.JOB_SUBMITTER.name());
            roleSelector.addItem(Role.JOB_CONTROLLER.name());
            roleSelector.addItem(Role.SYSTEM_ADMIN.name());
            roleSelector.setSelectedItem(Role.SYSTEM_ADMIN.name());
            return;
        }
        roleSelector.addItem(role.name());
        roleSelector.setSelectedItem(role.name());
    }

    private boolean isActionAllowedForCurrentUser(String selectedRoleText, String status) {
        Role role = currentUser.getRole();
        if (!"SUBMITTED".equals(status)) {
            return rolePermissionService.has(role, Permission.ADJUST_JOB_QUEUE)
                    || rolePermissionService.has(role, Permission.MANAGE_JOBS);
        }

        if ("VEHICLE_OWNER".equals(selectedRoleText)) {
            return rolePermissionService.has(role, Permission.REGISTER_VEHICLE);
        }
        if ("JOB_SUBMITTER".equals(selectedRoleText)) {
            return rolePermissionService.has(role, Permission.SUBMIT_JOBS);
        }
        if ("JOB_CONTROLLER".equals(selectedRoleText)) {
            return rolePermissionService.has(role, Permission.ADJUST_JOB_QUEUE);
        }
        if ("SYSTEM_ADMIN".equals(selectedRoleText)) {
            return rolePermissionService.has(role, Permission.MANAGE_JOBS);
        }
        return false;
    }

    static class DigitsOnlyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}
