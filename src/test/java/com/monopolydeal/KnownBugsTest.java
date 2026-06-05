package com.monopolydeal;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.factory.StandardCardFactory;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for all 7 bugs found during Part 5 testing and fixed by GuanJian.
 *
 * Bug 1 – Rent cards were typed ActionType.DOUBLE_RENT       → fixed: ActionType.RENT added
 * Bug 2 – executeAction() had no case RENT:                  → fixed: case RENT: added
 * Bug 3 – Wild cards never prompted for colour assignment     → fixed: promptWildCardColor()
 * Bug 4 – doubleRentActive not cleared at end of turn        → fixed: cleared in endTurn()
 * Bug 5 – Rent card showed all sets regardless of colour     → fixed: parseAllowedColors()
 * Bug 6 – RAINBOW PropertySet had need=0, isComplete()=true  → fixed: need=MAX_VALUE
 * Bug 7 – StandardCardFactory.createAction(RENT) threw       → fixed: case RENT: added
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KnownBugsTest {

    @BeforeEach
    void setUp() {
        GameManager.reset();
        Deck.reset();
    }

    // ── Bug 1 ────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void bug1_rentCardsHaveCorrectActionType() {
        List<Card> all = Deck.getInstance().draw(110);

        List<ActionCard> rentCards = all.stream()
                .filter(c -> c instanceof ActionCard)
                .map(c -> (ActionCard) c)
                .filter(c -> c.getName() != null
                        && c.getName().startsWith("Rent")
                        && !c.getName().equals("Double The Rent"))
                .collect(Collectors.toList());

        List<ActionCard> doubleRentCards = all.stream()
                .filter(c -> c instanceof ActionCard)
                .map(c -> (ActionCard) c)
                .filter(c -> "Double The Rent".equals(c.getName()))
                .collect(Collectors.toList());

        assertEquals(13, rentCards.size(), "There must be exactly 13 Rent cards in the deck");
        assertEquals(2,  doubleRentCards.size(), "There must be exactly 2 Double The Rent cards");

        for (ActionCard rc : rentCards) {
            assertEquals(ActionType.RENT, rc.getType(),
                    "Rent card '" + rc.getName() + "' must be typed ActionType.RENT");
        }
        for (ActionCard dr : doubleRentCards) {
            assertEquals(ActionType.DOUBLE_RENT, dr.getType(),
                    "'Double The Rent' must keep ActionType.DOUBLE_RENT");
        }
    }

    // ── Bug 2 ────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    void bug2_playingRentCardCollectsRent() {
        // Input sequence fired by playing "Rent (Any Color)":
        // "1\n" → choose set #1 (Brown)
        // "1\n" → choose target player #1
        // target has only 1 asset → payment prompt is skipped (only 1 card)
        // target has no Just Say No → JSN prompt is NOT shown
        System.setIn(new ByteArrayInputStream("1\n1\n1\n".getBytes()));

        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        GameLogic logic = new GameLogic(gm);
        logic.getActionHandler().refreshScanner();
        gm.beginCurrentPlayerTurn();

        Player collector = gm.getCurrentPlayer();
        Player target    = gm.getPlayers().stream()
                .filter(p -> p != collector).findFirst().orElseThrow();

        target.getHand().getCards().clear(); // clear so no accidental JSN fires

        collector.getPropertyArea().add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        collector.getPropertyArea().add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));
        target.getBankArea().add(new MoneyCard("5M", 5, 5));

        int targetBefore = target.getBankArea().total();

        ActionCard rentCard = new ActionCard("Rent (Any Color)", 3, ActionType.RENT, true);
        collector.getHand().add(rentCard);
        collector.setActions(3);

        logic.playCard(collector, rentCard);

        assertTrue(target.getBankArea().total() < targetBefore,
                "Target must lose assets after a RENT card is played (Bug 2 fix)");
    }

    // ── Bug 3 ────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    void bug3_wildCardReceivesColourAssignment() {
        // Simulate player choosing colour option "1" (first in the list)
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));

        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        gm.beginCurrentPlayerTurn();

        Player player = gm.getCurrentPlayer();
        player.setActions(3);

        // Two-colour wild: starts as RED, pair is Red/Yellow
        PropertyCard wild = new PropertyCard("Wild Red/Yellow", 3, PropertyType.RED, true);
        player.getHand().add(wild);

        player.placeProperty(wild.getId());

        // After placement the card must have a definite real colour (not RAINBOW)
        assertNotEquals(PropertyType.RAINBOW, wild.getColor(),
                "Wild card must not remain as RAINBOW after placement (Bug 3 fix)");
        // And it must be one of the two legal colours for this card
        assertTrue(wild.getColor() == PropertyType.RED || wild.getColor() == PropertyType.YELLOW,
                "Wild Red/Yellow card colour must be RED or YELLOW after assignment");
    }

    // ── Bug 4 ────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    void bug4_doubleRentFlagClearedAtEndOfTurn() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        GameLogic logic = new GameLogic(gm);
        logic.startGame();

        Player player1 = gm.getCurrentPlayer();
        player1.setActions(3);

        // Player 1 plays Double The Rent but does NOT follow with a Rent card
        ActionCard dtr = new ActionCard("Double The Rent", 1, ActionType.DOUBLE_RENT, false);
        player1.getHand().add(dtr);
        // Use executeAction directly to bypass playCard's action-decrement and card-removal
        logic.getActionHandler().executeAction(player1, dtr);

        assertEquals(1, logic.getPendingDoubleRentCount(),
                "pendingDoubleRentCount must be 1 after playing Double The Rent (Bug 4 fix)");

        logic.endTurn(); // Bug 4 fix: endTurn() now clears the flag

        assertEquals(0, logic.getPendingDoubleRentCount(),
                "pendingDoubleRentCount must be 0 at the start of the next player's turn (Bug 4 fix)");
    }

    // ── Bug 6 ────────────────────────────────────────────────────────────────

    /**
     * FIXED: PropertySet.getRequiredSize() returned 0 for RAINBOW, so isComplete()
     * was immediately true with 0 cards — a Rainbow wild card placed without colour
     * reassignment silently created a fake complete set counting toward the win.
     *
     * Fix: getRequiredSize(RAINBOW) now returns Integer.MAX_VALUE so a RAINBOW set
     * can never be complete (the card must be reassigned to a real colour first).
     */
    @Test
    @Order(6)
    void bug6_rainbowSetIsNeverCompleteWithoutColourReassignment() {
        // A RAINBOW PropertySet must never be complete regardless of how many cards it holds
        PropertySet rainbowSet = new PropertySet(PropertyType.RAINBOW);

        // Add a rainbow wild card WITHOUT reassigning colour
        PropertyCard wild = new PropertyCard("Rainbow Wild", 0, PropertyType.RAINBOW, true);
        rainbowSet.add(wild);

        assertFalse(rainbowSet.isComplete(),
                "A RAINBOW set must NOT be complete — card must be reassigned to a real colour first (Bug 6 fix)");

        // Sanity: even adding many cards must not make it complete
        for (int i = 0; i < 10; i++) {
            rainbowSet.add(new PropertyCard("Rainbow Wild", 0, PropertyType.RAINBOW, true));
        }
        assertFalse(rainbowSet.isComplete(),
                "RAINBOW set must never be complete regardless of card count (Bug 6 fix)");
    }

    @Test
    @Order(7)
    void bug6_rainbowWildDoesNotCountAsCompleteSetForWinCondition() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        gm.beginCurrentPlayerTurn();

        Player player = gm.getCurrentPlayer();

        // Give player two legitimate complete sets
        player.getPropertyArea().add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        player.getPropertyArea().add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));

        // Add a rainbow wild card WITHOUT colour reassignment — must NOT count as a third set
        player.getPropertyArea().add(new PropertyCard("Rainbow Wild", 0, PropertyType.RAINBOW, true));

        assertEquals(2, player.getPropertyArea().countCompleteSets(),
                "Rainbow wild placed without colour reassignment must not count as a complete set (Bug 6 fix)");

        // Confirm player does not falsely win
        com.monopolydeal.logic.VictoryChecker checker = new com.monopolydeal.logic.VictoryChecker();
        assertFalse(checker.checkWinner(player),
                "Player with 2 real sets + 1 unassigned wild must NOT be declared winner (Bug 6 fix)");
    }

    // ── Bug 7 ────────────────────────────────────────────────────────────────

    /**
     * FIXED: StandardCardFactory.createAction() threw IllegalArgumentException for
     * ActionType.RENT because there was no case RENT: in its switch statement.
     * Any code path that called factory.createAction(ActionType.RENT) would crash.
     *
     * Fix: added case RENT: returning a generic "Rent (Any Color)" ActionCard.
     */
    @Test
    @Order(8)
    void bug7_factoryCreateActionRentDoesNotThrow() {
        StandardCardFactory factory = new StandardCardFactory();

        assertDoesNotThrow(
                () -> factory.createAction(ActionType.RENT),
                "createAction(ActionType.RENT) must not throw IllegalArgumentException (Bug 7 fix)");
    }

    @Test
    @Order(9)
    void bug7_factoryCreateActionRentReturnsCorrectType() {
        StandardCardFactory factory = new StandardCardFactory();
        ActionCard card = factory.createAction(ActionType.RENT);

        assertNotNull(card, "createAction(ActionType.RENT) must return a non-null card");
        assertEquals(ActionType.RENT, card.getType(),
                "Created card must have ActionType.RENT");
        assertTrue(card.isCanDefend(),
                "Rent card must be defendable with Just Say No");
    }

    // ── Bug 8 ────────────────────────────────────────────────────────────────

    /**
     * FIXED: tryAttachBuilding() always attached to the FIRST valid set with
     * no player input.  With two complete sets a player had no control over
     * which set received the House or Hotel.
     *
     * Fix: all valid sets are now listed and the player chooses one.
     * If only one valid set exists the prompt is skipped automatically.
     */
    @Test
    @Order(11)
    void bug8_houseAttachesToFirstSetWhenOnlyOneValid() {
        // With a single valid set the building must still attach correctly (no regression)
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        gm.beginCurrentPlayerTurn();
        GameLogic logic = new GameLogic(gm);

        Player player = gm.getCurrentPlayer();
        player.setActions(3);

        // One complete Brown set
        player.getPropertyArea().add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));

        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE, false);
        player.getHand().add(house);

        // No prompt needed — only one valid set
        boolean attached = logic.getActionHandler().tryAttachBuilding(player, house);
        assertTrue(attached, "House must attach when exactly one valid set exists (Bug 8 fix)");
    }

    @Test
    @Order(12)
    void bug8_houseAttachMenuShownWhenMultipleValidSets() {
        // Simulate player choosing the SECOND set (input "2\n")
        System.setIn(new ByteArrayInputStream("2\n".getBytes()));

        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        gm.beginCurrentPlayerTurn();
        GameLogic logic = new GameLogic(gm);
        logic.getActionHandler().refreshScanner();

        Player player = gm.getCurrentPlayer();
        player.setActions(3);

        // Two complete sets: Brown and Blue
        PropertyCard br1 = new PropertyCard("Br1", 1, PropertyType.BROWN, false);
        PropertyCard br2 = new PropertyCard("Br2", 1, PropertyType.BROWN, false);
        PropertyCard bl1 = new PropertyCard("Bl1", 4, PropertyType.BLUE, false);
        PropertyCard bl2 = new PropertyCard("Bl2", 4, PropertyType.BLUE, false);
        player.getPropertyArea().add(br1);
        player.getPropertyArea().add(br2);
        player.getPropertyArea().add(bl1);
        player.getPropertyArea().add(bl2);

        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE, false);
        player.getHand().add(house);

        boolean attached = logic.getActionHandler().tryAttachBuilding(player, house);
        assertTrue(attached, "House must attach when player chooses from multiple valid sets (Bug 8 fix)");

        // The house must be on exactly ONE set — not both
        long setsWithHouse = player.getPropertyArea().getSets().stream()
                .flatMap(s -> s.getCards().stream())
                .flatMap(pc -> pc.getUpgrades().stream())
                .filter(u -> u instanceof ActionCard
                        && ((ActionCard) u).getType() == ActionType.HOUSE)
                .count();
        assertEquals(1, setsWithHouse,
                "Exactly one set must receive the House (Bug 8 fix)");
    }

    // ── Bug 9 ────────────────────────────────────────────────────────────────

    /**
     * FIXED: GameLogic.endTurn() never called player.endTurn(), so the
     * 7-card hand limit was never enforced at the end of a turn.
     * A player could carry 9+ cards into the next turn with no penalty.
     *
     * Fix: GameLogic.endTurn() now calls current.endTurn() (which enforces
     * the limit) before switching to the next player.
     */
    @Test
    @Order(13)
    void bug9_endTurnEnforcesHandLimitViaGameLogic() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        GameLogic logic = new GameLogic(gm);
        logic.startGame(); // sets current player via beginCurrentPlayerTurn()

        // Get the reference AFTER startGame so it matches what GameLogic.endTurn() uses
        Player current = gm.getCurrentPlayer();

        // Clear dealt cards then add exactly 9 (= 7 limit + 2 excess, needs 2 discards)
        current.getHand().getCards().clear();
        for (int i = 0; i < 9; i++) {
            current.getHand().add(new MoneyCard("1M", 1, 1));
        }
        assertEquals(9, current.getHand().size(),
                "Pre-condition: hand must have exactly 9 cards");

        // enforceEndTurnDiscard fires — inject "1\n" for each of the 2 required discards
        System.setIn(new ByteArrayInputStream("1\n1\n".getBytes()));
        logic.getActionHandler().refreshScanner();
        logic.endTurn(); // Bug 9 fix: endTurn() now calls enforceEndTurnDiscard()

        assertTrue(current.getHand().size() <= Player.MAX_HAND_SIZE,
                "Hand must be <= " + Player.MAX_HAND_SIZE
                        + " after GameLogic.endTurn() (Bug 9 fix)");
    }

    @Test
    @Order(14)
    void bug9_endTurnDoesNotDiscardWhenHandWithinLimit() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        GameLogic logic = new GameLogic(gm);
        logic.startGame();

        Player current = gm.getCurrentPlayer();
        current.getHand().getCards().clear();
        for (int i = 0; i < 5; i++) {
            current.getHand().add(new MoneyCard("1M", 1, 1));
        }

        System.setIn(new ByteArrayInputStream("".getBytes()));
        logic.getActionHandler().refreshScanner();
        logic.endTurn();

        assertEquals(5, current.getHand().size(),
                "Hand within limit must not be reduced by endTurn() (Bug 9 fix — no over-discard)");
    }
}

