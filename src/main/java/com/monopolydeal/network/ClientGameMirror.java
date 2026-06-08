package com.monopolydeal.network;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

import java.util.Locale;

/**
 * Rebuilds a lightweight {@link GameManager} from a network snapshot so the client
 * can run the same {@link com.monopolydeal.logic.RuleValidator} as local mode.
 */
public final class ClientGameMirror {

    private ClientGameMirror() {
    }

    public static void applySnapshot(GameManager gm, GameStateParser.Snapshot snap, int myIndex) {
        if (gm == null || snap == null || snap.players == null) {
            return;
        }

        gm.applyMirrorFlags(snap.turn, snap.gameOver);

        for (GameStateParser.PlayerInfo info : snap.players) {
            if (info.index < 0 || info.index >= gm.getPlayers().size()) {
                continue;
            }
            Player player = gm.getPlayers().get(info.index);
            player.setActions(info.actions);

            player.getHand().getCards().clear();
            if (info.index == myIndex && snap.myHand != null) {
                player.clearReportedHandSize();
                for (GameStateParser.CardInfo cardInfo : snap.myHand) {
                    Card card = toCard(cardInfo);
                    if (card != null) {
                        player.getHand().add(card);
                    }
                }
            } else {
                player.setReportedHandSize(info.handSize);
            }

            player.getBankArea().getMoney().clear();
            if (info.bankCards != null) {
                for (GameStateParser.CardInfo cardInfo : info.bankCards) {
                    Card card = toCard(cardInfo);
                    if (card != null) {
                        player.getBankArea().add(card);
                    }
                }
            }

            player.getPropertyArea().clearAll();
            if (info.propertySets != null) {
                for (GameStateParser.PropertySetInfo setInfo : info.propertySets) {
                    PropertyType color = parseColor(setInfo.color);
                    if (color == null) {
                        continue;
                    }
                    if (setInfo.cards != null) {
                        for (GameStateParser.CardInfo cardInfo : setInfo.cards) {
                            PropertyCard propertyCard = toPropertyCard(cardInfo, color);
                            if (propertyCard != null) {
                                player.getPropertyArea().add(propertyCard, color);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Card toCard(GameStateParser.CardInfo info) {
        if (info == null) {
            return null;
        }
        Card card;
        if ("PROPERTY".equals(info.cardType)) {
            PropertyType color = parseColor(info.color);
            if (color == null) {
                color = PropertyType.RAINBOW;
            }
            card = new PropertyCard(info.name, info.value, color, info.isWild);
        } else if ("ACTION".equals(info.cardType)) {
            ActionType type = ActionType.GO_PASS;
            if (info.actionType != null) {
                try {
                    type = ActionType.valueOf(info.actionType);
                } catch (IllegalArgumentException ignored) {
                }
            }
            card = new ActionCard(info.name, info.value, type, true);
        } else if ("MONEY".equals(info.cardType)) {
            card = new MoneyCard(info.name, info.value, info.value);
        } else {
            card = new MoneyCard(info.name, info.value, info.value);
        }
        card.syncIdFromNetwork(info.id);
        return card;
    }

    private static PropertyCard toPropertyCard(GameStateParser.CardInfo info, PropertyType laneColor) {
        if (info == null || laneColor == null) {
            return null;
        }
        PropertyType cardColor = parseColor(info.color);
        if (cardColor == null) {
            cardColor = laneColor;
        }
        boolean wild = info.isWild || looksLikeWildProperty(info.name);
        PropertyCard card = new PropertyCard(info.name, info.value, cardColor, wild);
        card.syncIdFromNetwork(info.id);
        return card;
    }

    private static boolean looksLikeWildProperty(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("wild") || name.contains("/");
    }

    private static PropertyType parseColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PropertyType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
