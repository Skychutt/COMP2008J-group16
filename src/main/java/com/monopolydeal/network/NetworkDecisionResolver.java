package com.monopolydeal.network;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.DecisionResolver;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Routes interactive player decisions from the server to the correct LAN client.
 */
public class NetworkDecisionResolver implements DecisionResolver {

    private final GameServer server;

    public NetworkDecisionResolver(GameServer server) {
        this.server = server;
    }

    @Override
    public int chooseOption(Player player,
                            String title,
                            String prompt,
                            List<String> options,
                            boolean allowCancel) {
        if (player == null || options == null || options.isEmpty()) {
            return -1;
        }
        int playerIndex = server.getPlayerIndex(player);
        if (playerIndex < 0) {
            return -1;
        }

        String requestId = UUID.randomUUID().toString();
        DecisionPayload payload = new DecisionPayload(requestId, title, prompt, options, allowCancel);
        return server.awaitDecision(playerIndex, payload);
    }

    @Override
    public PropertyType choosePropertyColor(Player player, PropertyCard card) {
        if (card == null) {
            return null;
        }
        List<PropertyType> colors = new ArrayList<>(card.getAssignableColors());
        if (colors == null || colors.isEmpty()) {
            return card.getColor();
        }
        if (colors.size() == 1) {
            return colors.get(0);
        }

        List<String> labels = new ArrayList<>();
        for (PropertyType color : colors) {
            labels.add(color.name());
        }
        int index = chooseOption(
                player,
                "Choose Color",
                "Choose a color for " + card.getName() + ":",
                labels,
                false
        );
        if (index < 0 || index >= colors.size()) {
            return colors.get(0);
        }
        return colors.get(index);
    }
}
