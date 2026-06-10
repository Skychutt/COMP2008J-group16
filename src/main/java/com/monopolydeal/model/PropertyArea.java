package com.monopolydeal.model;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Clears all property sets (used when rebuilding LAN client mirror state). */
    public void clearAll() {
        sets.clear();
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
        add(c, null);
    }

    /**
     * @param chosenColor player-selected color for two-color wilds; ignored if already committed
     */
    public void add(Card c, PropertyType chosenColor) {
        if (c instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) c;
            PropertyType color = resolvePlacementColor(pc, chosenColor);
            sets.computeIfAbsent(color, PropertySet::new);
            if (!pc.isColorCommitted()) {
                pc.commitColor(chosenColor != null ? chosenColor : color);
            }
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
            for (Map.Entry<PropertyType, PropertySet> entry : new ArrayList<>(sets.entrySet())) {
                PropertySet set = entry.getValue();
                if (set.getCards().remove(pc)) {
                    if (set.getCards().isEmpty()) {
                        sets.remove(entry.getKey());
                    }
                    return;
                }
            }
        }
    }

    /**
     * Rearranges a property card between sets (used for wild-card repositioning on turn).
     * @return true if moved successfully
     */
    public boolean moveProperty(PropertyCard card, PropertyType targetColor) {
        if (card == null || targetColor == null) {
            return false;
        }
        if (card.isColorCommitted() && card.getColor() != targetColor) {
            return false;
        }
        Set<PropertyType> options = card.getAssignableColors();
        if (!options.contains(targetColor)) {
            return false;
        }

        remove(card);
        if (!card.isColorCommitted()) {
            card.setColor(targetColor);
        }
        sets.computeIfAbsent(targetColor, PropertySet::new);
        sets.get(targetColor).add(card);
        return true;
    }

    /**
     * When a real property of color {@code placedColor} is played onto that set,
     * reclaim one two-color wildcard already in the set (official swap rule).
     * @return the wildcard returned to hand, or null if none was swapped
     */
    public PropertyCard tryReclaimWildcardAfterPlacement(PropertyCard placed) {
        if (placed == null || placed.isWild()) {
            return null;
        }
        PropertyType placedColor = placed.getColor();
        PropertySet set = sets.get(placedColor);
        if (set == null) {
            return null;
        }

        for (PropertyCard existing : new ArrayList<>(set.getCards())) {
            if (existing == placed) {
                continue;
            }
            if (!existing.isWild() || existing.isFullColorWild()) {
                continue;
            }
            if (existing.getAssignableColors().contains(placedColor)) {
                set.remove(existing);
                if (set.getCards().isEmpty()) {
                    sets.remove(placedColor);
                }
                return existing;
            }
        }
        return null;
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

    private PropertyType resolvePlacementColor(PropertyCard card, PropertyType chosenColor) {
        if (card.isColorCommitted()) {
            return card.getColor();
        }
        if (chosenColor != null && card.getAssignableColors().contains(chosenColor)) {
            return chosenColor;
        }

        Set<PropertyType> options = card.getAssignableColors();
        if (options.isEmpty()) {
            return card.getColor();
        }
        if (options.size() == 1) {
            return options.iterator().next();
        }

        // Multi-color wild must be chosen by the player in GUI; console/auto uses best-fit color.
        PropertyType best = null;
        int bestScore = Integer.MIN_VALUE;
        for (PropertyType option : options) {
            if (option == PropertyType.RAINBOW) {
                continue;
            }
            int score = scorePlacement(option);
            if (score > bestScore) {
                bestScore = score;
                best = option;
            }
        }

        if (best != null) {
            return best;
        }
        return options.iterator().next();
    }

    private int scorePlacement(PropertyType color) {
        PropertySet existing = sets.get(color);
        int size = existing == null ? 0 : existing.getCards().size();
        int need = requiredSize(color);
        int deficit = Math.max(0, need - size);

        // Higher is better: prioritize nearly complete existing sets.
        return (size * 10) - deficit;
    }

    private int requiredSize(PropertyType color) {
        switch (color) {
            case BROWN:
            case BLUE:
            case LIGHTGREEN:
                return 2;
            case LIGHTBLUE:
            case PURPLE:
            case ORANGE:
            case RED:
            case YELLOW:
            case GREEN:
                return 3;
            case BLACK:
                return 4;
            default:
                return Integer.MAX_VALUE;
        }
    }
}