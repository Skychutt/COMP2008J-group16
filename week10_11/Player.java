package com.monopolydeal.model;

import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.interfaces.ISubject;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player in the Monopoly Deal game.
 * Each player has a hand, a bank area, and a property area.
 * Implements ISubject (Observer pattern) to notify observers of player actions.
 *
 * Week 10-11 completed:
 *  - draw()               : draw 2 cards normally, 5 if hand is empty
 *  - playCard()           : play any card from hand, consume 1 action
 *  - putMoneyInBank()     : deposit a money/action card to bank, consume 1 action
 *  - payAmount()          : pay a required amount from bank or property area
 *  - endTurn()            : enforce 7-card hand limit at end of turn
 *
 * Week 11-12 stub (for the next member):
 *  - receivePayment()     : receive transferred cards from another player
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

    // =====================================================================
    //  Getters & Setters
    // =====================================================================

    /** @return the unique player ID */
    public String getId() { return id; }

    /** @return the player's display name */
    public String getName() { return name; }

    /** @return the number of remaining actions this turn */
    public int getActions() { return actions; }

    /** Set the number of remaining actions for this turn. */
    public void setActions(int actions) { this.actions = actions; }

    /** @return the player's hand */
    public Hand getHand() { return hand; }

    /** @return the player's bank area */
    public BankArea getBankArea() { return bankArea; }

    /** @return the player's property area */
    public PropertyArea getPropertyArea() { return propertyArea; }

    // =====================================================================
    //  Core Turn Actions  (Week 10-11)
    // =====================================================================

    /**
     * Draw cards from the deck at the start of a turn.
     * Rule: draw 2 cards normally; draw 5 cards if hand is empty (recovery mechanism).
     */
    public void draw() {
        Deck deck = Deck.getInstance();
        int count = hand.isEmpty() ? 5 : 2;   // empty hand → draw 5, else draw 2
        List<Card> drawn = deck.draw(count);
        for (Card c : drawn) {
            hand.add(c);
        }
        notifyAllObservers(name + " drew " + drawn.size() + " card(s). Hand size: " + hand.size());
    }

    /**
     * Play a card from the hand by its unique ID.
     * The card's executePlay() is called, which routes it to bank or property area automatically.
     * Consumes 1 action. Does nothing if no actions remain or the card is not found.
     * @param cardId the unique ID of the card to play
     */
    public void playCard(int cardId) {
        // Check actions first
        if (actions <= 0) {
            notifyAllObservers(name + " has no actions left!");
            return;
        }

        // Remove card from hand
        Card c = hand.removeCard(cardId);
        if (c == null) {
            notifyAllObservers("Card not found in hand (id=" + cardId + ")");
            return;
        }

        // Card's own executePlay handles the routing (bank / property area / action effect)
        c.executePlay(this);
        actions--;
        notifyAllObservers(name + " played [" + c.getName() + "]. Actions left: " + actions);
    }

    /**
     * Deposit a money or action card from hand into the bank area.
     * Property cards cannot be deposited as money.
     * Consumes 1 action.
     * @param cardId the unique ID of the card to deposit
     */
    public void putMoneyInBank(int cardId) {
        if (actions <= 0) {
            notifyAllObservers(name + " has no actions left!");
            return;
        }

        // Find the card without removing it first, so we can validate its type
        Card found = null;
        for (Card c : hand.getCards()) {
            if (c.getId() == cardId) {
                found = c;
                break;
            }
        }

        if (found == null) {
            notifyAllObservers("Card not found in hand (id=" + cardId + ")");
            return;
        }

        // Property cards cannot be deposited into the bank
        if (found instanceof PropertyCard) {
            notifyAllObservers("Cannot put a Property Card into the bank!");
            return;
        }

        // Now safely remove from hand and add to bank
        hand.removeCard(cardId);
        bankArea.add(found);
        actions--;
        notifyAllObservers(name + " deposited [" + found.getName() + "] to bank. Bank total: " + bankArea.total() + "M");
    }

    /**
     * Place a property card from hand into the property area.
     * Consumes 1 action.
     * @param cardId the unique ID of the property card to place
     */
    public void placeProperty(int cardId) {
        if (actions <= 0) {
            notifyAllObservers(name + " has no actions left!");
            return;
        }

        // Validate it is actually a property card before removing
        Card found = null;
        for (Card c : hand.getCards()) {
            if (c.getId() == cardId) {
                found = c;
                break;
            }
        }

        if (found == null) {
            notifyAllObservers("Card not found in hand (id=" + cardId + ")");
            return;
        }

        if (!(found instanceof PropertyCard)) {
            notifyAllObservers("Card [" + found.getName() + "] is not a Property Card!");
            return;
        }

        hand.removeCard(cardId);
        propertyArea.add(found);       // PropertyArea.add() handles grouping by color
        actions--;
        notifyAllObservers(name + " placed property [" + found.getName() + "]. "
                + "Complete sets: " + propertyArea.countCompleteSets());
    }

    /**
     * Pay a required amount using cards from the bank.
     * Cards are selected automatically from lowest value first (greedy approach).
     * No-change rule: if the card value exceeds the amount, the excess is forfeited.
     * The paid cards are returned so the caller can transfer them to the recipient.
     *
     * @param amount the amount to pay in millions
     * @return list of cards that were taken out of the bank as payment
     */
    public List<Card> payAmount(int amount) {
        List<Card> paid = new ArrayList<>();
        int totalPaid = 0;

        // Make a copy so we can safely iterate while modifying bankArea
        List<Card> bankCards = new ArrayList<>(bankArea.getMoney());

        for (Card c : bankCards) {
            if (totalPaid >= amount) break;   // Paid enough, stop
            bankArea.remove(c);
            paid.add(c);
            totalPaid += c.getValue();
        }

        // If bank was not enough, also take property cards (no-change rule still applies)
        if (totalPaid < amount) {
            List<Card> propCards = new ArrayList<>();
            for (var set : propertyArea.getSets()) {
                propCards.addAll(set.getCards());
            }
            for (Card c : propCards) {
                if (totalPaid >= amount) break;
                propertyArea.remove(c);
                paid.add(c);
                totalPaid += c.getValue();
            }
        }

        notifyAllObservers(name + " paid " + totalPaid + "M (owed " + amount + "M).");
        return paid;    // Caller (GameManager / rent logic) transfers these to the recipient
    }

    /**
     * Receive cards transferred from another player (e.g., as rent payment).
     * All received cards go into this player's bank area.
     *
     * Week 11-12 note for next member:
     *  The rent logic in GameManager calls payer.payAmount(amount) to get the cards,
     *  then calls recipient.receivePayment(cards) to give them here.
     *
     * @param cards the list of cards received
     */
    public void receivePayment(List<Card> cards) {
        for (Card c : cards) {
            bankArea.add(c);
        }
        notifyAllObservers(name + " received " + cards.size() + " card(s) as payment. Bank: " + bankArea.total() + "M");
    }

    /**
     * End the current turn.
     * Enforces the 7-card hand limit: if more than 7 cards remain, the player must
     * discard down to 7. The discarded cards are added to the deck's discard pile.
     *
     * Note: In a GUI version, the player would choose which cards to discard.
     * Here we auto-discard from the end of the hand list for simplicity.
     */
    public void endTurn() {
        Deck deck = Deck.getInstance();

        // Discard excess cards until hand size <= MAX_HAND_SIZE
        while (hand.size() > MAX_HAND_SIZE) {
            // Take the last card in hand and discard it
            List<Card> cards = hand.getCards();
            Card discard = cards.get(cards.size() - 1);
            hand.removeCard(discard.getId());
            deck.addToDiscard(discard);
            notifyAllObservers(name + " discarded [" + discard.getName() + "] (hand limit).");
        }

        notifyAllObservers(name + "'s turn ended. Hand: " + hand.size() + " cards | Bank: " + bankArea.total() + "M");
    }

    // =====================================================================
    //  Observer pattern (ISubject)
    // =====================================================================

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

    /** Internal helper to broadcast an event to all registered observers. */
    private void notifyAllObservers(String event) {
        for (IGameObserver o : observers) {
            o.onGameUpdate(event);
        }
    }

    /** Handle incoming game event updates (as an observer of GameManager). */
    public void onGameUpdate(String event) {
        // Player can react to game events here if needed
    }

    // =====================================================================
    //  Utility
    // =====================================================================

    @Override
    public String toString() {
        return "Player: " + name
                + " | " + bankArea
                + " | Properties: " + propertyArea.countCompleteSets() + " complete sets"
                + " | Hand: " + hand.size() + " cards"
                + " | Actions left: " + actions;
    }
}
