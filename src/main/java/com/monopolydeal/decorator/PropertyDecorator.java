package com.monopolydeal.decorator;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.interfaces.IProperty;

/**
 * Abstract Decorator in the Decorator design pattern.
 *
 * Wraps an {@link IProperty} component and exposes a default implementation
 * of the interface. Concrete subclasses ({@link HouseDecorator},
 * {@link HotelDecorator}) add an extra rent bonus on top of the wrapped
 * component's rent.
 *
 * <p>This class intentionally does NOT call {@code decorated.getTotalRent()}
 * recursively to avoid an infinite cycle when the innermost component is a
 * {@link com.monopolydeal.model.card.PropertyCard} - which itself walks the
 * decorator chain iteratively. Each layer instead exposes its own single
 * bonus via {@link #getLayerBonus()}.
 */
public abstract class PropertyDecorator implements IProperty {

    /** The wrapped (decorated) component - may itself be another decorator. */
    protected final IProperty decorated;

    /** Rent bonus (in millions) added by this decoration layer. */
    protected final int bonusRent;

    /** Display name used for logging / debugging. */
    protected final String decoratorName;

    /**
     * @param decorated     the component being wrapped
     * @param bonusRent     rent bonus (in millions) added by this decorator
     * @param decoratorName human-readable label for the layer
     */
    protected PropertyDecorator(IProperty decorated, int bonusRent, String decoratorName) {
        this.decorated = decorated;
        this.bonusRent = bonusRent;
        this.decoratorName = decoratorName;
    }

    /** Base rent is delegated to the wrapped component. */
    @Override
    public int getBaseRent() {
        return decorated.getBaseRent();
    }

    /**
     * @return the rent bonus for this single decoration layer only. The full
     * chain is summed iteratively by the owning card.
     */
    @Override
    public int getTotalRent() {
        return bonusRent;
    }

    /** @return the rent bonus (in millions) for this single decoration layer. */
    public int getLayerBonus() {
        return bonusRent;
    }

    @Override
    public int getValue() {
        return decorated.getValue();
    }

    @Override
    public PropertyType getColor() {
        return decorated.getColor();
    }

    /** @return the wrapped component. */
    public IProperty getDecorated() {
        return decorated;
    }

    @Override
    public String toString() {
        return decorated + " + " + decoratorName + "(+" + bonusRent + "M)";
    }
}
