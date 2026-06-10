package com.monopolydeal.gui.menu;

import com.monopolydeal.gui.theme.UITheme;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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

        UITheme.applySetupPanelRoot(this);
        setPadding(new Insets(24, 32, 24, 32));
        setSpacing(14);
        setAlignment(Pos.CENTER);
        setMaxWidth(560);
        setFillWidth(false);

        getChildren().add(UITheme.createSetupTitleRow("Single Player vs AI"));

        HBox nameRow = new HBox(14);
        nameRow.setAlignment(Pos.CENTER);
        Label nameLabel = new Label("Your name:");
        UITheme.styleSetupLabel(nameLabel);
        nameField = new TextField("Player 1");
        UITheme.styleSetupField(nameField);
        nameField.setPrefWidth(220);
        nameRow.getChildren().addAll(nameLabel, nameField);
        getChildren().add(nameRow);

        HBox aiRow = new HBox(14);
        aiRow.setAlignment(Pos.CENTER);
        Label aiLabel = new Label("AI opponents:");
        UITheme.styleSetupLabel(aiLabel);
        aiCountCombo = new ComboBox<>();
        aiCountCombo.getItems().addAll(AI_COUNT_OPTIONS);
        UITheme.styleSetupCombo(aiCountCombo);
        aiCountCombo.getSelectionModel().select(0);
        aiCountCombo.setPrefWidth(220);
        aiRow.getChildren().addAll(aiLabel, aiCountCombo);
        getChildren().add(aiRow);

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
}
