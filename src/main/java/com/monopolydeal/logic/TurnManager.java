package com.monopolydeal.logic;

import com.monopolydeal.model.Player;

/**
 * Manages the turn flow for each player, including draw phase, action phase, and end phase.
 */
public class TurnManager {
    private Player currentPlayer;
    private int remainingActions;
    private boolean isTurnActive;

    /**
     * Starts a new turn for the given player.
     * @param player the player whose turn it is
     */
    public void startTurn(Player player) {}

    /**
     * Executes the draw phase of the turn.
     * Players draw 2 cards (or 5 if hand is empty).
     */
    public void executeDrawPhase() {}

    /**
     * Checks if the player can perform another action this turn.
     * @return true if actions remain
     */
    public boolean canPerformAction() { return false; }

    /**
     * Consumes one available action after a card is played.
     */
    public void consumeAction() {}

    /**
     * Executes the end phase of the turn, including hand limit checks.
     */
    public void executeEndPhase() {}

    /**
     * Ends the current turn.
     */
    public void endTurn() {}

    // Getters and Setters
    public Player getCurrentPlayer() { return currentPlayer; }
    public int getRemainingActions() { return remainingActions; }
}