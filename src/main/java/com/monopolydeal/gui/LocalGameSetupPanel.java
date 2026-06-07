package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    private final VBox namesBox;
    private final List<TextField> nameFields = new ArrayList<>();

    public LocalGameSetupPanel(SetupListener listener) {
        this.listener = listener;

        UITheme.applySetupPanelRoot(this);
        setPadding(new Insets(24, 32, 24, 32));
        setSpacing(14);
        setAlignment(Pos.CENTER);
        setMaxWidth(560);
        setFillWidth(false);

        getChildren().add(UITheme.createSetupTitleRow("Local Game Setup"));

        HBox countRow = new HBox(14);
        countRow.setAlignment(Pos.CENTER);
        Label countLabel = new Label("Number of players:");
        UITheme.styleSetupLabel(countLabel);
        playerCountCombo = new ComboBox<>();
        playerCountCombo.getItems().addAll(PLAYER_COUNT_OPTIONS);
        UITheme.styleSetupCombo(playerCountCombo);
        playerCountCombo.setPrefWidth(180);
        playerCountCombo.getSelectionModel().select(1);
        countRow.getChildren().addAll(countLabel, playerCountCombo);
        getChildren().add(countRow);

        HBox namesHeading = new HBox();
        namesHeading.setAlignment(Pos.CENTER);
        Label namesLabel = new Label("Player names:");
        UITheme.styleSetupLabel(namesLabel);
        namesHeading.getChildren().add(namesLabel);
        getChildren().add(namesHeading);

        namesBox = new VBox(10);
        namesBox.setAlignment(Pos.CENTER);
        getChildren().add(namesBox);

        Button back = new Button("Back");
        Button start = new Button("Start Game");
        UITheme.styleMenuButton(back);
        UITheme.styleMenuButton(start);
        back.setPrefWidth(150);
        start.setPrefWidth(150);
        back.setOnAction(e -> listener.onBack());
        start.setOnAction(e -> onStartClicked());

        HBox actions = new HBox(18, back, start);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8, 0, 0, 0));
        getChildren().add(actions);

        playerCountCombo.setOnAction(e -> rebuildNameFields());
        rebuildNameFields();
    }

    public void resetToDefaults() {
        playerCountCombo.getSelectionModel().select(1);
        rebuildNameFields();
    }

    private int getSelectedPlayerCount() {
        return playerCountCombo.getSelectionModel().getSelectedIndex() + 2;
    }

    private void rebuildNameFields() {
        int count = getSelectedPlayerCount();
        namesBox.getChildren().clear();
        nameFields.clear();

        for (int i = 0; i < count; i++) {
            Label lbl = new Label("Player " + (i + 1) + ":");
            UITheme.styleSetupLabel(lbl);
            lbl.setMinWidth(88);

            TextField field = new TextField("Player " + (i + 1));
            UITheme.styleSetupField(field);
            field.setPrefWidth(220);
            nameFields.add(field);

            HBox row = new HBox(12, lbl, field);
            row.setAlignment(Pos.CENTER);
            namesBox.getChildren().add(row);
        }
    }

    private void onStartClicked() {
        int count = getSelectedPlayerCount();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nameFields.size(); i++) {
            String text = nameFields.get(i).getText();
            if (text == null || text.trim().isEmpty()) {
                text = "Player " + (i + 1);
            }
            names.add(text.trim());
        }
        listener.onStart(count, names);
    }
}
