package com.monopolydeal;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Hand;
import com.monopolydeal.model.card.Card;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Hand – drawing, finding, removing, and size-limit checks.
 * Covers Sequence Diagrams 2 (Turn Start – Draw Cards).
 */
class HandTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    void testNewHandIsEmpty() {
        Hand hand = new Hand();
        assertTrue(hand.isEmpty(), "A newly created hand must be empty");
        assertEquals(0, hand.size());
    }

    @Test
    void testAddAndSize() {
        Deck deck = Deck.getInstance();
        Hand hand = new Hand();
        Card c = deck.draw(1).get(0);
        hand.add(c);
        assertEquals(1, hand.size());
        assertFalse(hand.isEmpty());
    }

    @Test
    void testFindCardById() {
        Deck deck = Deck.getInstance();
        Hand hand = new Hand();
        List<Card> drawn = deck.draw(3);
        for (Card c : drawn) hand.add(c);

        Card target = drawn.get(1);
        Card found = hand.findCard(target.getId());
        assertNotNull(found, "findCard must return the card with the given ID");
        assertEquals(target.getId(), found.getId());
    }

    @Test
    void testFindCardReturnsNullForUnknownId() {
        Hand hand = new Hand();
        assertNull(hand.findCard(99999), "findCard must return null for unknown IDs");
    }

    @Test
    void testRemoveCardById() {
        Deck deck = Deck.getInstance();
        Hand hand = new Hand();
        List<Card> drawn = deck.draw(2);
        for (Card c : drawn) hand.add(c);

        Card toRemove = drawn.get(0);
        Card removed = hand.removeCard(toRemove.getId());
        assertNotNull(removed);
        assertEquals(toRemove.getId(), removed.getId());
        assertEquals(1, hand.size(), "Hand must shrink by 1 after removal");
        assertNull(hand.findCard(toRemove.getId()), "Removed card must no longer be findable");
    }

    @Test
    void testRemoveCardReturnsNullIfNotFound() {
        Hand hand = new Hand();
        assertNull(hand.removeCard(99999), "removeCard must return null for unknown IDs");
    }

    @Test
    void testCheckLimitTrueWhenUnderLimit() {
        Deck deck = Deck.getInstance();
        Hand hand = new Hand();
        List<Card> drawn = deck.draw(5);
        for (Card c : drawn) hand.add(c);
        assertTrue(hand.checkLimit(7), "Hand of 5 must satisfy limit of 7");
    }

    @Test
    void testCheckLimitFalseWhenOverLimit() {
        Deck deck = Deck.getInstance();
        Hand hand = new Hand();
        List<Card> drawn = deck.draw(8);
        for (Card c : drawn) hand.add(c);
        assertFalse(hand.checkLimit(7), "Hand of 8 must violate limit of 7");
    }

    @Test
    void testGetCardsReturnsAllAdded() {
        Deck deck = Deck.getInstance();
        Hand hand = new Hand();
        List<Card> drawn = deck.draw(4);
        for (Card c : drawn) hand.add(c);
        assertEquals(4, hand.getCards().size());
    }
}
