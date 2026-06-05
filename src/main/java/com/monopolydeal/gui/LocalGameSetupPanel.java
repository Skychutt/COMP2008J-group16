package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Local game lobby: player-count dropdown + name fields.
 */
public class LocalGameSetupPanel extends VBox {

    public interface SetupListener {
        void onBack();
        void onStart(int playerCount, List<String> playerNames);
    }

    private static final String[] PLAYER_COUNT_OPTIONS = {
            "2 Players", "3 Players", "4 Players", "5 Players"
    };

    private final SetupListener listener;
    private final ComboBox<String> playerCountCombo;
    private final GridPane namesGrid;
    private final List<TextField> nameFields = new ArrayList<>();

    public LocalGameSetupPanel(SetupListener listener) {
        this.listener = listener;

        setStyle(
            "-fx-background-color: rgba(255,255,255,0.863);" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px;"
        );
        setPadding(new Insets(24, 40, 24, 40));
        setSpacing(0);
        setAlignment(Pos.TOP_CENTER);

        // Title
        Label title = new Label("Local Game Setup");
        title.setFont(UITheme.FONT_MENU_SUBTITLE);
        title.setTextFill(Color.BLACK);
        getChildren().add(title);
        getChildren().add(spacer(16));

        // Player count row
        HBox countRow = new HBox(12);
        countRow.setAlignment(Pos.CENTER);
        Label countLabel = new Label("Number of players:");
        countLabel.setFont(UITheme.FONT_BODY);
        countLabel.setTextFill(Color.BLACK);
        playerCountCombo = new ComboBox<>();
        playerCountCombo.getItems().addAll(PLAYER_COUNT_OPTIONS);
        playerCountCombo.setStyle("-fx-font-size: 12px;");
        playerCountCombo.setPrefWidth(160);
        playerCountCombo.getSelectionModel().select(1); // default: 3 players
        countRow.getChildren().addAll(countLabel, playerCountCombo);
        getChildren().add(countRow);
        getChildren().add(spacer(14));

        // Names label
        Label namesLabel = new Label("Player names:");
        namesLabel.setFont(UITheme.FONT_BODY);
        namesLabel.setTextFill(Color.BLACK);
        getChildren().add(namesLabel);
        getChildren().add(spacer(8));

        // Names grid
        namesGrid = new GridPane();
        namesGrid.setHgap(8);
        namesGrid.setVgap(8);
        namesGrid.setAlignment(Pos.CENTER);
        getChildren().add(namesGrid);
        getChildren().add(spacer(20));

        // Action buttons
        Button back  = new Button("Back");
        Button start = new Button("Start Game");
        UITheme.styleMenuButton(back);
        UITheme.styleMenuButton(start);
        back.setOnAction(e -> listener.onBack());
        start.setOnAction(e -> onStartClicked());

        HBox actions = new HBox(16, back, start);
        actions.setAlignment(Pos.CENTER);
        getChildren().add(actions);

        // Rebuild name fields whenever the combo changes
        playerCountCombo.setOnAction(e -> rebuildNameFields());
        rebuildNameFields();
    }

    public void resetToDefaults() {
        playerCountCombo.getSelectionModel().select(1);
        rebuildNameFields();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private int getSelectedPlayerCount() {
        return playerCountCombo.getSelectionModel().getSelectedIndex() + 2;
    }

    private void rebuildNameFields() {
        int count = getSelectedPlayerCount();
        namesGrid.getChildren().clear();
        nameFields.clear();

        for (int i = 0; i < count; i++) {
            Label lbl = new Label("Player " + (i + 1) + ":");
            lbl.setFont(UITheme.FONT_BODY);
            lbl.setTextFill(Color.BLACK);
            lbl.setMinWidth(72);

            TextField field = new TextField("Player " + (i + 1));
            field.setFont(UITheme.FONT_BODY);
            field.setPrefWidth(200);
            nameFields.add(field);

            namesGrid.add(lbl,   0, i);
            namesGrid.add(field, 1, i);
        }
    }

    private void onStartClicked() {
        int count = getSelectedPlayerCount();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nameFields.size(); i++) {
            String text = nameFields.get(i).getText();
            if (text == null || text.trim().isEmpty()) text = "Player " + (i + 1);
            names.add(text.trim());
        }
        listener.onStart(count, names);
    }

    private static javafx.scene.Node spacer(double height) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setPrefHeight(height);
        r.setMinHeight(height);
        return r;
    }
}
