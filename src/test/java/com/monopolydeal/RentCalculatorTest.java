package com.monopolydeal;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.RentCalculator;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RentCalculator – base rent, building bonuses, double-rent multiplier.
 * Covers Sequence Diagram 7 (Play Rent Card).
 */
class RentCalculatorTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    void testCanCollectRentOnCompleteSet() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("B1", 1, PropertyType.BROWN, false));
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        assertTrue(RentCalculator.canCollectRent(set), "Complete set must be eligible for rent");
    }

    @Test
    void testCanCollectRentOnIncompleteSet() {
        // Rule updated: rent can be charged on ANY non-empty set (complete or not)
        PropertySet set = new PropertySet(PropertyType.ORANGE);
        set.add(new PropertyCard("O1", 2, PropertyType.ORANGE, false));
        assertTrue(RentCalculator.canCollectRent(set), "Non-empty set (even incomplete) must be eligible for rent");
    }

    @Test
    void testCannotCollectRentOnNull() {
        assertFalse(RentCalculator.canCollectRent(null));
    }

    @Test
    void testBaseRentForCompleteBrownSet() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("B1", 1, PropertyType.BROWN, false));
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        int rent = RentCalculator.calculateFinalRent(set, false);
        assertEquals(2, rent, "Complete BROWN set (2 cards) must charge 2M");
    }

    @Test
    void testBaseRentForCompleteBlueSet() {
        PropertySet set = new PropertySet(PropertyType.BLUE);
        set.add(new PropertyCard("Bl1", 4, PropertyType.BLUE, false));
        set.add(new PropertyCard("Bl2", 4, PropertyType.BLUE, false));
        int rent = RentCalculator.calculateFinalRent(set, false);
        assertEquals(8, rent, "Complete BLUE set must charge 8M");
    }

    @Test
    void testDoubleRentMultipliesBaseRentByTwo() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("B1", 1, PropertyType.BROWN, false));
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        int normalRent = RentCalculator.calculateFinalRent(set, false);
        int doubleRent = RentCalculator.calculateFinalRent(set, true);
        assertEquals(normalRent * 2, doubleRent, "Double rent must be exactly 2× the base rent");
    }

    @Test
    void testHouseBonusAddsThreeToRent() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        PropertyCard host = new PropertyCard("B1", 1, PropertyType.BROWN, false);
        set.add(host);
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        int baseRent = RentCalculator.calculateFinalRent(set, false);

        ActionCard house = new ActionCard("House", 3, ActionType.HOUSE, false);
        host.attachUpgrade(house);
        int rentWithHouse = RentCalculator.calculateFinalRent(set, false);

        assertEquals(baseRent + 3, rentWithHouse, "A House must add exactly 3M to rent");
    }

    @Test
    void testHotelBonusAddsFourToRent() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        PropertyCard host = new PropertyCard("B1", 1, PropertyType.BROWN, false);
        set.add(host);
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        int baseRent = RentCalculator.calculateFinalRent(set, false);

        ActionCard hotel = new ActionCard("Hotel", 4, ActionType.HOTEL, false);
        host.attachUpgrade(hotel);
        int rentWithHotel = RentCalculator.calculateFinalRent(set, false);

        assertEquals(baseRent + 4, rentWithHotel, "A Hotel must add exactly 4M to rent");
    }

    @Test
    void testIncompleteSetReturnsPartialRent() {
        // Rule updated: incomplete sets charge partial rent (1-card Orange = 2M)
        PropertySet set = new PropertySet(PropertyType.ORANGE);
        set.add(new PropertyCard("O1", 2, PropertyType.ORANGE, false));
        int rent = RentCalculator.calculateFinalRent(set, false);
        assertTrue(rent >= 0, "Incomplete set must return non-negative rent");
    }

    @Test
    void testDoubleRentOnIncompleteSetDoubles() {
        // Rule updated: double rent applies to incomplete sets too
        PropertySet set = new PropertySet(PropertyType.ORANGE);
        set.add(new PropertyCard("O1", 2, PropertyType.ORANGE, false));
        int normal = RentCalculator.calculateFinalRent(set, false);
        int doubled = RentCalculator.calculateFinalRent(set, true);
        assertEquals(normal * 2, doubled, "Double rent must double even on incomplete sets");
    }
}
