package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.network.GameStateParser;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Main game board: oval felt table with player seats arranged around the perimeter.
 * Uses a fixed 1280×800 internal coordinate system that scales uniformly to the window.
 */
public class GameBoardPane extends StackPane {

    private static final double BOARD_W = 1280;
    private static final double BOARD_H = 800;

    // TopStatusPanel (deck / drop zone) — centered on the table
    private static final double CENTER_W = 650;
    private static final double CENTER_X = (BOARD_W - CENTER_W) / 2.0;  // 315
    private static final double CENTER_Y = 192;

    // ControlPanel — narrow strip between board edge and oval
    private static final double CTRL_X = 5;
    private static final double CTRL_Y = 195;
    private static final double CTRL_W = 140;

    // PlayerPanel — full-width bottom strip
    private static final double PLAYER_Y = 535;
    private static final double PLAYER_H = 265;

    // Opponent seat zones — top row, tilted inward (八字) by rotation
    private static final double OPP_W        = OpponentSeatPane.ZONE_W;
    private static final double OPP_Y        = 32;
    private static final double OPP_GAP      = 30;
    private static final double OPP_MAX_TILT = 15.0; // degrees at the outermost seats

    private final Pane boardLayer;
    // Holds either OpponentSeatPane (local) or NetworkOpponentSeatPane (LAN)
    private final List<Region> opponentSeats = new ArrayList<>();

    /**
     * Local game constructor — accepts the concrete panel types used in local mode.
     */
    public GameBoardPane(TopStatusPanel topPanel,
                         PlayerPanel playerPanel,
                         ControlPanel controlPanel) {
        this(topPanel, (Region) playerPanel, (Region) controlPanel);
    }

    /**
     * General constructor — accepts any Region for the side panels.
     * Used by both local (PlayerPanel/ControlPanel) and LAN
     * (NetworkPlayerPanel/NetworkControlPanel) modes.
     */
    public GameBoardPane(TopStatusPanel topPanel,
                         Region playerPanel,
                         Region controlPanel) {

        setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");

        // ── Fixed-size layer ──
        boardLayer = new Pane();
        boardLayer.setMinSize(BOARD_W, BOARD_H);
        boardLayer.setMaxSize(BOARD_W, BOARD_H);
        boardLayer.setPrefSize(BOARD_W, BOARD_H);

        // Wood outer frame
        Rectangle woodOuter = new Rectangle(0, 0, BOARD_W, BOARD_H);
        woodOuter.setFill(UITheme.WOOD_OUTER);
        woodOuter.setArcWidth(36);
        woodOuter.setArcHeight(36);

        // Wood inner frame
        Rectangle woodInner = new Rectangle(8, 8, BOARD_W - 16, BOARD_H - 16);
        woodInner.setFill(UITheme.WOOD_INNER);
        woodInner.setArcWidth(28);
        woodInner.setArcHeight(28);

        // Green felt fill
        Rectangle felt = new Rectangle(15, 15, BOARD_W - 30, BOARD_H - 30);
        felt.setFill(UITheme.TABLE_FELT_BOTTOM);
        felt.setArcWidth(20);
        felt.setArcHeight(20);

        // Oval table surface (lighter green)
        double ovalCX = BOARD_W / 2.0;
        double ovalCY = 360;
        Ellipse tableOval = new Ellipse(ovalCX, ovalCY, 488, 220);
        tableOval.setFill(UITheme.TABLE_FELT_TOP);
        tableOval.setStroke(UITheme.WOOD_INNER);
        tableOval.setStrokeWidth(10);

        // Subtle inner highlight ring on the oval
        Ellipse ovalRing = new Ellipse(ovalCX, ovalCY, 478, 210);
        ovalRing.setFill(Color.TRANSPARENT);
        ovalRing.setStroke(Color.rgb(255, 255, 255, 0.06));
        ovalRing.setStrokeWidth(3);

        boardLayer.getChildren().addAll(woodOuter, woodInner, felt, tableOval, ovalRing);

        // ── Components ──

        topPanel.setLayoutX(CENTER_X);
        topPanel.setLayoutY(CENTER_Y);
        topPanel.setPrefWidth(CENTER_W);
        boardLayer.getChildren().add(topPanel);

        controlPanel.setLayoutX(CTRL_X);
        controlPanel.setLayoutY(CTRL_Y);
        controlPanel.setPrefWidth(CTRL_W);
        boardLayer.getChildren().add(controlPanel);

        playerPanel.setLayoutX(155);
        playerPanel.setLayoutY(PLAYER_Y);
        playerPanel.setPrefWidth(BOARD_W - 155);
        playerPanel.setPrefHeight(PLAYER_H);
        boardLayer.getChildren().add(playerPanel);

        getChildren().add(boardLayer);
        StackPane.setAlignment(boardLayer, Pos.CENTER);

        // ── Uniform scale to fit window ──
        NumberBinding scale = Bindings.min(
                widthProperty().divide(BOARD_W),
                heightProperty().divide(BOARD_H)
        );
        boardLayer.scaleXProperty().bind(scale);
        boardLayer.scaleYProperty().bind(scale);
    }

    /**
     * Rebuild the opponent seats around the top of the table.
     * Call this every time the player list or current player changes.
     */
    public void updateOpponents(List<Player> allPlayers, Player current, CardImageResolver resolver) {
        boardLayer.getChildren().removeAll(opponentSeats);
        opponentSeats.clear();

        List<Player> opponents = new ArrayList<>();
        for (Player p : allPlayers) {
            if (p != current) opponents.add(p);
        }
        if (opponents.isEmpty()) return;

        double[] xPositions = computeXPositions(opponents.size());

        int n = opponents.size();
        for (int i = 0; i < n; i++) {
            double x = xPositions[i];

            // 八字 tilt: outermost seats lean inward most, center seat is upright.
            // normalizedPos runs -1 (leftmost) → +1 (rightmost).
            double normalizedPos = (n > 1) ? (2.0 * i / (n - 1) - 1.0) : 0.0;
            double rotation = normalizedPos * OPP_MAX_TILT;

            OpponentSeatPane seat = new OpponentSeatPane(opponents.get(i), resolver);
            seat.setLayoutX(x);
            seat.setLayoutY(OPP_Y);
            seat.setRotate(rotation);
            opponentSeats.add(seat);
            boardLayer.getChildren().add(seat);
        }
    }

    /**
     * LAN mode: rebuild opponent seats from a game-state snapshot.
     * Highlights the seat of whichever player's turn it currently is.
     */
    public void updateFromSnapshot(GameStateParser.Snapshot snap, int myIndex,
                                   CardImageResolver resolver) {
        boardLayer.getChildren().removeAll(opponentSeats);
        opponentSeats.clear();

        if (snap == null || snap.players == null) return;

        List<GameStateParser.PlayerInfo> opponents = new ArrayList<>();
        for (GameStateParser.PlayerInfo p : snap.players) {
            if (p.index != myIndex) opponents.add(p);
        }
        if (opponents.isEmpty()) return;

        double[] xPositions = computeXPositions(opponents.size());
        int n = opponents.size();
        for (int i = 0; i < n; i++) {
            double normalizedPos = (n > 1) ? (2.0 * i / (n - 1) - 1.0) : 0.0;
            double rotation      = normalizedPos * OPP_MAX_TILT;
            boolean isTurn       = (opponents.get(i).index == snap.turn);

            NetworkOpponentSeatPane seat =
                    new NetworkOpponentSeatPane(opponents.get(i), resolver, isTurn);
            seat.setLayoutX(xPositions[i]);
            seat.setLayoutY(OPP_Y);
            seat.setRotate(rotation);
            opponentSeats.add(seat);
            boardLayer.getChildren().add(seat);
        }
    }

    // Spread n opponent zones evenly across the top of the board.
    private double[] computeXPositions(int n) {
        double totalW = n * OPP_W + (n - 1) * OPP_GAP;
        double startX = (BOARD_W - totalW) / 2.0;
        double[] xs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = startX + i * (OPP_W + OPP_GAP);
        }
        return xs;
    }
}
