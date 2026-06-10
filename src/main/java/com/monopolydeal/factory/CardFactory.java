package com.monopolydeal.factory;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

/**
 * Abstract Factory for creating different types of cards.
 * Uses the Abstract Factory design pattern to support unified card production.
 * Subclasses (StandardCardFactory, PremiumCardFactory) provide concrete implementations.
 */
public abstract class CardFactory {

    /** Create an action card of the specified type. */
    public abstract ActionCard createAction(ActionType type);

    /** Create a property card of the specified color type. */
    public abstract PropertyCard createProperty(PropertyType type);

    /** Create a money card with the specified denomination value. */
    public abstract MoneyCard createMoney(int value);
}
