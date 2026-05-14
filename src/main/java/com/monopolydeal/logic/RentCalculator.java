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
     * @param set the completed property set to collect rent from
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

    private static int getBuildingBonus(PropertySet set) {
        int bonus = 0;
        for (PropertyCard pc : set.getCards()) {
            for (Card u : pc.getUpgrades()) {
                if (u instanceof ActionCard) {
                    ActionType t = ((ActionCard) u).getType();
                    if (t == ActionType.HOUSE) {
                        bonus += HOUSE_BONUS;
                    } else if (t == ActionType.HOTEL) {
                        bonus += HOTEL_BONUS;
                    }
                }
            }
        }
        return bonus;
    }

    /**
     * Checks if the property set is complete and eligible for rent collection.
     * @param set the property set to check
     * @return true if the set is complete and rent can be collected
     */
    public static boolean canCollectRent(PropertySet set) {
        return set != null && set.isComplete();
    }
}
