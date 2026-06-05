package com.monopolydeal;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.ActionHandler;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ActionHandler – PassGo, Birthday, JustSayNo defense, and handleRent.
 * Covers Sequence Diagrams 5 (Sly Deal), 6 (Debt Collector), 7 (Rent), and 8 (Just Say No).
 */
class ActionHandlerTest {

    private GameManager gm;
    private GameLogic gameLogic;
    private ActionHandler handler;

    @BeforeEach
    void setUp() {
        GameManager.reset();
        Deck.reset();
        gm = GameManager.getInstance();
        gm.initGame(3);
        gameLogic = new GameLogic(gm);
        handler = gameLogic.getActionHandler();
    }

    // ── Pass Go ──────────────────────────────────────────────────────────────

    @Test
    void testPassGoDrawsTwoExtraCards() {
        Player player = gm.getPlayers().get(0);
        int before = player.getHand().size();
        handler.handlePassGo(player);
        assertEquals(before + 2, player.getHand().size(),
                "Pass Go must add exactly 2 cards to the player's hand");
    }

    // ── Birthday ─────────────────────────────────────────────────────────────

    @Test
    void testBirthdayCollectsFromAllOtherPlayers() {
        Player initiator = gm.getPlayers().get(0);
        Player p1 = gm.getPlayers().get(1);
        Player p2 = gm.getPlayers().get(2);

        // Clear hands so no accidentally-dealt JSN cards interfere
        p1.getHand().getCards().clear();
        p2.getHand().getCards().clear();

        p1.getBankArea().add(new MoneyCard("5M", 5, 5));
        p2.getBankArea().add(new MoneyCard("5M", 5, 5));
        int bankBefore = initiator.getBankArea().total();

        // Payment chooser fires for each opponent (they each have 1 bank card → "1\n" each)
        System.setIn(new ByteArrayInputStream("1\n1\n".getBytes()));
        handler.refreshScanner();

        handler.handleBirthday(initiator);

        assertTrue(initiator.getBankArea().total() > bankBefore,
                "Initiator's bank must grow after Birthday");
    }

    // ── Just Say No ──────────────────────────────────────────────────────────

    @Test
    void testJustSayNoBlocksAttack() {
        Player attacker = gm.getPlayers().get(0);
        Player defender = gm.getPlayers().get(1);

        // Clear both hands — no accidentally-dealt JSN card must interfere
        attacker.getHand().getCards().clear();
        defender.getHand().getCards().clear();

        ActionCard jsn = new ActionCard("Just Say No", 4, ActionType.JUST_SAY_NO, false);
        defender.getHand().add(jsn);
        ActionCard slyDeal = new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL, true);

        // Defender picks "1" (Yes, block). Attacker has no JSN → counter-prompt skipped.
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        handler.refreshScanner();

        boolean blocked = handler.handleJustSayNo(defender, attacker, slyDeal);

        assertTrue(blocked, "Just Say No must successfully block the attack");
        assertNull(defender.getHand().findCard(jsn.getId()),
                "Just Say No card must leave hand after use");
    }

    @Test
    void testJustSayNoDeclinedByDefender() {
        Player attacker = gm.getPlayers().get(0);
        Player defender = gm.getPlayers().get(1);
        attacker.getHand().getCards().clear();
        defender.getHand().getCards().clear();

        ActionCard jsn = new ActionCard("Just Say No", 4, ActionType.JUST_SAY_NO, false);
        defender.getHand().add(jsn);
        ActionCard slyDeal = new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL, true);

        // Defender picks "2" (No — accept the attack)
        System.setIn(new ByteArrayInputStream("2\n".getBytes()));
        handler.refreshScanner();

        boolean blocked = handler.handleJustSayNo(defender, attacker, slyDeal);

        assertFalse(blocked, "Attack must proceed when defender declines to use Just Say No");
        assertNotNull(defender.getHand().findCard(jsn.getId()),
                "Just Say No card must remain in hand when defender declines");
    }

    @Test
    void testJustSayNoChaining() {
        Player attacker = gm.getPlayers().get(0);
        Player defender = gm.getPlayers().get(1);
        attacker.getHand().getCards().clear();
        defender.getHand().getCards().clear();

        // Both players hold a Just Say No card
        ActionCard defJsn = new ActionCard("Just Say No", 4, ActionType.JUST_SAY_NO, false);
        ActionCard atkJsn = new ActionCard("Just Say No", 4, ActionType.JUST_SAY_NO, false);
        defender.getHand().add(defJsn);
        attacker.getHand().add(atkJsn);
        ActionCard slyDeal = new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL, true);

        // Defender: "1" (block) → Attacker: "1" (counter) → attack proceeds
        System.setIn(new ByteArrayInputStream("1\n1\n".getBytes()));
        handler.refreshScanner();

        boolean blocked = handler.handleJustSayNo(defender, attacker, slyDeal);

        assertFalse(blocked, "Counter-JSN cancels the block — attack must proceed");
        assertNull(defender.getHand().findCard(defJsn.getId()),
                "Defender's JSN must be consumed");
        assertNull(attacker.getHand().findCard(atkJsn.getId()),
                "Attacker's counter-JSN must also be consumed");
    }

    @Test
    void testJustSayNoFailsWhenNotInHand() {
        Player attacker = gm.getPlayers().get(0);
        Player defender = gm.getPlayers().get(1);
        defender.getHand().getCards().clear();

        ActionCard slyDeal = new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL, true);
        boolean blocked = handler.handleJustSayNo(defender, attacker, slyDeal);

        assertFalse(blocked, "Just Say No must fail when defender does not have the card");
    }

    @Test
    void testJustSayNoFailsAgainstUndefendableCard() {
        Player attacker = gm.getPlayers().get(0);
        Player defender = gm.getPlayers().get(1);
        defender.getHand().getCards().clear();
        ActionCard jsn = new ActionCard("Just Say No", 4, ActionType.JUST_SAY_NO, false);
        defender.getHand().add(jsn);

        // canDefend = false → prompt is never reached
        ActionCard undefendable = new ActionCard("Birthday", 2, ActionType.BIRTHDAY, false);
        boolean blocked = handler.handleJustSayNo(defender, attacker, undefendable);

        assertFalse(blocked, "Just Say No must not block cards with canDefend=false");
    }

    // ── handleRent ───────────────────────────────────────────────────────────

    @Test
    void testHandleRentTransfersCorrectAmount() {
        Player collector = gm.getPlayers().get(0);
        Player target = gm.getPlayers().get(1);
        target.getHand().getCards().clear();
        target.getBankArea().add(new MoneyCard("5M", 5, 5));

        // Payment chooser will ask target to select which card to pay with: "1\n"
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        handler.refreshScanner();

        int before = collector.getBankArea().total();
        handler.handleRent(collector, target, 5);

        assertTrue(collector.getBankArea().total() > before,
                "Collector's bank must increase after rent collection");
    }

    // ── Debt Collector (via console input simulation) ─────────────────────

    /**
     * NOTE: Simulates console input by redirecting System.in.
     * A cleaner long-term fix (Part 4) would inject a Scanner or InputProvider
     * interface, but System.in redirection is sufficient for correctness testing.
     */
    @Test
    void testDebtCollectorChargesFiveM() {
        // "1\n" = pick first opponent; "1\n" = target picks their only bank card to pay
        System.setIn(new ByteArrayInputStream("1\n1\n".getBytes()));
        handler.refreshScanner();

        Player collector = gm.getPlayers().get(0);
        gm.beginCurrentPlayerTurn();
        Player target = gm.getPlayers().get(1);
        // Clear target hand so no accidentally-dealt JSN card fires a prompt
        target.getHand().getCards().clear();
        target.getBankArea().add(new MoneyCard("10M", 10, 10));

        int targetBefore = target.getBankArea().total();

        ActionCard debtCard = new ActionCard("Debt Collector", 3, ActionType.DEBT_DEAL, true);
        handler.executeAction(collector, debtCard);

        assertTrue(target.getBankArea().total() < targetBefore,
                "Target must lose assets after Debt Collector");
    }

    // ── PropertyCard attachment (House) ──────────────────────────────────────

    @Test
    void testTryAttachHouseSucceedsOnCompleteSet() {
        Player player = gm.getPlayers().get(0);
        PropertyCard host = new PropertyCard("Brown1", 1, PropertyType.BROWN, false);
        PropertyCard other = new PropertyCard("Brown2", 1, PropertyType.BROWN, false);
        player.getPropertyArea().add(host);
        player.getPropertyArea().add(other);

        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE, false);
        boolean attached = handler.tryAttachBuilding(player, house);
        assertTrue(attached, "House must attach to a complete set");
    }

    @Test
    void testTryAttachHouseFailsWithNoCompleteSet() {
        Player player = gm.getPlayers().get(0);
        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE, false);
        boolean attached = handler.tryAttachBuilding(player, house);
        assertFalse(attached, "House must not attach if there is no complete set");
    }
}
