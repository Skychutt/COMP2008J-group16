package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Home screen: background image + menu/setup overlay using a {@link StackPane}.
 */
public class MainMenuPanel extends StackPane {

    private final VBox menuOverlay;
    private final LocalGameSetupPanel setupPanel;
    private final SinglePlayerSetupPanel singlePlayerSetupPanel;
    private LanSetupPanel lanSetupPanel;

    // Buttons stored for external wiring in MainMenuFrame
    private Button btnLocalGame;
    private Button btnSinglePlayer;
    private Button btnOnline;
    private Button btnRules;
    private Button btnExit;

    public MainMenuPanel(LocalGameSetupPanel.SetupListener setupListener,
                         SinglePlayerSetupPanel.SetupListener singlePlayerListener) {
        // Background image fills the pane using CSS cover
        BackgroundUtil.applyCoverBackground(this, BackgroundUtil.getMainBackground());
        if (BackgroundUtil.getMainBackground() == null) {
            setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");
        }

        setupPanel = new LocalGameSetupPanel(setupListener);
        setupPanel.setVisible(false);
        setupPanel.setManaged(false);

        singlePlayerSetupPanel = new SinglePlayerSetupPanel(singlePlayerListener);
        singlePlayerSetupPanel.setVisible(false);
        singlePlayerSetupPanel.setManaged(false);

        menuOverlay = buildMenuOverlay();
        VBox topLeftMusic = buildTopLeftMusicControl();

        StackPane.setAlignment(topLeftMusic, Pos.TOP_LEFT);
        StackPane.setAlignment(menuOverlay, Pos.CENTER);
        StackPane.setAlignment(setupPanel, Pos.CENTER);
        StackPane.setAlignment(singlePlayerSetupPanel, Pos.CENTER);
        // Music control first (bottom layer) so it cannot block menu buttons.
        getChildren().addAll(topLeftMusic, menuOverlay, setupPanel, singlePlayerSetupPanel);

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
        singlePlayerSetupPanel.setVisible(false);
        singlePlayerSetupPanel.setManaged(false);
        if (lanSetupPanel != null) {
            lanSetupPanel.setVisible(false);
            lanSetupPanel.setManaged(false);
        }
    }

    public void showLocalSetup() {
        setupPanel.resetToDefaults();
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        setupPanel.setVisible(true);
        setupPanel.setManaged(true);
        singlePlayerSetupPanel.setVisible(false);
        singlePlayerSetupPanel.setManaged(false);
        if (lanSetupPanel != null) {
            lanSetupPanel.setVisible(false);
            lanSetupPanel.setManaged(false);
        }
    }

    public void showSinglePlayerSetup() {
        singlePlayerSetupPanel.resetToDefaults();
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        setupPanel.setVisible(false);
        setupPanel.setManaged(false);
        singlePlayerSetupPanel.setVisible(true);
        singlePlayerSetupPanel.setManaged(true);
        if (lanSetupPanel != null) {
            lanSetupPanel.setVisible(false);
            lanSetupPanel.setManaged(false);
        }
    }

    /**
     * Display LAN online settings interface
     *
     */
    public void showLanSetup(LanSetupPanel.LanSetupListener listener) {
        // Lazily create LanSetupPanel the first time it's needed
        if (lanSetupPanel == null) {
            lanSetupPanel = new LanSetupPanel(listener);
            StackPane.setAlignment(lanSetupPanel, Pos.CENTER);
            getChildren().add(lanSetupPanel);
        } else {
            // Update listener in case it changed
            lanSetupPanel = new LanSetupPanel(listener);
            // Remove old, add new
            getChildren().removeIf(n -> n instanceof LanSetupPanel);
            StackPane.setAlignment(lanSetupPanel, Pos.CENTER);
            getChildren().add(lanSetupPanel);
        }
        lanSetupPanel.resetToDefaults();
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        setupPanel.setVisible(false);
        setupPanel.setManaged(false);
        singlePlayerSetupPanel.setVisible(false);
        singlePlayerSetupPanel.setManaged(false);
        lanSetupPanel.setVisible(true);
        lanSetupPanel.setManaged(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button access (wired in MainMenuFrame)
    // ─────────────────────────────────────────────────────────────────────────

    public Button getButtonLocalGame()    { return btnLocalGame; }
    public Button getButtonSinglePlayer() { return btnSinglePlayer; }
    public Button getButtonOnline()       { return btnOnline; }
    public Button getButtonRules()        { return btnRules; }
    public Button getButtonExit()         { return btnExit; }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu overlay
    // ─────────────────────────────────────────────────────────────────────────

    private static final Font FONT_MENU_TITLE_LARGE =
            Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 52);
    private static final Font FONT_MENU_SUBTITLE_LARGE =
            Font.font("Segoe UI", FontWeight.BOLD, 22);

    private VBox buildMenuOverlay() {
        Text titleTop = createMenuTitleText("MONOPOLY");
        Text titleBottom = createMenuTitleText("DEAL");

        VBox titleBlock = new VBox(-6, titleTop, titleBottom);
        titleBlock.setAlignment(Pos.CENTER);

        Text subtitle = createMenuSubtitleText("Card Game");

        btnLocalGame    = createMenuButton("Local Game", 168);
        btnSinglePlayer = createMenuButton("Single Player", 168);
        btnOnline       = createMenuButton("Online Multiplayer", 196);
        btnRules        = createMenuButton("Game Rules", 168);
        btnExit         = createMenuButton("Exit Game", 168);

        HBox buttonRowTop = new HBox(14, btnLocalGame, btnSinglePlayer, btnOnline);
        buttonRowTop.setAlignment(Pos.CENTER);
        HBox buttonRowBottom = new HBox(14, btnRules, btnExit);
        buttonRowBottom.setAlignment(Pos.CENTER);

        VBox buttonBlock = new VBox(12, buttonRowTop, buttonRowBottom);
        buttonBlock.setAlignment(Pos.CENTER);

        VBox overlay = new VBox(10, titleBlock, subtitle, spacer(20), buttonBlock);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(0, 24, 48, 24));
        overlay.setStyle("-fx-background-color: transparent;");
        return overlay;
    }

    private static Text createMenuTitleText(String value) {
        Text text = new Text(value);
        text.setFont(FONT_MENU_TITLE_LARGE);
        text.setFill(Color.web("#FFD86B"));
        text.setStroke(Color.web("#2A1608"));
        text.setStrokeWidth(2.8);
        text.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        DropShadow shadow = new DropShadow();
        shadow.setRadius(14);
        shadow.setOffsetX(0);
        shadow.setOffsetY(3);
        shadow.setColor(Color.rgb(0, 0, 0, 0.82));
        text.setEffect(shadow);
        return text;
    }

    private static Text createMenuSubtitleText(String value) {
        Text text = new Text(value);
        text.setFont(FONT_MENU_SUBTITLE_LARGE);
        text.setFill(Color.web("#FFF1C8"));
        text.setStroke(Color.web("#2A1608"));
        text.setStrokeWidth(1.6);
        text.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        DropShadow shadow = new DropShadow();
        shadow.setRadius(8);
        shadow.setOffsetY(2);
        shadow.setColor(Color.rgb(0, 0, 0, 0.75));
        text.setEffect(shadow);
        return text;
    }

    private static Button createMenuButton(String text, double width) {
        Button btn = new Button(text);
        btn.setMinWidth(width);
        btn.setPrefWidth(width);
        btn.setMaxWidth(width);
        UITheme.styleMenuButton(btn);
        return btn;
    }

    private static VBox buildTopLeftMusicControl() {
        VBox box = new VBox(new GameVolumeControl());
        box.setPadding(new Insets(18, 0, 0, 18));
        box.setAlignment(Pos.TOP_LEFT);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPickOnBounds(false);
        return box;
    }

    private static javafx.scene.Node spacer(double height) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setPrefHeight(height);
        return r;
    }
}
