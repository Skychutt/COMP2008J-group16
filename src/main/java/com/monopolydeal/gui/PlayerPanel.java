package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Bottom player seat with hand cards, prompt area, and right-side shortcut dock.
 */
public class PlayerPanel extends BorderPane {

    private static final int CARD_W = 118;
    private static final int CARD_H = 178;
    private static final int CARD_STEP = CARD_W - 18;
    private static final int MIN_CARD_STEP = 18;
    private static final int LEFT_INFO_W = 146;
    private static final int DOCK_W = 132;
    private static final int DOCK_H = 56;

    private final GameFrame mainFrame;
    private final Label lblActionsTitle;
    private final Label lblActions;
    private final Label lblSetsTitle;
    private final Label lblSets;
    private final Label lblPrompt;
    private final Popup hintPopup;
    private final Pane handCanvas;
    private final ButtonDropZone bankDropZone;
    private final ButtonDropZone propertyDropZone;
    private final ImageActionButton btnEndTurn;

    private IntConsumer bankDropHandler;
    private IntConsumer propertyDropHandler;
    private Runnable endTurnHandler;

    private Player displayedPlayer;
    private boolean gameOver = false;
    private boolean discardMode = false;
    private boolean interactive = true;

    public PlayerPanel(GameFrame mainFrame) {
        this.mainFrame = mainFrame;
        setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        setPadding(new Insets(8, 0, 0, 0));

        lblActionsTitle = new Label("Steps Left");
        lblActionsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 21));
        lblActionsTitle.setTextFill(UITheme.TEXT_MAIN);

        lblActions = new Label("0");
        lblActions.setFont(Font.font("Segoe UI", FontWeight.BOLD, 38));
        lblActions.setTextFill(UITheme.ACCENT_DARK);

        lblSetsTitle = new Label("Sets");
        lblSetsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 19));
        lblSetsTitle.setTextFill(UITheme.TEXT_MAIN);

        lblSets = new Label("0");
        lblSets.setFont(Font.font("Segoe UI", FontWeight.BOLD, 34));
        lblSets.setTextFill(UITheme.ACCENT_DARK);

        lblPrompt = new Label("Hint");
        lblPrompt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblPrompt.setTextFill(Color.rgb(240, 230, 205));
        lblPrompt.setCursor(Cursor.HAND);

        hintPopup = buildHintPopup(
                "Play up to 3 cards each turn.\n" +
                "Drag cards to the center to play them.\n" +
                "Drag target actions onto an opponent.\n" +
                "Drag money, action, or bankable property cards to Bank.\n" +
                "Drag property cards to a color lane. Wilds must match the lane.\n" +
                "Hover over an opponent to preview their property area.\n" +
                "End your turn with 7 cards or fewer."
        );
        lblPrompt.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || discardMode) {
                return;
            }
            if (hintPopup.isShowing()) {
                hintPopup.hide();
            } else {
                Bounds bounds = lblPrompt.localToScreen(lblPrompt.getBoundsInLocal());
                if (bounds != null && lblPrompt.getScene() != null) {
                    hintPopup.show(lblPrompt.getScene().getWindow(), bounds.getMaxX() + 10, bounds.getMinY() - 8);
                }
            }
        });

        VBox leftInfo = new VBox(2, lblActionsTitle, lblActions, lblSetsTitle, lblSets, lblPrompt);
        leftInfo.setAlignment(Pos.CENTER_LEFT);
        leftInfo.setPadding(new Insets(0, 8, 0, 0));
        leftInfo.setPrefWidth(LEFT_INFO_W);
        leftInfo.setMinWidth(LEFT_INFO_W);
        setLeft(leftInfo);
        BorderPane.setAlignment(leftInfo, Pos.CENTER_LEFT);

        handCanvas = new Pane();
        handCanvas.setStyle("-fx-background-color: transparent;");
        handCanvas.setPrefHeight(210);
        setCenter(handCanvas);
        BorderPane.setMargin(handCanvas, new Insets(0, 12, 0, 12));

        btnEndTurn = new ImageActionButton("End Turn.png", DOCK_W, DOCK_H, true);
        btnEndTurn.setImageOffsetX(-1);
        btnEndTurn.setOnAction(e -> {
            if (!gameOver && endTurnHandler != null) {
                endTurnHandler.run();
            }
        });

        bankDropZone = new ButtonDropZone("Bank.png", DOCK_W, DOCK_H);
        bankDropZone.setImageOffsetY(6);

        propertyDropZone = new ButtonDropZone("Property.png", DOCK_W, DOCK_H);

        wireDropZones();

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox rightDock = new VBox(8, spacer, bankDropZone, propertyDropZone, btnEndTurn);
        rightDock.setAlignment(Pos.BOTTOM_CENTER);
        rightDock.setPadding(new Insets(0, 0, 6, 0));
        rightDock.setPrefWidth(DOCK_W + 4);
        rightDock.setMinWidth(DOCK_W + 4);
        rightDock.setPrefHeight(210);
        rightDock.setFillWidth(false);
        setRight(rightDock);
        BorderPane.setAlignment(rightDock, Pos.BOTTOM_RIGHT);

        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (displayedPlayer != null) {
                renderHand(displayedPlayer);
            }
        });

        updateEndTurnButtonStyle(0, false, false, true);
    }

    public void setBankDropHandler(IntConsumer h) {
        this.bankDropHandler = h;
    }

    public void setPropertyDropHandler(IntConsumer h) {
        this.propertyDropHandler = h;
    }

    public void setEndTurnHandler(Runnable h) {
        this.endTurnHandler = h;
    }

    public void updatePlayerView(Player current, boolean gameOver, boolean discardMode, int discardRemaining) {
        updatePlayerView(current, gameOver, discardMode, discardRemaining, true);
    }

    public void updatePlayerView(Player current, boolean gameOver, boolean discardMode,
                                 int discardRemaining, boolean interactive) {
        if (current == null) {
            return;
        }

        this.gameOver = gameOver;
        this.discardMode = discardMode;
        this.interactive = interactive;
        this.displayedPlayer = current;

        lblActions.setText(String.valueOf(current.getActions()));
        lblSets.setText(String.valueOf(current.getPropertyArea().countCompleteSets()));
        lblPrompt.setText(discardMode ? "Discard" : "Hint");
        if (discardMode && hintPopup.isShowing()) {
            hintPopup.hide();
        }
        refreshDropZoneHighlight(false, false);
        updateEndTurnButtonStyle(current.getActions(), gameOver, discardMode, interactive);
        renderHand(current);
    }

    private void renderHand(Player current) {
        handCanvas.getChildren().clear();

        List<Card> cards = new ArrayList<>(current.getHand().getCards());
        if (cards.isEmpty()) {
            return;
        }

        double canvasW = Math.max(420, getHandAreaWidth());
        handCanvas.setPrefWidth(canvasW);

        double step = CARD_STEP;
        if (cards.size() > 1) {
            step = Math.min(CARD_STEP, (canvasW - CARD_W - 8) / (cards.size() - 1.0));
            step = Math.max(MIN_CARD_STEP, step);
        }

        double totalW = CARD_W + Math.max(0, cards.size() - 1) * step;
        double startX = Math.max(0, (canvasW - totalW) / 2.0);
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int x = (int) Math.round(startX + i * step);
            int y = 12 + Math.abs(i - ((cards.size() - 1) / 2)) * 4;
            buildDraggableCardView(current, card, x, y);
        }
    }

    private void buildDraggableCardView(Player current, Card card, int x, int y) {
        ImageView iv = new ImageView(mainFrame.getImageResolver().getCardIcon(card, CARD_W, CARD_H));
        iv.setFitWidth(CARD_W);
        iv.setFitHeight(CARD_H);
        iv.setPreserveRatio(false);

        boolean canPlay = mainFrame.getGameLogic().getRuleValidator().canPlayCard(current, card);
        boolean canBank = mainFrame.canBankCard(card.getId());
        boolean draggable = interactive && (discardMode || canPlay || canBank);

        StackPane wrapper = new StackPane(iv);
        wrapper.setPrefSize(CARD_W, CARD_H);
        wrapper.setMaxSize(CARD_W, CARD_H);
        wrapper.setLayoutX(x);
        wrapper.setLayoutY(y);
        wrapper.setStyle(
                "-fx-border-color: " + borderColor(card, draggable, canPlay, canBank) + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 4px;" +
                "-fx-background-color: rgba(255,255,255,0.02);"
        );

        if (!draggable) {
            wrapper.setOpacity(0.55);
            wrapper.setCursor(Cursor.DEFAULT);
        } else {
            wrapper.setCursor(Cursor.HAND);
            wireCardHover(wrapper);
            wireCardDrag(wrapper, iv, card.getId());
        }

        handCanvas.getChildren().add(wrapper);
    }

    private void wireCardHover(StackPane wrapper) {
        wrapper.setOnMouseEntered(e -> {
            wrapper.toFront();
            wrapper.setScaleX(1.25);
            wrapper.setScaleY(1.25);
        });
        wrapper.setOnMouseExited(e -> {
            wrapper.setScaleX(1.0);
            wrapper.setScaleY(1.0);
        });
    }

    private void wireCardDrag(StackPane wrapper, ImageView imageView, int cardId) {
        wrapper.setOnDragDetected(e -> {
            Dragboard db = wrapper.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(cardId));

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = imageView.snapshot(params, null);
            db.setDragView(snapshot, snapshot.getWidth() / 2.0, snapshot.getHeight() / 2.0);
            db.setContent(content);
            e.consume();
        });
    }

    private void wireDropZones() {
        bankDropZone.setOnDragOver(e -> {
            if (gameOver || discardMode || !interactive) {
                e.consume();
                return;
            }
            if (e.getDragboard().hasString()) {
                Card card = findDraggedCard(e.getDragboard().getString());
                boolean ok = card != null && mainFrame.canBankCard(card.getId());
                if (ok) {
                    e.acceptTransferModes(TransferMode.COPY);
                }
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
            if (!gameOver && !discardMode && interactive && e.getDragboard().hasString()) {
                try {
                    int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                    if (bankDropHandler != null) {
                        bankDropHandler.accept(cardId);
                        success = true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });

        propertyDropZone.setOnDragOver(e -> {
            if (gameOver || discardMode || !interactive) {
                e.consume();
                return;
            }
            if (e.getDragboard().hasString()) {
                Card card = findDraggedCard(e.getDragboard().getString());
                boolean ok = card instanceof PropertyCard;
                if (ok) {
                    e.acceptTransferModes(TransferMode.COPY);
                }
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
            if (!gameOver && !discardMode && interactive && e.getDragboard().hasString()) {
                try {
                    int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                    if (propertyDropHandler != null) {
                        propertyDropHandler.accept(cardId);
                        success = true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void refreshDropZoneHighlight(boolean bank, boolean prop) {
        boolean enabled = !gameOver && !discardMode && interactive;
        bankDropZone.setHighlight(enabled && bank);
        bankDropZone.setHoverText(null);
        propertyDropZone.setHighlight(enabled && prop);
        propertyDropZone.setHoverText(null);
    }

    private void updateEndTurnButtonStyle(int actionsLeft, boolean gameOver,
                                          boolean discardMode, boolean interactive) {
        if (gameOver || discardMode || !interactive) {
            btnEndTurn.setVisualState(ImageActionButton.VisualState.DISABLED);
        } else if (actionsLeft > 0) {
            btnEndTurn.setVisualState(ImageActionButton.VisualState.WAITING);
        } else {
            btnEndTurn.setVisualState(ImageActionButton.VisualState.ENABLED);
        }
    }

    private Card findDraggedCard(String data) {
        if (data == null) {
            return null;
        }
        try {
            int cardId = Integer.parseInt(data.trim());
            Player current = mainFrame.getGameManager().getCurrentPlayer();
            if (current == null) {
                return null;
            }
            return current.getHand().findCard(cardId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String borderColor(Card card, boolean draggable, boolean canPlay, boolean canBank) {
        if (!draggable) {
            return "rgba(130,130,130,0.8)";
        }
        if (discardMode) {
            return "rgba(246,229,173,0.8)";
        }
        if (card instanceof PropertyCard && canPlay) {
            return "rgba(76,130,68,0.95)";
        }
        if (canBank) {
            return UITheme.toCssHex(UITheme.BANK_ZONE_BORDER);
        }
        return canPlay ? "rgba(246,229,173,0.9)" : "rgba(130,130,130,0.8)";
    }

    private double getHandAreaWidth() {
        double currentWidth = getWidth();
        if (currentWidth <= 0) {
            currentWidth = getPrefWidth();
        }
        double reserved = LEFT_INFO_W + DOCK_W + 44;
        return Math.max(420, currentWidth - reserved);
    }

    private Popup buildHintPopup(String text) {
        Label content = new Label(text);
        content.setWrapText(true);
        content.setMaxWidth(290);
        content.setTextFill(UITheme.TEXT_MAIN);
        content.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        content.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-padding: 12 14 12 14;" +
                "-fx-line-spacing: 1px;"
        );

        StackPane bubble = new StackPane(content);
        bubble.setStyle(
                "-fx-background-color: rgba(248,243,229,0.97);" +
                "-fx-border-color: rgba(96,74,42,0.75);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 0;"
        );

        Popup popup = new Popup();
        popup.getContent().add(bubble);
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        return popup;
    }
}
