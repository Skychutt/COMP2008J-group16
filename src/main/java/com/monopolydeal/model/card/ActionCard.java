package com.monopolydeal.model.card;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.interfaces.IPlayable;
import com.monopolydeal.interfaces.ITargetable;
import com.monopolydeal.model.Player;

/**
 * Represents an action card that triggers special effects when played.
 * Action cards include rent collection, property theft, forced trades, defense, etc.
 * Implements IPlayable for turn-based play and ITargetable for opponent-targeting effects.
 * Note: Action cards can also be placed in the bank as money, losing their special effect.
 */
public class ActionCard extends Card implements IPlayable, ITargetable {
    private ActionType type;     // The specific action type of this card
    private boolean canDefend;   // True if the target player can use "Just Say No" to block

    /**
     * Construct a new ActionCard.
     * @param name       display name of the action
     * @param value      monetary value when used as bank deposit
     * @param type       the action type defining this card's effect
     * @param canDefend  true if this action can be blocked by "Just Say No"
     */
    public ActionCard(String name, int value, ActionType type, boolean canDefend) {
        super(name, value);
        this.type = type;
        this.canDefend = canDefend;
    }

    /** @return the action type of this card */
    public ActionType getType() {
        return type;
    }

    /** @return true if the target can defend against this action with "Just Say No" */
    public boolean isCanDefend() {
        return canDefend;
    }

    /**
     * Execute this action card's effect for the given player.
     * Concrete action logic to be implemented by game logic layer.
     * @param p the player who plays this card
     */
    @Override
    public void executePlay(Player p) {
        // Action card effect execution - to be implemented by game logic
    }

    /**
     * Apply this action card's targeted effect to an opponent.
     * Concrete targeting logic to be implemented by game logic layer.
     * @param target the opponent player being targeted
     */
    @Override
    public void applyToTarget(Player target) {
        // Apply targeted effect - to be implemented by game logic
    }

    @Override
    public String toString() {
        return name + " [" + type + "] (Value: " + value + "M)";
    }
}
