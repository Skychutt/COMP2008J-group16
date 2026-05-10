package com.monopolydeal.enums;

/**
 * Enum representing all property color types in the Monopoly Deal game.
 * Each color corresponds to a distinct property group on the board.
 * RAINBOW is a special wildcard color that can represent any color group.
 */
public enum PropertyType {
    RED,
    YELLOW,
    GREEN,
    BLACK,        // Railroad
    PURPLE,
    BLUE,
    ORANGE,
    LIGHTBLUE,
    LIGHTGREEN,   // Utility
    BROWN,
    RAINBOW       // Wildcard - can substitute for any color
}
