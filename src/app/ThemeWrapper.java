package app;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.UIManager;

public final class ThemeWrapper {
    private ThemeWrapper() {
    }

    public static void apply() {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("TitlePane.unifiedBackground", true);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
            UIManager.put("TableHeader.height", 30);
            UIManager.put("Focus.width", 1);
            UIManager.put("Button.innerFocusWidth", 0);
            UIManager.put("Component.focusColor", VcrtsTheme.ACCENT_ACTIVE);
            UIManager.put("Button.background", VcrtsTheme.ACCENT);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.hoverBackground", VcrtsTheme.ACCENT_ACTIVE);
            UIManager.put("Button.default.background", VcrtsTheme.ACCENT);
            UIManager.put("Button.default.foreground", Color.WHITE);
            UIManager.put("Button.default.focusedBackground", VcrtsTheme.ACCENT_ACTIVE);
            UIManager.put("Button.borderColor", VcrtsTheme.BORDER);
            UIManager.put("Button.default.borderColor", VcrtsTheme.BORDER_STRONG);
            UIManager.put("Panel.background", VcrtsTheme.SHELL);
            UIManager.put("TextField.background", VcrtsTheme.FIELD);
            UIManager.put("TextField.foreground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("ComboBox.background", VcrtsTheme.FIELD);
            UIManager.put("ComboBox.foreground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("TextArea.background", VcrtsTheme.LOG);
            UIManager.put("TextArea.foreground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("Label.foreground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("TabbedPane.selectedBackground", VcrtsTheme.SURFACE_ELEVATED);
            UIManager.put("TabbedPane.hoverColor", VcrtsTheme.withAlpha(VcrtsTheme.ACCENT, 50));
            UIManager.put("TabbedPane.underlineColor", VcrtsTheme.ACCENT);
            UIManager.put("TabbedPane.inactiveUnderlineColor", VcrtsTheme.BORDER);
            UIManager.put("TabbedPane.foreground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("TabbedPane.background", VcrtsTheme.SURFACE);
            UIManager.put("TabbedPane.contentAreaColor", VcrtsTheme.SURFACE);
            UIManager.put("Table.background", VcrtsTheme.SURFACE_ELEVATED);
            UIManager.put("Table.foreground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", VcrtsTheme.ACCENT_GHOST);
            UIManager.put("Table.selectionForeground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("Table.gridColor", VcrtsTheme.BORDER);
            UIManager.put("TableHeader.background", VcrtsTheme.SURFACE);
            UIManager.put("TableHeader.foreground", VcrtsTheme.TEXT_SECONDARY);
            UIManager.put("Separator.foreground", VcrtsTheme.BORDER);
            UIManager.put("OptionPane.background", VcrtsTheme.SHELL);
            UIManager.put("OptionPane.messageForeground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("OptionPane.foreground", VcrtsTheme.TEXT_PRIMARY);
            UIManager.put("PopupMenu.background", VcrtsTheme.SURFACE_ELEVATED);
            UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(VcrtsTheme.BORDER));
            UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        } catch (Throwable ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Throwable ignoredAgain) {
                // Keep the default look and feel as the last fallback.
            }
        }
    }
}
