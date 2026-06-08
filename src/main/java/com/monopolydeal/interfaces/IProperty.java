package com.monopolydeal.interfaces;

import com.monopolydeal.enums.PropertyType;

/**
 * Component interface for the Decorator design pattern.
 *
 * Any object that contributes to rent - property cards and their house/hotel
 * decorations - implements this interface so that rent contributions can be
 * composed at runtime.
 */
public interface IProperty {

    /**
     * @return the base rent declared by the innermost component, without
     * taking any attached decoration bonuses into account.
     */
    int getBaseRent();

    /**
     * @return the total rent contribution produced by this component, after
     * walking the decoration chain and summing every attached bonus.
     */
    int getTotalRent();

    /**
     * @return the monetary value (in millions) of this property when used as
     * payment.
     */
    int getValue();

    /**
     * @return the color group this property belongs to.
     */
    PropertyType getColor();
}
