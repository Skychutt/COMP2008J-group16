package com.monopolydeal.model.card;

import com.monopolydeal.model.Player;

/**
 * Abstract base class for all cards in the Monopoly Deal game.
 * Each card has a unique auto-incremented ID, a display name, and a monetary value.
 * Subclasses must implement executePlay() to define card-specific behavior.
 */
public abstract class Card {
    /** Static counter used to assign unique IDs to each card instance. */
    protected static int idCounter = 0;

    protected int id;       // Unique identifier for this card
    protected String name;  // Display name of the card
    protected int value;    // Monetary value of the card (in millions)

    /**
     * Construct a new Card with the given name and value.
     * A unique ID is automatically assigned.
     * @param name  the display name of the card
     * @param value the monetary value of the card
     */
    public Card(String name, int value) {
        this.id = ++idCounter;
        this.name = name;
        this.value = value;
    }

    /** @return the unique ID of this card */
    public int getId() {
        return id;
    }

    /** @return the monetary value of this card */
    public int getValue() {
        return value;
    }

    /** @return the display name of this card */
    public String getName() {
        return name;
    }

    /**
     * Execute this card's effect when played by a player.
     * @param p the player who is playing this card
     */
    public abstract void executePlay(Player p);

    @Override
    public String toString() {
        return name + " (Value: " + value + "M)";
    }
}
