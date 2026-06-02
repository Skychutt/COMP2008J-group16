package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Top row seats for opponents.
 */
public class OpponentsPanel extends JPanel {

    private final GameFrame mainFrame;
    private final JPanel seatRow;

    public OpponentsPanel(GameFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 4));

        JLabel title = new JLabel("Opponents");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(new java.awt.Color(248, 233, 191));
        add(title, BorderLayout.NORTH);

        seatRow = new JPanel(new GridLayout(1, 1, 12, 0));
        seatRow.setOpaque(false);
        add(seatRow, BorderLayout.CENTER);
    }

    public void updateOpponents(List<Player> players, Player current) {
        seatRow.removeAll();

        List<Player> opponents = new ArrayList<>();
        for (Player player : players) {
            if (player != current) {
                opponents.add(player);
            }
        }

        int columns = Math.max(1, opponents.size());
        seatRow.setLayout(new GridLayout(1, columns, 12, 0));

        if (opponents.isEmpty()) {
            JLabel empty = new JLabel("(no opponents)");
            empty.setFont(UITheme.FONT_BODY);
            empty.setForeground(new java.awt.Color(248, 233, 191));
            seatRow.add(empty);
        } else {
            for (Player opponent : opponents) {
                seatRow.add(buildSeatPanel(opponent));
            }
        }

        seatRow.revalidate();
        seatRow.repaint();
    }

    private JPanel buildSeatPanel(Player opponent) {
        JPanel seat = new JPanel();
        seat.setLayout(new BoxLayout(seat, BoxLayout.Y_AXIS));
        seat.setBackground(UITheme.PANEL_BG);
        seat.setOpaque(true);
        seat.setBorder(UITheme.createSectionBorder(opponent.getName()));

        JLabel info = new JLabel("Hand " + opponent.getHand().size() + " | Sets " + opponent.getPropertyArea().countCompleteSets());
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_SUB);
        info.setAlignmentX(CENTER_ALIGNMENT);
        seat.add(info);
        seat.add(Box.createVerticalStrut(4));

        JPanel bankRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        bankRow.setOpaque(false);
        bankRow.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UITheme.BORDER), "Bank"));
        addCardsPreview(bankRow, opponent.getBankArea().getMoney(), 34, 52, 4);
        seat.add(bankRow);

        JPanel propsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        propsRow.setOpaque(false);
        propsRow.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UITheme.BORDER), "Properties"));
        List<Card> propertyCards = collectPropertyCards(opponent);
        addCardsPreview(propsRow, propertyCards, 34, 52, 6);
        seat.add(propsRow);

        return seat;
    }

    private List<Card> collectPropertyCards(Player player) {
        List<Card> cards = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            for (PropertyCard card : set.getCards()) {
                cards.add(card);
            }
        }
        return cards;
    }

    private void addCardsPreview(JPanel row, List<? extends Card> cards, int iconW, int iconH, int maxCount) {
        if (cards == null || cards.isEmpty()) {
            JLabel empty = new JLabel("-");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setForeground(UITheme.TEXT_SUB);
            row.add(empty);
            return;
        }

        int count = Math.min(cards.size(), maxCount);
        for (int i = 0; i < count; i++) {
            Card card = cards.get(i);
            JLabel icon = new JLabel(mainFrame.getImageResolver().getCardIcon(card, iconW, iconH));
            icon.setToolTipText(card.getName());
            row.add(icon);
        }

        if (cards.size() > maxCount) {
            JLabel more = new JLabel("+" + (cards.size() - maxCount));
            more.setFont(UITheme.FONT_SMALL);
            more.setForeground(UITheme.TEXT_SUB);
            row.add(more);
        }
    }
}
