package com.monopolydeal.factory;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

/**
 * Premium factory implementation for creating high-value or rare cards.
 * Only supports a subset of action types and property colors (e.g., Deal Breaker, Rainbow Wild).
 */
public class PremiumCardFactory extends CardFactory {

    /**
     * Create a premium action card. Only supports Deal Breaker, Just Say No,
     * Sly Deal, and Forced Deal types.
     */
    @Override
    public ActionCard createAction(ActionType type) {
        switch (type) {
            case DEAL_BREAKER:
                return new ActionCard("Deal Breaker", 5, ActionType.DEAL_BREAKER, true);
            case JUST_SAY_NO:
                return new ActionCard("Just Say No", 4, ActionType.JUST_SAY_NO, false);
            case SLY_DEAL:
                return new ActionCard("Sly Deal", 3, ActionType.SLY_DEAL, true);
            case FORCED_DEAL:
                return new ActionCard("Forced Deal", 3, ActionType.FORCED_DEAL, true);
            default:
                throw new IllegalArgumentException("Premium factory does not support: " + type);
        }
    }

    /**
     * Create a premium property card. Only supports Rainbow, Blue, and Green types.
     */
    @Override
    public PropertyCard createProperty(PropertyType type) {
        switch (type) {
            case RAINBOW:
                return new PropertyCard("Rainbow Wild", 0, PropertyType.RAINBOW, true);
            case BLUE:
                return new PropertyCard("Blue Property", 4, PropertyType.BLUE, false);
            case GREEN:
                return new PropertyCard("Green Property", 4, PropertyType.GREEN, false);
            default:
                throw new IllegalArgumentException("Premium factory does not support: " + type);
        }
    }

    /**
     * Create a money card with the specified denomination.
     * @param value the denomination value
     */
    @Override
    public MoneyCard createMoney(int value) {
        return new MoneyCard(value + "M", value, value);
    }
}
