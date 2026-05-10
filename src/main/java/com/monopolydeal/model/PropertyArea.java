package com.monopolydeal.model;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a player's property area containing all their property sets.
 * Properties are organized by color group into PropertySet instances.
 * The player wins when they have 3 or more complete sets of different colors.
 */
public class PropertyArea {
    /** Map from property color to its corresponding PropertySet. */
    private Map<PropertyType, PropertySet> sets;

    /** Construct an empty property area. */
    public PropertyArea() {
        this.sets = new HashMap<>();
    }

    /** @return a list of all property sets in this area */
    public List<PropertySet> getSets() {
        return new ArrayList<>(sets.values());
    }

    /**
     * Get the property set for a specific color.
     * @param color the color group to look up
     * @return the PropertySet for that color, or null if none exists
     */
    public PropertySet getSet(PropertyType color) {
        return sets.get(color);
    }

    /**
     * Add a property card to the appropriate color set.
     * If no set exists for that color yet, a new one is created automatically.
     * @param c the card to add (must be a PropertyCard)
     */
    public void add(Card c) {
        if (c instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) c;
            PropertyType color = pc.getColor();
            sets.computeIfAbsent(color, PropertySet::new);
            sets.get(color).add(pc);
        }
    }

    /**
     * Remove a property card from its color set.
     * If the set becomes empty after removal, it is removed from the map.
     * @param c the card to remove (must be a PropertyCard)
     */
    public void remove(Card c) {
        if (c instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) c;
            PropertyType color = pc.getColor();
            PropertySet set = sets.get(color);
            if (set != null) {
                set.remove(pc);
                if (set.getCards().isEmpty()) {
                    sets.remove(color);
                }
            }
        }
    }

    /**
     * Count the number of fully completed property sets.
     * A player needs 3 complete sets to win the game.
     * @return the number of complete property sets
     */
    public int countCompleteSets() {
        int count = 0;
        for (PropertySet set : sets.values()) {
            if (set.isComplete()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Property Area:\n");
        for (PropertySet set : sets.values()) {
            sb.append("  ").append(set).append("\n");
        }
        return sb.toString();
    }
}
