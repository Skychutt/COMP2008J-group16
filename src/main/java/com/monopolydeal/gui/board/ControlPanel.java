package com.monopolydeal.gui.board;

import com.monopolydeal.gui.theme.UITheme;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Left-side Bank area for the local game board.
 */
public class ControlPanel extends VBox {

    private static final int CARD_W = 108;
    private static final int CARD_H = 162;

    private final GamePanelHost mainFrame;
    private final Label lblBankTotal;
    private final VBox cardList;
    private final ScrollPane scrollPane;

    public ControlPanel(GamePanelHost mainFrame) {
        this.mainFrame = mainFrame;

        setSpacing(10);
        setPadding(new Insets(12));
        setPrefWidth(170);
        setAlignment(Pos.TOP_CENTER);
        setStyle(UITheme.panelBorderStyle());

        Label title = new Label("Bank");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(UITheme.TEXT_MAIN);

        lblBankTotal = new Label("0M");
        lblBankTotal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblBankTotal.setTextFill(UITheme.ACCENT_DARK);

        cardList = new VBox(8);
        cardList.setAlignment(Pos.TOP_CENTER);

        scrollPane = new ScrollPane(cardList);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-width: 0;");
        scrollPane.setPrefViewportHeight(430);
        scrollPane.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                javafx.scene.Node viewport = scrollPane.lookup(".viewport");
                if (viewport != null) {
                    viewport.setStyle("-fx-background-color: transparent;");
                }
            }
        });

        wireBankDropTarget();
        getChildren().addAll(title, lblBankTotal, scrollPane);
    }

    public void updateSelfAssets(Player currentPlayer) {
        cardList.getChildren().clear();

        if (currentPlayer == null) {
            lblBankTotal.setText("0M");
            return;
        }

        lblBankTotal.setText(currentPlayer.getBankArea().total() + "M");

        List<Card> cards = new ArrayList<>(currentPlayer.getBankArea().getMoney());
        cards.sort(Comparator.comparingInt(Card::getValue)
                .thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER));

        for (Card card : cards) {
            cardList.getChildren().add(buildBankCard(card));
        }
    }

    private StackPane buildBankCard(Card card) {
        ImageView iv = new ImageView(mainFrame.getImageResolver().getCardIcon(card, CARD_W, CARD_H));
        iv.setFitWidth(CARD_W);
        iv.setFitHeight(CARD_H);
        iv.setPreserveRatio(false);

        Rectangle shade = new Rectangle(CARD_W, CARD_H, Color.rgb(0, 0, 0, 0.14));
        shade.setArcWidth(10);
        shade.setArcHeight(10);

        Label value = new Label(String.valueOf(card.getValue()));
        value.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 34));
        value.setTextFill(Color.rgb(255, 250, 224));
        value.setStyle(
                "-fx-background-color: rgba(28, 22, 14, 0.60);" +
                "-fx-background-radius: 999px;" +
                "-fx-padding: 6 15 6 15;"
        );

        StackPane cardPane = new StackPane(iv, shade, value);
        cardPane.setPrefSize(CARD_W, CARD_H);
        cardPane.setMaxSize(CARD_W, CARD_H);
        cardPane.setStyle(
                "-fx-background-color: rgba(255,255,255,0.24);" +
                "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
                "-fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;"
        );
        return cardPane;
    }

    private void wireBankDropTarget() {
        setOnDragOver(e -> {
            if (e.getDragboard().hasString() && canAcceptDrop(e.getDragboard().getString())) {
                e.acceptTransferModes(TransferMode.COPY);
                setStyle(
                        "-fx-background-color: " + UITheme.toCssRgba(UITheme.BANK_ZONE_ACTIVE) + ";" +
                        "-fx-border-color: " + UITheme.toCssHex(UITheme.BANK_ZONE_BORDER) + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 4px;" +
                        "-fx-background-radius: 4px;"
                );
            }
            e.consume();
        });

        setOnDragExited(e -> {
            setStyle(UITheme.panelBorderStyle());
            e.consume();
        });

        setOnDragDropped(e -> {
            setStyle(UITheme.panelBorderStyle());
            boolean success = false;
            if (e.getDragboard().hasString() && canAcceptDrop(e.getDragboard().getString())) {
                try {
                    int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                    mainFrame.bankCardById(cardId);
                    success = true;
                } catch (NumberFormatException ignored) {
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private boolean canAcceptDrop(String rawCardId) {
        if (rawCardId == null) {
            return false;
        }
        try {
            int cardId = Integer.parseInt(rawCardId.trim());
            return mainFrame.canBankCard(cardId);
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}