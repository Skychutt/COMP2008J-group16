package com.monopolydeal.model.card;

import com.monopolydeal.decorator.HotelDecorator;
import com.monopolydeal.decorator.HouseDecorator;
import com.monopolydeal.decorator.PropertyDecorator;
import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.interfaces.IPlayable;
import com.monopolydeal.interfaces.IProperty;
import com.monopolydeal.interfaces.IUpgradable;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.WildcardArtOrientation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Represents a property card that belongs to a specific color group.
 * Property cards are placed in the player's property area to form complete sets.
 * Wild property cards (isWild = true) can substitute for different color groups.
 *
 * <p>This class also acts as the <b>Concrete Component</b> in the Decorator
 * design pattern: House and Hotel upgrades wrap this card at runtime via
 * {@link HouseDecorator} and {@link HotelDecorator}, augmenting the rent
 * charged on the owning property set without subclassing.
 */
public class PropertyCard extends Card implements IPlayable, IUpgradable, IProperty {
    private PropertyType color;          // The color group this property belongs to
    private boolean isWild;              // True if this is a wildcard property
    private boolean colorCommitted;      // Once set for this game, assigned color cannot change
    private boolean displayFlipped;      // Locked 180° rotation so chosen color faces up on art
    private List<Card> upgrades;         // Attached upgrade cards (kept for GUI rendering)

    /**
     * Head of the decorator chain used for runtime rent composition. When no
     * decoration has been applied this is {@code null} and rent bonus is zero.
     */
    private PropertyDecorator decoratorChain;

    /**
     * Construct a new PropertyCard.
     * @param name   display name of the property
     * @param value  monetary value when used as payment
     * @param color  the color group of this property
     * @param isWild true if this card is a wildcard that can change color
     */
    public PropertyCard(String name, int value, PropertyType color, boolean isWild) {
        super(name, value);
        this.color = color;
        this.isWild = isWild;
        this.upgrades = new ArrayList<>();
        this.decoratorChain = null;
    }

    /** @return the color group of this property */
    public PropertyType getColor() {
        return color;
    }

    /** Set the color group before it is locked for this game. */
    public void setColor(PropertyType color) {
        if (colorCommitted) {
            return;
        }
        this.color = color;
    }

    /** @return true when this card's color choice is locked for the rest of the game */
    public boolean isColorCommitted() {
        return colorCommitted;
    }

    /**
     * Locks the assigned color for this card instance (survives trades and steals).
     */
    public void commitColor(PropertyType chosen) {
        if (chosen == null) {
            return;
        }
        if (colorCommitted) {
            return;
        }
        Set<PropertyType> options = getAssignableColors();
        if (!options.isEmpty() && !options.contains(chosen) && chosen != PropertyType.RAINBOW) {
            return;
        }
        this.color = chosen;
        if (isTwoColorWild()) {
            displayFlipped = WildcardArtOrientation.shouldFlipForChosenColor(this, chosen);
        }
        if (isWild()) {
            this.colorCommitted = true;
        }
    }

    /** Two-color wild property (not the 10-color rainbow wild). */
    public boolean isTwoColorWild() {
        return isWild() && !isFullColorWild() && getNameColorOrder().size() == 2;
    }

    /**
     * Player must pick a color before placing (two-color wilds and rainbow / multi-color wilds).
     */
    public boolean needsColorChoiceOnPlacement() {
        if (colorCommitted || !isWild()) {
            return false;
        }
        return getAssignableColors().size() > 1;
    }

    /**
     * Property cards with a monetary value may be banked (rainbow / 10-color wild cannot).
     */
    public boolean canBankAsMoney() {
        return canBeUsedAsPayment();
    }

    /**
     * Colors in card art order (top color first) parsed from the card name.
     */
    public List<PropertyType> getNameColorOrder() {
        return new ArrayList<>(parseColorsFromName());
    }

    /** Rotate 180° so the committed color appears at the top of the card art. */
    public boolean isDisplayFlipped() {
        return colorCommitted && isTwoColorWild() && displayFlipped;
    }

    /** @return true if this is a wildcard property */
    public boolean isWild() {
        return isWild;
    }

    /** @return true if this is the multi-color (10-color) wild card */
    public boolean isFullColorWild() {
        if (!isWild) {
            return false;
        }
        if (color == PropertyType.RAINBOW) {
            return true;
        }
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return lower.contains("rainbow") || lower.contains("multicolored");
    }

    /**
     * Returns all colors this card can represent on table.
     * Normal properties return a single fixed color.
     */
    public Set<PropertyType> getAssignableColors() {
        if (!isWild) {
            return Collections.singleton(color);
        }

        if (isFullColorWild()) {
            EnumSet<PropertyType> all = EnumSet.allOf(PropertyType.class);
            all.remove(PropertyType.RAINBOW);
            return all;
        }

        Set<PropertyType> parsed = parseColorsFromName();
        if (!parsed.isEmpty()) {
            return parsed;
        }

        if (color != null && color != PropertyType.RAINBOW) {
            return Collections.singleton(color);
        }
        return Collections.emptySet();
    }

    /**
     * Multi-color wild card cannot be used as payment on its own.
     * Other property cards can be used.
     */
    public boolean canBeUsedAsPayment() {
        return !isFullColorWild() && value > 0;
    }

    /** @return the list of upgrade cards (House/Hotel) attached to this property */
    public List<Card> getUpgrades() {
        return upgrades;
    }

    /**
     * Play this property card by placing it into the player's property area.
     * @param p the player who plays this card
     */
    @Override
    public void executePlay(Player p) {
        // Placement is handled by Player.placeProperty / GameLogic.playCard so actions and hand removal stay correct.
    }

    /**
     * Attach an upgrade card (House or Hotel) to this property.
     * <p>Kept for backward compatibility with callers that still work with raw
     * ActionCard instances. Prefer the Decorator-style {@link #attachHouse()}
     * and {@link #attachHotel()} methods in new code, which are responsible
     * for composing the runtime chain used by {@link #getTotalRent()}.
     *
     * @param c the upgrade card to attach
     */
    @Override
    public void attachUpgrade(Card c) {
        upgrades.add(c);
    }

    // ========================== Decorator-style attachment ==========================

    /**
     * Attach a House decoration via the Decorator design pattern. A new
     * {@link HouseDecorator} layer is pushed on top of the current chain,
     * adding +3M rent when the chain is summed.
     */
    public void attachHouse() {
        decoratorChain = new HouseDecorator(decoratorChain == null ? this : decoratorChain);
    }

    /**
     * Attach a Hotel decoration via the Decorator design pattern. A new
     * {@link HotelDecorator} layer is pushed on top of the current chain,
     * adding +4M rent when the chain is summed.
     */
    public void attachHotel() {
        decoratorChain = new HotelDecorator(decoratorChain == null ? this : decoratorChain);
    }

    /** @return the head of the decorator chain (null if no decoration is attached). */
    public PropertyDecorator getDecoratorChain() {
        return decoratorChain;
    }

    /** @return true if at least one House layer is present in the chain. */
    public boolean hasHouse() {
        return countDecoratorsOfType(HouseDecorator.class) > 0;
    }

    /** @return true if at least one Hotel layer is present in the chain. */
    public boolean hasHotel() {
        return countDecoratorsOfType(HotelDecorator.class) > 0;
    }

    /**
     * Count how many layers in the chain are instances of the given decorator subclass.
     */
    private int countDecoratorsOfType(Class<? extends PropertyDecorator> type) {
        // Walk from outside to inside and count matching layers.
        int count = 0;
        PropertyDecorator current = decoratorChain;
        while (current != null) {
            if (type.isInstance(current)) {
                count++;
            }
            com.monopolydeal.interfaces.IProperty inner = current.getDecorated();
            current = inner instanceof PropertyDecorator ? (PropertyDecorator) inner : null;
        }
        return count;
    }

    // ========================== IProperty implementation ==========================

    /**
     * The raw property card declares no base rent by itself - that is driven
     * by the color group and how many cards are in the owning property set.
     */
    @Override
    public int getBaseRent() {
        return 0;
    }

    /**
     * Walks the decorator chain iteratively from the outermost layer inward
     * and sums each layer's bonus. This avoids the recursive pattern that
     * would otherwise create an infinite cycle between PropertyCard and its
     * decorators.
     *
     * <p>As a fallback for legacy paths that still use {@link #attachUpgrade(Card)}
     * (used by unit tests and GUI drag-and-drop), when no decorator chain is
     * present the raw {@link #upgrades} list is scanned and House/Hotel
     * bonuses are counted using the same constants as the Decorator layer.
     */
    @Override
    public int getTotalRent() {
        if (decoratorChain != null) {
            int total = 0;
            PropertyDecorator current = decoratorChain;
            while (current != null) {
                total += current.getLayerBonus();
                com.monopolydeal.interfaces.IProperty inner = current.getDecorated();
                current = inner instanceof PropertyDecorator ? (PropertyDecorator) inner : null;
            }
            return total;
        }
        // Fallback for legacy callers that still use the raw upgrade list.
        int bonus = 0;
        for (Card u : upgrades) {
            if (u instanceof ActionCard) {
                ActionType t = ((ActionCard) u).getType();
                if (t == ActionType.HOUSE) {
                    bonus += HouseDecorator.HOUSE_BONUS;
                } else if (t == ActionType.HOTEL) {
                    bonus += HotelDecorator.HOTEL_BONUS;
                }
            }
        }
        return bonus;
    }

    @Override
    public String toString() {
        return name + " [" + color + "]" + (isWild ? " (Wild)" : "") + " (Value: " + value + "M)";
    }

    private LinkedHashSet<PropertyType> parseColorsFromName() {
        if (name == null) {
            return new LinkedHashSet<>();
        }
        String lower = name.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("wild");
        if (idx < 0) {
            return new LinkedHashSet<>();
        }

        String tail = name.substring(idx + 4).trim();
        if (tail.isEmpty()) {
            return new LinkedHashSet<>();
        }

        String[] parts = tail.split("/");
        LinkedHashSet<PropertyType> colors = new LinkedHashSet<>();
        for (String raw : parts) {
            PropertyType t = parsePropertyType(raw);
            if (t != null && t != PropertyType.RAINBOW) {
                colors.add(t);
            }
        }
        return colors.isEmpty() ? new LinkedHashSet<>() : colors;
    }

    private PropertyType parsePropertyType(String token) {
        if (token == null) {
            return null;
        }
        String n = token.replaceAll("[\\s_\\-]", "").toLowerCase(Locale.ROOT);
        switch (n) {
            case "brown":
                return PropertyType.BROWN;
            case "lightblue":
                return PropertyType.LIGHTBLUE;
            case "purple":
                return PropertyType.PURPLE;
            case "orange":
                return PropertyType.ORANGE;
            case "red":
                return PropertyType.RED;
            case "yellow":
                return PropertyType.YELLOW;
            case "green":
                return PropertyType.GREEN;
            case "blue":
                return PropertyType.BLUE;
            case "black":
            case "railroad":
                return PropertyType.BLACK;
            case "lightgreen":
            case "utility":
                return PropertyType.LIGHTGREEN;
            default:
                return null;
        }
    }
}
