package me.nakilex.levelplugin.debug;

import java.util.List;

/**
 * Static data source for stronghold template bounds.
 * <p>
 * Keeping these large coordinate tables outside of the generator class
 * keeps the generation runtime easier to read and maintain.
 */
public final class StrongholdTemplateData {
    private StrongholdTemplateData() {
    }

    public static final String SOURCE_WORLD = "flatland";

    public record Bounds(int x1, int y1, int z1, int x2, int y2, int z2) {
    }

    public record TemplateEntry(String id, Bounds bounds, String category, int weight) {
    }

    public record DetachedAssetEntry(String id, String assetType, Bounds bounds) {
    }

    public static List<TemplateEntry> templates() {
        return List.of(
                new TemplateEntry("straight_1", new Bounds(219, -39, -6337, 249, -61, -6347), "WALL", 2),
                new TemplateEntry("straight_2", new Bounds(219, -39, -6347, 249, -61, -6357), "WALL", 2),
                new TemplateEntry("straight_3", new Bounds(219, -39, -6357, 249, -61, -6367), "WALL", 2),
                new TemplateEntry("straight_4", new Bounds(219, -39, -6367, 249, -61, -6377), "WALL", 2),
                new TemplateEntry("straight_5", new Bounds(219, -39, -6377, 249, -61, -6387), "WALL", 2),
                new TemplateEntry("straight_6", new Bounds(219, -39, -6387, 249, -61, -6397), "WALL", 2),
                new TemplateEntry("straight_7", new Bounds(219, -39, -6397, 249, -61, -6407), "WALL", 2),
                new TemplateEntry("straight_8", new Bounds(219, -39, -6407, 249, -61, -6419), "WALL", 2),
                new TemplateEntry("straight_9", new Bounds(219, -39, -6419, 249, -61, -6429), "WALL", 2),
                new TemplateEntry("straight_10", new Bounds(219, -39, -6429, 249, -61, -6439), "WALL", 2),
                new TemplateEntry("corner_1", new Bounds(249, -61, -6439, 239, -40, -6449), "WALL", 1),
                new TemplateEntry("corner_2", new Bounds(239, -61, -6439, 229, -40, -6449), "WALL", 1),
                new TemplateEntry("deadend_1", new Bounds(249, -61, -6337, 287, -39, -6347), "DEAD_END", 1),
                new TemplateEntry("deadend_2", new Bounds(249, -61, -6347, 287, -39, -6363), "DEAD_END", 1),
                new TemplateEntry("corner_3", new Bounds(287, -61, -6337, 310, -39, -6361), "WALL", 1),
                new TemplateEntry("corner_4", new Bounds(287, -61, -6361, 310, -39, -6384), "WALL", 1),
                new TemplateEntry("corner_5", new Bounds(287, -61, -6384, 310, -39, -6408), "WALL", 1),
                new TemplateEntry("t_section", new Bounds(310, -61, -6337, 346, -23, -6364), "JUNCTION_LARGE", 3),
                new TemplateEntry("tower_1", new Bounds(310, -13, -6364, 334, -61, -6388), "JUNCTION_LARGE", 1),
                new TemplateEntry("smallfort", new Bounds(346, -61, -6337, 378, -30, -6395), "WALL", 1),
                new TemplateEntry("smallfortpassage", new Bounds(378, -61, -6401, 437, -28, -6434), "WALL", 1),
                new TemplateEntry("gate_1", new Bounds(378, -61, -6337, 450, -11, -6369), "JUNCTION_LARGE", 1),
                new TemplateEntry("gate_2", new Bounds(378, -61, -6369, 450, -11, -6401), "JUNCTION_LARGE", 1),
                new TemplateEntry("fort", new Bounds(450, -61, -6337, 520, -29, -6407), "WALL", 1),
                new TemplateEntry("fortpassage", new Bounds(450, -61, -6408, 520, -29, -6478), "WALL", 1),
                new TemplateEntry("church", new Bounds(520, -61, -6478, 450, 37, -6548), "JUNCTION_LARGE", 1)
        );
    }

    public static TemplateEntry connector() {
        return new TemplateEntry("connector_1", new Bounds(412, -61, -5711, 402, -38, -5701), "CONNECTOR", 1);
    }

    public static List<DetachedAssetEntry> detachedAssets() {
        return List.of(
                new DetachedAssetEntry("flag_1", "FLAG", new Bounds(184, -61, -6341, 188, -41, -6359)),
                new DetachedAssetEntry("tree_1", "TREE", new Bounds(210, -61, -6337, 200, -38, -6347)),
                new DetachedAssetEntry("tree_2", "TREE", new Bounds(210, -61, -6347, 195, -23, -6362)),
                new DetachedAssetEntry("tree_3", "TREE", new Bounds(210, -61, -6362, 191, -4, -6383)),
                new DetachedAssetEntry("rock_large_1", "ROCK", new Bounds(60, -34, -6342, 112, -61, -6396)),
                new DetachedAssetEntry("rock_large_2", "ROCK", new Bounds(112, -61, -6396, 60, -34, -6461)),
                new DetachedAssetEntry("rock_large_3", "ROCK", new Bounds(112, -34, -6461, 181, -61, -6408)),
                new DetachedAssetEntry("rock_large_4", "ROCK", new Bounds(181, -61, -6408, 112, -34, -6342)),
                new DetachedAssetEntry("ruin_1", "RUIN", new Bounds(590, -61, -6426, 624, -30, -6388))
        );
    }
}
