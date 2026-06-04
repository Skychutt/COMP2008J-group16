package com.monopolydeal.model;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.factory.StandardCardFactory;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Singleton class representing the game's card deck.
 * Manages the draw pile and discard pile for the Monopoly Deal game.
 * Initializes all playable cards on construction and provides shuffle, draw, and discard operations.
 * Only one Deck instance exists per game (Singleton pattern).
 */
public class Deck {
    /** Singleton instance of the Deck. */
    private static Deck instance;

    private Stack<Card> drawPile;           // The main draw pile players draw from
    private Stack<Card> discard;            // The discard pile for used/excess cards
    private StandardCardFactory factory;    // Factory used to create card instances
    private int totalDiscardedCount;        // Cards discarded (discard pile + hand-limit to draw bottom)
    private Card lastDiscardedCard;         // Most recently discarded card (for UI preview)

    /**
     * Private constructor - initializes empty piles, creates the factory,
     * and populates the deck with all 110 cards.
     */
    private Deck() {
        drawPile = new Stack<>();
        discard = new Stack<>();
        factory = new StandardCardFactory();
        initializeCards();
    }

    /**
     * Get the singleton Deck instance. Creates a new one if it doesn't exist.
     * @return the singleton Deck instance
     */
    public static Deck getInstance() {
        if (instance == null) {
            instance = new Deck();
        }
        return instance;
    }

    /** Reset the singleton instance (used for starting a new game). */
    public static void reset() {
        instance = null;
    }

    /**
     * Initialize all playable cards in the deck.
     * Composition follows the official card list (106 playable cards):
     * Money (20), Property (28), Wild Property (11), Rent (13), Action (34).
     */
    private void initializeCards() {
        // ==================== Money Cards (20 cards) ====================
        // 1M x 6
        for (int i = 0; i < 6; i++) drawPile.push(factory.createMoney(1));
        // 2M x 5
        for (int i = 0; i < 5; i++) drawPile.push(factory.createMoney(2));
        // 3M x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createMoney(3));
        // 4M x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createMoney(4));
        // 5M x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createMoney(5));
        // 10M x 1
        drawPile.push(factory.createMoney(10));

        // ==================== Property Cards (28 cards) ====================
        // Brown x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createProperty(PropertyType.BROWN));
        // Light Blue x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createProperty(PropertyType.LIGHTBLUE));
        // Purple (Magenta) x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createProperty(PropertyType.PURPLE));
        // Orange x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createProperty(PropertyType.ORANGE));
        // Red x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createProperty(PropertyType.RED));
        // Yellow x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createProperty(PropertyType.YELLOW));
        // Green x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createProperty(PropertyType.GREEN));
        // Blue x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createProperty(PropertyType.BLUE));
        // Railroad (Black) x 4
        for (int i = 0; i < 4; i++) drawPile.push(factory.createProperty(PropertyType.BLACK));
        // Utility (Light Green) x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createProperty(PropertyType.LIGHTGREEN));

        // ==================== Wild Property Cards (11 cards) ====================
        // Rainbow Wild x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createProperty(PropertyType.RAINBOW));
        // Two-color wild cards (9 cards) - represented as wild properties with initial color
        drawPile.push(new PropertyCard("Wild Red/Yellow", 3, PropertyType.RED, true));
        drawPile.push(new PropertyCard("Wild Red/Yellow", 3, PropertyType.RED, true));
        drawPile.push(new PropertyCard("Wild Blue/Green", 4, PropertyType.BLUE, true));
        drawPile.push(new PropertyCard("Wild Green/Black", 4, PropertyType.GREEN, true));
        drawPile.push(new PropertyCard("Wild Brown/LightBlue", 1, PropertyType.BROWN, true));
        drawPile.push(new PropertyCard("Wild LightBlue/Black", 4, PropertyType.LIGHTBLUE, true));
        drawPile.push(new PropertyCard("Wild Purple/Orange", 2, PropertyType.PURPLE, true));
        drawPile.push(new PropertyCard("Wild Black/LightGreen", 2, PropertyType.BLACK, true));
        drawPile.push(new PropertyCard("Wild LightBlue/Brown", 1, PropertyType.LIGHTBLUE, true));

        // ==================== Rent Cards (13 cards) ====================
        // Multi-color rent (wild rent) x 3
        for (int i = 0; i < 3; i++) {
            drawPile.push(new com.monopolydeal.model.card.ActionCard("Rent (Any Color)", 3, ActionType.DOUBLE_RENT, true));
        }
        // Two-color rent cards x 10 (2 each for 5 pairs)
        for (int i = 0; i < 2; i++) drawPile.push(new com.monopolydeal.model.card.ActionCard("Rent Blue/Green", 1, ActionType.DOUBLE_RENT, true));
        for (int i = 0; i < 2; i++) drawPile.push(new com.monopolydeal.model.card.ActionCard("Rent Red/Yellow", 1, ActionType.DOUBLE_RENT, true));
        for (int i = 0; i < 2; i++) drawPile.push(new com.monopolydeal.model.card.ActionCard("Rent Purple/Orange", 1, ActionType.DOUBLE_RENT, true));
        for (int i = 0; i < 2; i++) drawPile.push(new com.monopolydeal.model.card.ActionCard("Rent Black/LightGreen", 1, ActionType.DOUBLE_RENT, true));
        for (int i = 0; i < 2; i++) drawPile.push(new com.monopolydeal.model.card.ActionCard("Rent Brown/LightBlue", 1, ActionType.DOUBLE_RENT, true));

        // ==================== Action Cards (34 cards) ====================
        // Pass Go x 10
        for (int i = 0; i < 10; i++) drawPile.push(factory.createAction(ActionType.GO_PASS));
        // It's My Birthday x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createAction(ActionType.BIRTHDAY));
        // Debt Collector x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createAction(ActionType.DEBT_DEAL));
        // Just Say No x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createAction(ActionType.JUST_SAY_NO));
        // Sly Deal x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createAction(ActionType.SLY_DEAL));
        // Forced Deal x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createAction(ActionType.FORCED_DEAL));
        // Deal Breaker x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createAction(ActionType.DEAL_BREAKER));
        // Double The Rent x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createAction(ActionType.DOUBLE_RENT));
        // House x 3
        for (int i = 0; i < 3; i++) drawPile.push(factory.createAction(ActionType.HOUSE));
        // Hotel x 2
        for (int i = 0; i < 2; i++) drawPile.push(factory.createAction(ActionType.HOTEL));
    }

    /** Randomly shuffle all cards in the draw pile. */
    public void shuffle() {
        List<Card> list = new ArrayList<>(drawPile);
        Collections.shuffle(list);
        drawPile.clear();
        for (Card c : list) {
            drawPile.push(c);
        }
    }

    /**
     * Draw the specified number of cards from the draw pile.
     * If the draw pile is empty, the discard pile is reshuffled and reused.
     * @param count the number of cards to draw
     * @return list of drawn cards (may be fewer than requested if deck is exhausted)
     */
    public List<Card> draw(int count) {
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (drawPile.isEmpty()) {
                reshuffleDiscard();
            }
            if (!drawPile.isEmpty()) {
                drawn.add(drawPile.pop());
            }
        }
        return drawn;
    }

    /**
     * Add a card to the discard pile.
     * @param c the card to discard
     */
    public void addToDiscard(Card c) {
        if (c == null) {
            return;
        }
        discard.push(c);
        recordDiscard(c);
    }

    /**
     * Put a card at the bottom of draw pile.
     * Used for end-turn hand-limit discards per official rule.
     */
    public void addToDrawPileBottom(Card c) {
        if (c == null) {
            return;
        }
        drawPile.insertElementAt(c, 0);
        recordDiscard(c);
    }

    private void recordDiscard(Card c) {
        totalDiscardedCount++;
        lastDiscardedCard = c;
    }

    /**
     * Reshuffle the discard pile back into the draw pile.
     * Called automatically when the draw pile runs out of cards.
     */
    private void reshuffleDiscard() {
        if (!discard.isEmpty()) {
            List<Card> list = new ArrayList<>(discard);
            Collections.shuffle(list);
            discard.clear();
            for (Card c : list) {
                drawPile.push(c);
            }
        }
    }

    /** @return the number of cards remaining in the draw pile */
    public int drawPileSize() {
        return drawPile.size();
    }

    /** @return the number of cards in the discard pile */
    public int discardSize() {
        return discard.size();
    }

    /** Total cards discarded this game (actions to discard pile + hand-limit discards). */
    public int getTotalDiscardedCount() {
        return totalDiscardedCount;
    }

    /**
     * Top of the discard pile, or the last hand-limit discard if the pile is empty.
     */
    public Card getVisibleDiscardTop() {
        if (!discard.isEmpty()) {
            return discard.peek();
        }
        return lastDiscardedCard;
    }

    /** @return the draw pile stack */
    public Stack<Card> getDrawPile() {
        return drawPile;
    }

    /** @return the discard pile stack */
    public Stack<Card> getDiscard() {
        return discard;
    }
}
