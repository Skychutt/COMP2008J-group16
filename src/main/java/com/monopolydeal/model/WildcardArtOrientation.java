package com.monopolydeal.model;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.PropertyCard;

import java.util.HashMap;
import java.util.Map;

/**
 * Top/bottom colors on printed two-color wild artwork (from actual card PNG layout).
 */
public final class WildcardArtOrientation {

    private static final Map<String, PropertyType> TOP_COLOR = new HashMap<>();
    private static final Map<String, PropertyType> BOTTOM_COLOR = new HashMap<>();

    static {
        map("Wild Red/Yellow", PropertyType.YELLOW, PropertyType.RED);
        map("Wild Blue/Green", PropertyType.BLUE, PropertyType.GREEN);
        map("Wild Green/Black", PropertyType.GREEN, PropertyType.BLACK);
        map("Wild Brown/LightBlue", PropertyType.LIGHTBLUE, PropertyType.BROWN);
        map("Wild LightBlue/Brown", PropertyType.LIGHTBLUE, PropertyType.BROWN);
        map("Wild LightBlue/Black", PropertyType.LIGHTBLUE, PropertyType.BLACK);
        map("Wild Purple/Orange", PropertyType.ORANGE, PropertyType.PURPLE);
        map("Wild Black/LightGreen", PropertyType.LIGHTGREEN, PropertyType.BLACK);
    }

    private static void map(String name, PropertyType top, PropertyType bottom) {
        TOP_COLOR.put(name, top);
        BOTTOM_COLOR.put(name, bottom);
    }

    private WildcardArtOrientation() {
    }

    public static PropertyType getTopColorOnArt(PropertyCard card) {
        return card == null ? null : TOP_COLOR.get(card.getName());
    }

    public static PropertyType getBottomColorOnArt(PropertyCard card) {
        return card == null ? null : BOTTOM_COLOR.get(card.getName());
    }

    /**
     * True when the chosen color is printed on the bottom half of the card art.
     */
    public static boolean shouldFlipForChosenColor(PropertyCard card, PropertyType chosen) {
        if (card == null || chosen == null || !card.isTwoColorWild()) {
            return false;
        }
        PropertyType bottom = getBottomColorOnArt(card);
        if (bottom != null) {
            return chosen == bottom;
        }
        java.util.List<PropertyType> order = card.getNameColorOrder();
        return order.size() == 2 && chosen == order.get(1);
    }
}
