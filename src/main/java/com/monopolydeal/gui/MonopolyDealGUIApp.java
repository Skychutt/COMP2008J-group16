package com.monopolydeal.gui;

import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * GUI entry point.
 */
public class MonopolyDealGUIApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();

            int playerCount = choosePlayerCount();
            if (playerCount < 2) {
                return;
            }

            Deck.reset();
            GameManager.reset();

            GameManager gameManager = GameManager.getInstance();
            gameManager.initGame(playerCount);

            showInitialHandReview(gameManager);

            GameLogic logic = new GameLogic(gameManager);
            logic.getActionHandler().setUseDialogInput(true);

            GameFrame frame = new GameFrame(gameManager, logic);
            frame.setVisible(true);
            logic.startGame();
        });
    }

    private static int choosePlayerCount() {
        String[] options = {"2", "3", "4", "5"};
        int index = JOptionPane.showOptionDialog(
                null,
                "Choose player count (2-5):",
                "Monopoly Deal Setup",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );

        if (index < 0) {
            return -1;
        }
        return index + 2;
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    /**
     * Hot-seat helper: each player privately reviews their opening hand.
     */
    private static void showInitialHandReview(GameManager gameManager) {
        List<Player> players = gameManager.getPlayers();
        CardImageResolver resolver = new CardImageResolver();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            int ready = JOptionPane.showConfirmDialog(
                    null,
                    "Please pass the screen to " + player.getName() + ".\nOnly this player should look now.",
                    "Opening Hand Review",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );
            if (ready != JOptionPane.OK_OPTION) {
                continue;
            }

            JPanel preview = buildInitialHandPreview(player, resolver);
            JScrollPane scroll = new JScrollPane(preview);
            scroll.setPreferredSize(new java.awt.Dimension(760, 340));
            scroll.getViewport().setBackground(UITheme.PANEL_BG);

            JOptionPane.showMessageDialog(
                    null,
                    scroll,
                    player.getName() + " - Opening Hand",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (i < players.size() - 1) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please pass the screen to the next player.",
                        "Pass Screen",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
    }

    private static JPanel buildInitialHandPreview(Player player, CardImageResolver resolver) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(UITheme.PANEL_BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel(player.getName() + " opening hand");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);
        root.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        cards.setBackground(UITheme.PANEL_BG);

        int index = 1;
        for (Card card : player.getHand().getCards()) {
            JPanel tile = new JPanel();
            tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
            tile.setBackground(UITheme.PANEL_SOFT_BG);
            tile.setBorder(UITheme.createSectionBorder("#" + index++));

            JLabel icon = new JLabel(resolver.getCardIcon(card, 92, 136));
            icon.setAlignmentX(JPanel.CENTER_ALIGNMENT);
            JLabel text = new JLabel("<html><center>" + card.getName() + "<br/>" + card.getValue() + "M</center></html>");
            text.setFont(UITheme.FONT_SMALL);
            text.setForeground(UITheme.TEXT_SUB);
            text.setAlignmentX(JPanel.CENTER_ALIGNMENT);

            tile.add(icon);
            tile.add(text);
            tile.setToolTipText(card.toString());
            cards.add(tile);
        }

        if (player.getHand().getCards().isEmpty()) {
            JTextArea empty = new JTextArea("(No cards)");
            empty.setEditable(false);
            empty.setFont(UITheme.FONT_BODY);
            empty.setBackground(UITheme.PANEL_BG);
            cards.add(empty);
        }

        root.add(cards, BorderLayout.CENTER);
        return root;
    }

}

