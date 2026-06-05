package com.monopolydeal.gui;

import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * GUI entry point: home screen first, then local hot-seat game session.
 */
public class MonopolyDealGUIApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();
            MainMenuFrame.createAndShow();
        });
    }

    /**
     * Starts local hot-seat game after setup on the main menu (names already chosen).
     */
    public static void startLocalGame(JFrame homeFrame, int playerCount, java.util.List<String> playerNames) {
        if (playerCount < 2 || playerCount > 5) {
            if (homeFrame != null) {
                homeFrame.setVisible(true);
            }
            return;
        }

        Deck.reset();
        GameManager.reset();

        GameManager gameManager = GameManager.getInstance();
        gameManager.initGame(playerCount, playerNames);

        GameLogic logic = new GameLogic(gameManager);
        logic.getActionHandler().setUseDialogInput(true);

        GameFrame frame = new GameFrame(gameManager, logic, homeFrame);
        frame.setVisible(true);
        logic.startGame();
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }
}
