package me.nakilex.levelplugin.luxbridge.resource;

import me.nakilex.levelplugin.luxbridge.util.LuxBridgeElementUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LuxBridgeResourceManager {
    private static final String RESOURCE_ROOT = "luxbridge";
    private static final String NEXO_ASSET_PATH = "plugins/Nexo/pack/external_packs/levelplugin-dialogue-hud/assets/levelplugin_dialogue";
    private static final Map<String, Integer> FALLBACK_WIDTHS = Map.ofEntries(
            Map.entry("dialogue-background", 209), Map.entry("answer-background", 134), Map.entry("character-background", 32),
            Map.entry("hand", 14), Map.entry("fog", 256), Map.entry("name-start", 3), Map.entry("name-mid", 2), Map.entry("name-end", 3),
            Map.entry("kingdom-hand", 14), Map.entry("kingdom-dialogue", 209), Map.entry("kingdom-answer", 134), Map.entry("kingdom-character", 32),
            Map.entry("kingdom-name-start", 3), Map.entry("kingdom-name-mid", 2), Map.entry("kingdom-name-end", 3)
    );
    private static final Map<String, Integer> FALLBACK_HEIGHTS = Map.ofEntries(
            Map.entry("dialogue-background", 64), Map.entry("answer-background", 64), Map.entry("character-background", 64),
            Map.entry("hand", 16), Map.entry("fog", 256), Map.entry("name-start", 16), Map.entry("name-mid", 16), Map.entry("name-end", 16),
            Map.entry("kingdom-hand", 16), Map.entry("kingdom-dialogue", 64), Map.entry("kingdom-answer", 64), Map.entry("kingdom-character", 64),
            Map.entry("kingdom-name-start", 16), Map.entry("kingdom-name-mid", 16), Map.entry("kingdom-name-end", 16)
    );

    private final JavaPlugin plugin;
    private final Map<String, LuxImageDefinition> images = new LinkedHashMap<>();
    private LuxLineDefinitions lines = new LuxLineDefinitions(40, 5, 25, 9, 0, 3, 75, 9);

    public LuxBridgeResourceManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadAndGenerate() {
        loadDefinitions();
        generateFontJsons();
    }

    public Map<String, LuxImageDefinition> images() {
        return Collections.unmodifiableMap(images);
    }

    public LuxImageDefinition image(String id) {
        return images.get(id);
    }

    public LuxLineDefinitions lines() {
        return lines;
    }

    private void loadDefinitions() {
        images.clear();
        YamlConfiguration imageConfig = loadBundledYaml(RESOURCE_ROOT + "/Pack/Images/images.yml");
        ConfigurationSection imageSection = imageConfig.getConfigurationSection("Images");
        int codepoint = 0xE100;
        if (imageSection != null) {
            for (String id : imageSection.getKeys(false)) {
                ConfigurationSection section = imageSection.getConfigurationSection(id);
                if (section == null) continue;
                String file = section.getString("file", id + ".png");
                int[] size = detectImageSize(id, file);
                images.put(id, new LuxImageDefinition(
                        id,
                        file,
                        section.getBoolean("is-arrow", false),
                        Math.max(1, section.getInt("reduction-ratio", 1)),
                        section.getInt("ascent", 0),
                        size[0],
                        size[1],
                        new String(Character.toChars(codepoint++))
                ));
            }
        }

        YamlConfiguration lineConfig = loadBundledYaml(RESOURCE_ROOT + "/Pack/Lines/lines.yml");
        lines = new LuxLineDefinitions(
                lineConfig.getInt("Character-Name.ascent", 40),
                lineConfig.getInt("Dialogue-Lines.count", 5),
                lineConfig.getInt("Dialogue-Lines.ascent", 25),
                lineConfig.getInt("Dialogue-Lines.space", 9),
                lineConfig.getInt("Information-Line.ascent", 0),
                lineConfig.getInt("Answer-Lines.count", 3),
                lineConfig.getInt("Answer-Lines.ascent", 75),
                lineConfig.getInt("Answer-Lines.space", 9)
        );
    }

    private YamlConfiguration loadBundledYaml(String path) {
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(path), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            plugin.getLogger().warning("[LuxBridge] Could not load bundled resource " + path + ": " + exception.getMessage());
            return new YamlConfiguration();
        }
    }

    private int[] detectImageSize(String id, String fileName) {
        File file = new File(assetRoot(), "textures/" + fileName);
        if (!file.exists()) file = new File(assetRoot(), fileName);
        if (file.exists()) {
            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null) return new int[] {image.getWidth(), image.getHeight()};
            } catch (IOException ignored) {
            }
        }
        return new int[] {FALLBACK_WIDTHS.getOrDefault(id, 16), FALLBACK_HEIGHTS.getOrDefault(id, 16)};
    }

    private void generateFontJsons() {
        File fontDir = new File(assetRoot(), "font");
        if (!fontDir.exists() && !fontDir.mkdirs()) {
            plugin.getLogger().warning("[LuxBridge] Could not create Nexo font folder: " + fontDir.getPath());
            return;
        }
        write(new File(fontDir, "offset_chars.json"), offsetFontJson());
        for (LuxImageDefinition image : images.values()) {
            write(new File(fontDir, image.id() + ".json"), imageFontJson(image));
        }
        write(new File(fontDir, "levelplugin_dialogue_character_name.json"), textFontJson(lines.characterNameAscent()));
        write(new File(fontDir, "levelplugin_dialogue_info.json"), textFontJson(lines.informationLineAscent()));
        for (int i = 1; i <= lines.dialogueLineCount(); i++) {
            write(new File(fontDir, "levelplugin_dialogue_line_" + i + ".json"), textFontJson(lines.dialogueLineAscent() - ((i - 1) * lines.dialogueLineSpace())));
        }
        for (int i = 1; i <= lines.answerLineCount(); i++) {
            write(new File(fontDir, "levelplugin_dialogue_answer_" + i + ".json"), textFontJson(lines.answerLineAscent() - ((i - 1) * lines.answerLineSpace())));
        }
    }

    private File assetRoot() {
        return new File(plugin.getServer().getWorldContainer(), NEXO_ASSET_PATH);
    }

    private void write(File file, String json) {
        try {
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().warning("[LuxBridge] Failed to write " + file.getPath() + ": " + exception.getMessage());
        }
    }

    private String offsetFontJson() {
        return "{\n  \"providers\": [\n    {\"type\":\"space\",\"advances\":{\"" + LuxBridgeElementUtil.NEGATIVE_OFFSET + "\":-1,\"" + LuxBridgeElementUtil.POSITIVE_OFFSET + "\":1}}\n  ]\n}";
    }

    private String imageFontJson(LuxImageDefinition image) {
        String texture = image.file().endsWith(".png") ? image.file().substring(0, image.file().length() - 4) : image.file();
        return "{\n  \"providers\": [\n    {\n      \"type\": \"bitmap\",\n      \"file\": \"levelplugin_dialogue:" + escape(texture) + "\",\n      \"ascent\": " + image.ascent() + ",\n      \"height\": " + Math.max(1, image.height() / Math.max(1, image.reductionRatio())) + ",\n      \"chars\": [\"" + image.glyph() + "\"]\n    }\n  ]\n}";
    }

    private String textFontJson(int ascent) {
        return "{\n  \"providers\": [\n" +
                "    {\"type\":\"bitmap\",\"file\":\"minecraft:font/ascii\",\"ascent\":" + ascent + ",\"height\":8,\"chars\":[\"\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\",\" !\\\"#$%&'()*+,-./\",\"0123456789:;<=>?\",\"@ABCDEFGHIJKLMNO\",\"PQRSTUVWXYZ[\\\\]^_\",\"`abcdefghijklmno\",\"pqrstuvwxyz{|}~\\u0000\"]}\n" +
                "  ]\n}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
