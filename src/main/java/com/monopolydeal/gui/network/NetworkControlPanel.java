package com.monopolydeal.gui.network;

import com.monopolydeal.gui.theme.UITheme;

import com.monopolydeal.network.GameStateParser;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Left asset panel in LAN online mode
 *
 * Display the player's action count, bank balance, bank card, real estate group, and event log
 * 
 */
public class NetworkControlPanel extends VBox {

    private final NetworkGameFrame frame;
    private final Label lblActions;
    private final Label lblBank;
    private final VBox pnlBankCards;
    private final VBox pnlPropertySets;
    private final TextArea txtLog;

    public NetworkControlPanel(NetworkGameFrame frame) {
        this.frame = frame;
        setSpacing(6);
        setPadding(new Insets(8, 6, 6, 6));
        setStyle("-fx-background-color: transparent;");

        lblActions = new Label("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_SUBTITLE);
        lblActions.setTextFill(UITheme.ACCENT_DARK);

        lblBank = new Label("Bank: 0M");
        lblBank.setFont(UITheme.FONT_BANK_TOTAL);
        lblBank.setTextFill(UITheme.TEXT_MAIN);

        pnlBankCards = new VBox(2);
        pnlBankCards.setStyle(
            "-fx-background-color: " + UITheme.toCssHex(UITheme.PANEL_BG) + ";" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 1px; -fx-border-radius: 3px; -fx-padding: 4;"
        );
        ScrollPane bankScroll = new ScrollPane(pnlBankCards);
        bankScroll.setMaxHeight(80);
        bankScroll.setFitToWidth(true);
        bankScroll.setStyle("-fx-background-color: transparent;");

        pnlPropertySets = new VBox(2);
        pnlPropertySets.setStyle(
            "-fx-background-color: " + UITheme.toCssHex(UITheme.PANEL_BG) + ";" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 1px; -fx-border-radius: 3px; -fx-padding: 4;"
        );
        ScrollPane propScroll = new ScrollPane(pnlPropertySets);
        propScroll.setMaxHeight(120);
        propScroll.setFitToWidth(true);
        propScroll.setStyle("-fx-background-color: transparent;");

        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setWrapText(true);
        txtLog.setPrefRowCount(10);
        txtLog.setStyle(
            "-fx-background-color: " + UITheme.toCssHex(UITheme.LOG_BG) + ";" +
            "-fx-text-fill: " + UITheme.toCssHex(UITheme.LOG_TEXT) + ";" +
            "-fx-font-size: 11;"
        );

        Label lblBankTitle = sectionTitle("Bank Cards");
        Label lblPropTitle = sectionTitle("Properties");
        Label lblLogTitle  = sectionTitle("Event Log");

        getChildren().addAll(
                lblActions, lblBank,
                lblBankTitle, bankScroll,
                lblPropTitle, propScroll,
                lblLogTitle, txtLog
        );
    }

    // ─────────────────────────────────────────────────────────────────────────

    public void updateFromSnapshot(GameStateParser.Snapshot snap, int myIndex) {
        if (snap == null) return;
        GameStateParser.PlayerInfo me = snap.getMyInfo(myIndex);
        if (me == null) return;

        lblActions.setText("Actions: " + me.actions + " / 3");
        lblBank.setText("Bank: " + me.bank + "M");

        pnlBankCards.getChildren().clear();
        if (me.bankCards != null) {
            for (GameStateParser.CardInfo c : me.bankCards) {
                Label lbl = new Label(c.name + " (" + c.value + "M)");
                lbl.setFont(UITheme.FONT_SMALL);
                lbl.setTextFill(UITheme.TEXT_SUB);
                pnlBankCards.getChildren().add(lbl);
            }
        }

        pnlPropertySets.getChildren().clear();
        if (me.propertySets != null) {
            for (GameStateParser.PropertySetInfo psi : me.propertySets) {
                String mark = psi.complete ? " ✓" : "";
                String text = psi.color + mark +
                        " (" + psi.cards.size() + " card(s), rent " + psi.rent + "M)";
                Label lbl = new Label(text);
                lbl.setFont(UITheme.FONT_SMALL);
                lbl.setTextFill(psi.complete ? UITheme.ACCENT_DARK : UITheme.TEXT_SUB);
                pnlPropertySets.getChildren().add(lbl);
            }
        }
    }

    public void logEvent(String event) {
        if (event == null || event.isEmpty()) return;
        txtLog.appendText(event + "\n");
    }

    private static Label sectionTitle(String text) {
        Label lbl = new Label(text);
        lbl.setFont(UITheme.FONT_SUBTITLE);
        lbl.setTextFill(UITheme.TEXT_MAIN);
        return lbl;
    }
}
