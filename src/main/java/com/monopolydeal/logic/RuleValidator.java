package com.monopolydeal.logic;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.Card;

/**
 * Validates all game rules to prevent illegal operations.
 */
public class RuleValidator {

    /**
     * Checks if a player can play a card during their turn.
     * @param player the player playing the card
     * @param card the card to play
     * @return true if the play is allowed
     */
    public boolean canPlayCard(Player player, Card card) {
        return false;
    }

    /**
     * Checks if a House card can be added to a property set.
     * @param set the target property set
     * @return true if allowed
     */
    public boolean canAddHouse(PropertySet set) {
        return false;
    }

    /**
     * Checks if a Hotel card can be added to a property set.
     * @param set the target property set
     * @return true if allowed
     */
    public boolean canAddHotel(PropertySet set) {
        return false;
    }

    /**
     * Checks if a player's hand exceeds the maximum allowed limit (7 cards).
     * @param player the player to check
     * @return true if over the limit
     */
    public boolean isHandOverLimit(Player player) {
        return false;
    }
}