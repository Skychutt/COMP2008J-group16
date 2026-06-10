package com.monopolydeal.model;

import com.monopolydeal.model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic container for holding a collection of cards.
 * Provides basic add, remove, and query operations.
 * Used as a base utility for managing card lists throughout the game.
 */
public class CardHolder {
    /** The internal list of cards held by this container. */
    protected List<Card> cards;

    /** Construct an empty CardHolder. */
    public CardHolder() {
        this.cards = new ArrayList<>();
    }

    /** Add a card to this holder. */
    public void add(Card c) {
        cards.add(c);
    }

    /** Remove a specific card instance from this holder. */
    public void remove(Card c) {
        cards.remove(c);
    }

    /**
     * Remove and return a card by its unique ID.
     * @param id the unique ID of the card to remove
     * @return the removed card, or null if no card with the given ID was found
     */
    public Card remove(int id) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId() == id) {
                return cards.remove(i);
            }
        }
        return null;
    }

    /** @return the list of all cards in this holder */
    public List<Card> getCards() {
        return cards;
    }

    /** @return the number of cards in this holder */
    public int size() {
        return cards.size();
    }

    /** @return true if this holder contains no cards */
    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
