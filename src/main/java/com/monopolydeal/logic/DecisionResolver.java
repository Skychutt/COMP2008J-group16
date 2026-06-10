package com.monopolydeal.logic;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.PropertyCard;

import java.util.List;

/**
 * Resolves interactive choices for AI-controlled players.
 */
public interface DecisionResolver {
    int chooseOption(Player player, String title, String prompt, List<String> options, boolean allowCancel);

    PropertyType choosePropertyColor(Player player, PropertyCard card);
}
