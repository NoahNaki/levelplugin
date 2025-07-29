package me.nakilex.levelplugin.dungeon;

public class DungeonLayout {
    // Increased grid size to accommodate larger layouts and ensure
    // rooms far from the entrance (like the boss room) are persisted.
    public static final int WIDTH = 15;
    public static final int HEIGHT = 11;

    private final RoomType[][] grid = new RoomType[WIDTH][HEIGHT];
    private final TemplateType[][] templates = new TemplateType[WIDTH][HEIGHT];
    private final int[][] rotation = new int[WIDTH][HEIGHT];
    private final String[][] mobs = new String[WIDTH][HEIGHT];
    private final int[][] threat = new int[WIDTH][HEIGHT];
    private final int[][] offsetX = new int[WIDTH][HEIGHT];
    private final int[][] offsetZ = new int[WIDTH][HEIGHT];
    private int step = 0;

    public DungeonLayout() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = RoomType.NONE;
                templates[x][y] = TemplateType.NONE;
                rotation[x][y] = 0;
                mobs[x][y] = null;
                threat[x][y] = 0;
                offsetX[x][y] = 0;
                offsetZ[x][y] = 0;
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

    public TemplateType getTemplate(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return TemplateType.NONE;
        return templates[x][y];
    }

    public void setTemplate(int x, int y, TemplateType t) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        templates[x][y] = t;
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

    public int getThreat(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return 0;
        return threat[x][y];
    }

    public void setThreat(int x, int y, int level) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        threat[x][y] = level;
    }

    public int getOffsetX(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return 0;
        return offsetX[x][y];
    }

    public int getOffsetZ(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return 0;
        return offsetZ[x][y];
    }

    public void setOffset(int x, int y, int offX, int offZ) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        offsetX[x][y] = offX;
        offsetZ[x][y] = offZ;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    /** Return the highest threat level among all cells. */
    public int getMaxThreat() {
        int max = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (threat[x][y] > max) max = threat[x][y];
            }
        }
        return max;
    }

    public boolean hasEntrance() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] == RoomType.ENTRANCE) return true;
            }
        }
        return false;
    }

    public boolean hasExit() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] == RoomType.EXIT) return true;
            }
        }
        return false;
    }
}
