package com.monopolydeal.interfaces;

import com.monopolydeal.model.Player;

/**
 * Interface for cards that can be played during a player's turn.
 * Provides methods to execute the card's effect, and to retrieve its value and name.
 */
public interface IPlayable {

    /** Execute the card's play effect for the given player. */
    void executePlay(Player p);

    /** Get the monetary value of this card. */
    int getValue();

    /** Get the display name of this card. */
    String getName();
}
