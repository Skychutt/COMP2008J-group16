package com.monopolydeal.gui;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.network.GameStateParser;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.IntConsumer;

/**
 * Center table hub: deck/discard previews, current player status, drag-to-play drop zone.
 */
public class TopStatusPanel extends BorderPane {

    private static final Color DROP_ZONE_PLAY_BG       = UITheme.DROP_ZONE;
    private static final Color DROP_ZONE_PLAY_BORDER   = UITheme.DROP_ZONE_BORDER;
    private static final Color DROP_ZONE_HOVER_BORDER  = Color.rgb(126, 84, 24);
    private static final Color DROP_ZONE_DISCARD_BG    = Color.rgb(168, 56, 56);
    private static final Color DROP_ZONE_DISCARD_TEXT  = Color.WHITE;
    private static final Color DROP_ZONE_DISCARD_HINT  = Color.rgb(255, 235, 235);

    private final Label lblCurrentPlayer;
    private final Label lblActions;
    private final ImageView ivDrawTop;
    private final Label lblDrawCount;
    private final ImageView ivDiscardTop;
    private final Label lblDiscardCount;
    private final StackPane dropZone;
    private final Label lblDropText;
    private final Label lblDiscardHint;
    private final Label lblRecentEvent;

    private IntConsumer cardDropHandler;
    private boolean gameOver   = false;
    private boolean discardMode = false;
    private int discardRemaining = 0;

    public TopStatusPanel() {
        setStyle("-fx-background-color: transparent;");

        // ── Hub wrapper — transparent to blend with the felt ──
        VBox hub = new VBox(8);
        hub.setStyle("-fx-background-color: transparent; -fx-padding: 8;");
        setCenter(hub);

        // ── Status row ──
        lblCurrentPlayer = new Label("Current Player: -");
        lblCurrentPlayer.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblCurrentPlayer.setTextFill(Color.rgb(250, 241, 209));

        lblActions = new Label("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_TITLE);
        lblActions.setTextFill(UITheme.ACCENT);

        Label separator = new Label("|");
        separator.setTextFill(Color.rgb(200, 190, 155));

        HBox statusRow = new HBox(16, lblCurrentPlayer, separator, lblActions);
        statusRow.setAlignment(Pos.CENTER);
        hub.getChildren().add(statusRow);

        // ── Middle row: draw pile | drop zone | discard pile ──
        DeckSlot drawSlot    = new DeckSlot("Draw Pile");
        DeckSlot discardSlot = new DeckSlot("Discard Top");
        ivDrawTop    = drawSlot.imageView;
        lblDrawCount = drawSlot.countLabel;
        ivDiscardTop    = discardSlot.imageView;
        lblDiscardCount = discardSlot.countLabel;

        // Drop zone — initialize labels BEFORE calling refreshDropZoneStyle()
        lblDropText = new Label("Drag Card Here To Play");
        lblDropText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblDropText.setTextFill(UITheme.TEXT_MAIN);

        lblDiscardHint = new Label(" ");
        lblDiscardHint.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblDiscardHint.setTextFill(DROP_ZONE_DISCARD_HINT);

        dropZone = new StackPane();
        dropZone.setPrefSize(430, 168);
        dropZone.setMinSize(300, 120);
        dropZone.getChildren().add(lblDropText);
        StackPane.setAlignment(lblDropText, Pos.CENTER);
        refreshDropZoneStyle(false);

        // Hook drag-and-drop on the drop zone
        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasString() && !gameOver) {
                e.acceptTransferModes(TransferMode.COPY);
                refreshDropZoneStyle(true);
            }
            e.consume();
        });
        dropZone.setOnDragExited(e -> {
            refreshDropZoneStyle(false);
            e.consume();
        });
        dropZone.setOnDragDropped(e -> {
            refreshDropZoneStyle(false);
            boolean success = false;
            if (e.getDragboard().hasString()) {
                try {
                    int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                    if (cardDropHandler != null) cardDropHandler.accept(cardId);
                    success = true;
                } catch (NumberFormatException ignored) {}
            }
            e.setDropCompleted(success);
            e.consume();
        });

        lblRecentEvent = new Label("Recent: -");
        lblRecentEvent.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblRecentEvent.setTextFill(Color.rgb(210, 230, 210));

        VBox centerPlay = new VBox(10, dropZone, lblDiscardHint, lblRecentEvent);
        centerPlay.setAlignment(Pos.CENTER);
        centerPlay.setStyle("-fx-background-color: transparent;");

        HBox middle = new HBox(12, drawSlot.container, centerPlay, discardSlot.container);
        middle.setAlignment(Pos.CENTER);
        hub.getChildren().add(middle);
    }

    // ─────────────────────────────────────────────────────────────────────────

    public void setCardDropHandler(IntConsumer handler) {
        this.cardDropHandler = handler;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAN network mode helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Display waiting prompt
     */
    public void showWaitingMessage(String message) {
        lblCurrentPlayer.setText(message == null ? "Waiting..." : message);
        lblActions.setText("");
        lblDrawCount.setText("");
        lblDiscardCount.setText("");
        ivDrawTop.setImage(null);
        ivDiscardTop.setImage(null);
        refreshDropZoneStyle(false);
        lblRecentEvent.setText("");
    }

    /**
     * Update panel based on network status snapshot
     */
    public void updateFromSnapshot(GameStateParser.Snapshot snap,
                                   CardImageResolver resolver,
                                   boolean myTurn, boolean discardMode, int discardRemaining) {
        if (snap == null) return;

        this.gameOver         = snap.gameOver;
        this.discardMode      = discardMode;
        this.discardRemaining = discardRemaining;

        String currentName = snap.currentPlayer == null ? "-" : snap.currentPlayer;
        lblCurrentPlayer.setText("Current Player: " + currentName);

        GameStateParser.PlayerInfo current = null;
        if (snap.players != null) {
            for (GameStateParser.PlayerInfo p : snap.players) {
                if (p.name != null && p.name.equals(snap.currentPlayer)) {
                    current = p;
                    break;
                }
            }
        }
        lblActions.setText("Actions: " + (current != null ? current.actions : 0) + " / 3");
        lblDrawCount.setText("Remaining: " + snap.deckSize);
        lblDiscardCount.setText("");
        ivDrawTop.setImage(resolver.getFallbackIcon(76, 118));
        ivDiscardTop.setImage(null);
        refreshDropZoneStyle(false);
        lblRecentEvent.setText(myTurn ? "Your turn!" : "Waiting for " + currentName + "...");
    }

    public void updateTableCenter(Player currentPlayer, CardImageResolver resolver,
                                  String latestEvent, boolean gameOver,
                                  boolean discardMode, int discardRemaining) {
        this.gameOver        = gameOver;
        this.discardMode     = discardMode;
        this.discardRemaining = discardRemaining;

        if (currentPlayer == null) {
            showEmptyTableState(latestEvent);
            return;
        }

        Deck deck = Deck.getInstance();
        lblCurrentPlayer.setText("Current Player: " + currentPlayer.getName());
        lblActions.setText("Actions: " + currentPlayer.getActions() + " / 3");
        updateDeckPreviews(deck, resolver);
        refreshDropZoneStyle(false);
        setRecentEventText(latestEvent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void showEmptyTableState(String latestEvent) {
        lblCurrentPlayer.setText("Current Player: -");
        lblActions.setText("Actions: 0 / 3");
        lblDrawCount.setText("Remaining: 0");
        lblDiscardCount.setText("Total discarded: 0");
        ivDrawTop.setImage(null);
        ivDiscardTop.setImage(null);
        refreshDropZoneStyle(false);
        setRecentEventText(latestEvent);
    }

    private void updateDeckPreviews(Deck deck, CardImageResolver resolver) {
        int remaining = deck.drawPileSize();
        ivDrawTop.setImage(resolver.getFallbackIcon(76, 118));
        Tooltip.install(ivDrawTop, new Tooltip("Draw pile — " + remaining + " card(s) left"));
        lblDrawCount.setText("Remaining: " + remaining);

        int totalDiscarded = deck.getTotalDiscardedCount();
        lblDiscardCount.setText("Total discarded: " + totalDiscarded);

        Card discardTop = deck.getVisibleDiscardTop();
        if (discardTop == null) {
            ivDiscardTop.setImage(resolver.getFallbackIcon(76, 118));
            Tooltip.install(ivDiscardTop, new Tooltip("No cards discarded yet"));
        } else {
            ivDiscardTop.setImage(resolver.getCardIcon(discardTop, 76, 118));
            String note = deck.discardSize() > 0
                    ? "Top of discard pile"
                    : "Last discarded (placed under draw pile)";
            Tooltip.install(ivDiscardTop, new Tooltip(discardTop.getName() + " — " + note));
        }
    }

    private void refreshDropZoneStyle(boolean hovered) {
        if (discardMode) {
            dropZone.setStyle(
                "-fx-background-color: " + UITheme.toCssHex(DROP_ZONE_DISCARD_BG) + ";" +
                "-fx-border-color: " + UITheme.toCssHex(Color.rgb(176, 54, 54)) + ";" +
                "-fx-border-width: 3px; -fx-border-radius: 6px; -fx-background-radius: 6px;"
            );
            lblDropText.setText("Drag Here To Discard");
            lblDropText.setTextFill(DROP_ZONE_DISCARD_TEXT);
            lblDiscardHint.setText(discardRemaining > 0
                    ? "You still need to discard " + discardRemaining + " card(s)."
                    : "Discard the extra cards.");
            lblDiscardHint.setTextFill(DROP_ZONE_DISCARD_HINT);
        } else {
            String borderColor = hovered
                    ? UITheme.toCssHex(DROP_ZONE_HOVER_BORDER)
                    : UITheme.toCssHex(DROP_ZONE_PLAY_BORDER);
            String borderWidth = hovered ? "3px" : "2px";
            dropZone.setStyle(
                "-fx-background-color: " + UITheme.toCssHex(DROP_ZONE_PLAY_BG) + ";" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: " + borderWidth + "; -fx-border-radius: 6px; -fx-background-radius: 6px;"
            );
            lblDropText.setText("Drag Card Here To Play");
            lblDropText.setTextFill(UITheme.TEXT_MAIN);
            lblDiscardHint.setText(" ");
        }
    }

    private void setRecentEventText(String latestEvent) {
        String event = latestEvent == null ? "-" : latestEvent.trim();
        if (event.isEmpty()) event = "-";
        if (event.length() > 84) event = event.substring(0, 84) + "...";
        lblRecentEvent.setText("Recent: " + event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner class: deck slot
    // ─────────────────────────────────────────────────────────────────────────

    private static class DeckSlot {
        final VBox container;
        final ImageView imageView;
        final Label countLabel;

        DeckSlot(String title) {
            imageView = new ImageView();
            imageView.setFitWidth(76);
            imageView.setFitHeight(118);
            imageView.setPreserveRatio(false);
            imageView.setStyle(
                "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
                "-fx-border-width: 1px;"
            );

            Label titleLabel = new Label(title);
            titleLabel.setFont(UITheme.FONT_SUBTITLE);
            titleLabel.setTextFill(Color.rgb(240, 230, 205));

            countLabel = new Label(" ");
            countLabel.setFont(UITheme.FONT_SUBTITLE);
            countLabel.setTextFill(Color.rgb(210, 220, 195));

            container = new VBox(5, titleLabel, imageView, countLabel);
            container.setAlignment(Pos.CENTER);
            container.setStyle("-fx-background-color: transparent;");
        }
    }
}
