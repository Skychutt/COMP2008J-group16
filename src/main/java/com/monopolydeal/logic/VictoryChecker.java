package com.monopolydeal.logic;

import com.monopolydeal.model.Player;

/**
 * Checks the victory condition (3 complete property sets of different colors).
 */
public class VictoryChecker {

    private static final int SETS_TO_WIN = 3;

    /**
     * Determines if the player has met the victory condition.
     * @param player the player to check
     * @return true if the player has won
     */
    public boolean checkWinner(Player player) {
        return countCompletedSets(player) >= SETS_TO_WIN;
    }

    /**
     * Counts the number of complete, different-colored property sets a player owns.
     * @param player the player to check
     * @return the count of completed sets
     */
    public int countCompletedSets(Player player) {
        return player.getPropertyArea().countCompleteSets();
    }
}
