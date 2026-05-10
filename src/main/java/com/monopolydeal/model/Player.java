package com.monopolydeal.model;

import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.interfaces.ISubject;
import com.monopolydeal.model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player in the Monopoly Deal game.
 * Each player has a hand, a bank area, and a property area.
 * Implements ISubject (Observer pattern) to notify observers of player actions.
 */
public class Player implements ISubject {
    private String id;                          // Unique player identifier
    private String name;                        // Display name of the player
    private int actions;                        // Remaining actions in the current turn (max 3)
    private Hand hand;                          // Player's private hand of cards
    private BankArea bankArea;                  // Player's bank area for money storage
    private PropertyArea propertyArea;          // Player's property area for property sets
    private List<IGameObserver> observers;      // Registered observers for event notifications

    /**
     * Construct a new Player with the given ID and name.
     * Initializes an empty hand, bank area, and property area.
     * @param id   unique identifier for the player
     * @param name display name of the player
     */
    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.actions = 0;
        this.hand = new Hand();
        this.bankArea = new BankArea();
        this.propertyArea = new PropertyArea();
        this.observers = new ArrayList<>();
    }

    /** @return the unique player ID */
    public String getId() {
        return id;
    }

    /** @return the player's display name */
    public String getName() {
        return name;
    }

    /** @return the number of remaining actions this turn */
    public int getActions() {
        return actions;
    }

    /** Set the number of remaining actions for this turn. */
    public void setActions(int actions) {
        this.actions = actions;
    }

    /** @return the player's hand */
    public Hand getHand() {
        return hand;
    }

    /** @return the player's bank area */
    public BankArea getBankArea() {
        return bankArea;
    }

    /** @return the player's property area */
    public PropertyArea getPropertyArea() {
        return propertyArea;
    }

    /**
     * Draw cards from the deck at the start of a turn.
     * Draws 2 cards normally, or 5 cards if the hand is empty (recovery mechanism).
     */
    public void draw() {
        Deck deck = Deck.getInstance();
        int count = hand.isEmpty() ? 5 : 2;
        for (int i = 0; i < count; i++) {
            Card c = deck.draw(1).get(0);
            if (c != null) {
                hand.add(c);
            }
        }
    }

    /**
     * Play a card from the hand by its unique ID.
     * The card's effect is executed and the action count is decremented.
     * Does nothing if no actions remain or the card is not found.
     * @param cardId the unique ID of the card to play
     */
    public void playCard(int cardId) {
        if (actions <= 0) {
            return;
        }
        Card c = hand.removeCard(cardId);
        if (c != null) {
            c.executePlay(this);
            actions--;
            notifyAllObservers(name + " played " + c.getName());
        }
    }

    /**
     * Pay a specified amount using cards from the bank or property area.
     * To be implemented with UI interaction for card selection.
     * @param amount the amount to pay in millions
     */
    public void payAmount(int amount) {
        // Payment logic - player selects cards from bank/property area
        // to be implemented with UI interaction
    }

    /**
     * Handle incoming game event updates.
     * @param event description of the game event
     */
    public void onGameUpdate(String event) {
        // React to game events
    }

    /** Register an observer to receive notifications from this player. */
    @Override
    public void attach(IGameObserver o) {
        observers.add(o);
    }

    /** Remove a previously registered observer. */
    @Override
    public void detach(IGameObserver o) {
        observers.remove(o);
    }

    /** Notify all registered observers about a game event. */
    @Override
    public void notifyAllEvent(String event) {
        notifyAllObservers(event);
    }

    /**
     * Internal helper to broadcast an event to all registered observers.
     * @param event the event message to broadcast
     */
    private void notifyAllObservers(String event) {
        for (IGameObserver o : observers) {
            o.onGameUpdate(event);
        }
    }

    @Override
    public String toString() {
        return "Player: " + name + " | " + bankArea + " | Properties: " +
                propertyArea.countCompleteSets() + " complete sets | Hand: " + hand.size() + " cards";
    }
}
