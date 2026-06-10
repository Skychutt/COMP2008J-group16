package com.monopolydeal.factory;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

/**
 * Standard factory implementation for creating all standard card types.
 * Supports creation of all 10 action types, all 11 property colors, and all money denominations.
 */
public class StandardCardFactory extends CardFactory {

    /**
     * Create a standard action card based on the given action type.
     * Each type maps to a specific card name, value, and defense flag.
     */
    @Override
    public ActionCard createAction(ActionType type) {
        switch (type) {
            case GO_PASS:
                return new ActionCard("Pass Go", 1, ActionType.GO_PASS, false);
            case BIRTHDAY:
                return new ActionCard("It's My Birthday", 2, ActionType.BIRTHDAY, true);
            case DEBT_DEAL:
                return new ActionCard("Debt Collector", 3, ActionType.DEBT_DEAL, true);
            case SLY_DEAL:
                return new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL, true);
            case FORCED_DEAL:
                return new ActionCard("Forced Deal", 3, ActionType.FORCED_DEAL, true);
            case DEAL_BREAKER:
                return new ActionCard("Deal Breaker", 5, ActionType.DEAL_BREAKER, true);
            case JUST_SAY_NO:
                return new ActionCard("Just Say No", 4, ActionType.JUST_SAY_NO, false);
            case RENT:
                return new ActionCard("Rent (Any Color)", 3, ActionType.RENT, true);
            case DOUBLE_RENT:
                return new ActionCard("Double The Rent", 1, ActionType.DOUBLE_RENT, false);
            case HOUSE:
                return new ActionCard("House", 3, ActionType.HOUSE, false);
            case HOTEL:
                return new ActionCard("Hotel", 4, ActionType.HOTEL, false);
            default:
                throw new IllegalArgumentException("Unknown action type: " + type);
        }
    }

    /**
     * Create a standard property card for the given color type.
     * Each color maps to a specific card name and monetary value.
     */
    @Override
    public PropertyCard createProperty(PropertyType type) {
        switch (type) {
            case BROWN:
                return new PropertyCard("Brown Property", 1, PropertyType.BROWN, false);
            case LIGHTBLUE:
                return new PropertyCard("Light Blue Property", 1, PropertyType.LIGHTBLUE, false);
            case PURPLE:
                return new PropertyCard("Purple Property", 2, PropertyType.PURPLE, false);
            case ORANGE:
                return new PropertyCard("Orange Property", 2, PropertyType.ORANGE, false);
            case RED:
                return new PropertyCard("Red Property", 3, PropertyType.RED, false);
            case YELLOW:
                return new PropertyCard("Yellow Property", 3, PropertyType.YELLOW, false);
            case GREEN:
                return new PropertyCard("Green Property", 4, PropertyType.GREEN, false);
            case BLUE:
                return new PropertyCard("Blue Property", 4, PropertyType.BLUE, false);
            case BLACK:
                return new PropertyCard("Railroad", 2, PropertyType.BLACK, false);
            case LIGHTGREEN:
                return new PropertyCard("Utility", 2, PropertyType.LIGHTGREEN, false);
            case RAINBOW:
                return new PropertyCard("Rainbow Wild", 0, PropertyType.RAINBOW, true);
            default:
                throw new IllegalArgumentException("Unknown property type: " + type);
        }
    }

    /**
     * Create a money card with the specified denomination.
     * @param value the denomination value (1, 2, 3, 4, 5, or 10)
     */
    @Override
    public MoneyCard createMoney(int value) {
        return new MoneyCard(value + "M", value, value);
    }
}
