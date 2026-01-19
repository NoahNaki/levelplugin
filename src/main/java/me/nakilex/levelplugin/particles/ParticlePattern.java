package me.nakilex.levelplugin.particles;

public record ParticlePattern(ParticlePatternType type,
                              double radius,
                              double innerRadius,
                              int points,
                              int arms,
                              double height,
                              double rotationSpeed) {
    public static ParticlePattern point() {
        return new ParticlePattern(ParticlePatternType.POINT, 0.0, 0.0, 1, 0, 0.0, 0.0);
    }

    public static ParticlePattern ring(double radius, int points, double rotationSpeed) {
        return new ParticlePattern(ParticlePatternType.RING, radius, 0.0, points, 0, 0.0, rotationSpeed);
    }

    public static ParticlePattern star(double radius, double innerRadius, int arms, int points, double rotationSpeed) {
        return new ParticlePattern(ParticlePatternType.STAR, radius, innerRadius, points, arms, 0.0, rotationSpeed);
    }

    public static ParticlePattern spiral(double radius, int points, double height, double rotationSpeed) {
        return new ParticlePattern(ParticlePatternType.SPIRAL, radius, 0.0, points, 0, height, rotationSpeed);
    }
}
