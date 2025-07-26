package me.nakilex.levelplugin.dungeon;

public class DungeonLayout {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 6;

    private final RoomType[][] grid = new RoomType[WIDTH][HEIGHT];
    private final int[][] rotation = new int[WIDTH][HEIGHT];
    private final String[][] mobs = new String[WIDTH][HEIGHT];

    public DungeonLayout() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = RoomType.NONE;
                rotation[x][y] = 0;
                mobs[x][y] = null;
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

    public int getRotation(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return 0;
        return rotation[x][y];
    }

    public void setRotation(int x, int y, int rot) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        rotation[x][y] = rot & 3;
    }

    public String getMob(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return null;
        return mobs[x][y];
    }

    public void setMob(int x, int y, String mob) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        mobs[x][y] = mob;
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
