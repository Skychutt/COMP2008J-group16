package com.monopolydeal.gui;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Resolves card images from Card_Library on the classpath.
 *
 * All mappings are hard-coded against the actual files in
 * src/main/resources/Card_Library/.
 *
 * Network mode (CardInfo) works by reconstructing a temporary Card object
 * and delegating to the same getCardIcon(Card) path used by local mode,
 * so both modes are guaranteed to produce identical results.
 */
public class CardImageResolver {

    private static final String LIBRARY_PATH = "Card_Library";

    // ── Exact file paths relative to Card_Library/ ───────────────────────────

    private static final String F_PASS_GO        = "ActionCard/pass-go-action-card.jpeg";
    private static final String F_BIRTHDAY        = "ActionCard/it's-my-birthday-action-card.jpeg";
    private static final String F_DEBT_COLLECTOR  = "ActionCard/debt-collector-action-card.png";
    private static final String F_SLY_DEAL        = "ActionCard/sly-deal-action-card.png";
    private static final String F_FORCED_DEAL     = "ActionCard/force-deal-action-card.png";
    private static final String F_DEAL_BREAKER    = "ActionCard/deal-breaker-action-card.png";
    private static final String F_JUST_SAY_NO     = "ActionCard/just-say-no-action-card.jpeg";
    private static final String F_DOUBLE_RENT     = "ActionCard/double-the-rent-action-card.png";
    private static final String F_HOUSE           = "ActionCard/house-action-card.jpeg";
    private static final String F_HOTEL           = "ActionCard/hotel-action-card.png";
    private static final String F_RENT_ANY        = "ActionCard/all-color-wild-rent-card.jpg";
    private static final String F_RENT_BLUE_GREEN = "ActionCard/blue-and-green-rent-card.jpg";
    private static final String F_RENT_RED_YELLOW = "ActionCard/red-and-yellow-rent-card.png";
    private static final String F_RENT_PURP_ORAN  = "ActionCard/orange-and-pink-rent-card.jpeg";
    private static final String F_RENT_BLK_LGN    = "ActionCard/railroad-and-utility-rent-card.png";
    private static final String F_RENT_BRN_LBL    = "ActionCard/brown-and-light-blue-rent-card.jpg";

    private static final String F_MONEY_1  = "MoneyCard/$1M-money-card.jpg";
    private static final String F_MONEY_2  = "MoneyCard/$2M-money-card.jpg";
    private static final String F_MONEY_3  = "MoneyCard/$3M-money-card.jpg";
    private static final String F_MONEY_4  = "MoneyCard/$4M-money-card.jpg";
    private static final String F_MONEY_5  = "MoneyCard/$5M-money-card.jpg";
    private static final String F_MONEY_10 = "MoneyCard/$10M-money-card.jpg";

    // Property cards per color – order matches Deck.java insertion order
    private static final Map<PropertyType, List<String>> PROPERTY_IMAGES;
    static {
        Map<PropertyType, List<String>> m = new LinkedHashMap<>();
        m.put(PropertyType.BROWN, Arrays.asList(
                "PropertyCard/brown-Baltic_Avenue-property-card.png",
                "PropertyCard/brown-Mediterranean_Avenue-property-card.png"));
        m.put(PropertyType.LIGHTBLUE, Arrays.asList(
                "PropertyCard/light-blue-Connecticut_Avenue-property-card.png",
                "PropertyCard/light-blue-Oriental_Avenue-property-card.png",
                "PropertyCard/light-blue-Vermont_Avenue-property-card.png"));
        m.put(PropertyType.PURPLE, Arrays.asList(
                "PropertyCard/pink-ST.Charles_Place-property-card.png",
                "PropertyCard/pink-States_Avenue-property-card.png",
                "PropertyCard/pink-Virginia_Avenue-property-card.png"));
        m.put(PropertyType.ORANGE, Arrays.asList(
                "PropertyCard/orange-New_York_Avenue-property-card.png",
                "PropertyCard/orange-ST.James_Place-property-card.png",
                "PropertyCard/orange-Tennessee_Avenue-property-card.png"));
        m.put(PropertyType.RED, Arrays.asList(
                "PropertyCard/red-Illinois_Avenue-property-card.png",
                "PropertyCard/red-Indiana_Avenue-property-card.png",
                "PropertyCard/red-Kentucky_Avenue-property-card.png"));
        m.put(PropertyType.YELLOW, Arrays.asList(
                "PropertyCard/yellow-Atlantic_Avenue-property-card.png",
                "PropertyCard/yellow-Marvin_Gardens-property-card.png",
                "PropertyCard/yellow-Ventnor_Avenue-property-card.png"));
        m.put(PropertyType.GREEN, Arrays.asList(
                "PropertyCard/green-North_Carolina_Avenue-property-card.png",
                "PropertyCard/green-Pacific_Avenue-property-card.png",
                "PropertyCard/green-Pennsylvania_Avenue-property-card #3.png"));
        m.put(PropertyType.BLUE, Arrays.asList(
                "PropertyCard/dark-blue-Boardwalk-property-card.png",
                "PropertyCard/dark-blue-Park_Place-property-card.png"));
        m.put(PropertyType.BLACK, Arrays.asList(
                "PropertyCard/railroad-B.&O._Railroad-property-card.png",
                "PropertyCard/railroad-Pennsylvania_Railroad-property-card.png",
                "PropertyCard/railroad-Reading_Railroad-property-card.png",
                "PropertyCard/railroad-Short_Line-property-card.png"));
        m.put(PropertyType.LIGHTGREEN, Arrays.asList(
                "PropertyCard/utility-Electric_Company-property-card.png",
                "PropertyCard/utility-Water_Works-property-card.png"));
        m.put(PropertyType.RAINBOW, Collections.singletonList(
                "PropertyCard/multicolor-wildcard-card.png"));
        PROPERTY_IMAGES = Collections.unmodifiableMap(m);
    }

    // Wildcard cards: card name → file (from Deck.java wildcard section)
    private static final Map<String, String> WILDCARD_MAP;
    static {
        Map<String, String> w = new HashMap<>();
        w.put("Rainbow Wild",          "PropertyCard/multicolor-wildcard-card.png");
        w.put("Wild Red/Yellow",       "PropertyCard/red-and-yellow-wildcard-card.png");
        w.put("Wild Blue/Green",       "PropertyCard/dark-blue-and-green-wildcard-card.png");
        w.put("Wild Green/Black",      "PropertyCard/railraod-and-green-wildcard-card.png");
        w.put("Wild Brown/LightBlue",  "PropertyCard/light-blue-and-brown-wildcard-card.png");
        w.put("Wild LightBlue/Black",  "PropertyCard/railraod-and-light-blue-wildcard-card.png");
        w.put("Wild Purple/Orange",    "PropertyCard/orange-and-pink-wildcard-card.png");
        w.put("Wild Black/LightGreen", "PropertyCard/railroad-and-utility-wildcard-card.png");
        w.put("Wild LightBlue/Brown",  "PropertyCard/light-blue-and-brown-wildcard-card.png");
        WILDCARD_MAP = Collections.unmodifiableMap(w);
    }

    // Action card name → file (exact names from StandardCardFactory / Deck.java)
    private static final Map<String, String> ACTION_MAP;
    static {
        Map<String, String> a = new HashMap<>();
        a.put("Pass Go",              F_PASS_GO);
        a.put("It's My Birthday",     F_BIRTHDAY);
        a.put("Debt Collector",       F_DEBT_COLLECTOR);
        a.put("Sly Deal",             F_SLY_DEAL);
        a.put("Forced Deal",          F_FORCED_DEAL);
        a.put("Deal Breaker",         F_DEAL_BREAKER);
        a.put("Just Say No",          F_JUST_SAY_NO);
        a.put("Double The Rent",      F_DOUBLE_RENT);
        a.put("House",                F_HOUSE);
        a.put("Hotel",                F_HOTEL);
        a.put("Rent (Any Color)",     F_RENT_ANY);
        a.put("Rent Blue/Green",      F_RENT_BLUE_GREEN);
        a.put("Rent Red/Yellow",      F_RENT_RED_YELLOW);
        a.put("Rent Purple/Orange",   F_RENT_PURP_ORAN);
        a.put("Rent Black/LightGreen",F_RENT_BLK_LGN);
        a.put("Rent Brown/LightBlue", F_RENT_BRN_LBL);
        ACTION_MAP = Collections.unmodifiableMap(a);
    }

    // "COLOR|id" → resolved file path (avoids recomputing mod every frame)
    private final Map<String, String> propertyFileCache = new HashMap<>();
    // "filePath|WxH[|flip]" → JavaFX Image
    private final Map<String, Image> imageCache = new HashMap<>();

    public CardImageResolver() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – local game
    // ─────────────────────────────────────────────────────────────────────────

    /** Return the card image for a local-game {@link Card} object. */
    public Image getCardIcon(Card card, int width, int height) {
        if (card == null) return getFallbackIcon(width, height);

        String filePath = resolveFilePath(card);
        boolean flipped = card instanceof PropertyCard
                && ((PropertyCard) card).isDisplayFlipped();

        String key = filePath + "|" + width + "x" + height + (flipped ? "|flip" : "");
        return imageCache.computeIfAbsent(key, k -> {
            Image img = loadScaled(filePath, width, height, flipped);
            return img != null ? img : getFallbackIcon(width, height);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – network game
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Return the card image for a network
     * {@link com.monopolydeal.network.GameStateParser.CardInfo}.
     *
     * Reconstructs a temporary Card object from the info fields and delegates
     * to {@link #getCardIcon(Card, int, int)}, guaranteeing identical output
     * to local mode.
     */
    public Image getCardIconFromInfo(
            com.monopolydeal.network.GameStateParser.CardInfo info,
            int width, int height) {
        if (info == null) return getFallbackIcon(width, height);
        Card card = cardInfoToCard(info);
        if (card == null) return getFallbackIcon(width, height);
        return getCardIcon(card, width, height);
    }

    /** Alias kept for compatibility. */
    public Image getCardIconFX(String cardName, int width, int height) {
        String f = ACTION_MAP.get(cardName);
        if (f == null) f = WILDCARD_MAP.get(cardName);
        if (f != null) {
            Image img = loadScaled(f, width, height, false);
            if (img != null) return img;
        }
        return getFallbackIcon(width, height);
    }

    /** Alias kept for compatibility. */
    public Image getIconByName(String cardName, int width, int height) {
        return getCardIconFX(cardName, width, height);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fallback
    // ─────────────────────────────────────────────────────────────────────────

    public Image getFallbackIcon(int width, int height) {
        String key = "card_back.jpg|" + width + "x" + height;
        return imageCache.computeIfAbsent(key,
                k -> loadScaled("card_back.jpg", width, height, false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CardInfo → Card reconstruction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build a lightweight Card object from network CardInfo so that
     * getCardIcon(Card) can be reused without any duplication.
     */
    private Card cardInfoToCard(com.monopolydeal.network.GameStateParser.CardInfo info) {
        if (info.cardType == null) return null;

        switch (info.cardType) {

            case "MONEY":
                // MoneyCard(name, value, denomination)
                return new MoneyCard(info.name != null ? info.name : info.value + "M",
                        info.value, info.value);

            case "ACTION": {
                ActionType at = parseActionType(info.actionType, info.name);
                if (at == null) return null;
                // ActionCard(name, value, type, canDefend)
                ActionCard ac = new ActionCard(
                        info.name != null ? info.name : at.name(),
                        info.value, at, false);
                return ac;
            }

            case "PROPERTY": {
                PropertyType color = parseColor(info.color);
                if (color == null) color = PropertyType.RAINBOW;
                // PropertyCard(name, value, color, isWild)
                PropertyCard pc = new PropertyCard(
                        info.name != null ? info.name : color.name(),
                        info.value, color,
                        info.isWild || info.needsChoice);
                return pc;
            }

            default:
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File path resolution (used by getCardIcon)
    // ─────────────────────────────────────────────────────────────────────────

    private String resolveFilePath(Card card) {
        if (card instanceof MoneyCard) {
            return moneyFile(((MoneyCard) card).getDenomination());
        }
        if (card instanceof ActionCard) {
            String f = ACTION_MAP.get(card.getName());
            return f != null ? f : "card_back.jpg";
        }
        if (card instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) card;
            if (pc.isWild()) {
                String f = WILDCARD_MAP.get(pc.getName());
                return f != null ? f : "PropertyCard/multicolor-wildcard-card.png";
            }
            return resolvePropertyFile(pc.getColor(), card.getId());
        }
        return "card_back.jpg";
    }

    private String resolvePropertyFile(PropertyType color, int cardId) {
        // Composite key: color + id, so different-colored cards never collide
        String cacheKey = color.name() + "|" + cardId;
        return propertyFileCache.computeIfAbsent(cacheKey, k -> {
            List<String> files = PROPERTY_IMAGES.getOrDefault(color, Collections.emptyList());
            if (files.isEmpty()) return "card_back.jpg";
            return files.get(Math.floorMod(cardId - 1, files.size()));
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String moneyFile(int denomination) {
        switch (denomination) {
            case 1:  return F_MONEY_1;
            case 2:  return F_MONEY_2;
            case 3:  return F_MONEY_3;
            case 4:  return F_MONEY_4;
            case 5:  return F_MONEY_5;
            case 10: return F_MONEY_10;
            default: return "card_back.jpg";
        }
    }

    private static ActionType parseActionType(String typeName, String cardName) {
        // Try the enum name first (most reliable)
        if (typeName != null) {
            try { return ActionType.valueOf(typeName); }
            catch (IllegalArgumentException ignored) {}
        }
        // Fall back to card display name
        if (cardName == null) return null;
        if (ACTION_MAP.containsKey(cardName)) {
            // Derive ActionType from the known mapping
            switch (cardName) {
                case "Pass Go":            return ActionType.GO_PASS;
                case "It's My Birthday":   return ActionType.BIRTHDAY;
                case "Debt Collector":     return ActionType.DEBT_DEAL;
                case "Sly Deal":           return ActionType.SLY_DEAL;
                case "Forced Deal":        return ActionType.FORCED_DEAL;
                case "Deal Breaker":       return ActionType.DEAL_BREAKER;
                case "Just Say No":        return ActionType.JUST_SAY_NO;
                case "Double The Rent":    return ActionType.DOUBLE_RENT;
                case "House":              return ActionType.HOUSE;
                case "Hotel":              return ActionType.HOTEL;
                default:                   return ActionType.DOUBLE_RENT; // rent cards reuse DOUBLE_RENT
            }
        }
            return null;
        }

    private static PropertyType parseColor(String color) {
        if (color == null || color.isEmpty()) return null;
        try { return PropertyType.valueOf(color.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image loading
    // ─────────────────────────────────────────────────────────────────────────

    private Image loadScaled(String relativePath, int width, int height, boolean flip) {
        if (relativePath == null || width <= 0 || height <= 0) return null;
        BufferedImage raw = loadRaw(relativePath);
        if (raw == null) return null;
        BufferedImage scaled = ImageScaleUtil.scaleExact(raw, width, height);
        if (flip) scaled = ImageScaleUtil.rotate180(scaled);
        return ImageScaleUtil.toFXImage(scaled);
    }

    private BufferedImage loadRaw(String relativePath) {
        // 1. Classpath resource (JAR / Maven exec)
        String res = LIBRARY_PATH + "/" + relativePath.replace('\\', '/');
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(res)) {
            if (in != null) return ImageIO.read(in);
        } catch (IOException ignored) {}

        // 2. src/main/resources on disk (IntelliJ run-from-source)
        Path local = Paths.get("src", "main", "resources", LIBRARY_PATH)
                .resolve(relativePath.replace('/', java.io.File.separatorChar));
        if (Files.exists(local)) {
            try (InputStream in = new FileInputStream(local.toFile())) {
                return ImageIO.read(in);
            } catch (IOException ignored) {}
        }

        // 3. Project-root Card_Library
        Path root = Paths.get(LIBRARY_PATH)
                .resolve(relativePath.replace('/', java.io.File.separatorChar));
        if (Files.exists(root)) {
            try (InputStream in = new FileInputStream(root.toFile())) {
                return ImageIO.read(in);
            } catch (IOException ignored) {}
        }

        return null;
    }
}