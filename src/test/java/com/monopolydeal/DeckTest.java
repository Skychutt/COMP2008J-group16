package com.monopolydeal;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.card.Card;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Deck (Singleton, draw/discard, reshuffle)
 * Corresponds to Sequence Diagram 1 (Start Game) and draw-phase logic.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeckTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    @Order(1)
    void testSingletonPattern() {
        Deck d1 = Deck.getInstance();
        Deck d2 = Deck.getInstance();
        assertSame(d1, d2, "Deck.getInstance() must always return the same instance");
    }

    @Test
    @Order(2)
    void testResetCreatesFreshInstance() {
        Deck original = Deck.getInstance();
        Deck.reset();
        Deck fresh = Deck.getInstance();
        assertNotSame(original, fresh, "After reset(), getInstance() must return a new object");
    }

    @Test
    @Order(3)
    void testInitialDeckSize() {
        Deck deck = Deck.getInstance();
        assertEquals(Deck.TOTAL_PLAYABLE_CARDS, deck.drawPileSize(),
                "Deck must start with exactly 106 playable cards");
    }

    @Test
    @Order(4)
    void testShuffleDoesNotChangeDeckSize() {
        Deck deck = Deck.getInstance();
        int before = deck.drawPileSize();
        deck.shuffle();
        assertEquals(before, deck.drawPileSize(), "Shuffle must not add or remove cards");
    }

    @Test
    @Order(5)
    void testDrawReturnsCorrectCount() {
        Deck deck = Deck.getInstance();
        List<Card> drawn = deck.draw(2);
        assertEquals(2, drawn.size(), "draw(2) must return exactly 2 cards");
        assertEquals(104, deck.drawPileSize(), "Draw pile must shrink by 2 after drawing 2 cards");
    }

    @Test
    @Order(6)
    void testDrawFiveCards() {
        Deck deck = Deck.getInstance();
        List<Card> drawn = deck.draw(5);
        assertEquals(5, drawn.size(), "draw(5) must return exactly 5 cards");
        assertEquals(101, deck.drawPileSize());
    }

    @Test
    @Order(7)
    void testAddToDiscardIncreasesDiscardPile() {
        Deck deck = Deck.getInstance();
        Card card = deck.draw(1).get(0);
        assertEquals(0, deck.discardSize());
        deck.addToDiscard(card);
        assertEquals(1, deck.discardSize(), "Discard pile must grow after addToDiscard()");
    }

    @Test
    @Order(8)
    void testReshuffleDiscardWhenDrawPileEmpty() {
        Deck deck = Deck.getInstance();
        // Draw all cards
        List<Card> all = deck.draw(Deck.TOTAL_PLAYABLE_CARDS);
        assertEquals(Deck.TOTAL_PLAYABLE_CARDS, all.size());
        assertEquals(0, deck.drawPileSize());
        // Discard them all
        for (Card c : all) deck.addToDiscard(c);
        assertEquals(Deck.TOTAL_PLAYABLE_CARDS, deck.discardSize());
        // Drawing again should trigger reshuffle
        List<Card> redrawn = deck.draw(1);
        assertEquals(1, redrawn.size(), "Drawing from empty pile should reshuffle discard");
    }

    @Test
    @Order(9)
    void testDrawFromEmptyDeckReturnsEmptyList() {
        Deck deck = Deck.getInstance();
        deck.draw(Deck.TOTAL_PLAYABLE_CARDS); // exhaust deck without discarding
        List<Card> result = deck.draw(1);
        assertTrue(result.isEmpty(), "Drawing from exhausted deck with no discard should return empty list");
    }

    @Test
    @Order(10)
    void testDrawnCardsAreNotNull() {
        Deck deck = Deck.getInstance();
        List<Card> cards = deck.draw(10);
        for (Card c : cards) {
            assertNotNull(c, "No drawn card should be null");
        }
    }
}
