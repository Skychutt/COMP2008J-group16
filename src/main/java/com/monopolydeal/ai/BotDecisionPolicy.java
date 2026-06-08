package com.monopolydeal.ai;

import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Player;

/**
 * Policy contract for choosing bot turn actions and interrupt responses.
 */
public interface BotDecisionPolicy {
    BotTurnChoice pickTurnChoice(Player bot, GameLogic gameLogic);

    boolean wantsJustSayNoCounter(Player responder);
}
