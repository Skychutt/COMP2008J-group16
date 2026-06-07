package com.monopolydeal.ai;

import com.monopolydeal.logic.ActionHandler;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Runs AI turns on the JavaFX thread with short delays between actions.
 */
public class AITurnExecutor {
    private static final long ACTION_DELAY_MS = 1800;

    private final AIActionStrategy brain;
    private final GameLogic gameLogic;
    private final ActionHandler actionHandler;
    private final Runnable refreshCallback;

    private Player aiPlayer;
    private boolean running;

    public AITurnExecutor(AIActionStrategy brain, GameLogic gameLogic,
                          ActionHandler actionHandler, Runnable refreshCallback) {
        this.brain = brain;
        this.gameLogic = gameLogic;
        this.actionHandler = actionHandler;
        this.refreshCallback = refreshCallback;
    }

    public void startTurn(Player aiPlayer) {
        if (running && this.aiPlayer == aiPlayer) {
            return;
        }
        this.aiPlayer = aiPlayer;
        this.running = true;
        scheduleNextAction();
    }

    public void stop() {
        running = false;
    }

    private void scheduleNextAction() {
        GameManager gameManager = gameLogic.getGameManager();
        if (!running || gameManager.isGameOver()) {
            return;
        }
        if (gameManager.getCurrentPlayer() != aiPlayer) {
            running = false;
            return;
        }

        AIAction action = brain.decideNextAction(aiPlayer, gameLogic);
        executeAction(action);
    }

    private void executeAction(AIAction action) {
        if (!running || action == null) {
            return;
        }

        GameManager gameManager = gameLogic.getGameManager();
        actionHandler.setActiveDecisionPlayer(aiPlayer);

        try {
            switch (action.getType()) {
                case PLAY_CARD:
                    playCard(action);
                    break;
                case DEPOSIT_TO_BANK:
                    deposit(action.getCardId());
                    break;
                case DISCARD:
                    gameLogic.discardCard(aiPlayer, action.getCardId());
                    break;
                case END_TURN:
                    finishTurn();
                    return;
                default:
                    break;
            }
        } catch (Exception ex) {
            gameManager.notifyAllObservers("[AI] Action failed: " + ex.getMessage());
        } finally {
            actionHandler.clearActiveDecisionPlayer();
        }

        if (refreshCallback != null) {
            refreshCallback.run();
        }

        if (!running || gameManager.isGameOver()) {
            return;
        }

        PauseTransition delay = new PauseTransition(Duration.millis(ACTION_DELAY_MS));
        delay.setOnFinished(e -> scheduleNextAction());
        delay.play();
    }

    private void playCard(AIAction action) {
        Card card = aiPlayer.getHand().findCard(action.getCardId());
        if (card == null) {
            return;
        }
        if (card instanceof PropertyCard) {
            gameLogic.placeProperty(aiPlayer, (PropertyCard) card, action.getChosenColor());
        } else {
            gameLogic.playCard(aiPlayer, card);
        }
    }

    private void deposit(int cardId) {
        Card card = aiPlayer.getHand().findCard(cardId);
        if (card == null) {
            return;
        }
        aiPlayer.putMoneyInBank(cardId);
        gameLogic.checkGameOver();
    }

    private void finishTurn() {
        while (gameLogic.getRequiredDiscardCount(aiPlayer) > 0) {
            AIAction discard = brain.decideNextAction(aiPlayer, gameLogic);
            if (discard.getType() != AIAction.Type.DISCARD) {
                break;
            }
            gameLogic.discardCard(aiPlayer, discard.getCardId());
        }
        running = false;
        gameLogic.endTurn();
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }
}
