package app;

import java.awt.Color;
import java.awt.Font;

public final class VcrtsTheme {
    public static final Color CANVAS = new Color(15, 18, 23);
    public static final Color SIDEBAR = new Color(15, 18, 23);
    public static final Color SHELL = new Color(19, 23, 29);
    public static final Color SURFACE = new Color(24, 29, 36);
    public static final Color SURFACE_ELEVATED = new Color(29, 34, 42);
    public static final Color FIELD = new Color(18, 22, 28);
    public static final Color LOG = new Color(11, 14, 18);
    public static final Color BORDER = new Color(47, 56, 67);
    public static final Color BORDER_STRONG = new Color(71, 84, 98);
    public static final Color ACCENT = new Color(110, 143, 168);
    public static final Color ACCENT_ACTIVE = new Color(135, 170, 196);
    public static final Color ACCENT_GHOST = new Color(35, 40, 48);
    public static final Color SUCCESS = new Color(92, 141, 103);
    public static final Color WARNING = new Color(177, 138, 84);
    public static final Color DANGER = new Color(167, 91, 95);
    public static final Color TEXT_PRIMARY = new Color(228, 233, 239);
    public static final Color TEXT_SECONDARY = new Color(172, 182, 192);
    public static final Color TEXT_MUTED = new Color(131, 142, 153);

    public static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 22);
    public static final Font SECTION_FONT = new Font("Dialog", Font.BOLD, 15);
    public static final Font BODY_FONT = new Font("Dialog", Font.PLAIN, 12);
    public static final Font LABEL_FONT = new Font("Dialog", Font.BOLD, 12);
    public static final Font META_FONT = new Font("Dialog", Font.PLAIN, 11);
    public static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);

    private VcrtsTheme() {
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
