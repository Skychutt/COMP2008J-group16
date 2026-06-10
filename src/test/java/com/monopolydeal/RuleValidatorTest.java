package com.monopolydeal;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.RuleValidator;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RuleValidator – canPlayCard, canAddHouse, canAddHotel, and hand-limit checks.
 */
class RuleValidatorTest {

    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        Deck.reset();
        validator = new RuleValidator();
    }

    @Test
    void testCanPlayCardWithActionsAndCardInHand() {
        Player player = new Player("1", "Alice");
        player.setActions(1);
        MoneyCard card = new MoneyCard("2M", 2, 2);
        player.getHand().add(card);
        assertTrue(validator.canPlayCard(player, card));
    }

    @Test
    void testCannotPlayCardWithNoActions() {
        Player player = new Player("1", "Alice");
        player.setActions(0);
        MoneyCard card = new MoneyCard("2M", 2, 2);
        player.getHand().add(card);
        assertFalse(validator.canPlayCard(player, card), "Playing without actions must be rejected");
    }

    @Test
    void testCannotPlayCardNotInHand() {
        Player player = new Player("1", "Alice");
        player.setActions(3);
        MoneyCard card = new MoneyCard("2M", 2, 2);
        // Card not added to hand
        assertFalse(validator.canPlayCard(player, card), "Card not in hand must be rejected");
    }

    @Test
    void testCannotPlayNullCard() {
        Player player = new Player("1", "Alice");
        player.setActions(3);
        assertFalse(validator.canPlayCard(player, null));
    }

    @Test
    void testCanAddHouseToCompleteSetWithNoBuildings() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("B1", 1, PropertyType.BROWN, false));
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        assertTrue(validator.canAddHouse(set), "House must be addable to a complete set with no buildings");
    }

    @Test
    void testCannotAddHouseToIncompleteSet() {
        PropertySet set = new PropertySet(PropertyType.ORANGE);
        set.add(new PropertyCard("O1", 2, PropertyType.ORANGE, false));
        assertFalse(validator.canAddHouse(set), "House must not be addable to an incomplete set");
    }

    @Test
    void testCannotAddHouseIfAlreadyHasHouse() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        PropertyCard host = new PropertyCard("B1", 1, PropertyType.BROWN, false);
        host.attachUpgrade(new ActionCard("House", 3, ActionType.HOUSE, false));
        set.add(host);
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        assertFalse(validator.canAddHouse(set), "A second house must not be addable");
    }

    @Test
    void testCanAddHotelWhenSetHasHouse() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        PropertyCard host = new PropertyCard("B1", 1, PropertyType.BROWN, false);
        host.attachUpgrade(new ActionCard("House", 3, ActionType.HOUSE, false));
        set.add(host);
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        assertTrue(validator.canAddHotel(set), "Hotel must be addable when set has a house");
    }

    @Test
    void testCannotAddHotelWithoutHouse() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        set.add(new PropertyCard("B1", 1, PropertyType.BROWN, false));
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        assertFalse(validator.canAddHotel(set), "Hotel must require a house first");
    }

    @Test
    void testCannotAddHotelIfAlreadyHasHotel() {
        PropertySet set = new PropertySet(PropertyType.BROWN);
        PropertyCard host = new PropertyCard("B1", 1, PropertyType.BROWN, false);
        host.attachUpgrade(new ActionCard("House", 3, ActionType.HOUSE, false));
        host.attachUpgrade(new ActionCard("Hotel", 4, ActionType.HOTEL, false));
        set.add(host);
        set.add(new PropertyCard("B2", 1, PropertyType.BROWN, false));
        assertFalse(validator.canAddHotel(set), "A second hotel must not be addable");
    }

    @Test
    void testIsHandOverLimitTrue() {
        Player player = new Player("1", "Alice");
        for (int i = 0; i < 8; i++) player.getHand().add(new MoneyCard("1M", 1, 1));
        assertTrue(validator.isHandOverLimit(player));
    }

    @Test
    void testIsHandOverLimitFalseAtExactLimit() {
        Player player = new Player("1", "Alice");
        for (int i = 0; i < 7; i++) player.getHand().add(new MoneyCard("1M", 1, 1));
        assertFalse(validator.isHandOverLimit(player));
    }

    @Test
    void testCannotAddHouseToNullSet() {
        assertFalse(validator.canAddHouse(null));
    }

    @Test
    void testCannotAddHotelToNullSet() {
        assertFalse(validator.canAddHotel(null));
    }
}
