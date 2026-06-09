package com.monopolydeal.gui.network;

import com.monopolydeal.gui.image.CardImageResolver;
import com.monopolydeal.gui.board.OpponentSeatPane;
import com.monopolydeal.gui.theme.UITheme;

import com.monopolydeal.network.GameStateParser;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * Opponent seat in LAN mode — snapshot-driven, supports targeted card drops.
 */
public class NetworkOpponentSeatPane extends VBox {

    private static final String NORMAL_STYLE =
            "-fx-background-color: rgba(12,38,22,0.95);" +
            "-fx-border-color: rgba(210,165,70,0.88);" +
            "-fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
            "-fx-padding: 6 10 5 10;";

    private static final String ACTIVE_DROP_STYLE =
            "-fx-background-color: rgba(28,62,36,0.97);" +
            "-fx-border-color: rgba(255,222,120,0.98);" +
            "-fx-border-width: 3px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
            "-fx-padding: 6 10 5 10;";

    public NetworkOpponentSeatPane(GameStateParser.PlayerInfo player,
                                   CardImageResolver resolver,
                                   boolean isCurrentTurn) {
        this(player, resolver, isCurrentTurn, null, null);
    }

    public NetworkOpponentSeatPane(GameStateParser.PlayerInfo player,
                                   CardImageResolver resolver,
                                   boolean isCurrentTurn,
                                   IntPredicate dropValidator,
                                   IntConsumer dropHandler) {
        double zoneW = OpponentSeatPane.ZONE_W;
        double zoneH = OpponentSeatPane.ZONE_H;
        setPrefSize(zoneW, zoneH);
        setMaxSize(zoneW, zoneH);
        setStyle(isCurrentTurn
                ? "-fx-background-color: rgba(12,38,22,0.95);" +
                "-fx-border-color: rgba(238,190,82,0.95);" +
                "-fx-border-width: 3px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                "-fx-padding: 6 10 5 10;"
                : NORMAL_STYLE);
        setSpacing(4);
        setAlignment(Pos.TOP_CENTER);

        String nameText = (isCurrentTurn ? "▶ " : "") + player.name;
        Label name = new Label(nameText);
        name.setFont(UITheme.FONT_TITLE);
        name.setStyle(
            "-fx-text-fill: " + (isCurrentTurn ? "#ffe87a" : "#fff8e0") + ";" +
            "-fx-background-color: rgba(200,150,40,0.30);" +
            "-fx-padding: 1 8; -fx-background-radius: 4;"
        );

        Label stats = new Label(
            "Hand: " + player.handSize +
            "   Bank: " + player.bank + "M" +
            "   Sets: " + player.sets + "/3"
        );
        stats.setFont(UITheme.FONT_SUBTITLE);
        stats.setStyle("-fx-text-fill: #b8e8c0;");

        HBox handRow = buildHandRow(player.handSize, resolver);
        List<GameStateParser.CardInfo> props = collectProps(player);
        FlowPane propRow = new FlowPane(3, 0);
        propRow.setAlignment(Pos.CENTER);
        int maxShow = Math.min(props.size(), 6);
        for (int i = 0; i < maxShow; i++) {
            GameStateParser.CardInfo c = props.get(i);
            ImageView iv = new ImageView(resolver.getCardIconFromInfo(c, 26, 40));
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
        wireDropTarget(dropValidator, dropHandler);
    }

    private void wireDropTarget(IntPredicate dropValidator, IntConsumer dropHandler) {
        if (dropValidator == null || dropHandler == null) {
            return;
        }

        setOnDragOver(e -> {
            if (!e.getDragboard().hasString()) {
                e.consume();
                return;
            }
            int cardId = parseCardId(e.getDragboard().getString());
            boolean ok = cardId >= 0 && dropValidator.test(cardId);
            if (ok) {
                e.acceptTransferModes(TransferMode.COPY);
                setStyle(ACTIVE_DROP_STYLE);
            }
            e.consume();
        });

        setOnDragExited(e -> {
            setStyle(NORMAL_STYLE);
            e.consume();
        });

        setOnDragDropped(e -> {
            setStyle(NORMAL_STYLE);
            boolean success = false;
            int cardId = parseCardId(e.getDragboard().getString());
            if (cardId >= 0 && dropValidator.test(cardId)) {
                dropHandler.accept(cardId);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private static int parseCardId(String rawCardId) {
        if (rawCardId == null) {
            return -1;
        }
        try {
            return Integer.parseInt(rawCardId.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
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
                if (psi.cards != null) {
                    cards.addAll(psi.cards);
                }
            }
        }
        return cards;
    }
}
