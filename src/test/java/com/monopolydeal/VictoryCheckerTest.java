package com.monopolydeal;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.VictoryChecker;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VictoryChecker – counting complete sets and winner detection.
 * Covers Sequence Diagram 10 (Win Condition Check).
 */
class VictoryCheckerTest {

    private VictoryChecker checker;

    @BeforeEach
    void setUp() {
        Deck.reset();
        checker = new VictoryChecker();
    }

    @Test
    void testPlayerWithZeroSetsIsNotWinner() {
        Player player = new Player("1", "Alice");
        assertFalse(checker.checkWinner(player));
        assertEquals(0, checker.countCompletedSets(player));
    }

    @Test
    void testPlayerWithTwoSetsIsNotWinner() {
        Player player = new Player("1", "Alice");
        // Brown (2)
        player.getPropertyArea().add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));
        // Blue (2)
        player.getPropertyArea().add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        player.getPropertyArea().add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));

        assertEquals(2, checker.countCompletedSets(player));
        assertFalse(checker.checkWinner(player), "Player needs 3 complete sets to win");
    }

    @Test
    void testPlayerWithThreeSetsIsWinner() {
        Player player = new Player("1", "Alice");
        // Brown (2)
        player.getPropertyArea().add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));
        // Blue (2)
        player.getPropertyArea().add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        player.getPropertyArea().add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));
        // LightGreen (2)
        player.getPropertyArea().add(new PropertyCard("LG1", 2, PropertyType.LIGHTGREEN, false));
        player.getPropertyArea().add(new PropertyCard("LG2", 2, PropertyType.LIGHTGREEN, false));

        assertEquals(3, checker.countCompletedSets(player));
        assertTrue(checker.checkWinner(player), "Player with 3 complete sets must be declared winner");
    }

    @Test
    void testIncompleteSetsDoNotCountTowardWin() {
        Player player = new Player("1", "Alice");
        // Orange needs 3; only 2 added – incomplete
        player.getPropertyArea().add(new PropertyCard("Or1", 2, PropertyType.ORANGE, false));
        player.getPropertyArea().add(new PropertyCard("Or2", 2, PropertyType.ORANGE, false));
        // Brown complete
        player.getPropertyArea().add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));

        assertEquals(1, checker.countCompletedSets(player),
                "Incomplete Orange set must not count; only Brown complete");
    }

    @Test
    void testMoreThanThreeSetsStillWins() {
        Player player = new Player("1", "Alice");
        // Brown, Blue, LightGreen, and one more (Purple needs 3 – skip for simplicity)
        player.getPropertyArea().add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));
        player.getPropertyArea().add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        player.getPropertyArea().add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));
        player.getPropertyArea().add(new PropertyCard("LG1", 2, PropertyType.LIGHTGREEN, false));
        player.getPropertyArea().add(new PropertyCard("LG2", 2, PropertyType.LIGHTGREEN, false));
        // A 4th partial set (should still win)
        player.getPropertyArea().add(new PropertyCard("Or1", 2, PropertyType.ORANGE, false));

        assertTrue(checker.checkWinner(player), "Player with ≥3 complete sets must win");
    }
}
