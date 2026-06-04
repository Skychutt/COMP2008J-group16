package com.monopolydeal.model;

import com.monopolydeal.model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player's hand of private cards.
 * Hand cards are hidden from other players and can be played during a turn.
 * At the end of each turn, the hand size must not exceed 7 cards (hand limit).
 */
public class Hand {
    /** The list of cards currently in the player's hand. */
    private List<Card> cards;

    /** Construct an empty hand. */
    public Hand() {
        this.cards = new ArrayList<>();
    }

    /**
     * Find a card in hand by its ID without removing it.
     * Returns the card if found, or null if not found.
     */
    public Card findCard(int cardId) {
        for (Card c : cards) {
            if (c.getId() == cardId) {
                return c;
            }
        }
        return null;
    }

    /** @return the list of all cards in this hand */
    public List<Card> getCards() {
        return cards;
    }

    /** Add a card to the hand (e.g., when drawing from the deck). */
    public void add(Card c) {
        cards.add(c);
    }

    /**
     * Insert a card back into a specific position in hand.
     * Used when a played card is cancelled so the original order stays unchanged.
     */
    public void insertAt(int index, Card c) {
        if (c == null) {
            return;
        }
        if (index < 0 || index > cards.size()) {
            cards.add(c);
            return;
        }
        cards.add(index, c);
    }

    /**
     * Check if the hand size is within the specified limit.
     * Used to enforce the 7-card hand limit at end of turn.
     * @param limit the maximum allowed hand size
     * @return true if the hand size is within the limit
     */
    public boolean checkLimit(int limit) {
        return cards.size() <= limit;
    }

    /**
     * Remove and return a card from the hand by its unique ID.
     * @param id the unique ID of the card to remove
     * @return the removed card, or null if not found
     */
    public Card removeCard(int id) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId() == id) {
                return cards.remove(i);
            }
        }
        return null;
    }

    /** @return the number of cards in the hand */
    public int size() {
        return cards.size();
    }

    /** @return true if the hand is empty */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Hand (" + cards.size() + " cards):\n");
        for (Card c : cards) {
            sb.append("  [").append(c.getId()).append("] ").append(c).append("\n");
        }
        return sb.toString();
    }
}
