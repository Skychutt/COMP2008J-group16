package com.monopolydeal.enums;

/**
 * Enum representing all action card types in the Monopoly Deal game.
 * Each type defines a unique card effect that can be played during a turn.
 */
public enum ActionType {
    GO_PASS,        // Draw 2 extra cards from the deck
    DOUBLE_RENT,    // Double the rent amount charged to opponents
    BIRTHDAY,       // Collect 2M from every other player
    JUST_SAY_NO,    // Cancel any action played against you
    DEAL_BREAKER,   // Steal a complete property set from an opponent
    SLY_DEAL,       // Steal a single property from an opponent
    FORCED_DEAL,    // Force a property swap with an opponent
    DEBT_DEAL,      // Charge a single player 5M
    HOTEL,          // Add a hotel to a complete property set for extra rent
    HOUSE           // Add a house to a complete property set for extra rent
}
