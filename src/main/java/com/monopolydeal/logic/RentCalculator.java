package com.monopolydeal.logic;

import com.monopolydeal.model.card.PropertyCard;
import com.monopolydeal.model.PropertySet;

/**
 * Responsible for calculating rent values based on property sets, buildings, and modifiers.
 * Core logic for the rent collection phase.
 */
public class RentCalculator {

    /**
     * Calculates the final rent to charge, including base rent, building bonuses, and multipliers.
     * @param set the completed property set to collect rent from
     * @param isDoubleRent whether a Double Rent modifier is active
     * @return the total rent amount to be paid
     */
    public static int calculateFinalRent(PropertySet set, boolean isDoubleRent) {
        return 0;
    }

    /**
     * Retrieves the base rent value for a given property set, without any modifiers.
     * @param set the target property set
     * @return the base rent value
     */
    private static int getBaseRent(PropertySet set) {
        return 0;
    }

    /**
     * Calculates the additional rent bonus from House and Hotel cards on the property set.
     * @param set the target property set
     * @return the total building bonus
     */
    private static int getBuildingBonus(PropertySet set) {
        return 0;
    }

    /**
     * Checks if the property set is complete and eligible for rent collection.
     * @param set the property set to check
     * @return true if the set is complete and rent can be collected
     */
    public static boolean canCollectRent(PropertySet set) {
        return false;
    }
}