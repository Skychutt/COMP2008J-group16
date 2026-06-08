package com.monopolydeal.logic;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;
import com.monopolydeal.model.PropertySet;

/**
 * Responsible for calculating rent values based on property sets, buildings, and modifiers.
 */
public class RentCalculator {

    private static final int HOUSE_BONUS = 3;
    private static final int HOTEL_BONUS = 4;

    /**
     * Calculates the final rent to charge, including base rent, building bonuses, and multipliers.
     * @param set the property set to collect rent from
     * @param isDoubleRent whether a Double Rent modifier is active
     * @return the total rent amount to be paid
     */
    public static int calculateFinalRent(PropertySet set, boolean isDoubleRent) {
        if (!canCollectRent(set)) {
            return 0;
        }
        int rent = getBaseRent(set) + getBuildingBonus(set);
        if (isDoubleRent) {
            rent *= 2;
        }
        return rent;
    }

    private static int getBaseRent(PropertySet set) {
        return set.getRent();
    }

    /**
     * Sums the rent bonuses from any attached House/Hotel upgrades.
     * Delegates to each card's {@link PropertyCard#getTotalRent()},
     * which walks the Decorator chain when available and falls back to the
     * legacy upgrade list otherwise.
     */
    private static int getBuildingBonus(PropertySet set) {
        int bonus = 0;
        for (PropertyCard pc : set.getCards()) {
            bonus += pc.getTotalRent();
        }
        return bonus;
    }

    /**
     * Checks if the property set is eligible for rent collection.
     * In Monopoly Deal, rent can be charged on incomplete sets too.
     * @param set the property set to check
     * @return true if the set has a valid rent value
     */
    public static boolean canCollectRent(PropertySet set) {
        return set != null && !set.getCards().isEmpty() && set.getRent() > 0;
    }
}
