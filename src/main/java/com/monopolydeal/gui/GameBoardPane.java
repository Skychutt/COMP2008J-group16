package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.network.GameStateParser;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Main board surface. Local mode uses the new panel layout; network mode keeps the old layout.
 */
public class GameBoardPane extends StackPane {

    private static final double BOARD_W = 1280;
    private static final double BOARD_H = 800;

    private static final double LOCAL_TOP_X = 300;
    private static final double LOCAL_TOP_Y = 162;
    private static final double LOCAL_TOP_W = 680;

    private static final double LOCAL_EXIT_X = 18;
    private static final double LOCAL_EXIT_Y = 32;

    private static final double LOCAL_BANK_X = 18;
    private static final double LOCAL_BANK_Y = 218;
    private static final double LOCAL_BANK_W = 175;
    private static final double LOCAL_BANK_H = 350;

    private static final double LOCAL_LOG_X = 1088;
    private static final double LOCAL_LOG_Y = 218;
    private static final double LOCAL_LOG_W = 175;
    private static final double LOCAL_LOG_H = 350;

    private static final double LOCAL_PROP_X = 224;
    private static final double LOCAL_PROP_Y = 386;
    private static final double LOCAL_PROP_W = 838;
    private static final double LOCAL_PROP_H = 152;

    private static final double LOCAL_PLAYER_X = 18;
    private static final double LOCAL_PLAYER_Y = 578;
    private static final double LOCAL_PLAYER_W = 1244;
    private static final double LOCAL_PLAYER_H = 220;

    private static final double NETWORK_CENTER_W = 650;
    private static final double NETWORK_CENTER_X = (BOARD_W - NETWORK_CENTER_W) / 2.0;
    private static final double NETWORK_CENTER_Y = 192;
    private static final double NETWORK_CTRL_X = 5;
    private static final double NETWORK_CTRL_Y = 195;
    private static final double NETWORK_PLAYER_Y = 535;
    private static final double NETWORK_PLAYER_H = 265;

    private static final double OPP_W = OpponentSeatPane.ZONE_W;
    private static final double OPP_H = OpponentSeatPane.ZONE_H;
    private static final double TABLE_CX = BOARD_W / 2.0;

    private static final double SEAT_LEFT_SAFE = 205;
    private static final double SEAT_RIGHT_SAFE = LOCAL_LOG_X - 12;
    private static final double SEAT_TOP_SAFE = 24;
    private static final double SEAT_GAP = 14;

    private final Pane boardLayer;
    private final VBox topLeftControls;
    private final List<Region> opponentSeats = new ArrayList<>();
    private final GameFrame localFrame;
    private final NetworkGameFrame networkFrame;
    private final StackPane propertyPreviewLayer;
    private final Label propertyPreviewName;
    private final StackPane winnerLayer;
    private final Label winnerLabel;
    private final StackPane confirmOverlay;
    private Label confirmTitleLabel;
    private Label confirmMessageLabel;
    private Button confirmActionButton;
    private Button confirmCancelButton;

    public GameBoardPane(GameFrame owner,
                         TopStatusPanel topPanel,
                         PropertyAreaPanel propertyPanel,
                         PlayerPanel playerPanel,
                         ControlPanel controlPanel,
                         RecentLogPanel recentLogPanel) {
        this.localFrame = owner;
        this.networkFrame = null;
        setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");

        boardLayer = buildBoardLayer();
        propertyPreviewLayer = buildPropertyPreviewLayer();
        propertyPreviewName = buildPropertyPreviewName();
        winnerLayer = buildWinnerLayer();
        winnerLabel = buildWinnerLabel();

        topPanel.setLayoutX(LOCAL_TOP_X);
        topPanel.setLayoutY(LOCAL_TOP_Y);
        topPanel.setPrefWidth(LOCAL_TOP_W);
        topPanel.setMinWidth(LOCAL_TOP_W);
        topPanel.setMaxWidth(LOCAL_TOP_W);

        controlPanel.setLayoutX(LOCAL_BANK_X);
        controlPanel.setLayoutY(LOCAL_BANK_Y);
        controlPanel.setPrefSize(LOCAL_BANK_W, LOCAL_BANK_H);
        controlPanel.setMinSize(LOCAL_BANK_W, LOCAL_BANK_H);
        controlPanel.setMaxSize(LOCAL_BANK_W, LOCAL_BANK_H);

        recentLogPanel.setLayoutX(LOCAL_LOG_X);
        recentLogPanel.setLayoutY(LOCAL_LOG_Y);
        recentLogPanel.setPrefSize(LOCAL_LOG_W, LOCAL_LOG_H);
        recentLogPanel.setMinSize(LOCAL_LOG_W, LOCAL_LOG_H);
        recentLogPanel.setMaxSize(LOCAL_LOG_W, LOCAL_LOG_H);

        propertyPanel.setLayoutX(LOCAL_PROP_X);
        propertyPanel.setLayoutY(LOCAL_PROP_Y);
        propertyPanel.setPrefSize(LOCAL_PROP_W, LOCAL_PROP_H);
        propertyPanel.setMinSize(LOCAL_PROP_W, LOCAL_PROP_H);
        propertyPanel.setMaxSize(LOCAL_PROP_W, LOCAL_PROP_H);

        playerPanel.setLayoutX(LOCAL_PLAYER_X);
        playerPanel.setLayoutY(LOCAL_PLAYER_Y);
        playerPanel.setPrefSize(LOCAL_PLAYER_W, LOCAL_PLAYER_H);
        playerPanel.setMinSize(LOCAL_PLAYER_W, LOCAL_PLAYER_H);
        playerPanel.setMaxSize(LOCAL_PLAYER_W, LOCAL_PLAYER_H);

        topLeftControls = buildTopLeftControls(owner);
        confirmOverlay = buildConfirmOverlay();

        propertyPreviewLayer.getChildren().add(propertyPreviewName);
        winnerLayer.getChildren().add(winnerLabel);
        boardLayer.getChildren().addAll(
                topPanel, controlPanel, recentLogPanel,
                propertyPanel, propertyPreviewLayer, playerPanel, winnerLayer,
                topLeftControls);
        finishSetup();
    }

    public GameBoardPane(NetworkGameFrame networkFrame,
                         TopStatusPanel topPanel,
                         PropertyAreaPanel propertyPanel,
                         PlayerPanel playerPanel,
                         ControlPanel controlPanel,
                         RecentLogPanel recentLogPanel) {
        this.localFrame = null;
        this.networkFrame = networkFrame;
        setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");

        boardLayer = buildBoardLayer();
        propertyPreviewLayer = buildPropertyPreviewLayer();
        propertyPreviewName = buildPropertyPreviewName();
        winnerLayer = buildWinnerLayer();
        winnerLabel = buildWinnerLabel();

        topPanel.setLayoutX(LOCAL_TOP_X);
        topPanel.setLayoutY(LOCAL_TOP_Y);
        topPanel.setPrefWidth(LOCAL_TOP_W);
        topPanel.setMinWidth(LOCAL_TOP_W);
        topPanel.setMaxWidth(LOCAL_TOP_W);

        controlPanel.setLayoutX(LOCAL_BANK_X);
        controlPanel.setLayoutY(LOCAL_BANK_Y);
        controlPanel.setPrefSize(LOCAL_BANK_W, LOCAL_BANK_H);
        controlPanel.setMinSize(LOCAL_BANK_W, LOCAL_BANK_H);
        controlPanel.setMaxSize(LOCAL_BANK_W, LOCAL_BANK_H);

        recentLogPanel.setLayoutX(LOCAL_LOG_X);
        recentLogPanel.setLayoutY(LOCAL_LOG_Y);
        recentLogPanel.setPrefSize(LOCAL_LOG_W, LOCAL_LOG_H);
        recentLogPanel.setMinSize(LOCAL_LOG_W, LOCAL_LOG_H);
        recentLogPanel.setMaxSize(LOCAL_LOG_W, LOCAL_LOG_H);

        propertyPanel.setLayoutX(LOCAL_PROP_X);
        propertyPanel.setLayoutY(LOCAL_PROP_Y);
        propertyPanel.setPrefSize(LOCAL_PROP_W, LOCAL_PROP_H);
        propertyPanel.setMinSize(LOCAL_PROP_W, LOCAL_PROP_H);
        propertyPanel.setMaxSize(LOCAL_PROP_W, LOCAL_PROP_H);

        playerPanel.setLayoutX(LOCAL_PLAYER_X);
        playerPanel.setLayoutY(LOCAL_PLAYER_Y);
        playerPanel.setPrefSize(LOCAL_PLAYER_W, LOCAL_PLAYER_H);
        playerPanel.setMinSize(LOCAL_PLAYER_W, LOCAL_PLAYER_H);
        playerPanel.setMaxSize(LOCAL_PLAYER_W, LOCAL_PLAYER_H);

        topLeftControls = buildNetworkTopLeftControls(networkFrame);
        confirmOverlay = buildConfirmOverlay();

        propertyPreviewLayer.getChildren().add(propertyPreviewName);
        winnerLayer.getChildren().add(winnerLabel);
        boardLayer.getChildren().addAll(
                topPanel, controlPanel, recentLogPanel,
                propertyPanel, propertyPreviewLayer, playerPanel, winnerLayer,
                topLeftControls);
        finishSetup();
    }

    public void updateOpponents(List<Player> allPlayers, Player bottomPlayer, CardImageResolver resolver) {
        boardLayer.getChildren().removeAll(opponentSeats);
        opponentSeats.clear();

        List<Player> opponents = new ArrayList<>();
        for (Player p : allPlayers) {
            if (p != bottomPlayer) {
                opponents.add(p);
            }
        }
        if (opponents.isEmpty()) {
            return;
        }

        SeatLayout[] layouts = computeArcLayouts(opponents.size());
        for (int i = 0; i < opponents.size(); i++) {
            Player opponent = opponents.get(i);
            SeatLayout layout = layouts[i];

            OpponentSeatPane seat;
            if (localFrame != null) {
                seat = new OpponentSeatPane(
                        opponent,
                        resolver,
                        cardId -> localFrame.canTargetOpponentWithCard(cardId, opponent),
                        cardId -> localFrame.playCardOnTarget(opponent, cardId),
                        localFrame::showPropertyPreview,
                        localFrame::clearPropertyPreview
                );
            } else if (networkFrame != null) {
                seat = new OpponentSeatPane(
                        opponent,
                        resolver,
                        cardId -> networkFrame.canTargetOpponentWithCard(cardId, opponent),
                        cardId -> networkFrame.playCardOnTarget(opponent, cardId),
                        null,
                        null
                );
            } else {
                seat = new OpponentSeatPane(opponent, resolver);
            }

            applySeatLayout(seat, layout);
            opponentSeats.add(seat);
            boardLayer.getChildren().add(seat);
        }
        topLeftControls.toFront();
    }

    public void updateFromSnapshot(GameStateParser.Snapshot snap, int myIndex,
                                   CardImageResolver resolver, NetworkGameFrame networkFrame) {
        boardLayer.getChildren().removeAll(opponentSeats);
        opponentSeats.clear();

        if (snap == null || snap.players == null) {
            return;
        }

        List<GameStateParser.PlayerInfo> opponents = new ArrayList<>();
        for (GameStateParser.PlayerInfo p : snap.players) {
            if (p.index != myIndex) {
                opponents.add(p);
            }
        }
        if (opponents.isEmpty()) {
            return;
        }

        SeatLayout[] layouts = computeArcLayouts(opponents.size());
        for (int i = 0; i < opponents.size(); i++) {
            SeatLayout layout = layouts[i];
            boolean isTurn = opponents.get(i).index == snap.turn;

            GameStateParser.PlayerInfo opp = opponents.get(i);
            NetworkOpponentSeatPane seat = new NetworkOpponentSeatPane(
                    opp,
                    resolver,
                    isTurn,
                    networkFrame == null ? null : cardId -> networkFrame.canTargetOpponentWithCard(cardId, opp.index),
                    networkFrame == null ? null : cardId -> networkFrame.playCardOnTarget(opp.index, cardId)
            );
            applySeatLayout(seat, layout);
            opponentSeats.add(seat);
            boardLayer.getChildren().add(seat);
        }
        topLeftControls.toFront();
    }

    private void applySeatLayout(Region seat, SeatLayout layout) {
        seat.setLayoutX(layout.x);
        seat.setLayoutY(layout.y);
        seat.setScaleX(layout.scale);
        seat.setScaleY(layout.scale);
        seat.setRotate(layout.rotation);
    }

    private Pane buildBoardLayer() {
        Pane layer = new Pane();
        layer.setMinSize(BOARD_W, BOARD_H);
        layer.setMaxSize(BOARD_W, BOARD_H);
        layer.setPrefSize(BOARD_W, BOARD_H);

        Rectangle woodOuter = new Rectangle(0, 0, BOARD_W, BOARD_H);
        woodOuter.setFill(UITheme.WOOD_OUTER);
        woodOuter.setArcWidth(36);
        woodOuter.setArcHeight(36);

        Rectangle woodInner = new Rectangle(8, 8, BOARD_W - 16, BOARD_H - 16);
        woodInner.setFill(UITheme.WOOD_INNER);
        woodInner.setArcWidth(28);
        woodInner.setArcHeight(28);

        Rectangle felt = new Rectangle(15, 15, BOARD_W - 30, BOARD_H - 30);
        felt.setFill(UITheme.TABLE_FELT_BOTTOM);
        felt.setArcWidth(20);
        felt.setArcHeight(20);

        double ovalCX = BOARD_W / 2.0;
        double ovalCY = 360;
        Ellipse tableOval = new Ellipse(ovalCX, ovalCY, 488, 220);
        tableOval.setFill(UITheme.TABLE_FELT_TOP);
        tableOval.setStroke(UITheme.WOOD_INNER);
        tableOval.setStrokeWidth(10);

        Ellipse ovalRing = new Ellipse(ovalCX, ovalCY, 478, 210);
        ovalRing.setFill(Color.TRANSPARENT);
        ovalRing.setStroke(Color.rgb(255, 255, 255, 0.06));
        ovalRing.setStrokeWidth(3);

        layer.getChildren().addAll(woodOuter, woodInner, felt, tableOval, ovalRing);
        return layer;
    }

    public void setPropertyPreviewName(String playerName) {
        boolean visible = playerName != null && !playerName.isBlank();
        propertyPreviewName.setText(visible ? playerName : "");
        propertyPreviewLayer.setVisible(visible);
        propertyPreviewLayer.setManaged(visible);
    }

    public void setWinnerBanner(String winnerText) {
        boolean visible = winnerText != null && !winnerText.isBlank();
        winnerLabel.setText(visible ? winnerText : "");
        winnerLayer.setVisible(visible);
        winnerLayer.setManaged(visible);
        if (visible) {
            winnerLayer.toFront();
        }
    }

    /**
     * In-scene confirmation — avoids JavaFX modal-stage issues during gameplay.
     */
    public void showConfirmDialog(String title,
                                  String message,
                                  String confirmLabel,
                                  String cancelLabel,
                                  Runnable onConfirm) {
        confirmTitleLabel.setText(title);
        confirmMessageLabel.setText(message);
        confirmActionButton.setText(confirmLabel);
        confirmCancelButton.setText(cancelLabel);
        confirmActionButton.setOnAction(e -> {
            hideConfirmOverlay();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });
        confirmCancelButton.setOnAction(e -> hideConfirmOverlay());
        confirmOverlay.setMouseTransparent(false);
        confirmOverlay.setPickOnBounds(false);
        confirmOverlay.setVisible(true);
        confirmOverlay.setManaged(true);
        confirmOverlay.toFront();
    }

    private void hideConfirmOverlay() {
        confirmOverlay.setVisible(false);
        confirmOverlay.setManaged(false);
        confirmOverlay.setMouseTransparent(true);
        topLeftControls.toFront();
    }

    private StackPane buildConfirmOverlay() {
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

        confirmTitleLabel = new Label();
        confirmTitleLabel.setFont(UITheme.FONT_TITLE);
        confirmTitleLabel.setTextFill(UITheme.TEXT_MAIN);

        confirmMessageLabel = new Label();
        confirmMessageLabel.setFont(UITheme.FONT_BODY);
        confirmMessageLabel.setTextFill(UITheme.TEXT_SUB);
        confirmMessageLabel.setWrapText(true);
        confirmMessageLabel.setMaxWidth(400);

        confirmCancelButton = new Button("Stay");
        UITheme.styleDialogCancelButton(confirmCancelButton);

        confirmActionButton = new Button("Leave");
        UITheme.styleDialogConfirmButton(confirmActionButton);

        HBox actions = new HBox(12, confirmCancelButton, confirmActionButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, accentBar, confirmTitleLabel, confirmMessageLabel, actions);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(22, 26, 20, 26));
        card.setMaxWidth(420);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
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

        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setMouseTransparent(true);
        overlay.setPickOnBounds(false);
        return overlay;
    }

    private void finishSetup() {
        getChildren().addAll(boardLayer, confirmOverlay);
        StackPane.setAlignment(boardLayer, Pos.CENTER);

        NumberBinding scale = Bindings.min(
                widthProperty().divide(BOARD_W),
                heightProperty().divide(BOARD_H)
        );
        boardLayer.scaleXProperty().bind(scale);
        boardLayer.scaleYProperty().bind(scale);
    }

    private StackPane buildPropertyPreviewLayer() {
        StackPane layer = new StackPane();
        layer.setVisible(false);
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.setLayoutX(LOCAL_PROP_X);
        layer.setLayoutY(LOCAL_PROP_Y);
        layer.setPrefSize(LOCAL_PROP_W, LOCAL_PROP_H);
        layer.setMinSize(LOCAL_PROP_W, LOCAL_PROP_H);
        layer.setMaxSize(LOCAL_PROP_W, LOCAL_PROP_H);
        layer.setAlignment(Pos.CENTER);
        layer.setStyle("-fx-background-color: transparent;");
        return layer;
    }

    private Label buildPropertyPreviewName() {
        Label label = new Label();
        label.setMouseTransparent(true);
        label.setAlignment(Pos.CENTER);
        label.setPrefSize(LOCAL_PROP_W, LOCAL_PROP_H);
        label.setMinSize(LOCAL_PROP_W, LOCAL_PROP_H);
        label.setMaxSize(LOCAL_PROP_W, LOCAL_PROP_H);
        label.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 60));
        label.setTextFill(Color.rgb(210, 48, 48, 0.52));
        label.setStyle("-fx-background-color: transparent;");
        return label;
    }

    private StackPane buildWinnerLayer() {
        StackPane layer = new StackPane();
        layer.setVisible(false);
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.setLayoutX(0);
        layer.setLayoutY(0);
        layer.setPrefSize(BOARD_W, BOARD_H);
        layer.setMinSize(BOARD_W, BOARD_H);
        layer.setMaxSize(BOARD_W, BOARD_H);
        layer.setAlignment(Pos.CENTER);
        layer.setStyle("-fx-background-color: transparent;");
        return layer;
    }

    private Label buildWinnerLabel() {
        Label label = new Label();
        label.setMouseTransparent(true);
        label.setAlignment(Pos.CENTER);
        label.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 76));
        label.setTextFill(Color.rgb(255, 246, 208, 0.96));
        label.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-effect: dropshadow(gaussian, rgba(24,16,8,0.72), 18, 0.35, 0, 4);"
        );
        return label;
    }

    private static VBox buildNetworkTopLeftControls(NetworkGameFrame networkFrame) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPickOnBounds(false);
        box.setLayoutX(LOCAL_EXIT_X);
        box.setLayoutY(LOCAL_EXIT_Y);
        if (networkFrame != null) {
            Button exit = new Button("Exit");
            UITheme.styleExitButton(exit);
            exit.setOnAction(e -> networkFrame.requestExitToHome());
            box.getChildren().add(exit);
        }
        box.getChildren().add(new GameVolumeControl());
        return box;
    }

    private static VBox buildTopLeftControls(GameFrame owner) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPickOnBounds(false);
        box.setLayoutX(LOCAL_EXIT_X);
        box.setLayoutY(LOCAL_EXIT_Y);
        if (owner != null) {
            Button exit = new Button("Exit");
            UITheme.styleExitButton(exit);
            exit.setOnAction(e -> owner.requestExitToHome());
            box.getChildren().add(exit);
        }
        box.getChildren().add(new GameVolumeControl());
        return box;
    }

    private SeatLayout[] computeArcLayouts(int n) {
        SeatLayout[] layouts = new SeatLayout[n];
        if (n <= 0) {
            return layouts;
        }

        double scale = seatScale(n);
        double effW = OPP_W * scale;
        double minCenterX = SEAT_LEFT_SAFE + effW / 2.0;
        double maxCenterX = SEAT_RIGHT_SAFE - effW / 2.0;
        double[] centerXs = symmetricCenterXs(n, minCenterX, maxCenterX, effW + SEAT_GAP);

        for (int i = 0; i < n; i++) {
            double norm = normalizedOffset(i, n);
            double y = SEAT_TOP_SAFE + 24.0 * Math.abs(norm);
            double rotation = norm * (n >= 4 ? 11.0 : 13.0);

            layouts[i] = new SeatLayout(
                    centerXs[i] - OPP_W / 2.0,
                    y,
                    rotation,
                    scale
            );
        }
        return layouts;
    }

    private static double seatScale(int opponentCount) {
        if (opponentCount >= 4) {
            return 0.76;
        }
        if (opponentCount == 3) {
            return 0.9;
        }
        return 1.0;
    }

    private static double normalizedOffset(int index, int count) {
        if (count <= 1) {
            return 0.0;
        }
        return (2.0 * index / (count - 1)) - 1.0;
    }

    private static double[] symmetricCenterXs(int count,
                                              double minCenterX,
                                              double maxCenterX,
                                              double minGap) {
        double[] centers = new double[count];
        if (count == 1) {
            centers[0] = TABLE_CX;
            return centers;
        }

        double half = (count - 1) / 2.0;
        double stepByBounds = Math.min(
                (maxCenterX - TABLE_CX) / half,
                (TABLE_CX - minCenterX) / half
        );
        double step = Math.max(minGap, stepByBounds);

        for (int i = 0; i < count; i++) {
            centers[i] = TABLE_CX + (i - half) * step;
        }
        return centers;
    }

    private static final class SeatLayout {
        final double x;
        final double y;
        final double rotation;
        final double scale;

        SeatLayout(double x, double y, double rotation, double scale) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scale = scale;
        }
    }
}
