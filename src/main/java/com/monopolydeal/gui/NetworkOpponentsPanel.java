package com.monopolydeal.gui;

import com.monopolydeal.network.GameStateParser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * LAN Battle Game opponent panel
 */
public class NetworkOpponentsPanel extends JPanel {

    private static final int ICON_W = 34;
    private static final int ICON_H = 52;

    private final NetworkGameFrame frame;
    private final JPanel seatRow;

    public NetworkOpponentsPanel(NetworkGameFrame frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 4));

        JLabel title = new JLabel("Opponents");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(new Color(248, 233, 191));
        add(title, BorderLayout.NORTH);

        seatRow = new JPanel(new GridLayout(1, 1, 12, 0));
        seatRow.setOpaque(false);
        add(seatRow, BorderLayout.CENTER);
    }

    public void updateFromSnapshot(GameStateParser.Snapshot snap, int myIndex) {
        seatRow.removeAll();

        if (snap == null || snap.players == null) {
            seatRow.revalidate();
            seatRow.repaint();
            return;
        }

        List<GameStateParser.PlayerInfo> opponents = new java.util.ArrayList<>();
        for (GameStateParser.PlayerInfo p : snap.players) {
            if (p.index != myIndex) opponents.add(p);
        }

        int columns = Math.max(1, opponents.size());
        seatRow.setLayout(new GridLayout(1, columns, 12, 0));

        if (opponents.isEmpty()) {
            JLabel empty = new JLabel("(No opponents)");
            empty.setFont(UITheme.FONT_BODY);
            empty.setForeground(new Color(248, 233, 191));
            seatRow.add(empty);
        } else {
            for (GameStateParser.PlayerInfo opp : opponents) {
                seatRow.add(buildSeatPanel(opp, opp.index == snap.turn));
            }
        }

        seatRow.revalidate();
        seatRow.repaint();
    }

    private JPanel buildSeatPanel(GameStateParser.PlayerInfo p, boolean isCurrentTurn) {
        JPanel seat = new JPanel();
        seat.setLayout(new BoxLayout(seat, BoxLayout.Y_AXIS));
        seat.setBackground(UITheme.PANEL_BG);
        seat.setOpaque(true);
        seat.setBorder(UITheme.createSectionBorder(
                (isCurrentTurn ? "** " : "") + p.name));

        JLabel info = new JLabel("Hand " + p.handSize + " | Sets " + p.sets);
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_SUB);
        info.setAlignmentX(CENTER_ALIGNMENT);
        seat.add(info);
        seat.add(Box.createVerticalStrut(4));

        JPanel bankRow = new JPanel(new BorderLayout(6, 0));
        bankRow.setOpaque(false);
        bankRow.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER), "Bank"));

        JLabel bankTotal = new JLabel(p.bank + "M");
        bankTotal.setFont(UITheme.FONT_BANK_TOTAL);
        bankTotal.setForeground(UITheme.TEXT_MAIN);
        bankRow.add(bankTotal, BorderLayout.WEST);

        JPanel bankCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        bankCards.setOpaque(false);
        addCardPreviews(bankCards, p.bankCards, 4);
        bankRow.add(bankCards, BorderLayout.CENTER);
        seat.add(bankRow);

        JPanel propsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        propsRow.setOpaque(false);
        propsRow.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER), "Properties"));

        List<GameStateParser.CardInfo> propCards = new java.util.ArrayList<>();
        if (p.propertySets != null) {
            for (GameStateParser.PropertySetInfo psi : p.propertySets) {
                if (psi.cards != null) propCards.addAll(psi.cards);
            }
        }
        addCardPreviews(propsRow, propCards, 6);
        seat.add(propsRow);

        return seat;
    }


    /**
     * Add card thumbnails to the panel
     */
    private void addCardPreviews(JPanel row, List<GameStateParser.CardInfo> cards, int maxCount) {
        if (cards == null || cards.isEmpty()) {
            JLabel empty = new JLabel("-");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setForeground(UITheme.TEXT_SUB);
            row.add(empty);
            return;
        }

        CardImageResolver resolver = frame.getImageResolver();
        int count = Math.min(cards.size(), maxCount);
        for (int i = 0; i < count; i++) {
            GameStateParser.CardInfo c = cards.get(i);
            ImageIcon icon = resolver.getIconByName(c.name, ICON_W, ICON_H);
            JLabel lbl = new JLabel(icon);
            lbl.setToolTipText(c.name + " (" + c.value + "M)");
            row.add(lbl);
        }

        if (cards.size() > maxCount) {
            JLabel more = new JLabel("+" + (cards.size() - maxCount));
            more.setFont(UITheme.FONT_SMALL);
            more.setForeground(UITheme.TEXT_SUB);
            row.add(more);
        }
    }
}