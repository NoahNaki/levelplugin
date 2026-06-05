package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentInstaller;
import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Installs and verifies the optional Nexo external pack fragment for the future dialogue HUD. */
public final class DialogueHudResourcePackManager {
    private static final String BUNDLED_FRAGMENT = "resourcepack/dialogue_hud";
    private static final String EXTERNAL_PACK_FOLDER = "levelplugin-dialogue-hud";
    public static final String DIALOGUE_FONT_JSON = "assets/levelplugin_dialogue/font/dialogue.json";
    public static final String OFFSET_FONT_JSON = "assets/levelplugin_dialogue/font/offset_chars.json";
    public static final String BACKGROUND_TEXTURE = "assets/levelplugin_dialogue/textures/dialogue/dialogue_background.png";
    public static final String LINE_1_FONT_JSON = "assets/levelplugin_dialogue/font/line_1.json";
    public static final String LINE_2_FONT_JSON = "assets/levelplugin_dialogue/font/line_2.json";
    public static final String LINE_3_FONT_JSON = "assets/levelplugin_dialogue/font/line_3.json";
    public static final String ANSWER_1_FONT_JSON = "assets/levelplugin_dialogue/font/answer_1.json";
    public static final String ANSWER_2_FONT_JSON = "assets/levelplugin_dialogue/font/answer_2.json";
    public static final String BACKGROUND_PROVIDER_FILE = "levelplugin_dialogue:dialogue/dialogue_background.png";
    private static final String BACKGROUND_GLYPH = Character.toString(DialogueHudGlyphs.DIALOGUE_BACKGROUND);
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[^{}]*}", Pattern.DOTALL);
    private static final Pattern ADVANCES_PATTERN = Pattern.compile("\"advances\"\\s*:\\s*\\{(.*?)}", Pattern.DOTALL);
    private static final Pattern ADVANCE_ENTRY_PATTERN = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private static final List<String> DEFAULT_REQUIRED_FILES = List.of(
            "pack.mcmeta",
            DIALOGUE_FONT_JSON,
            OFFSET_FONT_JSON,
            BACKGROUND_TEXTURE,
            LINE_1_FONT_JSON,
            LINE_2_FONT_JSON,
            LINE_3_FONT_JSON,
            ANSWER_1_FONT_JSON,
            ANSWER_2_FONT_JSON
    );
    private static DialogueHudResourcePackManager instance;

    private final Main plugin;
    private final ResourcePackFragmentInstaller installer;
    private final DialogueHudPackStatusListener packStatusListener;

    private DialogueHudResourcePackManager(Main plugin) {
        this.plugin = plugin;
        this.installer = new ResourcePackFragmentInstaller(plugin, "dialogue HUD", BUNDLED_FRAGMENT,
                EXTERNAL_PACK_FOLDER, configuredRequiredFiles(plugin));
        this.packStatusListener = new DialogueHudPackStatusListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(packStatusListener, plugin);
    }

    public static DialogueHudResourcePackManager initialize(Main plugin) {
        DialogueHudResourcePackManager manager = new DialogueHudResourcePackManager(plugin);
        instance = manager;
        if (manager.resourcePackEnabled()) {
            manager.installer.installBundledFragment();
            manager.logAvailability();
        }
        manager.configureDialogueOffsetGlyphs();
        return manager;
    }

    public static DialogueHudResourcePackManager getInstance() { return instance; }

    public ResourcePackFragmentStatus status() {
        return installer.status(resourcePackEnabled(), fallbackChatRendererEnabled());
    }

    public DialogueHudPackStatusListener packStatusListener() {
        return packStatusListener;
    }

    public boolean rendererEnabled() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.enabled", true);
    }

    public String rendererMode() {
        String mode = plugin.getConfig().getString("dialogue-hud.renderer.mode", "actionbar");
        return mode == null || mode.isBlank() ? "actionbar" : mode.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean actionBarMode() {
        return !"chat".equals(rendererMode());
    }

    public boolean useResourcePackGlyphs() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.use-resource-pack-glyphs", true);
    }

    public boolean requireClientPackLoaded() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.require-client-pack-loaded", true);
    }

    public boolean debugForceGlyphs() {
        return plugin.getConfig().getBoolean("dialogue-hud.renderer.debug-force-glyphs", false);
    }

    public boolean debugLogging() {
        return plugin.getConfig().getBoolean("dialogue-hud.debug.log-pack-status", true);
    }

    public int backgroundOffset() {
        return plugin.getConfig().getInt("dialogue-hud.renderer.background-offset", 0);
    }

    public int textOffsetAfterBackground() {
        return plugin.getConfig().getInt("dialogue-hud.renderer.text-offset-after-background", 8);
    }

    public String configuredNegativeOffsetGlyph() {
        return plugin.getConfig().getString("dialogue-hud.layout.offsets.negative-char",
                DialogueHudGlyphs.unicode(DialogueHudGlyphs.DEFAULT_NEGATIVE_OFFSET));
    }

    public String configuredPositiveOffsetGlyph() {
        return plugin.getConfig().getString("dialogue-hud.layout.offsets.positive-char",
                DialogueHudGlyphs.unicode(DialogueHudGlyphs.DEFAULT_POSITIVE_OFFSET));
    }

    public boolean canRenderGlyphUi(org.bukkit.entity.Player player) {
        return rendererEnabled()
                && actionBarMode()
                && serverGlyphFilesReady()
                && useResourcePackGlyphs()
                && (debugForceGlyphs() || !requireClientPackLoaded() || packStatusListener.hasLoadedPack(player));
    }

    public boolean playerCanUseGlyphs(org.bukkit.entity.Player player) {
        return canRenderGlyphUi(player);
    }

    public boolean serverGlyphFilesReady() {
        return status().glyphUiEnabled();
    }

    public String glyphDebugReason(org.bukkit.entity.Player player) {
        if (!rendererEnabled()) return "renderer disabled in config";
        if (!actionBarMode()) return "renderer mode is chat";
        if (!useResourcePackGlyphs()) return "renderer glyphs disabled in config";
        if (!serverGlyphFilesReady()) return "server-side dialogue HUD pack files are incomplete or Nexo is unavailable";
        if (debugForceGlyphs()) return "debug-force-glyphs enabled";
        if (requireClientPackLoaded() && !packStatusListener.hasLoadedPack(player)) {
            return "player has not reported SUCCESSFULLY_LOADED for the resource pack";
        }
        return requireClientPackLoaded() ? "glyphs enabled" : "glyphs enabled without client load requirement";
    }

    public boolean fallbackChatRendererEnabled() {
        return plugin.getConfig().getBoolean("dialogue-hud.resource-pack.fallback-chat-renderer", true);
    }

    public Path installedPackPath() {
        return installer.installedPackPath();
    }

    public Path dialogueFontJsonPath() {
        return installedPackPath().resolve(DIALOGUE_FONT_JSON);
    }

    public Path dialogueBackgroundPath() {
        return installedPackPath().resolve(BACKGROUND_TEXTURE);
    }

    public Path offsetFontJsonPath() {
        return installedPackPath().resolve(OFFSET_FONT_JSON);
    }

    public OffsetGlyphDebug offsetGlyphDebug() {
        Optional<OffsetGlyphPair> detected = detectOffsetGlyphs();
        return new OffsetGlyphDebug(offsetFontJsonPath(), Files.isRegularFile(offsetFontJsonPath()),
                DialogueHudGlyphs.negativeOffsetGlyph(), DialogueHudGlyphs.positiveOffsetGlyph(),
                DialogueHudGlyphs.offsetSource(), detected.orElse(null));
    }

    private void configureDialogueOffsetGlyphs() {
        Optional<OffsetGlyphPair> detected = detectOffsetGlyphs();
        if (detected.isPresent()) {
            OffsetGlyphPair pair = detected.get();
            DialogueHudGlyphs.configureOffsetGlyphs(pair.negative(), pair.positive(), "detected from offset_chars.json advances");
            return;
        }

        char negative = parseConfiguredGlyph(configuredNegativeOffsetGlyph(), DialogueHudGlyphs.DEFAULT_NEGATIVE_OFFSET);
        char positive = parseConfiguredGlyph(configuredPositiveOffsetGlyph(), DialogueHudGlyphs.DEFAULT_POSITIVE_OFFSET);
        DialogueHudGlyphs.configureOffsetGlyphs(negative, positive, "config/default dialogue-hud.layout.offsets");
    }

    private Optional<OffsetGlyphPair> detectOffsetGlyphs() {
        Path path = offsetFontJsonPath();
        if (!Files.isRegularFile(path)) return Optional.empty();
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Matcher advancesMatcher = ADVANCES_PATTERN.matcher(json);
            while (advancesMatcher.find()) {
                Matcher entryMatcher = ADVANCE_ENTRY_PATTERN.matcher(advancesMatcher.group(1));
                Character negative = null;
                Character positive = null;
                while (entryMatcher.find()) {
                    String key = decodeJsonString(entryMatcher.group(1));
                    if (key == null || key.isEmpty()) continue;
                    double advance = Double.parseDouble(entryMatcher.group(2));
                    if (advance < 0 && negative == null) negative = key.charAt(0);
                    if (advance > 0 && positive == null) positive = key.charAt(0);
                    if (negative != null && positive != null) {
                        return Optional.of(new OffsetGlyphPair(negative, positive));
                    }
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public BackgroundGlyphDebug backgroundGlyphDebug() {
        Path texturePath = dialogueBackgroundPath();
        Path fontPath = dialogueFontJsonPath();
        List<String> warnings = new ArrayList<>();
        boolean textureExists = Files.isRegularFile(texturePath);
        Integer width = null;
        Integer height = null;
        Boolean fullyTransparent = null;

        if (!textureExists) {
            warnings.add("dialogue_background.png is missing");
        } else {
            try {
                BufferedImage image = ImageIO.read(texturePath.toFile());
                if (image == null) {
                    warnings.add("dialogue_background.png could not be decoded as an image");
                } else {
                    width = image.getWidth();
                    height = image.getHeight();
                    fullyTransparent = isFullyTransparent(image);
                    if (width < 32 || height < 16) {
                        warnings.add("dialogue_background.png is smaller than 32x16");
                    }
                    if (fullyTransparent) {
                        warnings.add("dialogue_background.png is fully transparent");
                    }
                }
            } catch (IOException exception) {
                warnings.add("dialogue_background.png could not be read: " + exception.getMessage());
            }
        }

        boolean fontExists = Files.isRegularFile(fontPath);
        FontProviderDebug provider = null;
        if (!fontExists) {
            warnings.add("dialogue.json is missing");
        } else {
            try {
                provider = findBackgroundProvider(Files.readString(fontPath, StandardCharsets.UTF_8));
                if (provider == null) {
                    warnings.add("dialogue.json has no provider containing \\uE100");
                } else {
                    if (provider.height() != null && provider.height() < 16) {
                        warnings.add("dialogue background provider height is smaller than 16");
                    }
                    if (provider.ascent() != null && provider.height() != null && provider.ascent() > provider.height()) {
                        warnings.add("dialogue background provider ascent is greater than height");
                    }
                    if (provider.file() == null || !BACKGROUND_PROVIDER_FILE.equals(provider.file())) {
                        warnings.add("dialogue background provider file should be " + BACKGROUND_PROVIDER_FILE);
                    }
                }
            } catch (IOException exception) {
                warnings.add("dialogue.json could not be read: " + exception.getMessage());
            }
        }

        return new BackgroundGlyphDebug(texturePath, textureExists, width, height, fullyTransparent,
                fontPath, fontExists, provider, List.copyOf(warnings));
    }

    private FontProviderDebug findBackgroundProvider(String json) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(json);
        while (matcher.find()) {
            String object = matcher.group();
            String chars = extractJsonArray(object, "chars");
            if (chars != null && containsBackgroundGlyph(chars)) {
                return new FontProviderDebug(fontPathDisplay(dialogueFontJsonPath()),
                        extractJsonString(object, "file"),
                        extractJsonInt(object, "ascent"),
                        extractJsonInt(object, "height"),
                        chars.replace('\n', ' ').replace('\r', ' ').trim());
            }
        }
        return null;
    }

    private static boolean containsBackgroundGlyph(String chars) {
        String lower = chars.toLowerCase(java.util.Locale.ROOT);
        return chars.contains(BACKGROUND_GLYPH) || lower.contains("\\ue100");
    }

    private static char parseConfiguredGlyph(String configured, char fallback) {
        if (configured == null || configured.isBlank()) return fallback;
        String value = configured.trim();
        if (value.startsWith("\\u") || value.startsWith("\\U")) {
            try {
                return (char) Integer.parseInt(value.substring(2), 16);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return value.charAt(0);
    }

    private static String decodeJsonString(String value) {
        if (value == null) return null;
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\' || i + 1 >= value.length()) {
                output.append(current);
                continue;
            }
            char escaped = value.charAt(++i);
            switch (escaped) {
                case 'u', 'U' -> {
                    if (i + 4 < value.length()) {
                        try {
                            output.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException exception) {
                            output.append(escaped);
                        }
                    } else {
                        output.append(escaped);
                    }
                }
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                case 't' -> output.append('\t');
                case 'b' -> output.append('\b');
                case 'f' -> output.append('\f');
                default -> output.append(escaped);
            }
        }
        return output.toString();
    }

    private static String extractJsonString(String object, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(object);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Integer extractJsonInt(String object, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(object);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private static String extractJsonArray(String object, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(object);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean isFullyTransparent(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) return false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) != 0) return false;
            }
        }
        return true;
    }

    private static String fontPathDisplay(Path path) {
        return path == null ? "unknown" : path.toString();
    }

    public record BackgroundGlyphDebug(
            Path texturePath,
            boolean textureExists,
            Integer width,
            Integer height,
            Boolean fullyTransparent,
            Path fontPath,
            boolean fontExists,
            FontProviderDebug provider,
            List<String> warnings
    ) {
        public String size() {
            return width == null || height == null ? "unknown" : width + "x" + height;
        }
    }

    public record FontProviderDebug(
            String providerFilePath,
            String file,
            Integer ascent,
            Integer height,
            String chars
    ) { }

    public record OffsetGlyphDebug(
            Path fontPath,
            boolean fontExists,
            char activeNegative,
            char activePositive,
            String source,
            OffsetGlyphPair detectedPair
    ) { }

    public record OffsetGlyphPair(char negative, char positive) { }

    private boolean resourcePackEnabled() {
        return plugin.getConfig().getBoolean("dialogue-hud.resource-pack.enabled", true);
    }

    private static List<String> configuredRequiredFiles(Main plugin) {
        LinkedHashSet<String> files = new LinkedHashSet<>(DEFAULT_REQUIRED_FILES);
        for (String configuredFile : plugin.getConfig().getStringList("dialogue-hud.resource-pack.required-files")) {
            if (configuredFile != null && !configuredFile.isBlank()) {
                files.add(configuredFile.replace('\\', '/').replaceFirst("^/+", ""));
            }
        }
        return List.copyOf(files);
    }

    private void logAvailability() {
        ResourcePackFragmentStatus status = status();
        if (!status.bundledResourceExists()) {
            plugin.getLogger().warning("Bundled dialogue HUD resource-pack folder '" + BUNDLED_FRAGMENT
                    + "' is missing; dialogue glyph HUD will stay unavailable until assets are added.");
        }
        if (!status.glyphUiEnabled()) {
            plugin.getLogger().warning("Dialogue HUD resource-pack glyph UI is unavailable. Action-bar rendering will use plain text.");
            status.requiredFiles().forEach((file, exists) -> {
                if (!exists) {
                    plugin.getLogger().warning("Missing dialogue HUD resource-pack file in " + EXTERNAL_PACK_FOLDER
                            + ": " + file);
                }
            });
        } else {
            plugin.getLogger().info("Dialogue HUD resource-pack files verified at " + status.installedPackPath()
                    + ". Glyphs still require each player to successfully load the resource pack.");
        }
    }
}
