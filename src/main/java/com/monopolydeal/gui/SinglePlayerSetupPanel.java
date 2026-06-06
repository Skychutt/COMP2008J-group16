package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Single-player lobby: one human name plus AI opponent count.
 */
public class SinglePlayerSetupPanel extends VBox {

    public interface SetupListener {
        void onBack();
        void onStart(String playerName, int aiOpponents);
    }

    private static final String[] AI_COUNT_OPTIONS = {
            "1 AI Opponent", "2 AI Opponents", "3 AI Opponents", "4 AI Opponents"
    };

    private final SetupListener listener;
    private final TextField nameField;
    private final ComboBox<String> aiCountCombo;

    public SinglePlayerSetupPanel(SetupListener listener) {
        this.listener = listener;

        setStyle(
            "-fx-background-color: rgba(255,255,255,0.863);" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px;"
        );
        setPadding(new Insets(24, 40, 24, 40));
        setSpacing(0);
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Single Player vs AI");
        title.setFont(UITheme.FONT_MENU_SUBTITLE);
        title.setTextFill(Color.BLACK);
        getChildren().add(title);
        getChildren().add(spacer(16));

        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER);
        Label nameLabel = new Label("Your name:");
        nameLabel.setFont(UITheme.FONT_BODY);
        nameLabel.setTextFill(Color.BLACK);
        nameField = new TextField("Player 1");
        nameField.setFont(UITheme.FONT_BODY);
        nameField.setPrefWidth(220);
        nameRow.getChildren().addAll(nameLabel, nameField);
        getChildren().add(nameRow);
        getChildren().add(spacer(14));

        HBox aiRow = new HBox(12);
        aiRow.setAlignment(Pos.CENTER);
        Label aiLabel = new Label("AI opponents:");
        aiLabel.setFont(UITheme.FONT_BODY);
        aiLabel.setTextFill(Color.BLACK);
        aiCountCombo = new ComboBox<>();
        aiCountCombo.getItems().addAll(AI_COUNT_OPTIONS);
        aiCountCombo.getSelectionModel().select(0);
        aiCountCombo.setPrefWidth(220);
        aiRow.getChildren().addAll(aiLabel, aiCountCombo);
        getChildren().add(aiRow);
        getChildren().add(spacer(20));

        Button back = new Button("Back");
        Button start = new Button("Start Game");
        UITheme.styleMenuButton(back);
        UITheme.styleMenuButton(start);
        back.setOnAction(e -> listener.onBack());
        start.setOnAction(e -> onStartClicked());

        HBox actions = new HBox(16, back, start);
        actions.setAlignment(Pos.CENTER);
        getChildren().add(actions);
    }

    public void resetToDefaults() {
        nameField.setText("Player 1");
        aiCountCombo.getSelectionModel().select(0);
    }

    private void onStartClicked() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            name = "Player 1";
        }
        int aiCount = aiCountCombo.getSelectionModel().getSelectedIndex() + 1;
        listener.onStart(name.trim(), aiCount);
    }

    private static javafx.scene.Node spacer(double height) {
        javafx.scene.layout.Region region = new javafx.scene.layout.Region();
        region.setPrefHeight(height);
        region.setMinHeight(height);
        return region;
    }
}
