package com.monopolydeal.model;

import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.interfaces.ISubject;
import com.monopolydeal.model.card.*;

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

    /** Maximum cards allowed in hand at end of turn. */
    public static final int MAX_HAND_SIZE = 7;

    /** Number of actions allowed per turn. */
    public static final int ACTIONS_PER_TURN = 3;

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
        List<Card> drawn = deck.draw(count);
        hand.getCards().addAll(drawn);
        notifyAllObservers(name + " drew " + drawn.size() + " card(s). Hand size: " + hand.size());
    }

    /**
     * Play a card from the hand by its unique ID.
     * The card's effect is executed and the action count is decremented.
     * Does nothing if no actions remain or the card is not found.
     * @param cardId the unique ID of the card to play
     */
    public void playCard(int cardId) {
        if (actions <= 0) {
            notifyAllObservers(name + " has no actions left!");
            return;
        }
        Card c = hand.removeCard(cardId);
        if (c == null) {
            notifyAllObservers("Card not found in hand (id=" + cardId + ")");
            return;
        }
        c.executePlay(this);
        actions--;
        notifyAllObservers(name + " played " + c.getName());

    }

    /**
     * Deposit a money or action card from hand into the bank area.
     * Property cards cannot be deposited as money.
     * Consumes 1 action when successful.
     * @param cardId the unique ID of the card to deposit
     * @return true if the card was banked successfully
     */
    public boolean putMoneyInBank(int cardId) {
        if (actions <= 0) {
            notifyAllObservers(name + " has no actions left!");
            return false;
        }

        Card found = hand.findCard(cardId);
        if (found == null) {
            notifyAllObservers("Card not found in hand (id=" + cardId + ")");
            return false;
        }

        if (found instanceof PropertyCard) {
            notifyAllObservers("Cannot bank [" + found.getName() + "] because property cards must stay in the property area.");
            return false;
        }

        hand.removeCard(cardId);
        bankArea.add(found);
        actions--;
        notifyAllObservers(name + " deposited [" + found.getName() + "] to bank. Bank total: " + bankArea.total() + "M");
        return true;
    }


    /**
     * Place a property card from hand into the property area.
     * Consumes 1 action when successful.
     * @param cardId the unique ID of the property card to place
     * @return true if the property card was placed successfully
     */
    public boolean placeProperty(int cardId) {
        if (actions <= 0) {
            notifyAllObservers(name + " has no actions left!");
            return false;
        }

        Card card = hand.findCard(cardId);
        if (card == null) {
            notifyAllObservers("Card not found in hand (id=" + cardId + ")");
            return false;
        }

        if (!(card instanceof PropertyCard)) {
            notifyAllObservers("Card [" + card.getName() + "] is not a Property Card!");
            return false;
        }

        hand.removeCard(cardId);
        propertyArea.add(card);
        actions--;
        notifyAllObservers(name + " placed [" + card.getName() + "]. Complete sets: " + propertyArea.countCompleteSets());
        return true;
    }


    /**
     * Pay a specified amount using cards from the bank or property area.
     * To be implemented with UI interaction for card selection.
     * @param amount the amount to pay in millions
     */
    public List<Card> payAmount(int amount) {
        List<Card> paid = new ArrayList<>();
        int totalPaid = 0;

        // Step 1: Pay from bank cards first
        List<Card> bankCards = new ArrayList<>(bankArea.getMoney());
        for (Card c : bankCards) {
            if (totalPaid >= amount) break;
            bankArea.remove(c);
            paid.add(c);
            totalPaid += c.getValue();
        }

        // Step 2: If bank is not enough, also pay with property cards
        if (totalPaid < amount) {
            List<PropertySet> sets = propertyArea.getSets();
            for (PropertySet set : sets) {
                List<PropertyCard> propCards = new ArrayList<>(set.getCards());
                for (PropertyCard c : propCards) {
                    if (totalPaid >= amount) break;
                    propertyArea.remove(c);
                    paid.add(c);
                    totalPaid += c.getValue();
                }
            }
        }

        notifyAllObservers(name + " paid " + totalPaid + "M (owed " + amount + "M).");
        return paid;
    }

    /**
     * Receive cards transferred from another player (e.g., as rent payment).
     * All received cards go into this player's bank area.
     * @param cards the list of cards received
     */
    public void receivePayment(List<Card> cards) {
        cards.forEach(c -> {
            if (c instanceof PropertyCard) {
                propertyArea.add(c);
            } else {
                bankArea.add(c);
            }
        });
        notifyAllObservers(name + " received " + cards.size() + " card(s) as payment. Bank: " + bankArea.total() + "M");
    }

    /**
     * End the current turn.
     * Enforces the 7-card hand limit: if more than 7 cards remain, the player must
     * discard down to 7. The discarded cards are added to the discard pile.
     *
     * Note: In a GUI version, the player would choose which cards to discard.
     * Here we auto-discard from the end of the hand list for simplicity.
     */
    public void endTurn() {
        Deck deck = Deck.getInstance();

        // If hand has more than 7 cards, discard the extras one by one
        while (hand.size() > MAX_HAND_SIZE) {
            Card discard = hand.getCards().get(0);  // always take the first card
            hand.removeCard(discard.getId());
            deck.addToDiscard(discard);
            notifyAllObservers(name + " discarded [" + discard.getName() + "] (hand limit).");
        }

        notifyAllObservers(name + "'s turn ended. Hand: " + hand.size() + " cards | Bank: " + bankArea.total() + "M");
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
