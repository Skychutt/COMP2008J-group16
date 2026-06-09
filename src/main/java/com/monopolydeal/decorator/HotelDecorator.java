package com.monopolydeal.decorator;

import com.monopolydeal.interfaces.IProperty;

/**
 * Concrete Decorator: a Hotel adds +4M to the rent value of the
 * decorated property card. A Hotel should only be placed on a set that
 * already has at least one House — enforced by {@code RuleValidator}.
 */
public class HotelDecorator extends PropertyDecorator {

    /** Rent bonus (in millions) granted by a Hotel building card. */
    public static final int HOTEL_BONUS = 4;

    /**
     * @param decorated the component to wrap.
     */
    public HotelDecorator(IProperty decorated) {
        super(decorated, HOTEL_BONUS, "Hotel");
    }
}
