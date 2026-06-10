package com.monopolydeal;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PropertySet – completeness, rent calculation, and card management.
 * Covers Sequence Diagram 4 (Place Property Card) and Diagram 7 (Play Rent Card).
 */
class PropertySetTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    void testNewSetIsNotComplete() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        assertFalse(set.isComplete(), "A new empty set must not be complete");
    }

    @Test
    void testBrownSetNeedsTwoCards() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        assertEquals(2, set.getNeed());
    }

    @Test
    void testBlueSetNeedsTwoCards() {
        PropertySet set = new PropertySet(PropertyType.BLUE);
        assertEquals(2, set.getNeed());
    }

    @Test
    void testOrangeSetNeedsThreeCards() {
        PropertySet set = new PropertySet(PropertyType.ORANGE);
        assertEquals(3, set.getNeed());
    }

    @Test
    void testRailroadNeedsFourCards() {
        PropertySet set = new PropertySet(PropertyType.BLACK);
        assertEquals(4, set.getNeed());
    }

    @Test
    void testSetBecomesCompleteWhenFull() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("Brown1", 1, PropertyType.BROWN, false));
        assertFalse(set.isComplete(), "Set with 1 of 2 cards must not be complete");
        set.add(new PropertyCard("Brown2", 1, PropertyType.BROWN, false));
        assertTrue(set.isComplete(), "Set with 2 of 2 cards must be complete");
    }

    @Test
    void testRentIncreasesWithMoreCards() {
        PropertySet set = new PropertySet(PropertyType.LIGHTBLUE);
        set.add(new PropertyCard("LB1", 1, PropertyType.LIGHTBLUE, false));
        int rent1 = set.getRent();
        set.add(new PropertyCard("LB2", 1, PropertyType.LIGHTBLUE, false));
        int rent2 = set.getRent();
        set.add(new PropertyCard("LB3", 1, PropertyType.LIGHTBLUE, false));
        int rent3 = set.getRent();

        assertTrue(rent1 <= rent2, "Rent must not decrease as more properties are added");
        assertTrue(rent2 <= rent3, "Rent must not decrease as more properties are added");
        assertEquals(4, rent3, "Full LightBlue set (3 cards) must charge 4M rent");
    }

    @Test
    void testRentZeroForEmptySet() {
        PropertySet set = new PropertySet(PropertyType.RED);
        assertEquals(0, set.getRent(), "Empty set must have 0 rent");
    }

    @Test
    void testRemoveCard() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        PropertyCard card = new PropertyCard("Brown1", 1, PropertyType.BROWN, false);
        set.add(card);
        assertEquals(1, set.getCards().size());
        set.remove(card);
        assertEquals(0, set.getCards().size());
        assertFalse(set.isComplete());
    }

    @Test
    void testBrownRent() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("B1", 1, PropertyType.BROWN, false));
        assertEquals(1, set.getRent());
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        assertEquals(2, set.getRent());
    }

    @Test
    void testBlueRent() {
        PropertySet set = new PropertySet(PropertyType.BLUE);
        set.add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        assertEquals(3, set.getRent());
        set.add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));
        assertEquals(8, set.getRent());
    }

    // ── Wildcard regression tests (万能牌回归测试) ─────────────────────────

    /**
     * 2 normal + 1 two-color wild placed in a 3-card color set must be complete.
     * This was the reported bug: the set was not recognized as complete.
     */
    @Test
    void testTwoNormalPlusOneWildIsComplete() {
        // ORANGE needs 3 cards
        PropertySet set = new PropertySet(PropertyType.ORANGE);
        set.add(new PropertyCard("Orange1", 2, PropertyType.ORANGE, false));
        set.add(new PropertyCard("Orange2", 2, PropertyType.ORANGE, false));
        // Wild committed to ORANGE
        PropertyCard wild = new PropertyCard("Wild Red/Yellow", 3, PropertyType.RED, true);
        wild.commitColor(PropertyType.ORANGE);
        set.add(wild);
        assertTrue(set.isComplete(),
                "2 normal + 1 wild (committed to set color) must form a complete ORANGE set");
    }

    /**
     * 1 normal + 1 two-color wild in a 2-card color set (BROWN) must be complete.
     */
    @Test
    void testOneNormalPlusOneWildCompletesBrownSet() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("Brown1", 1, PropertyType.BROWN, false));
        PropertyCard wild = new PropertyCard("Wild Brown/LightBlue", 1, PropertyType.BROWN, true);
        wild.commitColor(PropertyType.BROWN);
        set.add(wild);
        assertTrue(set.isComplete(),
                "1 normal + 1 wild must form a complete BROWN set (needs 2)");
    }

    /**
     * RAINBOW set must NEVER be complete regardless of card count (Bug 6 / getRequiredSize fix).
     */
    @Test
    void testRainbowSetNeverComplete() {
        PropertySet rainbow = new PropertySet(PropertyType.RAINBOW);
        for (int i = 0; i < 5; i++) {
            rainbow.add(new PropertyCard("Rainbow Wild", 0, PropertyType.RAINBOW, true));
        }
        assertFalse(rainbow.isComplete(),
                "RAINBOW set must never be complete — need is MAX_VALUE");
        assertTrue(rainbow.getNeed() > 0,
                "RAINBOW set need must be > 0 so isComplete() short-circuits correctly");
    }

    /**
     * A set consisting only of wildcards (no standard property) must NOT be complete.
     */
    @Test
    void testAllWildsNotComplete() {
        // BROWN needs 2; fill with 2 wilds only
        PropertySet set = new PropertySet(PropertyType.BROWN);
        PropertyCard w1 = new PropertyCard("Wild Brown/LightBlue", 1, PropertyType.BROWN, true);
        PropertyCard w2 = new PropertyCard("Wild LightBlue/Brown", 1, PropertyType.LIGHTBLUE, true);
        w1.commitColor(PropertyType.BROWN);
        w2.commitColor(PropertyType.BROWN);
        set.add(w1);
        set.add(w2);
        assertFalse(set.isComplete(),
                "A set with only wildcards must NOT be considered complete (hasStandardProperty guard)");
    }
}
