package com.monopolydeal.gui;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * UI theme constants (colours + fonts) and CSS helper methods for JavaFX components.
 */
public final class UITheme {

    // ─────────────── Colours ───────────────
    public static final Color PAGE_BG           = Color.rgb(28, 68, 44);
    public static final Color TABLE_FELT_TOP    = Color.rgb(49, 120, 74);
    public static final Color TABLE_FELT_BOTTOM = Color.rgb(20, 74, 46);
    public static final Color WOOD_OUTER        = Color.rgb(78, 49, 28);
    public static final Color WOOD_INNER        = Color.rgb(118, 78, 49);
    public static final Color PANEL_BG          = Color.rgb(255, 255, 255, 236 / 255.0);
    public static final Color PANEL_SOFT_BG     = Color.rgb(247, 252, 246, 236 / 255.0);
    public static final Color ACCENT            = Color.rgb(238, 190, 82);
    public static final Color ACCENT_DARK       = Color.rgb(186, 131, 33);
    public static final Color BORDER            = Color.rgb(177, 150, 97);
    public static final Color TEXT_MAIN         = Color.rgb(45, 37, 18);
    public static final Color TEXT_SUB          = Color.rgb(87, 72, 45);
    public static final Color LOG_BG            = Color.rgb(20, 28, 36);
    public static final Color LOG_TEXT          = Color.rgb(171, 241, 188);
    public static final Color DROP_ZONE         = Color.rgb(255, 247, 218);
    public static final Color DROP_ZONE_BORDER  = Color.rgb(181, 142, 58);
    public static final Color DROP_ZONE_ACTIVE  = Color.rgb(255, 239, 186);
    public static final Color BANK_ZONE         = Color.rgb(235, 250, 223);
    public static final Color BANK_ZONE_BORDER  = Color.rgb(96, 150, 80);
    public static final Color BANK_ZONE_ACTIVE  = Color.rgb(214, 245, 195);

    // ─────────────── Fonts ───────────────
    public static final Font FONT_MENU_TITLE    = Font.font("Segoe UI", FontWeight.BOLD,   36);
    public static final Font FONT_MENU_SUBTITLE = Font.font("Segoe UI", FontWeight.NORMAL, 16);
    public static final Font FONT_MENU_BUTTON   = Font.font("Segoe UI", FontWeight.BOLD,   15);
    public static final Font FONT_TITLE         = Font.font("Segoe UI", FontWeight.BOLD,   16);
    public static final Font FONT_SUBTITLE      = Font.font("Segoe UI", FontWeight.BOLD,   13);
    public static final Font FONT_BODY          = Font.font("Segoe UI", FontWeight.NORMAL, 12);
    public static final Font FONT_SMALL         = Font.font("Segoe UI", FontWeight.NORMAL, 11);
    public static final Font FONT_BANK_TOTAL    = Font.font("Segoe UI", FontWeight.BOLD,   22);

    private UITheme() {}

    // ─────────────── CSS conversion helpers ───────────────

    /** Convert a JavaFX Color to a CSS rgba() string. */
    public static String toCssRgba(Color c) {
        return String.format("rgba(%d,%d,%d,%.4f)",
                (int) Math.round(c.getRed()   * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue()  * 255),
                c.getOpacity());
    }

    /** Convert a JavaFX Color to a 6-digit hex string (ignores alpha). */
    public static String toCssHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed()   * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue()  * 255));
    }

    // ─────────────── Button styling ───────────────

    private static final String MENU_BTN_NORMAL =
            "-fx-background-color: rgba(255,255,255,0.94);" +
            "-fx-text-fill: black;" +
            "-fx-border-color: #3c3c3c;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 4px;" +
            "-fx-background-radius: 4px;" +
            "-fx-padding: 10 24 10 24;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 15px; -fx-font-weight: bold;";

    private static final String MENU_BTN_HOVER =
            "-fx-background-color: rgba(238,190,82,0.85);" +
            "-fx-text-fill: black;" +
            "-fx-border-color: #3c3c3c;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 4px;" +
            "-fx-background-radius: 4px;" +
            "-fx-padding: 10 24 10 24;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 15px; -fx-font-weight: bold;";

    public static void styleMenuButton(Button btn) {
        btn.setFont(FONT_MENU_BUTTON);
        btn.setStyle(MENU_BTN_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(MENU_BTN_HOVER));
        btn.setOnMouseExited(e  -> btn.setStyle(MENU_BTN_NORMAL));
    }

    public static void stylePrimaryButton(Button btn) {
        btn.setFont(FONT_SUBTITLE);
        String style =
            "-fx-background-color: " + toCssHex(ACCENT) + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 4px;" +
            "-fx-padding: 8 14 8 14;" +
            "-fx-cursor: hand;";
        btn.setStyle(style);
    }

    public static void styleSecondaryButton(Button btn) {
        btn.setFont(FONT_SUBTITLE);
        btn.setStyle(
            "-fx-background-color: " + toCssRgba(PANEL_SOFT_BG) + ";" +
            "-fx-text-fill: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-color: " + toCssHex(BORDER) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 4px;" +
            "-fx-background-radius: 4px;" +
            "-fx-padding: 8 14 8 14;" +
            "-fx-cursor: hand;"
        );
    }

    public static void styleLogArea(TextArea logArea) {
        logArea.setFont(FONT_BODY);
        logArea.setStyle(
            "-fx-control-inner-background: " + toCssHex(LOG_BG) + ";" +
            "-fx-text-fill: " + toCssHex(LOG_TEXT) + ";" +
            "-fx-background-color: " + toCssHex(LOG_BG) + ";"
        );
    }

    /** CSS for a panel with a solid border. */
    public static String panelBorderStyle() {
        return "-fx-background-color: " + toCssRgba(PANEL_BG) + ";" +
               "-fx-border-color: " + toCssHex(BORDER) + ";" +
               "-fx-border-width: 1px;" +
               "-fx-border-radius: 4px;" +
               "-fx-background-radius: 4px;";
    }

    /** CSS for a softer panel (section containers). */
    public static String softPanelStyle() {
        return "-fx-background-color: " + toCssRgba(PANEL_SOFT_BG) + ";" +
               "-fx-border-color: " + toCssHex(BORDER) + ";" +
               "-fx-border-width: 1px;" +
               "-fx-border-radius: 4px;" +
               "-fx-background-radius: 4px;" +
               "-fx-padding: 4 6 4 6;";
    }
}
