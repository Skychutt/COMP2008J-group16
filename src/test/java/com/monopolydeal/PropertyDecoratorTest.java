package com.monopolydeal;

import com.monopolydeal.decorator.HouseDecorator;
import com.monopolydeal.decorator.HotelDecorator;
import com.monopolydeal.decorator.PropertyDecorator;
import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.RentCalculator;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.PropertyCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the Decorator design pattern implementation for the
 * House/Hotel rent bonus feature. Each test exercises a distinct usage
 * pattern and asserts the composed bonus matches the expected constant.
 */
class PropertyDecoratorTest {

    /** A bare property card has no decoration bonus via the chain. */
    @Test
    void barePropertyCardHasNoRentBonus() {
        PropertyCard card = new PropertyCard("Blue", 4, PropertyType.BLUE, false);
        assertEquals(0, card.getTotalRent(),
                "A property without any decorator must yield +0M rent bonus");
    }

    /** Attaching a single House adds a +3M bonus to the card's rent. */
    @Test
    void singleHouseAddsThreeMillionBonus() {
        PropertyCard card = new PropertyCard("Blue", 4, PropertyType.BLUE, false);
        card.attachHouse();

        assertEquals(HouseDecorator.HOUSE_BONUS, card.getTotalRent(),
                "A single House decorator must produce +3M bonus");
        assertTrue(card.hasHouse(), "hasHouse() must return true after attachHouse()");
        assertFalse(card.hasHotel(), "hasHotel() must be false when only house was attached");
    }

    /** Hotel added after House yields a +4M bonus (for the hotel layer alone). */
    @Test
    void hotelAddsFourMillionBonus() {
        PropertyCard card = new PropertyCard("Blue", 4, PropertyType.BLUE, false);
        card.attachHouse();
        card.attachHotel();

        assertEquals(HouseDecorator.HOUSE_BONUS + HotelDecorator.HOTEL_BONUS,
                card.getTotalRent(),
                "House + Hotel must yield a combined bonus of +7M (3M + 4M)");
        assertTrue(card.hasHouse());
        assertTrue(card.hasHotel());
    }

    /** Two houses stacked must return twice the single-house bonus. */
    @Test
    void multipleHousesAccumulateBonus() {
        PropertyCard card = new PropertyCard("Blue", 4, PropertyType.BLUE, false);
        card.attachHouse();
        card.attachHouse();

        assertEquals(2 * HouseDecorator.HOUSE_BONUS, card.getTotalRent(),
                "Two house layers must return 2 * 3M = 6M bonus");
    }

    /** Legacy attachUpgrade path must still be counted (backwards compatibility). */
    @Test
    void legacyAttachUpgradePathStillProducesBonus() {
        PropertyCard card = new PropertyCard("Blue", 4, PropertyType.BLUE, false);
        card.attachUpgrade(new ActionCard("House", 3, ActionType.HOUSE, false));

        assertEquals(HouseDecorator.HOUSE_BONUS, card.getTotalRent(),
                "Attaching via legacy attachUpgrade() must still be counted in rent bonus");
    }

    /** PropertySet + RentCalculator should honor the decoration chain bonus. */
    @Test
    void rentCalculatorIncludesDecoratorBonus() {
        PropertySet set = new PropertySet(PropertyType.BLUE);
        PropertyCard blue = new PropertyCard("Blue", 4, PropertyType.BLUE, false);
        PropertyCard blue2 = new PropertyCard("Blue 2", 4, PropertyType.BLUE, false);
        set.add(blue);
        set.add(blue2);
        blue.attachHouse();

        int baseRent = set.getRent();
        int expected = baseRent + HouseDecorator.HOUSE_BONUS;
        int calculated = RentCalculator.calculateFinalRent(set, false);

        assertEquals(expected, calculated,
                "RentCalculator.calculateFinalRent() must include the decorator bonus");
    }

    /** Decorator chain head can be inspected: type should alternate correctly. */
    @Test
    void decoratorChainHeadIsHotelAfterHotelAttach() {
        PropertyCard card = new PropertyCard("Blue", 4, PropertyType.BLUE, false);
        card.attachHouse();
        card.attachHotel();

        PropertyDecorator head = card.getDecoratorChain();
        assertNotNull(head, "After attachments, the decorator chain head must not be null");
        assertTrue(head instanceof HotelDecorator,
                "Last-attached layer (Hotel) must sit at the top of the chain");
        assertEquals(HotelDecorator.HOTEL_BONUS, head.getLayerBonus(),
                "Hotel layer alone must expose its own +4M bonus via getLayerBonus()");
    }

    /** Each card's decoration state is independent; modifications to one do not affect another. */
    @Test
    void decorationsArePerCard() {
        PropertyCard a = new PropertyCard("Blue A", 4, PropertyType.BLUE, false);
        PropertyCard b = new PropertyCard("Blue B", 4, PropertyType.BLUE, false);
        a.attachHouse();

        assertEquals(HouseDecorator.HOUSE_BONUS, a.getTotalRent());
        assertEquals(0, b.getTotalRent(),
                "Decorations must be independent between cards");
    }
}
