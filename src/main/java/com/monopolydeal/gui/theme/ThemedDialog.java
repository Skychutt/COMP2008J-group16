package com.monopolydeal.gui.theme;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.List;

/**
 * Unified themed dialogs for the Monopoly Deal UI (messages, confirm, choice).
 */
public final class ThemedDialog {

    private static final int BUTTON_CHOICE_LIMIT = 4;

    public enum Kind {
        INFO(UITheme.ACCENT),
        WARNING(Color.rgb(230, 145, 45)),
        ERROR(Color.rgb(196, 62, 52));

        private final Color accent;

        Kind(Color accent) {
            this.accent = accent;
        }
    }

    private ThemedDialog() {
    }

    public static void showInfo(Window owner, String title, String message) {
        showMessage(owner, Kind.INFO, title, message);
    }

    public static void showWarning(Window owner, String title, String message) {
        showMessage(owner, Kind.WARNING, title, message);
    }

    public static void showError(Window owner, String title, String message) {
        showMessage(owner, Kind.ERROR, title, message);
    }

    public static void showMessage(Window owner, Kind kind, String title, String message) {
        Stage dialog = createStage(owner, title);

        Button okBtn = new Button("OK");
        UITheme.styleDialogConfirmButton(okBtn);
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(okBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = buildCard(kind.accent, title, message, null, actions);
        showCard(dialog, card, owner);
    }

    public static boolean showConfirm(Window owner,
                                      String title,
                                      String message,
                                      String confirmLabel,
                                      String cancelLabel) {
        Stage dialog = createStage(owner, title);
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

        VBox card = buildCard(UITheme.ACCENT, title, message, null, actions);
        showCard(dialog, card, owner);
        return confirmed[0];
    }

    /**
     * @return selected option index, or -1 if cancelled / closed
     */
    public static int showChoice(Window owner,
                                 String title,
                                 String prompt,
                                 List<String> options,
                                 boolean allowCancel) {
        if (options == null || options.isEmpty()) {
            return -1;
        }
        if (options.size() <= BUTTON_CHOICE_LIMIT) {
            return showButtonChoice(owner, title, prompt, options, allowCancel);
        }
        return showComboChoice(owner, title, prompt, options, allowCancel);
    }

    private static int showButtonChoice(Window owner,
                                        String title,
                                        String prompt,
                                        List<String> options,
                                        boolean allowCancel) {
        Stage dialog = createStage(owner, title);
        final int[] selectedIndex = {-1};

        VBox optionButtons = new VBox(10);
        optionButtons.setFillWidth(true);
        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            Button optionBtn = new Button(options.get(i));
            UITheme.styleDialogOptionButton(optionBtn);
            optionBtn.setMaxWidth(Double.MAX_VALUE);
            optionBtn.setOnAction(e -> {
                selectedIndex[0] = index;
                dialog.close();
            });
            optionButtons.getChildren().add(optionBtn);
        }

        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (allowCancel) {
            Button cancelBtn = new Button("Cancel");
            UITheme.styleDialogCancelButton(cancelBtn);
            cancelBtn.setOnAction(e -> dialog.close());
            actions.getChildren().add(cancelBtn);
        }

        VBox card = buildCard(UITheme.ACCENT, title, prompt, optionButtons,
                actions.getChildren().isEmpty() ? null : actions);
        showCard(dialog, card, owner);
        return selectedIndex[0];
    }

    private static int showComboChoice(Window owner,
                                       String title,
                                       String prompt,
                                       List<String> options,
                                       boolean allowCancel) {
        Stage dialog = createStage(owner, title);
        final int[] selectedIndex = {-1};

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(options);
        combo.getSelectionModel().select(0);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setStyle(
                "-fx-background-color: rgba(42,22,8,0.78);" +
                "-fx-text-fill: #FFF8E7;" +
                "-fx-border-color: " + UITheme.toCssHex(UITheme.ACCENT_DARK) + ";" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-font-size: 13px; -fx-font-weight: bold;"
        );

        Button cancelBtn = null;
        if (allowCancel) {
            cancelBtn = new Button("Cancel");
            UITheme.styleDialogCancelButton(cancelBtn);
            cancelBtn.setOnAction(e -> dialog.close());
        }

        Button selectBtn = new Button("Select");
        UITheme.styleDialogConfirmButton(selectBtn);
        selectBtn.setDefaultButton(true);
        selectBtn.setOnAction(e -> {
            String chosen = combo.getSelectionModel().getSelectedItem();
            if (chosen != null) {
                selectedIndex[0] = options.indexOf(chosen);
            }
            dialog.close();
        });

        HBox actions = allowCancel
                ? new HBox(12, cancelBtn, selectBtn)
                : new HBox(selectBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = buildCard(UITheme.ACCENT, title, prompt, combo, actions);
        showCard(dialog, card, owner);
        return selectedIndex[0];
    }

    private static Stage createStage(Window owner, String title) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);
        dialog.setResizable(false);
        return dialog;
    }

    private static VBox buildCard(Color accentColor,
                                  String title,
                                  String message,
                                  javafx.scene.Node bodyContent,
                                  HBox actions) {
        Region accentBar = new Region();
        accentBar.setPrefHeight(4);
        accentBar.setMaxHeight(4);
        accentBar.setStyle(
                "-fx-background-color: linear-gradient(to right, "
                        + UITheme.toCssHex(UITheme.ACCENT_DARK) + ", "
                        + UITheme.toCssHex(accentColor) + ", "
                        + UITheme.toCssHex(UITheme.ACCENT_DARK) + ");"
                        + "-fx-background-radius: 2px;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setTextFill(UITheme.TEXT_MAIN);

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.setFont(UITheme.FONT_BODY);
        messageLabel.setTextFill(UITheme.TEXT_SUB);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(10);
        if (message != null && message.length() > 220 && bodyContent == null) {
            ScrollPane scroll = new ScrollPane(messageLabel);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportHeight(180);
            scroll.setMaxHeight(180);
            scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            body.getChildren().add(scroll);
        } else {
            body.getChildren().add(messageLabel);
        }
        if (bodyContent != null) {
            body.getChildren().add(bodyContent);
        }

        VBox card = new VBox(12, accentBar, titleLabel, body);
        if (actions != null && !actions.getChildren().isEmpty()) {
            card.getChildren().add(actions);
        }
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(22, 26, 20, 26));
        card.setMaxWidth(460);
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
        return card;
    }

    private static void showCard(Stage dialog, VBox card, Window owner) {
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: transparent;");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.setOnShown(e -> centerOnOwner(dialog, owner));

        dialog.setAlwaysOnTop(true);
        dialog.showAndWait();
        dialog.setAlwaysOnTop(false);
        dialog.setOnShown(null);
    }

    private static void centerOnOwner(Stage dialog, Window owner) {
        double ownerX;
        double ownerY;
        double ownerW;
        double ownerH;

        if (owner instanceof Stage && ((Stage) owner).isShowing()) {
            Stage ownerStage = (Stage) owner;
            ownerX = ownerStage.getX();
            ownerY = ownerStage.getY();
            ownerW = ownerStage.getWidth();
            ownerH = ownerStage.getHeight();
        } else {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            ownerX = bounds.getMinX();
            ownerY = bounds.getMinY();
            ownerW = bounds.getWidth();
            ownerH = bounds.getHeight();
        }

        double dialogW = dialog.getWidth() > 0 ? dialog.getWidth() : dialog.getScene().getWidth();
        double dialogH = dialog.getHeight() > 0 ? dialog.getHeight() : dialog.getScene().getHeight();

        dialog.setX(ownerX + (ownerW - dialogW) / 2.0);
        dialog.setY(ownerY + (ownerH - dialogH) / 2.0);
    }
}
