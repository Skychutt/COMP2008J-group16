package com.monopolydeal.logic;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;

/**
 * Handles the execution of all action card effects.
 */
public class ActionHandler {

    private final GameLogic gameLogic;

    public ActionHandler(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }

    /**
     * Executes the effect of a given action card.
     * @param player the player playing the card
     * @param card the action card to execute
     */
    public void executeAction(Player player, ActionCard card) {}

    /**
     * Handles the "Just Say No" defense mechanism.
     * @param defender the player being attacked
     * @param attacker the player using the action
     * @param attackCard the card being defended against
     * @return true if the attack was successfully defended
     */
    public boolean handleJustSayNo(Player defender, Player attacker, ActionCard attackCard) {
        return false;
    }

    // Individual handlers for specific action cards
    public void handlePassGo(Player player) {}
    public void handleRent(Player collector, Player target, int amount) {}
    public void handleBirthday(Player initiator) {}
    public void handleDealBreaker(Player thief, Player victim) {}
    public void handleSlyDeal(Player thief, Player victim, Card targetProperty) {}
}