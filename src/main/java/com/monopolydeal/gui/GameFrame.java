package com.monopolydeal.gui;

import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * Main game window styled as a large table.
 */
public class GameFrame extends JFrame implements IGameObserver {

    private final GameManager gameManager;
    private final GameLogic gameLogic;

    private final CardImageResolver imageResolver;
    private final TopStatusPanel topStatusPanel;
    private final PlayerPanel playerPanel;
    private final OpponentsPanel opponentsPanel;
    private final ControlPanel controlPanel;

    private boolean gameOverDialogShown;
    private String latestEvent;
    private boolean discardMode;
    private int discardRemaining;

    public GameFrame(GameManager gameManager, GameLogic gameLogic) {
        this.gameManager = gameManager;
        this.gameLogic = gameLogic;
        this.imageResolver = new CardImageResolver();
        this.latestEvent = "Welcome to Monopoly Deal.";
        this.discardMode = false;
        this.discardRemaining = 0;

        setTitle("Monopoly Deal");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1400, Math.max(1180, screen.width - 40));
        int height = Math.min(920, Math.max(680, screen.height - 80));
        setSize(width, height);
        setMinimumSize(new Dimension(1180, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        TableSurfacePanel table = new TableSurfacePanel();
        table.setLayout(new BorderLayout(14, 14));
        table.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(table);

        topStatusPanel = new TopStatusPanel();
        playerPanel = new PlayerPanel(this);
        opponentsPanel = new OpponentsPanel(this);
        controlPanel = new ControlPanel(this);

        topStatusPanel.setCardDropHandler(this::handleCenterCardDrop);
        playerPanel.setBankDropHandler(this::bankCardById);
        playerPanel.setEndTurnHandler(this::endCurrentTurn);

        add(opponentsPanel, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.WEST);
        add(topStatusPanel, BorderLayout.CENTER);
        add(playerPanel, BorderLayout.SOUTH);

        gameManager.attach(this);
        refreshUI();
    }

    @Override
    public void onGameUpdate(String event) {
        latestEvent = event == null ? latestEvent : event;
        controlPanel.logEvent(event);
        refreshUI();
    }

    public void refreshUI() {
        if (gameManager.getPlayers() == null || gameManager.getPlayers().isEmpty()) {
            return;
        }

        Player current = gameManager.getCurrentPlayer();
        topStatusPanel.updateTableCenter(current, imageResolver, latestEvent, gameManager.isGameOver(), discardMode, discardRemaining);
        playerPanel.updatePlayerView(current, gameManager.isGameOver(), discardMode, discardRemaining);
        opponentsPanel.updateOpponents(gameManager.getPlayers(), current);
        controlPanel.updateTurnStatus(current);
        controlPanel.updateSelfAssets(current);

        if (gameManager.isGameOver() && !gameOverDialogShown) {
            gameOverDialogShown = true;
            JOptionPane.showMessageDialog(this, "Game over! Check the log for the winner.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void playCard(Card card) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || card == null || gameManager.isGameOver() || discardMode) {
            return;
        }

        String reason = gameLogic.getRuleValidator().explainPlayCardFailure(current, card);
        if (reason != null) {
            reportProblem("Play Failed", reason);
            return;
        }

        gameLogic.playCard(current, card);
        refreshUI();
    }

    public void playCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || discardMode) {
            return;
        }
        Card card = current.getHand().findCard(cardId);
        if (card == null) {
            return;
        }
        playCard(card);
    }

    public void bankCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || discardMode) {
            return;
        }

        Card card = current.getHand().findCard(cardId);
        if (card == null) {
            reportProblem("Bank Failed", "This card is no longer in your hand, so it cannot be banked.");
            return;
        }

        if (current.getActions() <= 0) {
            reportProblem("Bank Failed",
                    "Cannot bank [" + card.getName() + "] because " + current.getName() + " has no actions left.");
            return;
        }

        if (card instanceof PropertyCard) {
            reportProblem("Bank Failed",
                    "Cannot bank [" + card.getName() + "] because property cards must stay in the property area.");
            return;
        }

        boolean success = current.putMoneyInBank(cardId);
        if (success) {
            gameManager.notifyAllObservers(current.getName() + " deposited [" + card.getName() + "] to bank.");
        } else {
            reportProblem("Bank Failed",
                    "Cannot bank [" + card.getName() + "] because the move is no longer legal. Please try again.");
            return;
        }
        refreshUI();
    }

    public void endCurrentTurn() {
        if (gameManager.isGameOver() || discardMode) {
            return;
        }
        Player current = gameManager.getCurrentPlayer();
        if (current == null) {
            return;
        }

        int needDiscard = gameLogic.getRequiredDiscardCount(current);
        if (needDiscard > 0) {
            discardMode = true;
            discardRemaining = needDiscard;
            gameManager.notifyAllObservers(current.getName() + " must discard " + needDiscard + " card(s) before ending the turn.");
            return;
        }

        gameLogic.endTurn();
        refreshUI();
    }

    public void handleCenterCardDrop(int cardId) {
        if (discardMode) {
            discardCardById(cardId);
        } else {
            playCardById(cardId);
        }
    }

    private void discardCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || !discardMode) {
            return;
        }

        Card discard = current.getHand().findCard(cardId);
        if (discard == null) {
            reportProblem("Discard Failed", "This card is no longer in your hand, so it cannot be discarded.");
            return;
        }

        if (!gameLogic.discardCard(current, cardId)) {
            reportProblem("Discard Failed", "Cannot discard [" + discard.getName() + "] because the move is no longer legal.");
            return;
        }

        discardRemaining = gameLogic.getRequiredDiscardCount(current);
        String event = current.getName() + " discarded [" + discard.getName() + "].";
        if (discardRemaining > 0) {
            event += " " + discardRemaining + " card(s) left to discard.";
            gameManager.notifyAllObservers(event);
            return;
        }

        event += " Turn will end now.";
        discardMode = false;
        discardRemaining = 0;
        gameManager.notifyAllObservers(event);
        gameLogic.endTurn();
    }

    public GameLogic getGameLogic() {
        return gameLogic;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public CardImageResolver getImageResolver() {
        return imageResolver;
    }

    /**
     * Shows the same problem both in a dialog and in the event log.
     * Keeping the message in one place makes GUI errors easier to read.
     */
    private void reportProblem(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
        gameManager.notifyAllObservers(message);
    }
}

