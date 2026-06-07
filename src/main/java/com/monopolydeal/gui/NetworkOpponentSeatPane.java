package com.monopolydeal.gui;

import com.monopolydeal.network.GameStateParser;

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
 * Opponent seat displayed on the circular table in LAN mode.
 * Mirrors OpponentSeatPane visually but is driven by a GameStateParser.PlayerInfo
 * snapshot instead of a live Player object.
 */
public class NetworkOpponentSeatPane extends VBox {

    public NetworkOpponentSeatPane(GameStateParser.PlayerInfo player,
                                   CardImageResolver resolver,
                                   boolean isCurrentTurn) {
        double zoneW = OpponentSeatPane.ZONE_W;
        double zoneH = OpponentSeatPane.ZONE_H;
        setPrefSize(zoneW, zoneH);
        setMaxSize(zoneW, zoneH);

        String borderColor = isCurrentTurn ? "rgba(238,190,82,0.95)" : "rgba(210,165,70,0.88)";
        int borderWidth    = isCurrentTurn ? 3 : 2;
        setStyle(
            "-fx-background-color: rgba(12,38,22,0.95);" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: " + borderWidth + "px;" +
            "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
            "-fx-padding: 6 10 5 10;"
        );
        setSpacing(4);
        setAlignment(Pos.TOP_CENTER);

        // Name
        String nameText = (isCurrentTurn ? "▶ " : "") + player.name;
        Label name = new Label(nameText);
        name.setFont(UITheme.FONT_TITLE);
        name.setStyle(
            "-fx-text-fill: " + (isCurrentTurn ? "#ffe87a" : "#fff8e0") + ";" +
            "-fx-background-color: rgba(200,150,40,0.30);" +
            "-fx-padding: 1 8; -fx-background-radius: 4;"
        );

        // Stats
        Label stats = new Label(
            "Hand: " + player.handSize +
            "   Bank: " + player.bank + "M" +
            "   Sets: " + player.sets + "/3"
        );
        stats.setFont(UITheme.FONT_SUBTITLE);
        stats.setStyle("-fx-text-fill: #b8e8c0;");

        // Face-down hand cards
        HBox handRow = buildHandRow(player.handSize, resolver);

        // Property card previews
        List<GameStateParser.CardInfo> props = collectProps(player);
        FlowPane propRow = new FlowPane(3, 0);
        propRow.setAlignment(Pos.CENTER);
        int maxShow = Math.min(props.size(), 6);
        for (int i = 0; i < maxShow; i++) {
            GameStateParser.CardInfo c = props.get(i);
            javafx.scene.image.Image img = resolver.getCardIconFromInfo(c, 26, 40);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(26);
            iv.setFitHeight(40);
            Tooltip.install(iv, new Tooltip(c.name));
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

    private static HBox buildHandRow(int count, CardImageResolver resolver) {
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

    private static List<GameStateParser.CardInfo> collectProps(GameStateParser.PlayerInfo player) {
        List<GameStateParser.CardInfo> cards = new ArrayList<>();
        if (player.propertySets != null) {
            for (GameStateParser.PropertySetInfo psi : player.propertySets) {
                if (psi.cards != null) cards.addAll(psi.cards);
            }
        }
        return cards;
    }
}
