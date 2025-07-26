package me.nakilex.levelplugin.dungeon;

public class DungeonLayout {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 6;

    private final RoomType[][] grid = new RoomType[WIDTH][HEIGHT];

    public DungeonLayout() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = RoomType.NONE;
            }
        }
    }

    public RoomType get(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return RoomType.NONE;
        return grid[x][y];
    }

    public void set(int x, int y, RoomType type) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        grid[x][y] = type;
    }

    public boolean hasEntrance() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] == RoomType.ENTRANCE) return true;
            }
        }
        return false;
    }
}
