package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Top row of opponent seats.
 */
public class OpponentsPanel extends VBox {

    private final GameFrame mainFrame;
    private final HBox seatRow;

    public OpponentsPanel(GameFrame mainFrame) {
        this.mainFrame = mainFrame;
        setStyle("-fx-background-color: transparent;");
        setSpacing(4);

        Label title = new Label("Opponents");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setTextFill(Color.rgb(248, 233, 191));
        getChildren().add(title);

        seatRow = new HBox(12);
        seatRow.setAlignment(Pos.TOP_LEFT);
        getChildren().add(seatRow);
    }

    public void updateOpponents(List<Player> players, Player current) {
        seatRow.getChildren().clear();

        List<Player> opponents = new ArrayList<>();
        for (Player p : players) {
            if (p != current) opponents.add(p);
        }

        if (opponents.isEmpty()) {
            Label empty = new Label("(No opponents)");
            empty.setFont(UITheme.FONT_BODY);
            empty.setTextFill(Color.rgb(248, 233, 191));
            seatRow.getChildren().add(empty);
        } else {
            for (Player opp : opponents) {
                seatRow.getChildren().add(buildSeatPanel(opp));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildSeatPanel(Player opponent) {
        VBox seat = new VBox(4);
        seat.setStyle(
            "-fx-background-color: " + UITheme.toCssRgba(UITheme.PANEL_BG) + ";" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 1px; -fx-border-radius: 4px; -fx-background-radius: 4px;" +
            "-fx-padding: 6 8 6 8;"
        );
        seat.setPrefWidth(220);
        seat.setMinWidth(180);

        // Header: name + brief stats
        Label header = new Label(opponent.getName());
        header.setFont(UITheme.FONT_SUBTITLE);
        header.setTextFill(UITheme.TEXT_MAIN);

        Label info = new Label(
            "Hand: " + opponent.getHand().size() +
            "  |  Sets: " + opponent.getPropertyArea().countCompleteSets()
        );
        info.setFont(UITheme.FONT_SMALL);
        info.setTextFill(UITheme.TEXT_SUB);
        seat.getChildren().addAll(header, info);

        // Bank section
        VBox bankSection = new VBox(2);
        bankSection.setStyle(UITheme.softPanelStyle());
        Label bankTitle = new Label("Bank: " + opponent.getBankArea().total() + "M");
        bankTitle.setFont(UITheme.FONT_SUBTITLE);
        bankTitle.setTextFill(UITheme.TEXT_MAIN);
        FlowPane bankCards = new FlowPane(3, 0);
        addCardsPreview(bankCards, opponent.getBankArea().getMoney(), 34, 52, 4);
        bankSection.getChildren().addAll(bankTitle, bankCards);
        seat.getChildren().add(bankSection);

        return seat;
    }

    private void addCardsPreview(FlowPane row, List<? extends Card> cards, int w, int h, int maxCount) {
        if (cards == null || cards.isEmpty()) {
            Label empty = new Label("-");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setTextFill(UITheme.TEXT_SUB);
            row.getChildren().add(empty);
            return;
        }
        int count = Math.min(cards.size(), maxCount);
        for (int i = 0; i < count; i++) {
            Card card = cards.get(i);
            ImageView iv = new ImageView(mainFrame.getImageResolver().getCardIcon(card, w, h));
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            Tooltip.install(iv, new Tooltip(card.getName()));
            row.getChildren().add(iv);
        }
        if (cards.size() > maxCount) {
            Label more = new Label("+" + (cards.size() - maxCount));
            more.setFont(UITheme.FONT_SMALL);
            more.setTextFill(UITheme.TEXT_SUB);
            row.getChildren().add(more);
        }
    }
}
