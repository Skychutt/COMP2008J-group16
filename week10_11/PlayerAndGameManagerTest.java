package com.monopolydeal.model;

import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import com.monopolydeal.enums.PropertyType;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Player and GameManager (Week 10-11).
 *
 * Tests cover:
 *  1.  Player creation and initial state
 *  2.  draw() – normal path (draw 2 cards)
 *  3.  draw() – empty hand path (draw 5 cards)
 *  4.  putMoneyInBank() – money card deposited correctly
 *  5.  putMoneyInBank() – property card rejected
 *  6.  putMoneyInBank() – no actions left
 *  7.  placeProperty() – property card placed in property area
 *  8.  playCard() – card routed via executePlay()
 *  9.  payAmount() – correct cards removed from bank
 * 10.  payAmount() – no-change rule (excess value forfeited)
 * 11.  receivePayment() – cards added to bank
 * 12.  endTurn() – hand trimmed to 7 cards
 * 13.  GameManager initGame() – players created and dealt 5 cards each
 * 14.  GameManager nextTurn() – turn index advances and wraps around
 * 15.  GameManager checkWin() – detects winner with 3 complete sets
 */
class PlayerAndGameManagerTest {

    // ---- helpers --------------------------------------------------------

    /** Create a fresh MoneyCard (bypasses Deck). */
    private MoneyCard money(int value) {
        return new MoneyCard(value + "M", value, value);
    }

    /** Create a fresh PropertyCard of the given color. */
    private PropertyCard property(PropertyType color, int value) {
        return new PropertyCard(color.name() + "-Prop", value, color, false);
    }

    /**
     * Return a Player whose turn is already set up:
     * actions = 3, and one card already in hand so draw() takes the 2-card path.
     */
    private Player playerReadyForTurn() {
        Deck.reset();   // fresh deck for each test
        Player p = new Player("t", "Tester");
        p.getHand().add(money(1));          // hand not empty → draw 2 (not 5)
        p.setActions(Player.ACTIONS_PER_TURN);
        return p;
    }

    // =====================================================================
    //  1. Player creation
    // =====================================================================

    @Test
    void test01_playerInitialState() {
        Player p = new Player("1", "Alice");
        assertEquals("Alice", p.getName());
        assertEquals(0, p.getActions());
        assertTrue(p.getHand().isEmpty());
        assertEquals(0, p.getBankArea().total());
        assertEquals(0, p.getPropertyArea().countCompleteSets());
    }

    // =====================================================================
    //  2. draw() – normal path
    // =====================================================================

    @Test
    void test02_drawNormalPath() {
        Deck.reset();
        Player p = new Player("1", "Alice");
        p.getHand().add(money(1));          // hand NOT empty → should draw 2
        int before = p.getHand().size();    // 1

        p.draw();

        // Should have drawn exactly 2 more cards
        assertEquals(before + 2, p.getHand().size());
    }

    // =====================================================================
    //  3. draw() – empty hand path
    // =====================================================================

    @Test
    void test03_drawEmptyHandPath() {
        Deck.reset();
        Player p = new Player("1", "Alice");
        // Hand is empty → should draw 5
        assertTrue(p.getHand().isEmpty());

        p.draw();

        assertEquals(5, p.getHand().size());
    }

    // =====================================================================
    //  4. putMoneyInBank() – success
    // =====================================================================

    @Test
    void test04_putMoneyInBank_success() {
        Player p = playerReadyForTurn();
        MoneyCard m = money(3);
        p.getHand().add(m);

        p.putMoneyInBank(m.getId());

        // Card removed from hand, added to bank
        assertFalse(p.getHand().getCards().contains(m));
        assertEquals(3, p.getBankArea().total());
        assertEquals(2, p.getActions());    // 1 action used
    }

    // =====================================================================
    //  5. putMoneyInBank() – property card is rejected
    // =====================================================================

    @Test
    void test05_putMoneyInBank_rejectsPropertyCard() {
        Player p = playerReadyForTurn();
        PropertyCard prop = property(PropertyType.RED, 3);
        p.getHand().add(prop);

        p.putMoneyInBank(prop.getId());

        // Property card must still be in hand, not in bank
        assertTrue(p.getHand().getCards().contains(prop));
        assertEquals(0, p.getBankArea().total());
        assertEquals(3, p.getActions());    // no action consumed
    }

    // =====================================================================
    //  6. putMoneyInBank() – no actions left
    // =====================================================================

    @Test
    void test06_putMoneyInBank_noActionsLeft() {
        Player p = new Player("1", "Alice");
        p.setActions(0);                    // no actions
        MoneyCard m = money(2);
        p.getHand().add(m);

        p.putMoneyInBank(m.getId());

        // Card should still be in hand (action was rejected)
        assertTrue(p.getHand().getCards().contains(m));
        assertEquals(0, p.getBankArea().total());
    }

    // =====================================================================
    //  7. placeProperty() – property card placed correctly
    // =====================================================================

    @Test
    void test07_placeProperty_success() {
        Player p = playerReadyForTurn();
        PropertyCard prop = property(PropertyType.GREEN, 4);
        p.getHand().add(prop);

        p.placeProperty(prop.getId());

        // Removed from hand, visible in property area
        assertFalse(p.getHand().getCards().contains(prop));
        assertNotNull(p.getPropertyArea().getSet(PropertyType.GREEN));
        assertEquals(2, p.getActions());    // 1 action consumed
    }

    // =====================================================================
    //  8. playCard() – routes via executePlay (MoneyCard → bank)
    // =====================================================================

    @Test
    void test08_playCard_moneyCardGoesToBank() {
        Player p = playerReadyForTurn();
        MoneyCard m = money(5);
        p.getHand().add(m);

        // MoneyCard.executePlay() calls p.getBankArea().add(this)
        p.playCard(m.getId());

        assertFalse(p.getHand().getCards().contains(m));
        assertEquals(5, p.getBankArea().total());
        assertEquals(2, p.getActions());
    }

    // =====================================================================
    //  9. payAmount() – correct cards removed from bank
    // =====================================================================

    @Test
    void test09_payAmount_removesFromBank() {
        Player p = new Player("1", "Alice");
        MoneyCard m3 = money(3);
        MoneyCard m2 = money(2);
        p.getBankArea().add(m3);
        p.getBankArea().add(m2);

        List<Card> paid = p.payAmount(4);

        // At least 4M must be paid; cards are removed from bank
        int totalPaid = paid.stream().mapToInt(Card::getValue).sum();
        assertTrue(totalPaid >= 4);
        // The paid cards must no longer be in the bank
        for (Card c : paid) {
            assertFalse(p.getBankArea().getMoney().contains(c));
        }
    }

    // =====================================================================
    // 10. payAmount() – no-change rule
    // =====================================================================

    @Test
    void test10_payAmount_noChangeRule() {
        Player p = new Player("1", "Alice");
        MoneyCard m10 = money(10);
        p.getBankArea().add(m10);

        // Owes only 3M but bank only has a 10M card → entire 10M card is paid, no change
        List<Card> paid = p.payAmount(3);

        assertEquals(1, paid.size());
        assertEquals(10, paid.get(0).getValue());   // whole card transferred
        assertEquals(0, p.getBankArea().total());   // bank is now empty
    }

    // =====================================================================
    // 11. receivePayment() – cards added to bank
    // =====================================================================

    @Test
    void test11_receivePayment() {
        Player recipient = new Player("2", "Bob");
        List<Card> incoming = List.of(money(3), money(2));

        recipient.receivePayment(incoming);

        assertEquals(5, recipient.getBankArea().total());
        assertEquals(2, recipient.getBankArea().size());
    }

    // =====================================================================
    // 12. endTurn() – hand trimmed to 7 cards
    // =====================================================================

    @Test
    void test12_endTurn_trimsHandToLimit() {
        Deck.reset();
        Player p = new Player("1", "Alice");

        // Give the player 10 cards (3 over the limit)
        for (int i = 0; i < 10; i++) {
            p.getHand().add(money(1));
        }
        assertEquals(10, p.getHand().size());

        p.endTurn();

        // Hand must be <= 7 after endTurn
        assertTrue(p.getHand().size() <= Player.MAX_HAND_SIZE,
                "Hand size after endTurn should be at most 7, was: " + p.getHand().size());
    }

    @Test
    void test12b_endTurn_noDiscardIfUnderLimit() {
        Deck.reset();
        Player p = new Player("1", "Alice");
        for (int i = 0; i < 5; i++) p.getHand().add(money(1));

        p.endTurn();

        // Under limit → no cards discarded
        assertEquals(5, p.getHand().size());
    }

    // =====================================================================
    // 13. GameManager initGame() – correct setup
    // =====================================================================

    @Test
    void test13_initGame_setupCorrectly() {
        Deck.reset();
        GameManager.reset();
        GameManager gm = GameManager.getInstance();

        gm.initGame(3);

        assertEquals(3, gm.getPlayers().size());
        // Every player should have 5 cards dealt
        for (Player p : gm.getPlayers()) {
            assertEquals(5, p.getHand().size(), p.getName() + " should have 5 cards");
        }
    }

    // =====================================================================
    // 14. GameManager nextTurn() – index advances and wraps
    // =====================================================================

    @Test
    void test14_nextTurn_advancesAndWraps() {
        Deck.reset();
        GameManager.reset();
        GameManager gm = GameManager.getInstance();
        gm.initGame(3);     // players: index 0, 1, 2

        // Give Player 1 (index 0) 3 actions so nextTurn() doesn't block
        gm.getCurrentPlayer().setActions(3);
        assertEquals("Player 1", gm.getCurrentPlayer().getName());

        gm.nextTurn();
        assertEquals("Player 2", gm.getCurrentPlayer().getName());

        gm.getCurrentPlayer().setActions(3);
        gm.nextTurn();
        assertEquals("Player 3", gm.getCurrentPlayer().getName());

        gm.getCurrentPlayer().setActions(3);
        gm.nextTurn();
        // Wraps back to Player 1
        assertEquals("Player 1", gm.getCurrentPlayer().getName());
    }

    // =====================================================================
    // 15. GameManager checkWin() – detects winner
    // =====================================================================

    @Test
    void test15_checkWin_detectsWinner() {
        GameManager.reset();
        GameManager gm = GameManager.getInstance();
        Deck.reset();
        gm.initGame(2);

        // Give Player 1 three complete property sets manually
        Player p1 = gm.getPlayers().get(0);

        // Brown set (needs 2)
        p1.getPropertyArea().add(property(PropertyType.BROWN, 1));
        p1.getPropertyArea().add(property(PropertyType.BROWN, 1));

        // Blue set (needs 2)
        p1.getPropertyArea().add(property(PropertyType.BLUE, 4));
        p1.getPropertyArea().add(property(PropertyType.BLUE, 4));

        // Utility / LIGHTGREEN set (needs 2)
        p1.getPropertyArea().add(property(PropertyType.LIGHTGREEN, 2));
        p1.getPropertyArea().add(property(PropertyType.LIGHTGREEN, 2));

        assertTrue(gm.checkWin(), "checkWin should return true when a player has 3 complete sets");
        assertTrue(gm.isGameOver());
    }

    @Test
    void test15b_checkWin_noWinnerYet() {
        GameManager.reset();
        GameManager gm = GameManager.getInstance();
        Deck.reset();
        gm.initGame(2);

        // Only give Player 1 two complete sets (not enough)
        Player p1 = gm.getPlayers().get(0);
        p1.getPropertyArea().add(property(PropertyType.BROWN, 1));
        p1.getPropertyArea().add(property(PropertyType.BROWN, 1));
        p1.getPropertyArea().add(property(PropertyType.BLUE, 4));
        p1.getPropertyArea().add(property(PropertyType.BLUE, 4));

        assertFalse(gm.checkWin(), "checkWin should return false when no player has 3 complete sets");
    }
}
