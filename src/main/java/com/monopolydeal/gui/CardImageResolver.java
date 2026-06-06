package com.monopolydeal.gui;

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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves card images from Card_Library on the classpath.
 * Returns {@link javafx.scene.image.Image} objects; caches by (path × size × flip).
 */
public class CardImageResolver {

    private static final String LIBRARY_PATH = "Card_Library";
    private static final String FALLBACK_FILE = "card_back.jpg";

    private final List<String> allFiles;
    private final Map<String, String> fileByBaseName;
    private final Map<String, String> normalizedNameToFile;
    private final Map<PropertyType, List<String>> propertyImagesByColor;
    private final Map<Integer, String> propertyImageByCardId;
    private final Map<String, Image> imageCache;

    public CardImageResolver() {
        this.allFiles            = scanLibraryFiles();
        this.fileByBaseName      = new HashMap<>();
        this.normalizedNameToFile = new HashMap<>();
        for (String rel : allFiles) {
            String base = baseName(rel);
            fileByBaseName.putIfAbsent(base, rel);
            normalizedNameToFile.putIfAbsent(normalize(removeExtension(base)), rel);
        }
        this.propertyImagesByColor = buildPropertyGroups(allFiles);
        this.propertyImageByCardId = new HashMap<>();
        this.imageCache            = new HashMap<>();
    }

    /** Return a scaled JavaFX Image for the given card at (width × height). */
    public Image getCardIcon(Card card, int width, int height) {
        if (card == null) return getFallbackIcon(width, height);

        String fileName = resolveFileName(card);
        boolean flipped = card instanceof PropertyCard && ((PropertyCard) card).isDisplayFlipped();

        String cacheSuffix = "";
        if (card instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) card;
            cacheSuffix = "|id" + card.getId() + "|" + pc.getColor()
                    + (pc.isDisplayFlipped()  ? "|flip"   : "")
                    + (pc.isColorCommitted()  ? "|locked" : "");
        }

        Image img = loadImage(fileName, width, height, flipped, cacheSuffix);
        return img != null ? img : getFallbackIcon(width, height);
    }

    public Image getFallbackIcon(int width, int height) {
        Image img = loadImage(FALLBACK_FILE, width, height, false, "");
        return img != null ? img : new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resolution chain
    // ─────────────────────────────────────────────────────────────────────────

    private String resolveFileName(Card card) {
        String exact = null;
        if (card instanceof MoneyCard)    exact = resolveMoneyFile((MoneyCard)    card);
        else if (card instanceof PropertyCard) exact = resolvePropertyFile((PropertyCard) card);
        else if (card instanceof ActionCard)   exact = resolveActionFile((ActionCard)    card);

        String located = locateFile(exact);
        if (located != null) return located;

        String normHit = exact == null ? null
                : normalizedNameToFile.get(normalize(removeExtension(exact)));
        if (normHit != null) return normHit;

        String fuzzy = findBestFuzzyMatch(buildFuzzyQuery(card, exact));
        if (fuzzy != null) return fuzzy;

        return locateFile(FALLBACK_FILE);
    }

    private String locateFile(String fileName) {
        if (fileName == null) return null;
        if (allFiles.contains(fileName)) return fileName;
        String byBase = fileByBaseName.get(fileName);
        if (byBase != null) return byBase;
        for (String path : allFiles) {
            if (path.endsWith("/" + fileName) || path.equals(fileName)) return path;
        }
        return null;
    }

    private String resolveMoneyFile(MoneyCard card) {
        return "$" + card.getDenomination() + "M-money-card.jpg";
    }

    private String resolveActionFile(ActionCard card) {
        String name = card.getName();
        if ("Pass Go".equals(name))                  return "pass-go-action-card.jpeg";
        if ("It's My Birthday".equals(name))         return "it's-my-birthday-action-card.jpeg";
        if ("Debt Collector".equals(name))           return "debt-collector-action-card.png";
        if ("Sly Deal".equals(name))                 return "sly-deal-action-card.png";
        if ("Forced Deal".equals(name))              return "force-deal-action-card.png";
        if ("Deal Breaker".equals(name))             return "deal-breaker-action-card.png";
        if ("Just Say No".equals(name))              return "just-say-no-action-card.jpeg";
        if ("Double The Rent".equals(name))          return "double-the-rent-action-card.png";
        if ("House".equals(name))                    return "house-action-card.jpeg";
        if ("Hotel".equals(name))                    return "hotel-action-card.png";
        if ("Rent (Any Color)".equals(name))         return "all-color-wild-rent-card.jpg";
        if ("Rent Blue/Green".equals(name))          return "blue-and-green-rent-card.jpg";
        if ("Rent Red/Yellow".equals(name))          return "red-and-yellow-rent-card.png";
        if ("Rent Purple/Orange".equals(name))       return "orange-and-pink-rent-card.jpeg";
        if ("Rent Black/LightGreen".equals(name))    return "railroad-and-utility-rent-card.png";
        if ("Rent Brown/LightBlue".equals(name))     return "brown-and-light-blue-rent-card.jpg";
        return null;
    }

    private String resolvePropertyFile(PropertyCard card) {
        if (card.isWild()) {
            String name = card.getName();
            if ("Rainbow Wild".equals(name))           return "multicolor-wildcard-card.png";
            if ("Wild Red/Yellow".equals(name))        return "red-and-yellow-wildcard-card.png";
            if ("Wild Blue/Green".equals(name))        return "dark-blue-and-green-wildcard-card.png";
            if ("Wild Green/Black".equals(name))       return "railraod-and-green-wildcard-card.png";
            if ("Wild Brown/LightBlue".equals(name))   return "light-blue-and-brown-wildcard-card.png";
            if ("Wild LightBlue/Black".equals(name))   return "railraod-and-light-blue-wildcard-card.png";
            if ("Wild Purple/Orange".equals(name))     return "orange-and-pink-wildcard-card.png";
            if ("Wild Black/LightGreen".equals(name))  return "railroad-and-utility-wildcard-card.png";
            if ("Wild LightBlue/Brown".equals(name))   return "light-blue-and-brown-wildcard-card.png";
        }

        List<String> files = propertyImagesByColor.getOrDefault(card.getColor(), Collections.emptyList());
        if (files.isEmpty()) return null;
        return propertyImageByCardId.computeIfAbsent(card.getId(), id -> {
            int index = Math.floorMod(id - 1, files.size());
            return files.get(index);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image loading + caching
    // ─────────────────────────────────────────────────────────────────────────

    private Image loadImage(String relativePath, int width, int height,
                            boolean flipped, String cacheSuffix) {
        if (relativePath == null || width <= 0 || height <= 0) return null;

        String key = relativePath + "|" + width + "x" + height
                + (flipped ? "|flip" : "") + cacheSuffix;
        if (imageCache.containsKey(key)) return imageCache.get(key);

        BufferedImage raw = loadRawImage(relativePath);
        if (raw == null) return null;

        BufferedImage scaled = ImageScaleUtil.scaleExact(raw, width, height);
        if (flipped) scaled = ImageScaleUtil.rotate180(scaled);

        Image fxImage = ImageScaleUtil.toFXImage(scaled);
        if (fxImage != null) imageCache.put(key, fxImage);
        return fxImage;
    }

    private BufferedImage loadRawImage(String relativePath) {
        String resourcePath = LIBRARY_PATH + "/" + relativePath.replace('\\', '/');
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) return ImageIO.read(in);
        } catch (IOException ignored) {}

        Path localPath = Paths.get("src", "main", "resources", LIBRARY_PATH)
                .resolve(relativePath.replace('/', java.io.File.separatorChar));
        if (Files.exists(localPath)) {
            try (InputStream in = new FileInputStream(localPath.toFile())) {
                return ImageIO.read(in);
            } catch (IOException ignored) {}
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File scanning + fuzzy matching
    // ─────────────────────────────────────────────────────────────────────────

    private Map<PropertyType, List<String>> buildPropertyGroups(List<String> fileNames) {
        Map<PropertyType, List<String>> map = new LinkedHashMap<>();
        map.put(PropertyType.BROWN,      collectByPrefix(fileNames, "brown-"));
        map.put(PropertyType.LIGHTBLUE,  collectByPrefix(fileNames, "light-blue-"));
        map.put(PropertyType.PURPLE,     collectByPrefix(fileNames, "pink-"));
        map.put(PropertyType.ORANGE,     collectByPrefix(fileNames, "orange-"));
        map.put(PropertyType.RED,        collectByPrefix(fileNames, "red-"));
        map.put(PropertyType.YELLOW,     collectByPrefix(fileNames, "yellow-"));
        map.put(PropertyType.GREEN,      collectByPrefix(fileNames, "green-"));
        map.put(PropertyType.BLUE,       collectByPrefix(fileNames, "dark-blue-"));
        map.put(PropertyType.BLACK,      collectByPrefix(fileNames, "railroad-"));
        map.put(PropertyType.LIGHTGREEN, collectByPrefix(fileNames, "utility-"));
        return map;
    }

    private List<String> collectByPrefix(List<String> fileNames, String prefix) {
        List<String> out = new ArrayList<>();
        for (String file : fileNames) {
            String lower = baseName(file).toLowerCase(Locale.ROOT);
            if (lower.startsWith(prefix) && lower.contains("property-card")) out.add(file);
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    private String findBestFuzzyMatch(String query) {
        if (query == null) return null;
        String nq = normalize(query);
        if (nq.isEmpty()) return null;

        int bestScore = -1;
        String bestFile = null;
        for (String file : allFiles) {
            String candidate = normalize(removeExtension(baseName(file)));
            int score = score(nq, candidate);
            if (score > bestScore
                    || (score == bestScore && bestFile != null && file.compareTo(bestFile) < 0)) {
                bestScore = score;
                bestFile  = file;
            }
        }
        return bestScore > 0 ? bestFile : null;
    }

    private int score(String query, String candidate) {
        if (query.equals(candidate)) return 200;
        int s = 0;
        if (candidate.contains(query) || query.contains(candidate)) s += 80;
        for (String token : query.split(" ")) {
            if (token.length() > 1 && candidate.contains(token)) s += 12;
        }
        return s;
    }

    private String buildFuzzyQuery(Card card, String exactGuess) {
        StringBuilder sb = new StringBuilder();
        if (card.getName() != null) sb.append(card.getName()).append(' ');
        if (exactGuess != null) sb.append(exactGuess).append(' ');
        if (card instanceof PropertyCard)
            sb.append(((PropertyCard) card).getColor()).append(" property card");
        else if (card instanceof ActionCard) sb.append("action card");
        else if (card instanceof MoneyCard)  sb.append("money card");
        return sb.toString();
    }

    private List<String> scanLibraryFiles() {
        List<String> files = new ArrayList<>();
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(LIBRARY_PATH);
            if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
                Path dir = Paths.get(url.toURI());
                if (Files.isDirectory(dir)) collectImageFiles(dir, dir, files);
            }
        } catch (IOException | URISyntaxException ignored) {}

        if (files.isEmpty()) {
            Path localDir = Paths.get("src", "main", "resources", LIBRARY_PATH);
            if (Files.isDirectory(localDir)) {
                try { collectImageFiles(localDir, localDir, files); } catch (IOException ignored) {}
            }
        }

        // Also scan project-root Card_Library (IntelliJ / run-from-source layout)
        if (files.isEmpty()) {
            Path rootDir = Paths.get(LIBRARY_PATH);
            if (Files.isDirectory(rootDir)) {
                try { collectImageFiles(rootDir, rootDir, files); } catch (IOException ignored) {}
            }
        }

        files.sort(Comparator.naturalOrder());
        return files;
    }

    private void collectImageFiles(Path root, Path current, List<String> files) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.list(current)) {
            List<Path> entries = new ArrayList<>();
            stream.forEach(entries::add);
            entries.sort(Comparator.naturalOrder());
            for (Path path : entries) {
                if (Files.isDirectory(path)) {
                    collectImageFiles(root, path, files);
                } else if (isImageFile(path)) {
                    String rel = root.relativize(path).toString().replace('\\', '/');
                    files.add(rel);
                }
            }
        }
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // String helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String baseName(String rel) {
        if (rel == null) return "";
        int slash = Math.max(rel.lastIndexOf('/'), rel.lastIndexOf('\\'));
        return slash >= 0 ? rel.substring(slash + 1) : rel;
    }

    private String removeExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(0, dot) : fileName;
    }

    private String normalize(String text) {
        if (text == null) return "";
        String s = text.toLowerCase(Locale.ROOT);
        s = s.replace("forced",     "force");
        s = s.replace("purple",     "pink");
        s = s.replace("lightgreen", "utility");
        s = s.replace("light green","utility");
        s = s.replace("black",      "railroad");
        s = s.replace("railraod",   "railroad");
        s = s.replace("any color",  "all color");
        s = s.replace("wild ",      "wildcard ");
        s = s.replaceAll("[^a-z0-9]+", " ").trim();
        s = s.replaceAll("\\s+", " ");
        return s;
    }
}
