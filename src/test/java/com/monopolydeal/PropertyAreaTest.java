package com.monopolydeal;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.PropertyArea;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PropertyArea – grouping, complete-set counting, and removal.
 * Covers Sequence Diagram 10 (Win Condition Check).
 */
class PropertyAreaTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    void testNewAreaHasNoCompleteSets() {
        PropertyArea area = new PropertyArea();
        assertEquals(0, area.countCompleteSets());
    }

    @Test
    void testAddPropertyCreatesSet() {
        PropertyArea area = new PropertyArea();
        area.add(new PropertyCard("Brown1", 1, PropertyType.BROWN, false));
        assertNotNull(area.getSet(PropertyType.BROWN));
        assertEquals(1, area.getSet(PropertyType.BROWN).getCards().size());
    }

    @Test
    void testCountCompleteSetsOneBrown() {
        PropertyArea area = new PropertyArea();
        area.add(new PropertyCard("Brown1", 1, PropertyType.BROWN, false));
        area.add(new PropertyCard("Brown2", 1, PropertyType.BROWN, false));
        assertEquals(1, area.countCompleteSets(), "Two BROWN cards must form one complete set");
    }

    @Test
    void testCountCompleteSetsTwoColors() {
        PropertyArea area = new PropertyArea();
        // Brown (needs 2)
        area.add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        area.add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));
        // Blue (needs 2)
        area.add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        area.add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));
        assertEquals(2, area.countCompleteSets());
    }

    @Test
    void testThreeCompleteSetsTriggerWinCondition() {
        PropertyArea area = new PropertyArea();
        // Brown (2)
        area.add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        area.add(new PropertyCard("Br2", 1, PropertyType.BROWN, false));
        // Blue (2)
        area.add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        area.add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));
        // LightGreen (2)
        area.add(new PropertyCard("LG1", 2, PropertyType.LIGHTGREEN, false));
        area.add(new PropertyCard("LG2", 2, PropertyType.LIGHTGREEN, false));
        assertEquals(3, area.countCompleteSets(), "Three complete sets must satisfy the win condition");
    }

    @Test
    void testIncompleteSetNotCounted() {
        PropertyArea area = new PropertyArea();
        area.add(new PropertyCard("Orange1", 2, PropertyType.ORANGE, false));
        // Orange needs 3; only 1 added
        assertEquals(0, area.countCompleteSets(), "Incomplete set must not be counted");
    }

    @Test
    void testRemoveCardFromSet() {
        PropertyArea area = new PropertyArea();
        PropertyCard card = new PropertyCard("Brown1", 1, PropertyType.BROWN, false);
        area.add(card);
        area.add(new PropertyCard("Brown2", 1, PropertyType.BROWN, false));
        assertEquals(1, area.countCompleteSets());

        area.remove(card);
        assertEquals(0, area.countCompleteSets(), "Set must no longer be complete after card removal");
    }

    @Test
    void testRemovingLastCardCleansUpSet() {
        PropertyArea area = new PropertyArea();
        PropertyCard card = new PropertyCard("Brown1", 1, PropertyType.BROWN, false);
        area.add(card);
        area.remove(card);
        assertNull(area.getSet(PropertyType.BROWN), "Empty set must be removed from area");
    }

    @Test
    void testGetSetsReturnsAllColors() {
        PropertyArea area = new PropertyArea();
        area.add(new PropertyCard("Br1", 1, PropertyType.BROWN, false));
        area.add(new PropertyCard("Or1", 2, PropertyType.ORANGE, false));
        assertEquals(2, area.getSets().size(), "Area must have one set per distinct color");
    }

    // ── Wildcard placement regression (万能牌放置颜色一致性) ─────────────────

    /**
     * When a two-color wild is placed via add(card, chosenColor), it must land
     * in the chosen color's set (not the card's initial default color set).
     */
    @Test
    void testWildcardLandsInChosenColorSet() {
        PropertyArea area = new PropertyArea();
        // Wild starts as RED but player commits it to YELLOW
        PropertyCard wild = new PropertyCard("Wild Red/Yellow", 3, PropertyType.RED, true);
        area.add(wild, PropertyType.YELLOW);

        assertNotNull(area.getSet(PropertyType.YELLOW),
                "YELLOW set must exist after wild is placed with chosenColor=YELLOW");
        assertNull(area.getSet(PropertyType.RED),
                "RED set must NOT be created when wild is committed to YELLOW");
        assertEquals(PropertyType.YELLOW, wild.getColor(),
                "Wild card's committed color must match the chosen color (YELLOW)");
    }

    /**
     * 2 normal ORANGE + 1 wild committed to ORANGE = complete set via PropertyArea.
     */
    @Test
    void testTwoNormalPlusOneWildViaAreaIsComplete() {
        PropertyArea area = new PropertyArea();
        area.add(new PropertyCard("Or1", 2, PropertyType.ORANGE, false));
        area.add(new PropertyCard("Or2", 2, PropertyType.ORANGE, false));
        // Wild Red/Yellow committed to ORANGE via chosenColor
        PropertyCard wild = new PropertyCard("Wild Red/Yellow", 3, PropertyType.RED, true);
        area.add(wild, PropertyType.ORANGE);

        assertEquals(1, area.countCompleteSets(),
                "2 normal + 1 wild in ORANGE must count as 1 complete set");
        assertTrue(area.getSet(PropertyType.ORANGE).isComplete(),
                "ORANGE set with 2 normals + 1 wild must be complete");
    }
}
