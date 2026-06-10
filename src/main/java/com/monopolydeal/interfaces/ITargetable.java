package com.monopolydeal.interfaces;

import com.monopolydeal.model.Player;

/**
 * Interface for action cards that target a specific opponent player.
 * Examples include Sly Deal, Forced Deal, and Debt Collector.
 */
public interface ITargetable {

    /** Apply this card's targeted effect to the specified opponent player. */
    void applyToTarget(Player target);
}
