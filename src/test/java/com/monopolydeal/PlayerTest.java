package com.monopolydeal;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Player – drawing, placing properties, banking money, and end-of-turn discard.
 * Covers Sequence Diagrams 2 (Draw), 3 (Money), 4 (Property), and 9 (End Turn).
 */
class PlayerTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    void testPlayerInitialization() {
        Player player = new Player("1", "Alice");
        assertEquals("Alice", player.getName());
        assertEquals("1", player.getId());
        assertEquals(0, player.getActions());
        assertTrue(player.getHand().isEmpty());
        assertEquals(0, player.getBankArea().total());
    }

    @Test
    void testDrawTwoCardsNormally() {
        Player player = new Player("1", "Alice");
        player.setActions(3);
        // Hand is not empty so should draw 2
        Deck.getInstance().draw(1); // warm up counter
        Deck.reset();
        player = new Player("1", "Alice");
        // Add one card so hand is not empty
        player.getHand().add(Deck.getInstance().draw(1).get(0));
        int before = player.getHand().size();
        player.draw();
        assertEquals(before + 2, player.getHand().size(), "Non-empty hand: player must draw exactly 2 cards");
    }

    @Test
    void testDrawFiveCardsWhenHandEmpty() {
        Player player = new Player("1", "Alice");
        assertTrue(player.getHand().isEmpty(), "Hand must be empty before drawing");
        player.draw();
        assertEquals(5, player.getHand().size(), "Empty hand: player must draw 5 cards");
    }

    @Test
    void testPlacePropertyConsumesAction() {
        Deck deck = Deck.getInstance();
        Player player = new Player("1", "Alice");
        player.setActions(3);
        PropertyCard pc = new PropertyCard("TestProp", 1, PropertyType.BROWN, false);
        player.getHand().add(pc);

        player.placeProperty(pc.getId());

        assertEquals(2, player.getActions(), "Placing a property must consume 1 action");
        assertEquals(1, player.getPropertyArea().getSets().size());
        assertNull(player.getHand().findCard(pc.getId()), "Card must leave hand after placement");
    }

    @Test
    void testPlacePropertyFailsWithNoActions() {
        Player player = new Player("1", "Alice");
        player.setActions(0);
        PropertyCard pc = new PropertyCard("TestProp", 1, PropertyType.BROWN, false);
        player.getHand().add(pc);

        player.placeProperty(pc.getId());

        assertEquals(0, player.getPropertyArea().getSets().size(), "Property must not be placed without actions");
    }

    @Test
    void testPutMoneyInBankConsumesAction() {
        Player player = new Player("1", "Alice");
        player.setActions(3);
        MoneyCard money = new MoneyCard("2M", 2, 2);
        player.getHand().add(money);

        player.putMoneyInBank(money.getId());

        assertEquals(2, player.getActions(), "Depositing a money card must consume 1 action");
        assertEquals(2, player.getBankArea().total());
    }

    @Test
    void testPutPropertyCardWithValueInBankIsAccepted() {
        // Rule updated: property cards with value > 0 can be banked as money
        Player player = new Player("1", "Alice");
        player.setActions(3);
        PropertyCard pc = new PropertyCard("TestProp", 1, PropertyType.BROWN, false);
        player.getHand().add(pc);

        boolean result = player.putMoneyInBank(pc.getId());

        assertTrue(result, "Property card with value > 0 must be bankable");
        assertEquals(1, player.getBankArea().total(), "Property card value must be added to bank");
        assertEquals(2, player.getActions(), "Action must be consumed for a valid bank deposit");
    }

    @Test
    void testEndTurnHandLimitEnforcedByGameLogic() {
        // Hand-limit enforcement moved to GameLogic.enforceEndTurnDiscard() (not Player.finalizeTurnEnd).
        // Verify GameLogic.endTurn() correctly reduces oversized hand.
        GameManager.reset();
        Deck.reset();
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        com.monopolydeal.logic.GameLogic logic = new com.monopolydeal.logic.GameLogic(gm);
        logic.startGame();

        Player player = gm.getCurrentPlayer();
        player.getHand().getCards().clear();
        for (int i = 0; i < 9; i++) {
            player.getHand().add(new MoneyCard("1M", 1, 1));
        }

        // enforceEndTurnDiscard fires: choose card "1" twice
        System.setIn(new java.io.ByteArrayInputStream("1\n1\n".getBytes()));
        logic.getActionHandler().refreshScanner();
        logic.endTurn();

        assertTrue(player.getHand().size() <= Player.MAX_HAND_SIZE,
                "Hand must be reduced to <= 7 after GameLogic.endTurn()");
    }

    @Test
    void testEndTurnDoesNotDiscardWhenUnderLimit() {
        Player player = new Player("1", "Alice");
        for (int i = 0; i < 5; i++) {
            player.getHand().add(new MoneyCard("1M", 1, 1));
        }
        player.finalizeTurnEnd();
        assertEquals(5, player.getHand().size(), "Cards under the limit must not be discarded");
    }

    @Test
    void testReceivePaymentGoesToBank() {
        Player player = new Player("1", "Alice");
        MoneyCard money = new MoneyCard("5M", 5, 5);
        player.receivePayment(List.of(money));
        assertEquals(5, player.getBankArea().total());
    }

    @Test
    void testReceivePaymentPropertyGoesToPropertyArea() {
        Player player = new Player("1", "Alice");
        PropertyCard pc = new PropertyCard("Brown1", 1, PropertyType.BROWN, false);
        player.receivePayment(List.of(pc));
        assertNotNull(player.getPropertyArea().getSet(PropertyType.BROWN));
    }

    @Test
    void testPayAmountUsesBankFirst() {
        Player player = new Player("1", "Alice");
        MoneyCard m1 = new MoneyCard("2M", 2, 2);
        MoneyCard m2 = new MoneyCard("3M", 3, 3);
        player.getBankArea().add(m1);
        player.getBankArea().add(m2);

        List<Card> paid = player.payAmount(3);
        assertFalse(paid.isEmpty(), "Payment must return at least one card");
    }

    @Test
    void testActionsSetCorrectly() {
        Player player = new Player("1", "Alice");
        player.setActions(3);
        assertEquals(3, player.getActions());
    }

    @Test
    void testCannotPlaceNonExistentCard() {
        Player player = new Player("1", "Alice");
        player.setActions(3);
        player.placeProperty(99999);
        assertEquals(3, player.getActions(), "Action count must not change for non-existent card");
    }
}
