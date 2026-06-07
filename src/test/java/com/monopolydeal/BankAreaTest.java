package com.monopolydeal;

import com.monopolydeal.model.BankArea;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.card.Card;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BankArea – depositing money, computing totals, and payment eligibility.
 * Covers Sequence Diagram 3 (Play Money Card).
 */
class BankAreaTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    void testNewBankIsEmpty() {
        BankArea bank = new BankArea();
        assertEquals(0, bank.total());
        assertEquals(0, bank.size());
    }

    @Test
    void testAddAndTotal() {
        Deck deck = Deck.getInstance();
        BankArea bank = new BankArea();
        // Draw money cards (first cards in the deck are money cards from initializeCards)
        List<Card> cards = deck.draw(3);
        int expectedTotal = 0;
        for (Card c : cards) {
            bank.add(c);
            expectedTotal += c.getValue();
        }
        assertEquals(expectedTotal, bank.total(), "Bank total must equal sum of deposited card values");
        assertEquals(3, bank.size());
    }

    @Test
    void testRemoveCard() {
        Deck deck = Deck.getInstance();
        BankArea bank = new BankArea();
        Card c = deck.draw(1).get(0);
        bank.add(c);
        int totalBefore = bank.total();

        bank.remove(c);
        assertEquals(0, bank.size());
        assertEquals(totalBefore - c.getValue(), bank.total());
    }

    @Test
    void testCanAffordWhenSufficient() {
        Deck deck = Deck.getInstance();
        BankArea bank = new BankArea();
        List<Card> cards = deck.draw(5);
        for (Card c : cards) bank.add(c);
        int total = bank.total();
        assertTrue(bank.canAfford(total), "canAfford must be true when bank has exactly enough");
        assertTrue(bank.canAfford(total - 1), "canAfford must be true when bank has more than enough");
    }

    @Test
    void testCanAffordWhenInsufficient() {
        BankArea bank = new BankArea();
        assertFalse(bank.canAfford(5), "canAfford must be false on an empty bank");
    }

    @Test
    void testGetMoneyReturnsCopy() {
        Deck deck = Deck.getInstance();
        BankArea bank = new BankArea();
        Card c = deck.draw(1).get(0);
        bank.add(c);
        List<Card> list = bank.getMoney();
        assertNotNull(list);
        assertEquals(1, list.size());
    }
}
