package com.monopolydeal.model;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of property cards of the same color group.
 * A complete set is required to count toward the victory condition (3 complete sets to win).
 * Each color group has a specific number of cards needed to complete the set.
 */
public class PropertySet {
    private PropertyType color;          // The color group of this set
    private int need;                    // Number of cards required to complete this set
    private List<PropertyCard> cards;    // Property cards currently in this set

    /**
     * Construct a new PropertySet for the given color.
     * The required set size is automatically determined based on game rules.
     * @param color the color group of this property set
     */
    public PropertySet(PropertyType color) {
        this.color = color;
        this.cards = new ArrayList<>();
        this.need = getRequiredSize(color);
    }

    /** @return the color group of this set */
    public PropertyType getColor() {
        return color;
    }

    /** @return the number of cards needed to complete this set */
    public int getNeed() {
        return need;
    }

    /** @return the list of property cards currently in this set */
    public List<PropertyCard> getCards() {
        return cards;
    }

    /** Add a property card to this set. */
    public void add(PropertyCard card) {
        cards.add(card);
    }

    /** Remove a property card from this set. */
    public void remove(PropertyCard card) {
        cards.remove(card);
    }

    /** @return true if this set has enough cards to be considered complete */
    public boolean isComplete() {
        return need > 0 && cards.size() >= need;
    }

    /**
     * Calculate the rent value based on the number of properties in this set.
     * Rent increases as more properties of the same color are collected.
     * @return the current rent value in millions
     */
    public int getRent() {
        int size = cards.size();
        if (size == 0) return 0;

        switch (color) {
            case BROWN:      return size >= 2 ? 2 : 1;
            case LIGHTBLUE:  return size >= 3 ? 4 : (size >= 2 ? 2 : 1);
            case PURPLE:     return size >= 3 ? 6 : (size >= 2 ? 4 : 2);
            case ORANGE:     return size >= 3 ? 8 : (size >= 2 ? 5 : 2);
            case RED:        return size >= 3 ? 8 : (size >= 2 ? 4 : 2);
            case YELLOW:     return size >= 3 ? 8 : (size >= 2 ? 4 : 2);
            case GREEN:      return size >= 3 ? 7 : (size >= 2 ? 4 : 2);
            case BLUE:       return size >= 2 ? 8 : 3;
            case BLACK:      return size >= 4 ? 4 : (size >= 3 ? 3 : (size >= 2 ? 2 : 1));
            case LIGHTGREEN: return size >= 2 ? 2 : 1;
            default:         return 0;
        }
    }

    /**
     * Determine the number of property cards needed to complete a set of the given color.
     * @param color the property color group
     * @return the required number of cards for a complete set
     */
    private int getRequiredSize(PropertyType color) {
        switch (color) {
            case BROWN:
            case BLUE:
            case LIGHTGREEN:
                return 2;   // These colors only need 2 cards
            case LIGHTBLUE:
            case PURPLE:
            case ORANGE:
            case RED:
            case YELLOW:
            case GREEN:
                return 3;   // These colors need 3 cards
            case BLACK:
                return 4;   // Railroad needs 4 cards
            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        return color + " Set (" + cards.size() + "/" + need + ")" + (isComplete() ? " [COMPLETE]" : "");
    }
}
