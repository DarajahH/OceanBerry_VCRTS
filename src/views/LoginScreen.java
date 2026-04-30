package views;

import app.VcrtsTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import services.CloudDataService;

public class LoginScreen {
    private final CloudDataService service;
    private final JFrame frame;
    private final JLabel statusLabel;
    private final JPanel statusPanel;

    public LoginScreen(CloudDataService service) {
        this.service = service;
        frame = new JFrame("VCRTS Login Portal");
        frame.setSize(980, 620);
        frame.setMinimumSize(new Dimension(900, 560));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(VcrtsTheme.CANVAS);

        JPanel shell = new JPanel(new GridBagLayout());
        shell.setBackground(VcrtsTheme.CANVAS);
        shell.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel content = new JPanel(new GridLayout(1, 2, 18, 0));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(1080, 540));
        content.add(createBrandRail());

        JPanel authColumn = new JPanel(new GridBagLayout());
        authColumn.setOpaque(false);
        authColumn.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JPanel card = new JPanel();
        card.setBackground(VcrtsTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VcrtsTheme.BORDER),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 430));
        card.setMinimumSize(new Dimension(420, 430));

        JLabel overline = new JLabel("ACCESS");
        overline.setForeground(VcrtsTheme.TEXT_MUTED);
        overline.setFont(VcrtsTheme.META_FONT);
        overline.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(overline);
        card.add(Box.createVerticalStrut(8));

        JLabel title = new JLabel("Sign in");
        title.setForeground(VcrtsTheme.TEXT_PRIMARY);
        title.setFont(new Font("Dialog", Font.BOLD, 26));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        card.add(Box.createVerticalStrut(18));

        statusLabel = new JLabel();
        statusLabel.setFont(VcrtsTheme.BODY_FONT);
        statusLabel.setForeground(VcrtsTheme.TEXT_PRIMARY);
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(true);
        statusPanel.setBackground(VcrtsTheme.ACCENT_GHOST);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VcrtsTheme.BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.setVisible(false);
        card.add(statusPanel);
        card.add(Box.createVerticalStrut(14));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        JTextField userField = new JTextField(18);
        JPasswordField passField = new JPasswordField(18);
        styleTextField(userField);
        styleTextField(passField);

        form.add(createFieldBlock("Username", userField), gbc);
        gbc.gridy++;
        form.add(createFieldBlock("Password", passField), gbc);

        JButton loginBtn = new JButton("Sign In");
        JButton regBtn = new JButton("Create Account");
        stylePrimaryButton(loginBtn);
        styleSecondaryButton(regBtn);

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(loginBtn);
        buttonRow.add(regBtn);
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 0, 0);
        form.add(buttonRow, gbc);
        card.add(form);

        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.gridx = 0;
        cardGbc.gridy = 0;
        cardGbc.anchor = GridBagConstraints.CENTER;
        authColumn.add(card, cardGbc);

        content.add(authColumn);
        shell.add(content);
        frame.add(shell, BorderLayout.CENTER);
        frame.getRootPane().setDefaultButton(loginBtn);

        loginBtn.addActionListener(e -> {
            clearStatus();
            if (service.validateUser(userField.getText(), new String(passField.getPassword()))) {
                String role = service.getCurrentUserRole();
                frame.dispose();
                new VCRTSDashboard(service, role);
            } else {
                showStatus("Invalid username or password.", VcrtsTheme.DANGER);
            }
        });

        regBtn.addActionListener(e -> {
            clearStatus();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                showStatus("Username and password cannot be blank.", VcrtsTheme.WARNING);
                return;
            }

            String role = chooseRole();
            if (role == null) {
                showStatus("Account creation canceled.", VcrtsTheme.ACCENT_GHOST);
                return;
            }

            try {
                service.registerUser(username, password, role);
                showStatus("Account created. You can sign in now.", VcrtsTheme.SUCCESS);
            } catch (IllegalArgumentException ex) {
                showStatus(ex.getMessage(), VcrtsTheme.WARNING);
            } catch (Exception ex) {
                showStatus("Unable to save user right now.", VcrtsTheme.DANGER);
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createBrandRail() {
        JPanel rail = new JPanel(new BorderLayout(0, 0));
        rail.setBackground(VcrtsTheme.SIDEBAR);
        rail.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VcrtsTheme.BORDER),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));
        rail.setPreferredSize(new Dimension(0, 0));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("VCRTS");
        eyebrow.setForeground(VcrtsTheme.TEXT_MUTED);
        eyebrow.setFont(VcrtsTheme.META_FONT);
        top.add(eyebrow);
        top.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("<html>Vehicular cloud<br>control workspace</html>");
        title.setForeground(VcrtsTheme.TEXT_PRIMARY);
        title.setFont(new Font("Dialog", Font.BOLD, 25));
        top.add(title);
        top.add(Box.createVerticalStrut(14));

        top.add(Box.createVerticalStrut(6));

        rail.add(top, BorderLayout.NORTH);

        JPanel stack = new JPanel(new GridLayout(3, 1, 0, 10));
        stack.setOpaque(false);
        stack.add(createRoleCard("Client", null));
        stack.add(createRoleCard("Owner", null));
        stack.add(createRoleCard("Admin", null));
        rail.add(stack, BorderLayout.CENTER);
        return rail;
    }

    private JPanel createRoleCard(String title, String description) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(VcrtsTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VcrtsTheme.BORDER),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(VcrtsTheme.TEXT_PRIMARY);
        titleLabel.setFont(VcrtsTheme.LABEL_FONT);
        card.add(titleLabel, BorderLayout.NORTH);

        if (description != null && !description.isBlank()) {
            JLabel body = new JLabel("<html>" + description + "</html>");
            body.setForeground(VcrtsTheme.TEXT_MUTED);
            body.setFont(VcrtsTheme.BODY_FONT);
            card.add(body, BorderLayout.CENTER);
        }
        return card;
    }

    private JPanel createFieldBlock(String labelText, JComponent field) {
        JPanel block = new JPanel(new BorderLayout(0, 8));
        block.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(VcrtsTheme.TEXT_SECONDARY);
        label.setFont(VcrtsTheme.LABEL_FONT);
        block.add(label, BorderLayout.NORTH);
        block.add(field, BorderLayout.CENTER);
        return block;
    }

    private String chooseRole() {
        JComboBox<String> roleBox = new JComboBox<>(new String[] {"CLIENT", "OWNER", "ADMIN"});
        styleComboBox(roleBox);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(VcrtsTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VcrtsTheme.BORDER),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel label = new JLabel("Role");
        label.setForeground(VcrtsTheme.TEXT_PRIMARY);
        label.setFont(VcrtsTheme.BODY_FONT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(roleBox, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
            frame,
            panel,
            "Create Account",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        return result == JOptionPane.OK_OPTION ? String.valueOf(roleBox.getSelectedItem()) : null;
    }

    private void showStatus(String message, java.awt.Color tone) {
        statusLabel.setText(message);
        statusPanel.setBackground(tone);
        statusLabel.setForeground(VcrtsTheme.TEXT_PRIMARY);
        statusPanel.setVisible(true);
        frame.revalidate();
        frame.repaint();
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusPanel.setVisible(false);
    }

    private void styleTextField(JTextField field) {
        field.setBackground(VcrtsTheme.FIELD);
        field.setForeground(VcrtsTheme.TEXT_PRIMARY);
        field.setCaretColor(VcrtsTheme.TEXT_PRIMARY);
        field.setFont(new Font("Dialog", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VcrtsTheme.BORDER),
            BorderFactory.createEmptyBorder(9, 11, 9, 11)
        ));
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(VcrtsTheme.FIELD);
        comboBox.setForeground(VcrtsTheme.TEXT_PRIMARY);
        comboBox.setFont(new Font("Dialog", Font.PLAIN, 13));
        comboBox.setBorder(BorderFactory.createLineBorder(VcrtsTheme.BORDER));
    }

    private void stylePrimaryButton(AbstractButton button) {
        styleButton(button, VcrtsTheme.ACCENT, VcrtsTheme.TEXT_PRIMARY);
    }

    private void styleSecondaryButton(AbstractButton button) {
        styleButton(button, VcrtsTheme.ACCENT_GHOST, VcrtsTheme.TEXT_PRIMARY);
    }

    private void styleButton(AbstractButton button, java.awt.Color background, java.awt.Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFont(new Font("Dialog", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VcrtsTheme.BORDER),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public String toString() {
        return "LoginScreen{"
            + "service=" + service
            + ", frame=" + frame
            + '}';
    }
}
