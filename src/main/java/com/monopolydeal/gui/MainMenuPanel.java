package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Home screen: background image + menu/setup overlay using a {@link StackPane}.
 */
public class MainMenuPanel extends StackPane {

    private final VBox menuOverlay;
    private final LocalGameSetupPanel setupPanel;

    // Buttons stored for external wiring in MainMenuFrame
    private Button btnLocalGame;
    private Button btnSinglePlayer;
    private Button btnOnline;
    private Button btnRules;

    public MainMenuPanel(LocalGameSetupPanel.SetupListener setupListener) {
        // Background image fills the pane using CSS cover
        BackgroundUtil.applyCoverBackground(this, BackgroundUtil.getMainBackground());
        if (BackgroundUtil.getMainBackground() == null) {
            setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");
        }

        setupPanel = new LocalGameSetupPanel(setupListener);
        setupPanel.setVisible(false);
        setupPanel.setManaged(false);

        menuOverlay = buildMenuOverlay();

        StackPane.setAlignment(menuOverlay, Pos.CENTER);
        StackPane.setAlignment(setupPanel, Pos.CENTER);
        getChildren().addAll(menuOverlay, setupPanel);

        showMainMenu();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    public void showMainMenu() {
        menuOverlay.setVisible(true);
        menuOverlay.setManaged(true);
        setupPanel.setVisible(false);
        setupPanel.setManaged(false);
    }

    public void showLocalSetup() {
        setupPanel.resetToDefaults();
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        setupPanel.setVisible(true);
        setupPanel.setManaged(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button access (wired in MainMenuFrame)
    // ─────────────────────────────────────────────────────────────────────────

    public Button getButtonLocalGame()    { return btnLocalGame; }
    public Button getButtonSinglePlayer() { return btnSinglePlayer; }
    public Button getButtonOnline()       { return btnOnline; }
    public Button getButtonRules()        { return btnRules; }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu overlay
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildMenuOverlay() {
        Label title = new Label("MONOPOLY DEAL");
        title.setFont(UITheme.FONT_MENU_TITLE);
        title.setTextFill(Color.BLACK);

        Label subtitle = new Label("Card Game");
        subtitle.setFont(UITheme.FONT_MENU_SUBTITLE);
        subtitle.setTextFill(Color.BLACK);

        btnLocalGame    = createMenuButton("Local Game");
        btnSinglePlayer = createMenuButton("Single Player");
        btnOnline       = createMenuButton("Online Multiplayer");
        btnRules        = createMenuButton("Game Rules");

        VBox overlay = new VBox(12, title, subtitle,
                spacer(16), btnLocalGame, btnSinglePlayer, btnOnline, btnRules);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(28, 48, 28, 48));
        overlay.setStyle(
            "-fx-background-color: rgba(255,255,255,0.784);" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px;"
        );
        overlay.setMaxWidth(340);
        return overlay;
    }

    private static Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(280);
        btn.setMinWidth(280);
        UITheme.styleMenuButton(btn);
        return btn;
    }

    private static javafx.scene.Node spacer(double height) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setPrefHeight(height);
        return r;
    }
}
