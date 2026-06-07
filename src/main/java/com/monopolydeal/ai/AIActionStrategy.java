package com.monopolydeal.ai;

import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Player;

/**
 * Strategy contract for choosing AI turn actions and interrupt responses.
 */
public interface AIActionStrategy {
    AIAction decideNextAction(Player ai, GameLogic gameLogic);

    boolean shouldUseJustSayNo(Player responder);
}
