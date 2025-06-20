public enum GateAnimation {
    INSTANT,
    GATE,
    ELEVATOR,
    WATERFALL;

    public static GateAnimation fromString(String name) {
        if (name == null) return INSTANT;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INSTANT;
        }
    }
}
