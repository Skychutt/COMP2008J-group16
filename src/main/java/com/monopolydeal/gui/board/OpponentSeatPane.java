package com.monopolydeal.gui.board;

import com.monopolydeal.gui.image.CardImageResolver;
import com.monopolydeal.gui.theme.UITheme;

import com.monopolydeal.model.Player;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Consumer;

/**
 * A single opponent seat displayed around the top edge of the board.
 */
public class OpponentSeatPane extends VBox {

    public static final double ZONE_W = 255;
    public static final double ZONE_H = 115;

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

    public OpponentSeatPane(Player player, CardImageResolver resolver) {
        this(player, resolver, null, null, null, null);
    }

    public OpponentSeatPane(Player player, CardImageResolver resolver,
                            IntPredicate dropValidator, IntConsumer dropHandler,
                            Consumer<Player> hoverHandler, Runnable exitHandler) {
        setPrefSize(ZONE_W, ZONE_H);
        setMaxSize(ZONE_W, ZONE_H);
        setMinSize(ZONE_W, ZONE_H);
        setClip(new Rectangle(ZONE_W, ZONE_H));
        setStyle(NORMAL_STYLE);
        setSpacing(4);
        setAlignment(Pos.TOP_CENTER);

        Label name = new Label(player.getName());
        name.setFont(UITheme.FONT_TITLE);
        name.setStyle(
                "-fx-text-fill: #fff8e0;" +
                "-fx-background-color: rgba(200,150,40,0.30);" +
                "-fx-padding: 1 8; -fx-background-radius: 4;"
        );

        int sets = player.getPropertyArea().countCompleteSets();
        int visibleHand = player.getVisibleHandSize();
        Label stats = new Label(
                "Hand: " + visibleHand +
                        "   Bank: " + player.getBankArea().total() + "M" +
                        "   Sets: " + sets + "/3"
        );
        stats.setFont(UITheme.FONT_SUBTITLE);
        stats.setStyle("-fx-text-fill: #b8e8c0;");

        HBox handRow = buildHandRow(visibleHand, resolver);
        getChildren().addAll(name, stats, handRow);
        wireDropTarget(dropValidator, dropHandler);
        wireHoverPreview(player, hoverHandler, exitHandler);
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

    private int parseCardId(String rawCardId) {
        if (rawCardId == null) {
            return -1;
        }
        try {
            return Integer.parseInt(rawCardId.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
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
        return row;
    }

    private void wireHoverPreview(Player player, Consumer<Player> hoverHandler, Runnable exitHandler) {
        if (hoverHandler != null) {
            setOnMouseEntered(e -> hoverHandler.accept(player));
        }
        if (exitHandler != null) {
            setOnMouseExited(e -> exitHandler.run());
        }
    }
}
