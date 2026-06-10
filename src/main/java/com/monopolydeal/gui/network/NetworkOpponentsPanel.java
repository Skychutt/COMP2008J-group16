package com.monopolydeal.gui.network;

import com.monopolydeal.gui.image.CardImageResolver;
import com.monopolydeal.gui.theme.UITheme;

import com.monopolydeal.network.GameStateParser;
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
 * Opponent Information Panel in LAN Online Mode
 *
 * Display public information of all non players
 */
public class NetworkOpponentsPanel extends HBox {

    private static final int ICON_W = 30;
    private static final int ICON_H = 46;

    private final NetworkGameFrame frame;

    public NetworkOpponentsPanel(NetworkGameFrame frame) {
        this.frame = frame;
        setSpacing(12);
        setPadding(new Insets(6, 0, 6, 0));
        setAlignment(Pos.TOP_LEFT);
        setStyle("-fx-background-color: transparent;");

        Label title = new Label("Opponents");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setTextFill(Color.rgb(248, 233, 191));
        getChildren().add(title);
    }

    // ─────────────────────────────────────────────────────────────────────────

    public void updateFromSnapshot(GameStateParser.Snapshot snap, int myIndex) {
        getChildren().clear();

        Label title = new Label("Opponents");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setTextFill(Color.rgb(248, 233, 191));
        getChildren().add(title);

        if (snap == null || snap.players == null) return;

        List<GameStateParser.PlayerInfo> opponents = new ArrayList<>();
        for (GameStateParser.PlayerInfo p : snap.players) {
            if (p.index != myIndex) opponents.add(p);
        }

        if (opponents.isEmpty()) {
            Label empty = new Label("(No opponents)");
            empty.setFont(UITheme.FONT_BODY);
            empty.setTextFill(Color.rgb(248, 233, 191));
            getChildren().add(empty);
            return;
        }

        for (GameStateParser.PlayerInfo opp : opponents) {
            getChildren().add(buildSeatPane(opp, opp.index == snap.turn));
        }
    }

    private VBox buildSeatPane(GameStateParser.PlayerInfo p, boolean isCurrentTurn) {
        VBox seat = new VBox(4);
        seat.setAlignment(Pos.TOP_CENTER);
        seat.setPadding(new Insets(6));
        seat.setMinWidth(180);
        seat.setMaxWidth(220);
        seat.setStyle(
                "-fx-background-color: " + UITheme.toCssHex(UITheme.PANEL_BG) + ";" +
                        "-fx-border-color: " + UITheme.toCssHex(isCurrentTurn ? UITheme.ACCENT_DARK : UITheme.BORDER) + ";" +
                        "-fx-border-width: " + (isCurrentTurn ? "2px" : "1px") + ";" +
                        "-fx-border-radius: 5px; -fx-background-radius: 5px;"
        );

        String nameText = (isCurrentTurn ? "▶ " : "") + p.name;
        Label lblName = new Label(nameText);
        lblName.setFont(UITheme.FONT_SUBTITLE);
        lblName.setTextFill(isCurrentTurn ? UITheme.ACCENT_DARK : UITheme.TEXT_MAIN);

        Label lblInfo = new Label("Hand " + p.handSize + " | Sets " + p.sets);
        lblInfo.setFont(UITheme.FONT_SMALL);
        lblInfo.setTextFill(UITheme.TEXT_SUB);

        Label lblBank = new Label("Bank: " + p.bank + "M");
        lblBank.setFont(UITheme.FONT_BANK_TOTAL);
        lblBank.setTextFill(UITheme.TEXT_MAIN);

        // Property card thumbnails
        FlowPane propFlow = new FlowPane(3, 2);
        propFlow.setPadding(new Insets(2));
        List<GameStateParser.CardInfo> propCards = new ArrayList<>();
        if (p.propertySets != null) {
            for (GameStateParser.PropertySetInfo psi : p.propertySets) {
                if (psi.cards != null) propCards.addAll(psi.cards);
            }
        }
        addCardPreviews(propFlow, propCards, 6);

        seat.getChildren().addAll(lblName, lblInfo, lblBank, propFlow);
        return seat;
    }

    private void addCardPreviews(FlowPane row, List<GameStateParser.CardInfo> cards, int maxCount) {
        if (cards == null || cards.isEmpty()) {
            Label dash = new Label("-");
            dash.setFont(UITheme.FONT_SMALL);
            dash.setTextFill(UITheme.TEXT_SUB);
            row.getChildren().add(dash);
            return;
        }

        CardImageResolver resolver = frame.getImageResolver();
        int count = Math.min(cards.size(), maxCount);
        for (int i = 0; i < count; i++) {
            GameStateParser.CardInfo c = cards.get(i);
            javafx.scene.image.Image img = resolver.getCardIconFromInfo(c, ICON_W, ICON_H);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(ICON_W);
            iv.setFitHeight(ICON_H);
            iv.setPreserveRatio(false);
            Tooltip.install(iv, new Tooltip(c.name + " (" + c.value + "M)"));
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