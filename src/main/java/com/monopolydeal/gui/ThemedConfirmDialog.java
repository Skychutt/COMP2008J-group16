package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Themed confirmation dialog matching the Monopoly Deal UI palette.
 */
public final class ThemedConfirmDialog {

    private ThemedConfirmDialog() {
    }

    /**
     * @return true if the user confirmed, false if cancelled or closed
     */
    public static boolean show(Window owner,
                               String title,
                               String message,
                               String confirmLabel,
                               String cancelLabel) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);

        Label titleLabel = new Label(title);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setTextFill(UITheme.TEXT_MAIN);

        Label messageLabel = new Label(message);
        messageLabel.setFont(UITheme.FONT_BODY);
        messageLabel.setTextFill(UITheme.TEXT_SUB);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);
        messageLabel.setAlignment(Pos.CENTER_LEFT);

        Region accentBar = new Region();
        accentBar.setPrefHeight(4);
        accentBar.setMaxHeight(4);
        accentBar.setStyle(
                "-fx-background-color: linear-gradient(to right, "
                        + UITheme.toCssHex(UITheme.ACCENT_DARK) + ", "
                        + UITheme.toCssHex(UITheme.ACCENT) + ", "
                        + UITheme.toCssHex(UITheme.ACCENT_DARK) + ");"
                        + "-fx-background-radius: 2px;"
        );

        final boolean[] confirmed = {false};

        Button cancelBtn = new Button(cancelLabel);
        UITheme.styleDialogCancelButton(cancelBtn);
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button(confirmLabel);
        UITheme.styleDialogConfirmButton(confirmBtn);
        confirmBtn.setDefaultButton(true);
        confirmBtn.setOnAction(e -> {
            confirmed[0] = true;
            dialog.close();
        });

        HBox actions = new HBox(12, cancelBtn, confirmBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        VBox card = new VBox(12, accentBar, titleLabel, messageLabel, actions);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(22, 26, 20, 26));
        card.setMaxWidth(420);
        card.setStyle(
                "-fx-background-color: " + UITheme.toCssRgba(UITheme.PANEL_BG) + ";" +
                "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 12px;" +
                "-fx-background-radius: 12px;"
        );

        DropShadow shadow = new DropShadow();
        shadow.setRadius(22);
        shadow.setOffsetY(6);
        shadow.setColor(Color.rgb(0, 0, 0, 0.45));
        card.setEffect(shadow);

        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
        return confirmed[0];
    }
}
