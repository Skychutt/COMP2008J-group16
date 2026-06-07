package com.monopolydeal;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

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
}
