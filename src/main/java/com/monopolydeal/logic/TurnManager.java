package com.monopolydeal.logic;

import com.monopolydeal.model.Player;

/**
 * Manages the turn flow for each player, including draw phase, action phase, and end phase.
 */
public class TurnManager {
    private Player currentPlayer;
    private boolean isTurnActive;

    /**
     * Starts a new turn for the given player.
     * @param player the player whose turn it is
     */
    public void startTurn(Player player) {
        this.currentPlayer = player;
        this.isTurnActive = true;
    }

    /**
     * Executes the draw phase of the turn.
     * Players draw 2 cards (or 5 if hand is empty).
     */
    public void executeDrawPhase() {
        if (currentPlayer != null) {
            currentPlayer.draw();
        }
    }

    /**
     * Checks if the player can perform another action this turn.
     * @return true if actions remain
     */
    public boolean canPerformAction() {
        return currentPlayer != null && currentPlayer.getActions() > 0;
    }

    /**
     * Consumes one available action after a card is played.
     */
    public void consumeAction() {
        if (currentPlayer == null || currentPlayer.getActions() <= 0) {
            return;
        }
        currentPlayer.setActions(currentPlayer.getActions() - 1);
    }

    /**
     * Executes the end phase of the turn, including hand limit checks.
     */
    public void executeEndPhase() {
        if (currentPlayer != null) {
            currentPlayer.finalizeTurnEnd();
        }
    }

    /**
     * Ends the current turn.
     */
    public void endTurn() {
        isTurnActive = false;
    }

    public Player getCurrentPlayer() { return currentPlayer; }
    public int getRemainingActions() {
        return currentPlayer != null ? currentPlayer.getActions() : 0;
    }
    public boolean isTurnActive() { return isTurnActive; }
}
