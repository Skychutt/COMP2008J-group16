package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Left sidebar: current player's actions counter, bank total, asset previews, and event log.
 */
public class ControlPanel extends VBox {

    private final GameFrame mainFrame;
    private final Label lblActionsLeft;
    private final Label lblBankTotal;
    private final FlowPane pnlBank;
    private final FlowPane pnlProperty;
    private final TextArea txtGameLog;

    public ControlPanel(GameFrame mainFrame) {
        this.mainFrame = mainFrame;

        setSpacing(6);
        setPadding(new Insets(10));
        setPrefWidth(330);
        setStyle("-fx-background-color: transparent;");

        // Actions counter
        lblActionsLeft = new Label("Actions: 0 / 3");
        lblActionsLeft.setFont(UITheme.FONT_SUBTITLE);
        lblActionsLeft.setTextFill(UITheme.ACCENT_DARK);
        getChildren().add(lblActionsLeft);

        // Bank total
        lblBankTotal = new Label("Bank: 0M");
        lblBankTotal.setFont(UITheme.FONT_BANK_TOTAL);
        lblBankTotal.setTextFill(UITheme.TEXT_MAIN);
        getChildren().add(lblBankTotal);

        // Bank cards section
        pnlBank = createStripPanel();
        getChildren().add(createSection("Bank Cards", pnlBank, 96));

        // Property cards section
        pnlProperty = createStripPanel();
        getChildren().add(createSection("Property Cards", pnlProperty, 120));

        // Game log
        txtGameLog = new TextArea("[System] Ready.\n");
        txtGameLog.setEditable(false);
        txtGameLog.setWrapText(true);
        txtGameLog.setPrefHeight(200);
        UITheme.styleLogArea(txtGameLog);

        ScrollPane logScroll = new ScrollPane(txtGameLog);
        logScroll.setFitToWidth(true);
        logScroll.setStyle(
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 1px; -fx-border-radius: 4px; -fx-background-radius: 4px;" +
            "-fx-background-color: " + UITheme.toCssHex(UITheme.LOG_BG) + ";"
        );

        VBox logBox = new VBox(2, styledLabel("Recent Log"), logScroll);
        logBox.setStyle(UITheme.softPanelStyle());
        getChildren().add(logBox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update methods
    // ─────────────────────────────────────────────────────────────────────────

    public void updateTurnStatus(Player currentPlayer) {
        lblActionsLeft.setText(currentPlayer == null
                ? "Actions: 0 / 3"
                : "Actions: " + currentPlayer.getActions() + " / 3");
    }

    public void updateSelfAssets(Player currentPlayer) {
        pnlBank.getChildren().clear();
        pnlProperty.getChildren().clear();

        if (currentPlayer == null) {
            lblBankTotal.setText("Bank: 0M");
            return;
        }

        lblBankTotal.setText("Bank: " + currentPlayer.getBankArea().total() + "M");
        renderCardStrip(pnlBank,     new ArrayList<>(currentPlayer.getBankArea().getMoney()), 40, 60, 8);
        renderCardStrip(pnlProperty, collectPropertyCards(currentPlayer),                     40, 60, 10);
    }

    public void logEvent(String event) {
        if (event == null || event.trim().isEmpty()) return;
        txtGameLog.appendText("[Event] " + event + "\n");
        trimLogLines(16);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void renderCardStrip(FlowPane panel, List<Card> cards, int w, int h, int maxShow) {
        if (cards.isEmpty()) {
            Label none = new Label("-");
            none.setFont(UITheme.FONT_SMALL);
            none.setTextFill(UITheme.TEXT_SUB);
            panel.getChildren().add(none);
            return;
        }
        int count = Math.min(cards.size(), maxShow);
        for (int i = 0; i < count; i++) {
            Card card = cards.get(i);
            ImageView iv = new ImageView(mainFrame.getImageResolver().getCardIcon(card, w, h));
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            Tooltip.install(iv, new Tooltip(card.getName()));
            panel.getChildren().add(iv);
        }
        if (cards.size() > maxShow) {
            Label more = new Label("+" + (cards.size() - maxShow));
            more.setFont(UITheme.FONT_SMALL);
            more.setTextFill(UITheme.TEXT_SUB);
            panel.getChildren().add(more);
        }
    }

    private static List<Card> collectPropertyCards(Player player) {
        List<Card> cards = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            for (PropertyCard pc : set.getCards()) cards.add(pc);
        }
        return cards;
    }

    private static FlowPane createStripPanel() {
        FlowPane fp = new FlowPane(3, 3);
        fp.setStyle("-fx-background-color: " + UITheme.toCssRgba(UITheme.PANEL_SOFT_BG) + ";");
        return fp;
    }

    private static VBox createSection(String title, javafx.scene.Node content, double prefHeight) {
        VBox box = new VBox(2, styledLabel(title), content);
        box.setStyle(UITheme.softPanelStyle());
        box.setPrefHeight(prefHeight);
        box.setMaxHeight(prefHeight);
        return box;
    }

    private static Label styledLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setTextFill(UITheme.TEXT_SUB);
        return lbl;
    }

    private void trimLogLines(int maxLines) {
        String[] lines = txtGameLog.getText().split("\n");
        if (lines.length <= maxLines) return;
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - maxLines; i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
        }
        txtGameLog.setText(sb.toString());
    }
}
