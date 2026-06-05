package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Bottom player seat: fan-style hand cards with JavaFX drag-and-drop.
 */
public class PlayerPanel extends BorderPane {

    private static final int CARD_W = 118;
    private static final int CARD_H = 178;
    private static final int DROP_ZONE_W = 170;
    private static final int DROP_ZONE_H = 100;

    private final GameFrame mainFrame;
    private final Label lblSeat;
    private final Label lblActions;
    private final Pane handCanvas;
    private final ButtonDropZone bankDropZone;
    private final ButtonDropZone propertyDropZone;
    private final ImageActionButton btnEndTurn;

    private IntConsumer bankDropHandler;
    private IntConsumer propertyDropHandler;
    private Runnable endTurnHandler;

    private boolean gameOver     = false;
    private boolean discardMode  = false;
    private int discardRemaining = 0;

    private int lastActionsLeft = Integer.MIN_VALUE;
    private boolean lastGameOver;
    private boolean lastDiscardMode;

    public PlayerPanel(GameFrame mainFrame) {
        this.mainFrame = mainFrame;
        setStyle("-fx-background-color: transparent;");
        setPadding(new Insets(0, 0, 6, 0));

        // ── Header ──
        lblSeat = new Label("Your Seat");
        lblSeat.setFont(UITheme.FONT_SUBTITLE);
        lblSeat.setTextFill(Color.rgb(250, 241, 209));
        lblActions = new Label("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_BODY);
        lblActions.setTextFill(Color.rgb(252, 239, 197));

        HBox header = new HBox();
        header.getChildren().addAll(lblSeat, lblActions);
        HBox.setHgrow(lblSeat, javafx.scene.layout.Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        setTop(header);

        // ── Hand canvas (absolute-positioned cards inside a ScrollPane) ──
        handCanvas = new Pane();
        handCanvas.setStyle("-fx-background-color: transparent;");
        handCanvas.setPrefHeight(220);

        ScrollPane scroll = new ScrollPane(handCanvas);
        scroll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: rgba(176,142,75,0.47);" +
            "-fx-border-width: 1px; -fx-border-radius: 4px; -fx-background-radius: 4px;"
        );
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToHeight(true);
        scroll.setPrefHeight(220);
        setCenter(scroll);

        // ── Right dock: Bank / Property / End Turn ──
        btnEndTurn = new ImageActionButton("End Turn.png", DROP_ZONE_W, DROP_ZONE_H, true);
        btnEndTurn.setImageOffsetX(-1);
        Tooltip.install(btnEndTurn, new Tooltip("End Turn"));
        btnEndTurn.setOnAction(e -> {
            if (!gameOver && endTurnHandler != null) endTurnHandler.run();
        });

        bankDropZone = new ButtonDropZone("Bank.png", DROP_ZONE_W, DROP_ZONE_H);
        bankDropZone.setImageOffsetY(6);
        Tooltip.install(bankDropZone, new Tooltip("Drag money, action, or property cards here to bank"));

        propertyDropZone = new ButtonDropZone("Property.png", DROP_ZONE_W, DROP_ZONE_H);
        Tooltip.install(propertyDropZone, new Tooltip("Drag property cards here to play"));

        wireDropZones();

        VBox rightDock = new VBox(8, bankDropZone, propertyDropZone, btnEndTurn);
        rightDock.setAlignment(Pos.TOP_CENTER);
        rightDock.setPadding(new Insets(0, 0, 4, 8));
        rightDock.setPrefWidth(DROP_ZONE_W + 16);
        setRight(rightDock);

        updateEndTurnButtonStyle(0, false, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setters
    // ─────────────────────────────────────────────────────────────────────────

    public void setBankDropHandler(IntConsumer h)     { this.bankDropHandler     = h; }
    public void setPropertyDropHandler(IntConsumer h) { this.propertyDropHandler = h; }
    public void setEndTurnHandler(Runnable h)         { this.endTurnHandler      = h; }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    public void updatePlayerView(Player current, boolean gameOver, boolean discardMode, int discardRemaining) {
        if (current == null) return;
        this.gameOver        = gameOver;
        this.discardMode     = discardMode;
        this.discardRemaining = Math.max(0, discardRemaining);

        if (discardMode) {
            lblSeat.setText(current.getName()
                    + " - Drag to center to discard " + this.discardRemaining + " card(s)");
        } else {
            lblSeat.setText(current.getName()
                    + " - Drag to center to play actions, bank (right) for money, property (right) for land cards");
        }

        refreshDropZoneHighlight(false, false);
        lblActions.setText("Actions: " + current.getActions() + " / 3");
        updateEndTurnButtonStyle(current.getActions(), gameOver, discardMode);
        renderHand(current);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hand rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void renderHand(Player current) {
        handCanvas.getChildren().clear();

        List<Card> cards = new ArrayList<>(current.getHand().getCards());
        if (cards.isEmpty()) {
            Label empty = new Label("No cards in hand");
            empty.setFont(UITheme.FONT_BODY);
            empty.setTextFill(Color.rgb(250, 241, 209));
            empty.setLayoutX(12);
            empty.setLayoutY(24);
            handCanvas.getChildren().add(empty);
            return;
        }

        int step   = CARD_W - 10;
        int n      = cards.size();
        int totalW = CARD_W + Math.max(0, n - 1) * step;
        double canvasW = Math.max(1040, totalW + 80);
        handCanvas.setPrefWidth(canvasW);

        int startX = (int) ((canvasW - totalW) / 2);
        double middle = (n - 1) / 2.0;

        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            int x = startX + i * step;
            double d = Math.abs(i - middle);
            int y = 14 + (int) (d * d * 2.2);
            buildDraggableCardView(current, card, x, y);
        }
    }

    private void buildDraggableCardView(Player current, Card card, int x, int y) {
        ImageView iv = new ImageView(mainFrame.getImageResolver().getCardIcon(card, CARD_W, CARD_H));
        iv.setFitWidth(CARD_W);
        iv.setFitHeight(CARD_H);
        iv.setPreserveRatio(false);
        iv.setLayoutX(x);
        iv.setLayoutY(y);

        boolean canPlay   = mainFrame.getGameLogic().getRuleValidator().canPlayCard(current, card);
        boolean canBank   = current.getActions() > 0
                && (!(card instanceof PropertyCard) || ((PropertyCard) card).canBankAsMoney());
        boolean canProp   = card instanceof PropertyCard && canPlay;
        boolean draggable = discardMode || canPlay || canBank;

        String tooltipText = card.getName() + " (" + card.getValue() + "M)";
        String borderColor;

        if (!draggable) {
            iv.setOpacity(0.55);
            iv.setCursor(Cursor.DEFAULT);
            borderColor = "rgba(130,130,130,0.8)";
        } else {
            iv.setCursor(Cursor.HAND);
            if (discardMode) {
                tooltipText += " - Drag to center to discard";
                borderColor  = "rgba(246,229,173,0.8)";
            } else if (canProp && canBank) {
                tooltipText += " - Property zone, center, or Bank";
                borderColor  = "rgba(76,130,68,0.9)";
            } else if (canProp) {
                tooltipText += " - Drag to Property zone or center";
                borderColor  = "rgba(76,130,68,0.9)";
            } else if (canBank) {
                tooltipText += " - Bank only";
                borderColor  = UITheme.toCssHex(UITheme.BANK_ZONE_BORDER);
            } else {
                borderColor  = "rgba(246,229,173,0.8)";
            }

            final int cardId = card.getId();
            iv.setOnDragDetected(e -> {
                Dragboard db = iv.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(cardId));
                // Use a snapshot of the card image as the drag cursor
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                WritableImage snapshot = iv.snapshot(params, null);
                db.setDragView(snapshot, snapshot.getWidth() / 2, snapshot.getHeight() / 2);
                db.setContent(content);
                e.consume();
            });
        }

        // Wrap in a StackPane so we can add a CSS border
        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(iv);
        wrapper.setStyle(
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-color: rgba(255,255,255,0.07);"
        );
        wrapper.setPrefSize(CARD_W, CARD_H);
        wrapper.setMaxSize(CARD_W, CARD_H);
        wrapper.setLayoutX(x);
        wrapper.setLayoutY(y);
        Tooltip.install(wrapper, new Tooltip(tooltipText));

        // Re-attach drag to wrapper as well
        if (draggable) {
            final int cardId = card.getId();
            wrapper.setOnDragDetected(e -> {
                Dragboard db = wrapper.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(cardId));
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                WritableImage snapshot = iv.snapshot(params, null);
                db.setDragView(snapshot, snapshot.getWidth() / 2, snapshot.getHeight() / 2);
                db.setContent(content);
                e.consume();
            });
        }

        handCanvas.getChildren().add(wrapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drop zone DnD handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void wireDropZones() {
        // ── Bank drop zone ──
        bankDropZone.setOnDragOver(e -> {
            if (gameOver || discardMode) { e.consume(); return; }
            if (e.getDragboard().hasString()) {
                Card card = findDraggedCard(e.getDragboard().getString());
                boolean ok = card != null && canDropOnBank(card);
                e.acceptTransferModes(ok ? new TransferMode[]{TransferMode.COPY} : new TransferMode[0]);
                refreshDropZoneHighlight(ok, false);
            }
            e.consume();
        });
        bankDropZone.setOnDragExited(e -> {
            refreshDropZoneHighlight(false, false);
            e.consume();
        });
        bankDropZone.setOnDragDropped(e -> {
            refreshDropZoneHighlight(false, false);
            boolean success = false;
            if (!gameOver && !discardMode && e.getDragboard().hasString()) {
                try {
                    int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                    if (bankDropHandler != null) bankDropHandler.accept(cardId);
                    success = true;
                } catch (NumberFormatException ignored) {}
            }
            e.setDropCompleted(success);
            e.consume();
        });

        // ── Property drop zone ──
        propertyDropZone.setOnDragOver(e -> {
            if (gameOver || discardMode) { e.consume(); return; }
            if (e.getDragboard().hasString()) {
                Card card = findDraggedCard(e.getDragboard().getString());
                boolean ok = card instanceof PropertyCard;
                e.acceptTransferModes(ok ? new TransferMode[]{TransferMode.COPY} : new TransferMode[0]);
                refreshDropZoneHighlight(false, ok);
            }
            e.consume();
        });
        propertyDropZone.setOnDragExited(e -> {
            refreshDropZoneHighlight(false, false);
            e.consume();
        });
        propertyDropZone.setOnDragDropped(e -> {
            refreshDropZoneHighlight(false, false);
            boolean success = false;
            if (!gameOver && !discardMode && e.getDragboard().hasString()) {
                try {
                    int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                    if (propertyDropHandler != null) propertyDropHandler.accept(cardId);
                    success = true;
                } catch (NumberFormatException ignored) {}
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshDropZoneHighlight(boolean bank, boolean prop) {
        boolean enabled = !gameOver && !discardMode;
        bankDropZone.setHighlight(enabled && bank);
        bankDropZone.setHoverText(enabled && bank ? "Move to Bank" : null);
        propertyDropZone.setHighlight(enabled && prop);
        propertyDropZone.setHoverText(enabled && prop ? "Move to Property" : null);
    }

    private void updateEndTurnButtonStyle(int actionsLeft, boolean gameOver, boolean discardMode) {
        if (actionsLeft == lastActionsLeft
                && gameOver == lastGameOver
                && discardMode == lastDiscardMode) {
            return;
        }
        if (gameOver || discardMode) {
            btnEndTurn.setVisualState(ImageActionButton.VisualState.DISABLED);
        } else if (actionsLeft > 0) {
            btnEndTurn.setVisualState(ImageActionButton.VisualState.WAITING);
        } else {
            btnEndTurn.setVisualState(ImageActionButton.VisualState.ENABLED);
        }
        lastActionsLeft  = actionsLeft;
        lastGameOver     = gameOver;
        lastDiscardMode  = discardMode;
    }

    private Card findDraggedCard(String data) {
        if (data == null) return null;
        try {
            int cardId = Integer.parseInt(data.trim());
            Player current = mainFrame.getGameManager().getCurrentPlayer();
            if (current == null) return null;
            return current.getHand().findCard(cardId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean canDropOnBank(Card card) {
        if (card == null) return false;
        if (!(card instanceof PropertyCard)) return true;
        return ((PropertyCard) card).canBankAsMoney();
    }
}
