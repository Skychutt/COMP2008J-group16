package com.monopolydeal.gui.board;

import com.monopolydeal.gui.image.CardImageResolver;
import com.monopolydeal.gui.theme.UITheme;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.network.GameStateParser;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
import java.util.function.IntPredicate;

/**
 * Top play area: draw pile, center play zone, discard pile, current player, and steps left.
 */
public class TopStatusPanel extends BorderPane {

    private static final Color DROP_ZONE_PLAY_BG      = UITheme.DROP_ZONE;
    private static final Color DROP_ZONE_PLAY_BORDER  = UITheme.DROP_ZONE_BORDER;
    private static final Color DROP_ZONE_HOVER_BORDER = Color.rgb(126, 84, 24);
    private static final Color DROP_ZONE_DISCARD_BG   = Color.rgb(168, 56, 56);

    private final Label lblCurrentPlayer;
    private final Label lblActions;
    private final ImageView ivDrawTop;
    private final Label lblDrawCount;
    private final ImageView ivDiscardTop;
    private final Label lblDiscardCount;
    private final StackPane dropZone;
    private final Label lblDropText;

    private IntConsumer cardDropHandler;
    private IntPredicate cardDropValidator;
    private boolean gameOver = false;
    private boolean discardMode = false;

    public TopStatusPanel() {
        setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(12);
        root.setPadding(new Insets(6, 8, 6, 8));
        root.setAlignment(Pos.TOP_CENTER);
        setCenter(root);

        lblCurrentPlayer = new Label("-");
        lblCurrentPlayer.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblCurrentPlayer.setTextFill(Color.rgb(250, 241, 209));

        lblActions = new Label("Steps Left: 0");
        lblActions.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblActions.setTextFill(UITheme.ACCENT);

        HBox statusRow = new HBox(28, lblCurrentPlayer, lblActions);
        statusRow.setAlignment(Pos.CENTER);
        root.getChildren().add(statusRow);

        DeckSlot drawSlot = new DeckSlot();
        DeckSlot discardSlot = new DeckSlot();
        drawSlot.titleLabel.setText("Draw Pile");
        discardSlot.titleLabel.setText("Discard Pile");
        ivDrawTop = drawSlot.imageView;
        lblDrawCount = drawSlot.countLabel;
        ivDiscardTop = discardSlot.imageView;
        lblDiscardCount = discardSlot.countLabel;

        lblDropText = new Label("Play Area");
        lblDropText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblDropText.setTextFill(UITheme.TEXT_MAIN);

        dropZone = new StackPane(lblDropText);
        dropZone.setPrefSize(340, 140);
        dropZone.setMinSize(280, 120);
        refreshDropZoneStyle(false);
        wireDropZone();

        HBox middle = new HBox(20, drawSlot.container, dropZone, discardSlot.container);
        middle.setAlignment(Pos.CENTER);
        root.getChildren().add(middle);
    }

    public void setCardDropHandler(IntConsumer handler) {
        this.cardDropHandler = handler;
    }

    public void setCardDropValidator(IntPredicate validator) {
        this.cardDropValidator = validator;
    }

    public void showWaitingMessage(String message) {
        lblCurrentPlayer.setText(message == null ? "Waiting..." : message);
        lblActions.setText("");
        lblDrawCount.setText("");
        lblDiscardCount.setText("");
        ivDrawTop.setImage(null);
        ivDiscardTop.setImage(null);
        refreshDropZoneStyle(false);
    }

    public void updateFromSnapshot(GameStateParser.Snapshot snap,
                                   CardImageResolver resolver,
                                   boolean myTurn, boolean discardMode, int discardRemaining) {
        if (snap == null) {
            return;
        }

        this.gameOver = snap.gameOver;
        this.discardMode = discardMode;

        String currentName = snap.currentPlayer == null ? "-" : snap.currentPlayer;
        lblCurrentPlayer.setText(currentName);

        GameStateParser.PlayerInfo current = null;
        if (snap.players != null) {
            for (GameStateParser.PlayerInfo p : snap.players) {
                if (p.name != null && p.name.equals(snap.currentPlayer)) {
                    current = p;
                    break;
                }
            }
        }

        lblActions.setText("Steps Left: " + (current != null ? current.actions : 0));
        lblDrawCount.setText(String.valueOf(snap.deckSize));
        lblDiscardCount.setText(String.valueOf(snap.discardSize));
        ivDrawTop.setImage(resolver.getFallbackIcon(82, 124));
        if (snap.discardTop != null) {
            ivDiscardTop.setImage(resolver.getCardIconFromInfo(snap.discardTop, 82, 124));
        } else {
            ivDiscardTop.setImage(resolver.getFallbackIcon(82, 124));
        }
        if (!myTurn) {
            refreshDropZoneStyle(false);
        } else {
            refreshDropZoneStyle(false);
        }
    }

    public void updateTableCenter(Player currentPlayer, CardImageResolver resolver,
                                  boolean gameOver, boolean discardMode, int discardRemaining) {
        this.gameOver = gameOver;
        this.discardMode = discardMode;

        if (currentPlayer == null) {
            lblCurrentPlayer.setText("-");
            lblActions.setText("Steps Left: 0");
            lblDrawCount.setText("0");
            lblDiscardCount.setText("0");
            ivDrawTop.setImage(null);
            ivDiscardTop.setImage(null);
            refreshDropZoneStyle(false);
            return;
        }

        Deck deck = Deck.getInstance();
        lblCurrentPlayer.setText(currentPlayer.getName());
        lblActions.setText("Steps Left: " + currentPlayer.getActions());
        updateDeckPreviews(deck, resolver);
        refreshDropZoneStyle(false);
    }

    private void wireDropZone() {
        dropZone.setOnDragOver(e -> {
            if (!e.getDragboard().hasString() || gameOver) {
                e.consume();
                return;
            }

            boolean ok = canAcceptCard(e.getDragboard().getString());
            if (ok) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            refreshDropZoneStyle(ok);
            e.consume();
        });

        dropZone.setOnDragExited(e -> {
            refreshDropZoneStyle(false);
            e.consume();
        });

        dropZone.setOnDragDropped(e -> {
            refreshDropZoneStyle(false);
            boolean success = false;
            if (e.getDragboard().hasString() && canAcceptCard(e.getDragboard().getString())) {
                try {
                    int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                    if (cardDropHandler != null) {
                        cardDropHandler.accept(cardId);
                        success = true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private boolean canAcceptCard(String rawCardId) {
        if (rawCardId == null) {
            return false;
        }
        try {
            int cardId = Integer.parseInt(rawCardId.trim());
            return cardDropValidator == null || cardDropValidator.test(cardId);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void updateDeckPreviews(Deck deck, CardImageResolver resolver) {
        ivDrawTop.setImage(resolver.getFallbackIcon(82, 124));
        lblDrawCount.setText(String.valueOf(deck.drawPileSize()));

        Card discardTop = deck.getVisibleDiscardTop();
        if (discardTop == null) {
            ivDiscardTop.setImage(resolver.getFallbackIcon(82, 124));
        } else {
            ivDiscardTop.setImage(resolver.getCardIcon(discardTop, 82, 124));
        }
        lblDiscardCount.setText(String.valueOf(deck.getTotalDiscardedCount()));
    }

    private void refreshDropZoneStyle(boolean hovered) {
        if (discardMode) {
            dropZone.setStyle(
                    "-fx-background-color: " + UITheme.toCssHex(DROP_ZONE_DISCARD_BG) + ";" +
                    "-fx-border-color: " + UITheme.toCssHex(Color.rgb(176, 54, 54)) + ";" +
                    "-fx-border-width: 3px; -fx-border-radius: 8px; -fx-background-radius: 8px;"
            );
            lblDropText.setText("Discard");
            lblDropText.setTextFill(Color.WHITE);
            return;
        }

        String borderColor = hovered
                ? UITheme.toCssHex(DROP_ZONE_HOVER_BORDER)
                : UITheme.toCssHex(DROP_ZONE_PLAY_BORDER);
        String borderWidth = hovered ? "3px" : "2px";

        dropZone.setStyle(
                "-fx-background-color: " + UITheme.toCssHex(DROP_ZONE_PLAY_BG) + ";" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: " + borderWidth + ";" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;"
        );
        lblDropText.setText("Play Area");
        lblDropText.setTextFill(UITheme.TEXT_MAIN);
    }

    private static class DeckSlot {
        final VBox container;
        final ImageView imageView;
        final Label titleLabel;
        final Label countLabel;

        DeckSlot() {
            imageView = new ImageView();
            imageView.setFitWidth(82);
            imageView.setFitHeight(124);
            imageView.setPreserveRatio(false);
            imageView.setStyle(
                    "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
                    "-fx-border-width: 1px;"
            );

            titleLabel = new Label(" ");
            titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            titleLabel.setTextFill(Color.rgb(240, 230, 205));

            countLabel = new Label("0");
            countLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            countLabel.setTextFill(Color.rgb(240, 230, 205));

            container = new VBox(8, titleLabel, imageView, countLabel);
            container.setAlignment(Pos.CENTER);
            container.setStyle("-fx-background-color: transparent;");
        }
    }
}
