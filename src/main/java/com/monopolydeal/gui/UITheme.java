package com.monopolydeal.gui;

import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

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

    private static final String EXIT_BTN_NORMAL =
            "-fx-background-color: rgba(255,252,240,0.94);" +
            "-fx-text-fill: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-color: " + toCssHex(BORDER) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 8 18 8 18;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;";

    private static final String EXIT_BTN_HOVER =
            "-fx-background-color: " + toCssHex(ACCENT) + ";" +
            "-fx-text-fill: " + toCssHex(TEXT_MAIN) + ";" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 8 18 8 18;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;";

    /** In-game Exit button with hover brighten + slight scale-up. */
    public static void styleExitButton(Button btn) {
        btn.setFont(FONT_SUBTITLE);

        DropShadow glow = new DropShadow();
        glow.setRadius(12);
        glow.setSpread(0.08);
        glow.setColor(Color.rgb(255, 220, 120, 0.55));

        ScaleTransition grow = new ScaleTransition(Duration.millis(140), btn);
        grow.setToX(1.1);
        grow.setToY(1.1);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(140), btn);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        btn.setStyle(EXIT_BTN_NORMAL);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(EXIT_BTN_HOVER);
            btn.setEffect(glow);
            grow.playFromStart();
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(EXIT_BTN_NORMAL);
            btn.setEffect(null);
            shrink.playFromStart();
        });
    }

    private static final String DIALOG_CONFIRM_NORMAL =
            "-fx-background-color: " + toCssHex(ACCENT) + ";" +
            "-fx-text-fill: " + toCssHex(TEXT_MAIN) + ";" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 9 22 9 22;" +
            "-fx-cursor: hand;";

    private static final String DIALOG_CONFIRM_HOVER =
            "-fx-background-color: #ffd66e;" +
            "-fx-text-fill: " + toCssHex(TEXT_MAIN) + ";" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 9 22 9 22;" +
            "-fx-cursor: hand;";

    private static final String DIALOG_CANCEL_NORMAL =
            "-fx-background-color: " + toCssRgba(PANEL_SOFT_BG) + ";" +
            "-fx-text-fill: " + toCssHex(TEXT_SUB) + ";" +
            "-fx-border-color: " + toCssHex(BORDER) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 9 22 9 22;" +
            "-fx-cursor: hand;";

    private static final String DIALOG_CANCEL_HOVER =
            "-fx-background-color: rgba(255,255,255,0.98);" +
            "-fx-text-fill: " + toCssHex(TEXT_SUB) + ";" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 9 22 9 22;" +
            "-fx-cursor: hand;";

    public static void styleDialogConfirmButton(Button btn) {
        btn.setFont(FONT_MENU_BUTTON);
        btn.setStyle(DIALOG_CONFIRM_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(DIALOG_CONFIRM_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(DIALOG_CONFIRM_NORMAL));
    }

    public static void styleDialogCancelButton(Button btn) {
        btn.setFont(FONT_MENU_BUTTON);
        btn.setStyle(DIALOG_CANCEL_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(DIALOG_CANCEL_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(DIALOG_CANCEL_NORMAL));
    }

    /** Equal-weight choice buttons in option dialogs (all options share one look). */
    public static void styleDialogOptionButton(Button btn) {
        btn.setFont(FONT_MENU_BUTTON);
        btn.setStyle(DIALOG_OPTION_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(DIALOG_OPTION_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(DIALOG_OPTION_NORMAL));
    }

    private static final String DIALOG_OPTION_NORMAL =
            "-fx-background-color: " + toCssRgba(PANEL_SOFT_BG) + ";" +
            "-fx-text-fill: " + toCssHex(TEXT_MAIN) + ";" +
            "-fx-border-color: " + toCssHex(BORDER) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 11 22 11 22;" +
            "-fx-cursor: hand;";

    private static final String DIALOG_OPTION_HOVER =
            "-fx-background-color: rgba(255,255,255,0.98);" +
            "-fx-text-fill: " + toCssHex(TEXT_MAIN) + ";" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 11 22 11 22;" +
            "-fx-cursor: hand;";

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

    // ─────────────── Setup screen styling (transparent overlay on menu bg) ───────────────

    private static final Font FONT_SETUP_TITLE =
            Font.font("Segoe UI", FontWeight.BOLD, 30);

    private static final Font FONT_SETUP_LABEL =
            Font.font("Segoe UI", FontWeight.BOLD, 14);

    private static final String SETUP_LABEL_PILL =
            "-fx-background-color: rgba(255,248,231,0.92);" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 4 10 4 10;";

    private static final String SETUP_ACCENT_PILL =
            "-fx-background-color: rgba(255,216,107,0.94);" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 4 12 4 12;";

    private static final String SETUP_FIELD_STYLE =
            "-fx-background-color: rgba(42,22,8,0.78);" +
            "-fx-text-fill: #FFF8E7;" +
            "-fx-prompt-text-fill: #E8D8A8;" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 8 12 8 12;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;";

    private static final String SETUP_COMBO_STYLE =
            "-fx-background-color: rgba(42,22,8,0.78);" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;";

    private static final String SETUP_STEPPER_NORMAL =
            "-fx-background-color: rgba(255,252,240,0.9);" +
            "-fx-text-fill: " + toCssHex(TEXT_MAIN) + ";" +
            "-fx-border-color: " + toCssHex(BORDER) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;";

    private static final String SETUP_STEPPER_HOVER =
            "-fx-background-color: " + toCssHex(ACCENT) + ";" +
            "-fx-text-fill: " + toCssHex(TEXT_MAIN) + ";" +
            "-fx-border-color: " + toCssHex(ACCENT_DARK) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;";

    public static void applySetupPanelRoot(javafx.scene.layout.Region panel) {
        panel.setStyle("-fx-background-color: transparent;");
    }

    public static javafx.scene.layout.HBox createSetupTitleRow(String value) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(createSetupTitleText(value));
        row.setAlignment(javafx.geometry.Pos.CENTER);
        return row;
    }

    public static Text createSetupTitleText(String value) {
        Text text = new Text(value);
        text.setFont(FONT_SETUP_TITLE);
        text.setFill(Color.web("#FFD86B"));
        text.setStroke(Color.web("#2A1608"));
        text.setStrokeWidth(1.8);
        text.setStrokeType(StrokeType.OUTSIDE);
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setOffsetY(2);
        shadow.setColor(Color.rgb(0, 0, 0, 0.78));
        text.setEffect(shadow);
        return text;
    }

    public static void styleSetupLabel(Labeled label) {
        label.setFont(FONT_SETUP_LABEL);
        label.setTextFill(Color.web("#2A1608"));
        label.setStyle(SETUP_LABEL_PILL);
    }

    public static void styleSetupAccentLabel(Labeled label) {
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        label.setTextFill(Color.web("#2A1608"));
        label.setStyle(SETUP_ACCENT_PILL);
    }

    public static void styleSetupField(TextField field) {
        field.setFont(FONT_BODY);
        field.setStyle(SETUP_FIELD_STYLE);
    }

    public static void styleSetupCombo(ComboBox<?> combo) {
        combo.setStyle(SETUP_COMBO_STYLE);
    }

    public static void styleSetupRadio(RadioButton radio) {
        radio.setFont(FONT_SETUP_LABEL);
        radio.setTextFill(Color.web("#2A1608"));
        radio.setStyle(
                SETUP_LABEL_PILL +
                "-fx-mark-color: " + toCssHex(ACCENT_DARK) + ";" +
                "-fx-font-size: 14px; -fx-font-weight: bold;"
        );
    }

    public static void styleSetupStepperButton(Button btn) {
        btn.setFont(FONT_SUBTITLE);
        btn.setStyle(SETUP_STEPPER_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(SETUP_STEPPER_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(SETUP_STEPPER_NORMAL));
    }
}
