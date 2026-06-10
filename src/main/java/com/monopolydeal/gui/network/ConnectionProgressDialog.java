package com.monopolydeal.gui.network;

import com.monopolydeal.gui.theme.UITheme;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * A modal dialog that shows a progress bar while connecting to a LAN game server.
 * Provides visual feedback so the user knows the connection attempt is in progress.
 */
public class ConnectionProgressDialog {

    private final Stage dialog;
    private final Label lblStatus;
    private final ProgressBar progressBar;
    private final Timeline progressAnimation;
    private volatile boolean cancelled = false;
    private Runnable onCancel;

    public ConnectionProgressDialog(Stage owner) {
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Connecting...");
        dialog.setResizable(false);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(32, 48, 28, 48));
        content.setStyle(
                "-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";" +
                "-fx-border-color: " + UITheme.toCssHex(UITheme.ACCENT) + ";" +
                "-fx-border-width: 3px;" +
                "-fx-border-radius: 12px;" +
                "-fx-background-radius: 12px;"
        );

        Label title = new Label("Connecting to Server");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.rgb(250, 241, 209));

        lblStatus = new Label("Establishing connection...");
        lblStatus.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        lblStatus.setTextFill(Color.rgb(210, 230, 200));
        lblStatus.setWrapText(true);
        lblStatus.setMaxWidth(280);
        lblStatus.setAlignment(Pos.CENTER);

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(280);
        progressBar.setPrefHeight(18);
        progressBar.setStyle(
                "-fx-accent: " + UITheme.toCssHex(UITheme.ACCENT) + ";"
        );

        Button btnCancel = new Button("Cancel");
        UITheme.styleMenuButton(btnCancel);
        btnCancel.setPrefWidth(120);
        btnCancel.setOnAction(e -> {
            cancelled = true;
            if (onCancel != null) {
                onCancel.run();
            }
            close();
        });

        content.getChildren().addAll(title, progressBar, lblStatus, btnCancel);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // Animate progress bar indeterminate-style with a smooth loop
        progressAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(progressBar.progressProperty(), 0.85)),
                new KeyFrame(Duration.seconds(2.5), new KeyValue(progressBar.progressProperty(), 0.95))
        );
        progressAnimation.setCycleCount(Timeline.INDEFINITE);
        progressAnimation.setAutoReverse(true);

        dialog.setOnCloseRequest(e -> {
            e.consume();
            cancelled = true;
            if (onCancel != null) {
                onCancel.run();
            }
            close();
        });
    }

    public void show() {
        dialog.show();
        dialog.centerOnScreen();
        progressAnimation.play();
    }

    public void close() {
        progressAnimation.stop();
        dialog.close();
    }

    public void setStatus(String message) {
        Platform.runLater(() -> lblStatus.setText(message));
    }

    public void setProgress(double value) {
        Platform.runLater(() -> {
            progressAnimation.stop();
            progressBar.setProgress(value);
        });
    }

    public void setOnCancel(Runnable handler) {
        this.onCancel = handler;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Stage getDialog() {
        return dialog;
    }
}
