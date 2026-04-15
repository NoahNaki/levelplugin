package me.nakilex.levelplugin.cursormenu.model;

public class MenuActor {
    private final String id;
    private final String type;
    private final String name;
    private final boolean useViewerSkin;
    private final boolean lookAtCamera;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public MenuActor(String id,
                     String type,
                     String name,
                     boolean useViewerSkin,
                     boolean lookAtCamera,
                     double x,
                     double y,
                     double z,
                     float yaw,
                     float pitch) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.useViewerSkin = useViewerSkin;
        this.lookAtCamera = lookAtCamera;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String id() { return id; }
    public String type() { return type; }
    public String name() { return name; }
    public boolean useViewerSkin() { return useViewerSkin; }
    public boolean lookAtCamera() { return lookAtCamera; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
}
