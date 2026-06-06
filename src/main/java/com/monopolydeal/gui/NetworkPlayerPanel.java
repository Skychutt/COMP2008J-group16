package com.monopolydeal.gui;

import com.monopolydeal.network.GameStateParser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Local player's hand panel in LAN online mode
 *
 * Display player name, number of actions, and draggable hand, Bank placement area, attribute placement area, and end turn button
 */
public class NetworkPlayerPanel extends BorderPane {

    private static final int CARD_W = 90;
    private static final int CARD_H = 140;

    private final NetworkGameFrame frame;

    private final Label lblSeat;
    private final Label lblActions;
    private final FlowPane handCanvas;

    private IntConsumer bankDropHandler;
    private IntConsumer propertyDropHandler;
    private Runnable endTurnHandler;

    private boolean myTurn      = false;
    private boolean discardMode = false;
    private boolean gameOver    = false;

    public NetworkPlayerPanel(NetworkGameFrame frame) {
        this.frame = frame;
        setStyle("-fx-background-color: transparent;");
        setPadding(new Insets(6, 10, 6, 10));

        // ── Left info ──
        lblSeat = new Label("You");
        lblSeat.setFont(UITheme.FONT_SUBTITLE);
        lblSeat.setTextFill(UITheme.ACCENT_DARK);

        lblActions = new Label("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_BODY);
        lblActions.setTextFill(UITheme.TEXT_SUB);

        VBox leftInfo = new VBox(4, lblSeat, lblActions);
        leftInfo.setAlignment(Pos.TOP_CENTER);
        leftInfo.setMinWidth(120);
        leftInfo.setPadding(new Insets(6, 0, 0, 0));
        setLeft(leftInfo);

        // ── Center hand canvas ──
        handCanvas = new FlowPane(6, 4);
        handCanvas.setAlignment(Pos.CENTER_LEFT);
        handCanvas.setPadding(new Insets(4));

        VBox handWrapper = new VBox(handCanvas);
        handWrapper.setStyle(
                "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
                        "-fx-border-width: 1px; -fx-border-radius: 4px;" +
                        "-fx-padding: 6;"
        );

        Label handTitle = new Label("Your Hand");
        handTitle.setFont(UITheme.FONT_SUBTITLE);
        handTitle.setTextFill(UITheme.TEXT_MAIN);

        VBox centerBox = new VBox(4, handTitle, handWrapper);
        centerBox.setAlignment(Pos.TOP_LEFT);
        setCenter(centerBox);
        BorderPane.setMargin(centerBox, new Insets(0, 8, 0, 8));

        // ── Right dock: bank / property / end turn ──
        VBox rightDock = buildRightDock();
        setRight(rightDock);
    }

    private VBox buildRightDock() {
        VBox dock = new VBox(8);
        dock.setAlignment(Pos.TOP_CENTER);
        dock.setMinWidth(150);
        dock.setPadding(new Insets(0, 0, 4, 8));

        // Bank drop zone
        Label bankZone = makeDropZoneLabel("Bank Card Here");
        bankZone.setOnDragOver(e -> {
            if (myTurn && !discardMode && !gameOver && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
                bankZone.setStyle(dropZoneStyle(true));
            }
            e.consume();
        });
        bankZone.setOnDragExited(e -> bankZone.setStyle(dropZoneStyle(false)));
        bankZone.setOnDragDropped(e -> {
            bankZone.setStyle(dropZoneStyle(false));
            if (!myTurn || discardMode || gameOver) { e.setDropCompleted(false); return; }
            try {
                int id = Integer.parseInt(e.getDragboard().getString().trim());
                if (bankDropHandler != null) bankDropHandler.accept(id);
                e.setDropCompleted(true);
            } catch (NumberFormatException ex) { e.setDropCompleted(false); }
            e.consume();
        });

        // Property drop zone
        Label propZone = makeDropZoneLabel("Place Property");
        propZone.setOnDragOver(e -> {
            if (myTurn && !discardMode && !gameOver && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
                propZone.setStyle(dropZoneStyle(true));
            }
            e.consume();
        });
        propZone.setOnDragExited(e -> propZone.setStyle(dropZoneStyle(false)));
        propZone.setOnDragDropped(e -> {
            propZone.setStyle(dropZoneStyle(false));
            if (!myTurn || discardMode || gameOver) { e.setDropCompleted(false); return; }
            try {
                int id = Integer.parseInt(e.getDragboard().getString().trim());
                if (propertyDropHandler != null) propertyDropHandler.accept(id);
                e.setDropCompleted(true);
            } catch (NumberFormatException ex) { e.setDropCompleted(false); }
            e.consume();
        });

        // End turn button (reuses ImageActionButton from main branch)
        ImageActionButton endTurnBtn = new ImageActionButton("End Turn.png", 148, 80);
        endTurnBtn.setTooltip(new Tooltip("End your turn"));
        endTurnBtn.setOnAction(e -> { if (endTurnHandler != null) endTurnHandler.run(); });
        endTurnBtn.setDisable(true);

        // Store reference for later enable/disable
        dock.getProperties().put("endTurnBtn", endTurnBtn);
        dock.getChildren().addAll(bankZone, propZone, endTurnBtn);
        return dock;
    }

    // ─────────────────────────────────────────────────────────────────────────

    public void setBankDropHandler(IntConsumer h)     { this.bankDropHandler = h; }
    public void setPropertyDropHandler(IntConsumer h) { this.propertyDropHandler = h; }
    public void setEndTurnHandler(Runnable r)         { this.endTurnHandler = r; }

    public void updateFromSnapshot(GameStateParser.Snapshot snap,
                                   boolean myTurn, boolean discardMode, int discardRemaining) {
        if (snap == null) return;
        this.myTurn      = myTurn;
        this.discardMode = discardMode;
        this.gameOver    = snap.gameOver;

        GameStateParser.PlayerInfo me = snap.getMyInfo(frame.getMyPlayerIndex());
        if (me != null) {
            lblSeat.setText(me.name + " (You)");
            lblActions.setText("Actions: " + me.actions + " / 3");
        }

        if (discardMode && discardRemaining > 0) {
            lblActions.setText("Discard " + discardRemaining + " card(s)!");
            lblActions.setTextFill(Color.RED);
        } else {
            lblActions.setTextFill(UITheme.TEXT_SUB);
        }

        handCanvas.getChildren().clear();
        List<GameStateParser.CardInfo> hand = snap.myHand;
        if (hand != null) {
            for (GameStateParser.CardInfo card : hand) {
                handCanvas.getChildren().add(buildHandCard(card));
            }
        }
        refreshEndTurnButton();
    }

    public void setMyTurn(boolean myTurn, boolean discardMode, int discardRemaining) {
        this.myTurn      = myTurn;
        this.discardMode = discardMode;
        if (discardMode) {
            lblActions.setText("Discard " + discardRemaining + " card(s)!");
            lblActions.setTextFill(Color.RED);
        }
        refreshEndTurnButton();
    }

    private void refreshEndTurnButton() {
        VBox dock = (VBox) getRight();
        if (dock == null) return;
        ImageActionButton btn = (ImageActionButton) dock.getProperties().get("endTurnBtn");
        if (btn == null) return;
        boolean canEnd = myTurn && !gameOver;
        btn.setDisable(!canEnd);
        if (canEnd && !discardMode) {
            btn.setVisualState(ImageActionButton.VisualState.ENABLED);
        } else if (myTurn && discardMode) {
            btn.setVisualState(ImageActionButton.VisualState.WAITING);
        } else {
            btn.setVisualState(ImageActionButton.VisualState.DISABLED);
        }
    }

    private VBox buildHandCard(GameStateParser.CardInfo card) {
        ImageView iv = new ImageView();
        iv.setFitWidth(CARD_W);
        iv.setFitHeight(CARD_H);
        iv.setPreserveRatio(false);

        javafx.scene.image.Image img = frame.getImageResolver().getCardIconFromInfo(card, CARD_W, CARD_H);
        if (img != null) {
            iv.setImage(img);
        } else {
            iv.setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PANEL_BG) + ";");
        }
        iv.setStyle(iv.getStyle() + "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
                "-fx-border-width: 1px;");
        Tooltip.install(iv, new Tooltip(card.name + "  " + card.value + "M  [" + card.cardType + "]"));

        if (myTurn) {
            iv.setCursor(Cursor.HAND);
            iv.setOnMouseClicked(e -> frame.handleCenterDrop(card.id));
            iv.setOnDragDetected(e -> {
                Dragboard db = iv.startDragAndDrop(TransferMode.COPY);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(String.valueOf(card.id));
                db.setContent(cc);
                e.consume();
            });
        }

        VBox wrapper = new VBox(iv);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private static Label makeDropZoneLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setTextFill(UITheme.TEXT_MAIN);
        lbl.setMinSize(148, 68);
        lbl.setAlignment(Pos.CENTER);
        lbl.setStyle(dropZoneStyle(false));
        return lbl;
    }

    private static String dropZoneStyle(boolean hovered) {
        String border = hovered
                ? UITheme.toCssHex(UITheme.ACCENT_DARK)
                : UITheme.toCssHex(UITheme.BORDER);
        String width  = hovered ? "2px" : "1px";
        return "-fx-background-color: " + UITheme.toCssHex(UITheme.DROP_ZONE) + ";" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: " + width + ";" +
                "-fx-border-radius: 4px; -fx-background-radius: 4px;";
    }
}