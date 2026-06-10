package com.monopolydeal.model.card;

import com.monopolydeal.interfaces.IPlayable;
import com.monopolydeal.model.Player;

/**
 * Represents a money card used as currency in the Monopoly Deal game.
 * Money cards are deposited into the player's bank area and used for paying rent or fees.
 * Denominations include 1M, 2M, 3M, 4M, 5M, and 10M.
 */
public class MoneyCard extends Card implements IPlayable {
    private int denomination;  // The face value denomination of this money card

    /**
     * Construct a new MoneyCard.
     * @param name         display name (e.g., "1M", "5M")
     * @param value        monetary value for payment calculations
     * @param denomination the face denomination of this money card
     */
    public MoneyCard(String name, int value, int denomination) {
        super(name, value);
        this.denomination = denomination;
    }

    /** @return the face denomination of this money card */
    public int getDenomination() {
        return denomination;
    }

    /**
     * Play this money card by depositing it into the player's bank area.
     * @param p the player who plays this card
     */
    @Override
    public void executePlay(Player p) {
        p.getBankArea().add(this);
    }

    @Override
    public String toString() {
        return name + " (" + denomination + "M)";
    }
}
