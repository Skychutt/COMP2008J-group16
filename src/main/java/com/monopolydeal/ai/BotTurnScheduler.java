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
 * Runs bot turns on the JavaFX thread with short delays between actions.
 */
public class BotTurnScheduler {
    private static final long ACTION_DELAY_MS = 1800;

    private final BotDecisionPolicy policy;
    private final GameLogic gameLogic;
    private final ActionHandler actionHandler;
    private final Runnable refreshCallback;

    private Player activeBot;
    private boolean running;

    public BotTurnScheduler(BotDecisionPolicy policy, GameLogic gameLogic,
                            ActionHandler actionHandler, Runnable refreshCallback) {
        this.policy = policy;
        this.gameLogic = gameLogic;
        this.actionHandler = actionHandler;
        this.refreshCallback = refreshCallback;
    }

    public void beginTurn(Player botPlayer) {
        if (running && this.activeBot == botPlayer) {
            return;
        }
        this.activeBot = botPlayer;
        this.running = true;
        queueNextChoice();
    }

    public void halt() {
        running = false;
    }

    private void queueNextChoice() {
        GameManager gameManager = gameLogic.getGameManager();
        if (!running || gameManager.isGameOver()) {
            return;
        }
        if (gameManager.getCurrentPlayer() != activeBot) {
            running = false;
            return;
        }

        BotTurnChoice choice = policy.pickTurnChoice(activeBot, gameLogic);
        applyChoice(choice);
    }

    private void applyChoice(BotTurnChoice choice) {
        if (!running || choice == null) {
            return;
        }

        GameManager gameManager = gameLogic.getGameManager();
        actionHandler.setActiveDecisionPlayer(activeBot);

        try {
            switch (choice.getKind()) {
                case PLAY_CARD:
                    playChosenCard(choice);
                    break;
                case DEPOSIT_TO_BANK:
                    depositToBank(choice.getTargetCardId());
                    break;
                case DISCARD:
                    gameLogic.discardCard(activeBot, choice.getTargetCardId());
                    break;
                case END_TURN:
                    endTurnSequence();
                    return;
                default:
                    break;
            }
        } catch (Exception ex) {
            gameManager.notifyAllObservers("[Bot] Action failed: " + ex.getMessage());
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
        delay.setOnFinished(e -> queueNextChoice());
        delay.play();
    }

    private void playChosenCard(BotTurnChoice choice) {
        Card card = activeBot.getHand().findCard(choice.getTargetCardId());
        if (card == null) {
            return;
        }
        if (card instanceof PropertyCard) {
            gameLogic.placeProperty(activeBot, (PropertyCard) card, choice.getWildColor());
        } else {
            gameLogic.playCard(activeBot, card);
        }
    }

    private void depositToBank(int cardId) {
        Card card = activeBot.getHand().findCard(cardId);
        if (card == null) {
            return;
        }
        activeBot.putMoneyInBank(cardId);
        gameLogic.checkGameOver();
    }

    private void endTurnSequence() {
        while (gameLogic.getRequiredDiscardCount(activeBot) > 0) {
            BotTurnChoice discard = policy.pickTurnChoice(activeBot, gameLogic);
            if (discard.getKind() != BotTurnChoice.Kind.DISCARD) {
                break;
            }
            gameLogic.discardCard(activeBot, discard.getTargetCardId());
        }
        running = false;
        gameLogic.endTurn();
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }
}
