package com.monopolydeal.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Compact music volume slider for the in-game top-left corner.
 */
public class GameVolumeControl extends VBox {

    private static final double CONTROL_WIDTH = 130;

    public GameVolumeControl() {
        setSpacing(6);
        setAlignment(Pos.CENTER);
        setFillWidth(false);
        setPrefWidth(CONTROL_WIDTH);
        setMaxSize(CONTROL_WIDTH, Region.USE_PREF_SIZE);
        setPickOnBounds(true);

        Label title = new Label("Music");
        UITheme.styleSetupLabel(title);
        HBox titleRow = centeredRow(title);

        Slider slider = new Slider(0, 100, BackgroundMusicManager.getInstance().getVolumePercent());
        slider.setPrefWidth(CONTROL_WIDTH);
        slider.setMinWidth(CONTROL_WIDTH);
        slider.setMaxWidth(CONTROL_WIDTH);
        slider.setBlockIncrement(5);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setStyle(
                "-fx-control-inner-background: rgba(42,22,8,0.78);" +
                "-fx-accent: " + UITheme.toCssHex(UITheme.ACCENT) + ";"
        );

        Label valueLabel = new Label(BackgroundMusicManager.getInstance().getVolumePercent() + "%");
        UITheme.styleSetupAccentLabel(valueLabel);
        valueLabel.setMinWidth(56);
        valueLabel.setAlignment(Pos.CENTER);
        HBox valueRow = centeredRow(valueLabel);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int percent = newVal.intValue();
            BackgroundMusicManager.getInstance().setVolume(percent / 100.0);
            valueLabel.setText(percent + "%");
        });

        getChildren().addAll(titleRow, slider, valueRow);
    }

    private static HBox centeredRow(javafx.scene.Node node) {
        HBox row = new HBox(node);
        row.setAlignment(Pos.CENTER);
        row.setPrefWidth(CONTROL_WIDTH);
        row.setMinWidth(CONTROL_WIDTH);
        row.setMaxWidth(CONTROL_WIDTH);
        return row;
    }
}
