package com.monopolydeal.ai;

import com.monopolydeal.enums.PropertyType;

/**
 * One turn step chosen by the bot controller for the current player.
 */
public class BotTurnChoice {
    public enum Kind {
        PLAY_CARD,
        DEPOSIT_TO_BANK,
        DISCARD,
        END_TURN
    }

    private final Kind kind;
    private final int targetCardId;
    private final PropertyType wildColor;

    public BotTurnChoice(Kind kind) {
        this(kind, -1, null);
    }

    public BotTurnChoice(Kind kind, int targetCardId) {
        this(kind, targetCardId, null);
    }

    public BotTurnChoice(Kind kind, int targetCardId, PropertyType wildColor) {
        this.kind = kind;
        this.targetCardId = targetCardId;
        this.wildColor = wildColor;
    }

    public Kind getKind() {
        return kind;
    }

    public int getTargetCardId() {
        return targetCardId;
    }

    public PropertyType getWildColor() {
        return wildColor;
    }
}
