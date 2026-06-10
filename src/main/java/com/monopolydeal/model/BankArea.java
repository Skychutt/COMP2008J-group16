package com.monopolydeal.model;

import com.monopolydeal.model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player's bank area where money cards and action cards
 * deposited as currency are stored.
 * Cards in the bank can be used to pay rent or fees during the game.
 */
public class BankArea {
    /** List of cards stored in the bank (money cards or action cards used as money). */
    private List<Card> money;

    /** Construct an empty bank area. */
    public BankArea() {
        this.money = new ArrayList<>();
    }

    /**
     * Calculate the total monetary value of all cards in the bank.
     * @return the sum of all card values in millions
     */
    public int total() {
        int sum = 0;
        for (Card c : money) {
            sum += c.getValue();
        }
        return sum;
    }

    /**
     * Check if the bank has enough money to pay the given amount.
     * @param amount the amount to check
     * @return true if bank total >= amount
     */
    public boolean canAfford(int amount) {
        return total() >= amount;
    }

    /** Deposit a card into the bank. */
    public void add(Card c) {
        money.add(c);
    }

    /** Remove a card from the bank (e.g., for payment). */
    public void remove(Card c) {
        money.remove(c);
    }

    /** @return the list of all cards currently in the bank */
    public List<Card> getMoney() {
        return money;
    }

    /** @return the number of cards in the bank */
    public int size() {
        return money.size();
    }

    @Override
    public String toString() {
        return "Bank: " + total() + "M (" + money.size() + " cards)";
    }
}
