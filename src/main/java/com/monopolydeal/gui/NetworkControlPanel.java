package com.monopolydeal.gui;

import com.monopolydeal.network.GameStateParser;

import javax.swing.*;
import java.awt.*;

/**
 * Left control/asset panel
 *
 */
public class NetworkControlPanel extends JPanel {

    private final NetworkGameFrame frame;
    private final JLabel lblActions;
    private final JLabel lblBank;
    private final JPanel pnlBankCards;
    private final JPanel pnlPropertySets;
    private final JTextArea txtLog;

    public NetworkControlPanel(NetworkGameFrame frame) {
        this.frame = frame;
        setOpaque(false);
        setPreferredSize(new Dimension(330, 420));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblActions = new JLabel("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_SUBTITLE);
        lblActions.setForeground(UITheme.ACCENT_DARK);
        lblActions.setAlignmentX(LEFT_ALIGNMENT);
        add(lblActions);

        lblBank = new JLabel("Bank: 0M");
        lblBank.setFont(UITheme.FONT_BANK_TOTAL);
        lblBank.setForeground(UITheme.TEXT_MAIN);
        lblBank.setAlignmentX(LEFT_ALIGNMENT);
        add(lblBank);
        add(Box.createVerticalStrut(6));

        pnlBankCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        pnlBankCards.setBackground(UITheme.PANEL_BG);
        add(makeSectionBox("Bank Cards", pnlBankCards, 80));
        add(Box.createVerticalStrut(6));

        pnlPropertySets = new JPanel();
        pnlPropertySets.setLayout(new BoxLayout(pnlPropertySets, BoxLayout.Y_AXIS));
        pnlPropertySets.setBackground(UITheme.PANEL_BG);
        add(makeSectionBox("Properties", pnlPropertySets, 120));
        add(Box.createVerticalStrut(6));

        txtLog = new JTextArea(10, 24);
        txtLog.setEditable(false);
        txtLog.setBackground(UITheme.LOG_BG);
        txtLog.setForeground(UITheme.LOG_TEXT);
        txtLog.setFont(UITheme.FONT_SMALL);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(320, 180));
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true), "Event Log"));
        add(scroll);
    }

    /**
     * Update your asset information
     */
    public void updateFromSnapshot(GameStateParser.Snapshot snap, int myIndex) {
        if (snap == null) return;

        GameStateParser.PlayerInfo me = snap.getMyInfo(myIndex);
        if (me == null) return;

        lblActions.setText("Actions: " + me.actions + " / 3");
        lblBank.setText("Bank: " + me.bank + "M");

        pnlBankCards.removeAll();
        if (me.bankCards != null) {
            for (GameStateParser.CardInfo c : me.bankCards) {
                JLabel lbl = new JLabel(c.name + "(" + c.value + "M)");
                lbl.setFont(UITheme.FONT_SMALL);
                lbl.setForeground(UITheme.TEXT_SUB);
                lbl.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
                pnlBankCards.add(lbl);
            }
        }
        pnlBankCards.revalidate();
        pnlBankCards.repaint();

        pnlPropertySets.removeAll();
        if (me.propertySets != null) {
            for (GameStateParser.PropertySetInfo psi : me.propertySets) {
                String mark = psi.complete ? " ✓" : "";
                String text = psi.color + mark + " (" + psi.cards.size() + " card(s), rent " + psi.rent + "M)";
                JLabel lbl = new JLabel(text);
                lbl.setFont(UITheme.FONT_SMALL);
                lbl.setForeground(psi.complete ? UITheme.ACCENT_DARK : UITheme.TEXT_SUB);
                pnlPropertySets.add(lbl);
            }
        }
        pnlPropertySets.revalidate();
        pnlPropertySets.repaint();
    }

    public void logEvent(String event) {
        if (event == null || event.isEmpty()) return;
        txtLog.append(event + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private static JPanel makeSectionBox(String title, JPanel content, int height) {
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(false);
        box.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true), title));
        box.setAlignmentX(LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(320, height));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        box.add(scroll, BorderLayout.CENTER);
        return box;
    }
}
