package com.monopolydeal.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;

/**
 * Small UI theme helper to keep styling consistent and low-coupling.
 */
public final class UITheme {

    public static final Color PAGE_BG = new Color(28, 68, 44);
    public static final Color TABLE_FELT_TOP = new Color(49, 120, 74);
    public static final Color TABLE_FELT_BOTTOM = new Color(20, 74, 46);
    public static final Color WOOD_OUTER = new Color(78, 49, 28);
    public static final Color WOOD_INNER = new Color(118, 78, 49);
    public static final Color PANEL_BG = new Color(255, 255, 255, 236);
    public static final Color PANEL_SOFT_BG = new Color(247, 252, 246, 236);
    public static final Color ACCENT = new Color(238, 190, 82);
    public static final Color ACCENT_DARK = new Color(186, 131, 33);
    public static final Color BORDER = new Color(177, 150, 97);
    public static final Color TEXT_MAIN = new Color(45, 37, 18);
    public static final Color TEXT_SUB = new Color(87, 72, 45);
    public static final Color LOG_BG = new Color(20, 28, 36);
    public static final Color LOG_TEXT = new Color(171, 241, 188);
    public static final Color DROP_ZONE = new Color(255, 247, 218);
    public static final Color DROP_ZONE_BORDER = new Color(181, 142, 58);
    public static final Color DROP_ZONE_ACTIVE = new Color(255, 239, 186);
    public static final Color BANK_ZONE = new Color(235, 250, 223);
    public static final Color BANK_ZONE_BORDER = new Color(96, 150, 80);
    public static final Color BANK_ZONE_ACTIVE = new Color(214, 245, 195);

    public static final Font FONT_MENU_TITLE = new Font("Segoe UI", Font.BOLD, 36);
    public static final Font FONT_MENU_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 16);
    public static final Font FONT_MENU_BUTTON = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BANK_TOTAL = new Font("Segoe UI", Font.BOLD, 22);

    private UITheme() {
    }

    public static Border createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                title
        );
    }

    public static void styleCardPanel(JComponent component, String title) {
        component.setBackground(PANEL_BG);
        component.setBorder(createSectionBorder(title));
    }

    public static void styleSoftPanel(JComponent component, String title) {
        component.setBackground(PANEL_SOFT_BG);
        component.setBorder(createSectionBorder(title));
    }

    public static JScrollPane styleScrollPane(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.getViewport().setBackground(PANEL_BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        return scroll;
    }

    public static void stylePrimaryButton(JButton button) {
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(FONT_SUBTITLE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    public static void styleSecondaryButton(JButton button) {
        button.setBackground(PANEL_SOFT_BG);
        button.setForeground(ACCENT_DARK);
        button.setFocusPainted(false);
        button.setFont(FONT_SUBTITLE);
        button.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
    }

    public static void styleMenuButton(JButton button) {
        button.setBackground(new Color(255, 255, 255, 240));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setFont(FONT_MENU_BUTTON);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 2, true),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));
    }

    public static void styleLogArea(JTextArea logArea) {
        logArea.setFont(FONT_BODY);
        logArea.setBackground(LOG_BG);
        logArea.setForeground(LOG_TEXT);
        logArea.setCaretColor(LOG_TEXT);
    }
}
