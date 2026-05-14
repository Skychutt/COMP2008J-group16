package com.monopolydeal.logic;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

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
        if (player == null || card == null) {
            return false;
        }
        if (player.getActions() <= 0) {
            return false;
        }
        if (player.getHand().findCard(card.getId()) == null) {
            return false;
        }
        if (card instanceof ActionCard) {
            ActionType t = ((ActionCard) card).getType();
            if (t == ActionType.HOUSE) {
                return canAddHouseToAnySet(player);
            }
            if (t == ActionType.HOTEL) {
                return canAddHotelToAnySet(player);
            }
        }
        return true;
    }

    private boolean canAddHouseToAnySet(Player player) {
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (canAddHouse(set)) {
                return true;
            }
        }
        return false;
    }

    private boolean canAddHotelToAnySet(Player player) {
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (canAddHotel(set)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a House card can be added to a property set.
     * @param set the target property set
     * @return true if allowed
     */
    public boolean canAddHouse(PropertySet set) {
        if (set == null || !set.isComplete()) {
            return false;
        }
        return countUpgrade(set, ActionType.HOUSE) == 0
                && countUpgrade(set, ActionType.HOTEL) == 0;
    }

    /**
     * Checks if a Hotel card can be added to a property set.
     * @param set the target property set
     * @return true if allowed
     */
    public boolean canAddHotel(PropertySet set) {
        if (set == null || !set.isComplete()) {
            return false;
        }
        return countUpgrade(set, ActionType.HOUSE) >= 1
                && countUpgrade(set, ActionType.HOTEL) == 0;
    }

    private static int countUpgrade(PropertySet set, ActionType type) {
        int n = 0;
        for (PropertyCard pc : set.getCards()) {
            for (Card u : pc.getUpgrades()) {
                if (u instanceof ActionCard && ((ActionCard) u).getType() == type) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Checks if a player's hand exceeds the maximum allowed limit (7 cards).
     * @param player the player to check
     * @return true if over the limit
     */
    public boolean isHandOverLimit(Player player) {
        return player != null && player.getHand().size() > Player.MAX_HAND_SIZE;
    }
}
