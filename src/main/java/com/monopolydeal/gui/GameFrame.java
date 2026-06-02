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
        setSize(1400, 920);
        setMinimumSize(getSize());
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
            JOptionPane.showMessageDialog(this, "Game over! Check the log for the winner.");
        }
    }

    public void playCard(Card card) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || card == null || gameManager.isGameOver() || discardMode) {
            return;
        }

        boolean legal = gameLogic.getRuleValidator().canPlayCard(current, card);
        if (!legal) {
            JOptionPane.showMessageDialog(this,
                    "This card cannot be played now (action limit or card rule).",
                    "Illegal Play",
                    JOptionPane.WARNING_MESSAGE);
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
            return;
        }

        if (card instanceof PropertyCard) {
            JOptionPane.showMessageDialog(this,
                    "Property cards cannot be deposited to bank.",
                    "Invalid Bank Move",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean success = current.putMoneyInBank(cardId);
        if (success) {
            gameManager.notifyAllObservers(current.getName() + " deposited [" + card.getName() + "] to bank.");
        } else {
            gameManager.notifyAllObservers(current.getName() + " could not deposit [" + card.getName() + "] to bank.");
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
            return;
        }

        if (!gameLogic.discardCard(current, cardId)) {
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
}
