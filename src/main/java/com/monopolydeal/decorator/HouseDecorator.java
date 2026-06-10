package com.monopolydeal.decorator;

import com.monopolydeal.interfaces.IProperty;

/**
 * Concrete Decorator: a House adds +3M to the rent value of the
 * decorated property card.
 */
public class HouseDecorator extends PropertyDecorator {

    /** Rent bonus (in millions) granted by a House building card. */
    public static final int HOUSE_BONUS = 3;

    /**
     * @param decorated the component to wrap.
     */
    public HouseDecorator(IProperty decorated) {
        super(decorated, HOUSE_BONUS, "House");
    }
}
