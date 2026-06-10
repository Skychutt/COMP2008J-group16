package com.monopolydeal.network;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON payload for interactive decisions routed to a specific client.
 */
public final class DecisionPayload {

    public final String requestId;
    public final String title;
    public final String prompt;
    public final List<String> options;
    public final boolean allowCancel;

    public DecisionPayload(String requestId,
                           String title,
                           String prompt,
                           List<String> options,
                           boolean allowCancel) {
        this.requestId = requestId;
        this.title = title;
        this.prompt = prompt;
        this.options = options == null ? List.of() : new ArrayList<>(options);
        this.allowCancel = allowCancel;
    }

    public String toJson() {
        return "{"
                + "\"requestId\":\"" + NetworkJson.esc(requestId) + "\","
                + "\"title\":\"" + NetworkJson.esc(title) + "\","
                + "\"prompt\":\"" + NetworkJson.esc(prompt) + "\","
                + "\"allowCancel\":" + allowCancel + ","
                + "\"options\":[" + NetworkJson.joinStrings(options) + "]"
                + "}";
    }

    public static DecisionPayload fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new DecisionPayload("", "", "", List.of(), false);
        }
        String requestId = NetworkJson.str(json, "requestId");
        String title = NetworkJson.str(json, "title");
        String prompt = NetworkJson.str(json, "prompt");
        boolean allowCancel = NetworkJson.bool(json, "allowCancel");

        List<String> options = new ArrayList<>();
        String optionsBody = NetworkJson.array(json, "options");
        if (optionsBody != null) {
            for (String part : splitStringLiterals(optionsBody)) {
                options.add(part);
            }
        }
        return new DecisionPayload(requestId, title, prompt, options, allowCancel);
    }

    public static String responseJson(String requestId, int choiceIndex) {
        return "{"
                + "\"requestId\":\"" + NetworkJson.esc(requestId) + "\","
                + "\"choice\":" + choiceIndex
                + "}";
    }

    public static String parseRequestId(String json) {
        return NetworkJson.str(json, "requestId");
    }

    public static int parseChoice(String json) {
        return NetworkJson.num(json, "choice");
    }

    private static List<String> splitStringLiterals(String body) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < body.length()) {
            int quote = body.indexOf('"', i);
            if (quote < 0) {
                break;
            }
            int end = quote + 1;
            while (end < body.length()) {
                char c = body.charAt(end);
                if (c == '\\') {
                    end += 2;
                } else if (c == '"') {
                    break;
                } else {
                    end++;
                }
            }
            String raw = body.substring(quote + 1, end);
            out.add(raw.replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\\\", "\\"));
            i = end + 1;
        }
        return out;
    }
}
