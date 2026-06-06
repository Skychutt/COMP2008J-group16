package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * A single opponent seat displayed around the circular table.
 * The parent (GameBoardPane) rotates this pane to face the center.
 */
public class OpponentSeatPane extends VBox {

    static final double ZONE_W = 255;
    static final double ZONE_H = 115;

    public OpponentSeatPane(Player player, CardImageResolver resolver) {
        setPrefSize(ZONE_W, ZONE_H);
        setMaxSize(ZONE_W, ZONE_H);
        setStyle(
            "-fx-background-color: rgba(12,38,22,0.95);" +
            "-fx-border-color: rgba(210,165,70,0.88);" +
            "-fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
            "-fx-padding: 6 10 5 10;"
        );
        setSpacing(4);
        setAlignment(Pos.TOP_CENTER);

        // Name badge
        Label name = new Label(player.getName());
        name.setFont(UITheme.FONT_TITLE);
        name.setStyle(
            "-fx-text-fill: #fff8e0;" +
            "-fx-background-color: rgba(200,150,40,0.30);" +
            "-fx-padding: 1 8; -fx-background-radius: 4;"
        );

        // Stats row
        int sets = player.getPropertyArea().countCompleteSets();
        Label stats = new Label(
            "Hand: " + player.getHand().size() +
            "   Bank: " + player.getBankArea().total() + "M" +
            "   Sets: " + sets + "/3"
        );
        stats.setFont(UITheme.FONT_SUBTITLE);
        stats.setStyle("-fx-text-fill: #b8e8c0;");

        // Face-down hand cards
        HBox handRow = buildHandRow(player.getHand().size(), resolver);

        // Property card previews
        List<Card> props = collectPropertyCards(player);
        FlowPane propRow = new FlowPane(3, 0);
        propRow.setAlignment(Pos.CENTER);
        int maxProps = Math.min(props.size(), 6);
        for (int i = 0; i < maxProps; i++) {
            ImageView iv = new ImageView(resolver.getCardIcon(props.get(i), 26, 40));
            iv.setFitWidth(26);
            iv.setFitHeight(40);
            Tooltip.install(iv, new Tooltip(props.get(i).getName()));
            propRow.getChildren().add(iv);
        }
        if (props.size() > 6) {
            Label more = new Label("+" + (props.size() - 6));
            more.setFont(UITheme.FONT_SUBTITLE);
            more.setStyle("-fx-text-fill: #d0e8b8;");
            propRow.getChildren().add(more);
        }
        if (props.isEmpty()) {
            Label noProps = new Label("No properties yet");
            noProps.setFont(UITheme.FONT_BODY);
            noProps.setStyle("-fx-text-fill: #7db890;");
            propRow.getChildren().add(noProps);
        }

        getChildren().addAll(name, stats, handRow, propRow);
    }

    private HBox buildHandRow(int count, CardImageResolver resolver) {
        HBox row = new HBox(-8);
        row.setAlignment(Pos.CENTER);
        int show = Math.min(count, 8);
        for (int i = 0; i < show; i++) {
            ImageView iv = new ImageView(resolver.getFallbackIcon(28, 43));
            iv.setFitWidth(28);
            iv.setFitHeight(43);
            iv.setOpacity(0.85);
            row.getChildren().add(iv);
        }
        if (count > 8) {
            Label more = new Label("+" + (count - 8));
            more.setFont(UITheme.FONT_SUBTITLE);
            more.setStyle("-fx-text-fill: #d0e8b8;");
            row.getChildren().add(more);
        }
        if (count == 0) {
            Label empty = new Label("(no cards)");
            empty.setFont(UITheme.FONT_BODY);
            empty.setStyle("-fx-text-fill: #7db890;");
            row.getChildren().add(empty);
        }
        return row;
    }

    private static List<Card> collectPropertyCards(Player player) {
        List<Card> cards = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            for (PropertyCard pc : set.getCards()) cards.add(pc);
        }
        return cards;
    }
}
