package me.nakilex.levelplugin.particles.util;

import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.particles.parents.TravellingParticle;
import me.nakilex.levelplugin.particles.shapers.*;
import me.nakilex.levelplugin.particles.util.Validate;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** contains static utility methods with various functions meant to showcase how this dependency can be used. */
public final class ParticleSFX {

    private static final ThreadLocalRandom rng = ThreadLocalRandom.current();
    private static Plugin plugin = null;

    private ParticleSFX() {
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param size the X and Z radius will be the same as the given size, and the Y radius will be the size * 0.75.
     * @param particleFrequency particle amount
     * @return the shape displaying the cube
     */
    public static ParticlePolygon cube(Particle particle, LocationSafe center, double size, int particleFrequency) {
        return new ParticlePolygon(particle, center, 4, 2, size, size * 0.75, size, particleFrequency);
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param size the X and Z radius will be the same as the given size, and the Y radius will be the size * 0.75.
     * @param particleFrequency particle amount
     * @return the shape displaying the cube
     */
    public static ParticlePolygonFilled cubeFilled(Particle particle, LocationSafe center, double size, int particleFrequency) {
        return new ParticlePolygonFilled(particle, center, 4, 2, size, size * 0.75, size, particleFrequency);
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param sides how many sides the pyramid should have
     * @param size the X, Y, and Z radius of this pyramid
     * @param particleFrequency particle amount
     * @return the shape displaying the pyramid
     */
    public static ParticlePolygon pyramid(Particle particle, LocationSafe center, int sides, double size, int particleFrequency) {
        Validate.isTrue(sides > 2, "Pyramids must have more than 2 sides!");

        return new ParticlePolygon(particle, center, particleFrequency,
                new PolygonLayer(1, 0, 0, size / 2),
                new PolygonLayer(sides, size, size, -(size / 2))
        );
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param sides how many sides the pyramid should have
     * @param size the X, Y, and Z radius of this pyramid
     * @param particleFrequency particle amount
     * @return the shape displaying the pyramid
     */
    public static ParticlePolygonFilled pyramidFilled(Particle particle, LocationSafe center, int sides, double size, int particleFrequency) {
        Validate.isTrue(sides > 2, "Pyramids must have more than 2 sides!");

        return new ParticlePolygonFilled(particle, center, particleFrequency,
                new PolygonLayer(1, 0, 0, size / 2),
                new PolygonLayer(sides, size, size, -(size / 2)),
                new PolygonLayer(1, 0, 0, -(size / 2))
        );
    }

    /**
     * the size parameter isn't followed exactly and will probably need some fine-tuning to get correct. <br>
     * if it helps, a size of 1 goes up from the center 1.2 blocks and down from the center 1.5 blocks. <br>
     * it'd be pretty complicated to fix and i don't want to spend that much time on an util method that barely anyone will use.
     *
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param size radius of the heart
     * @param particleFrequency particle amount
     * @return the shape displaying the heart
     */
    public static ParticleLineCurved heart2D(Particle particle, LocationSafe center, double size, int particleFrequency) {
        ParticleLineCurved heart = new ParticleLineCurved(particle, particleFrequency, center.clone().add(0, 0, -size - (size * 0.4)), center.clone().add(size * 1.2, 0, size - (size * 0.4)), center.clone().add(-(size * 1.2), 0, size - (size * 0.4)), center.clone().add(0, 0, -size - (size * 0.4)));
        Location[] locations = heart.getLocations();
        
        heart.addCurve(new CurveInfo(size * 0.4, locations[0].distance(locations[1]), locations[0].distance(locations[1]) / 2, 90, -65, 0));
        heart.addCurve(new CurveInfo(size * 0.7, locations[1].distance(locations[2]) / 2, locations[1].distance(locations[2]) / 4, 90, -20, 0));
        heart.addCurve(new CurveInfo(size * 0.7, locations[1].distance(locations[2]) / 2, locations[1].distance(locations[2]) / 4, 90, 20, 0));
        heart.addCurve(new CurveInfo(size * 0.4, locations[2].distance(locations[3]), locations[2].distance(locations[3]) / 2, 90, 65, 0));

        return heart;
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param size radius of the star
     * @param particleFrequency particle amount
     * @return the shape displaying the star
     */
    public static ParticleLine star(Particle particle, LocationSafe center, double size, int particleFrequency) {
        return new ParticleLine(
                particle, particleFrequency,
                center.clone().add(0, 0, size),
                center.clone().add(size * -0.2, 0, size * 0.33),
                center.clone().add(-size, 0, size * 0.33),
                center.clone().add(size * -0.33, 0, size * -0.2),
                center.clone().add(size * -0.56, 0, size * -0.89),
                center.clone().add(0, 0, size * -0.46),
                center.clone().add(size * 0.56, 0, size * -0.89),
                center.clone().add(size * 0.33, 0, size * -0.2),
                center.clone().add(size, 0, size * 0.33),
                center.clone().add(size * 0.2, 0, size * 0.33),
                center.clone().add(0, 0, size)
        );
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param size radius of the pentagram
     * @param particleFrequency particle amount
     * @return the shape displaying the pentagram
     */
    public static ParticleLine pentagram(Particle particle, LocationSafe center, double size, int particleFrequency) {
        return new ParticleLine(
                particle, particleFrequency,
                center.clone().add(0, 0, size),
                center.clone().add(size * -0.56, 0, size * -0.89),
                center.clone().add(size, 0, size * 0.33),
                center.clone().add(-size, 0, size * 0.33),
                center.clone().add(size * 0.56, 0, size * -0.89),
                center.clone().add(0, 0, size)
        );
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param xRadius what the X radius should be
     * @param zRadius what the Z radius should be
     * @param circleSize radius of the circles that the donut will be made out of
     * @param circleFrequency number of circles to make the donut out of
     * @param particleFrequency particle amount
     * @return the shape displaying the donut
     */
    public static ParticleCylinder donut(Particle particle, LocationSafe center, double xRadius, double zRadius, double circleSize, int circleFrequency, int particleFrequency) {
        CircleInfo[] circles = new CircleInfo[circleFrequency];

        for (int i = 0; i < circleFrequency; i++) {
            double radian = Math.PI * 2 / circleFrequency * i;

            circles[i] = new CircleInfo(center.clone().add(xRadius * Math.cos(radian), 0, zRadius * Math.sin(radian)), circleSize, circleSize, 90, (360D / circleFrequency * i), 0);
        }

        return new ParticleCylinder(particle, circleFrequency, particleFrequency, circles);
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param radius what the radius should be
     * @param circleSize radius of the circles that the donut will be made out of
     * @param circleFrequency number of circles to make the donut out of
     * @param particleFrequency particle amount
     * @return the shape displaying the donut
     */
    public static ParticleCylinder donut(Particle particle, LocationSafe center, double radius, double circleSize, int circleFrequency, int particleFrequency) {
        return donut(particle, center, radius, radius, circleSize, circleFrequency, particleFrequency);
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param radius what the radius should be
     * @param circleFrequency number of circles to make the donut out of
     * @param particleFrequency particle amount
     * @return the shape displaying the donut
     */
    public static ParticleCylinder donut(Particle particle, LocationSafe center, double radius, int circleFrequency, int particleFrequency) {
        return donut(particle, center, radius, radius, radius / 5, circleFrequency, particleFrequency);
    }

    /**
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param radius what the radius should be
     * @param particleFrequency particle amount
     * @return the shape displaying the donut
     */
    public static ParticleCylinder donut(Particle particle, LocationSafe center, double radius, int particleFrequency) {
        return donut(particle, center, radius, radius, radius / 5, (int) (radius * 15), particleFrequency);
    }

    /**
     * The given {@link ParticleShapeCompound} contains a {@link ParticleCylinder}, named "cup", a {@link ParticleSpiral}, named "bottom",
     * and a {@link ParticleLineCurved}, named "handle" <br><br>
     *
     * You can get the individual shapes using {@link ParticleShapeCompound#getShape(String)}
     *
     * @param particle particle to use
     * @param bottom where the bottom of the cup should be
     * @param xRadius what the x radius should be
     * @param zRadius what the z radius should be
     * @param height what the height should be
     * @param particleFrequency particle amount
     * @return a ParticleShapeCompound containing all the shapes used to display this cup
     */
    public static ParticleShapeCompound cup(Particle particle, LocationSafe bottom, double xRadius, double zRadius, double height, int particleFrequency) {
        ParticleShapeCompound cup = new ParticleShapeCompound();
        List<CircleInfo> circles = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            double heightRadian = (Math.PI * 0.5 / 20 * i);
            double curveRadian = (Math.PI * 0.2) + (Math.PI * 0.3 / 20 * i);

            circles.add(new CircleInfo(bottom.clone().add(0, height - (height * Math.cos(heightRadian)), 0), xRadius * Math.sin(curveRadian), zRadius * Math.sin(curveRadian)));
        }

        ParticleCylinder base = new ParticleCylinder(particle, (int) (height * 2), (int) (particleFrequency * 0.80), circles.toArray(new CircleInfo[0]));
        ParticleLineCurved handle = new ParticleLineCurved(particle, (int) (particleFrequency * 0.05), bottom.clone().add(circles.get(6).getXRadius(), circles.get(6).getCenter().getY() - bottom.getY(), 0), bottom.clone().add(circles.get(18).getXRadius(), circles.get(18).getCenter().getY() - bottom.getY(), 0));

        handle.addCurve(new CurveInfo(xRadius * 0.9, handle.getTotalDistance(), handle.getTotalDistance() / 2, 60, 270, 0));
        cup.addShape(new ParticleSpiral(particle, 10, 5, (int) (particleFrequency * 0.15), new CircleInfo(bottom.clone(), 0, 0), new CircleInfo(bottom.clone(), xRadius * Math.sin(Math.PI * 0.25), zRadius * Math.sin(Math.PI * 0.25))), "bottom");
        cup.addShape(base, "cup");
        cup.addShape(handle, "handle");

        return cup;
    }

    /**
     * The given {@link ParticleShapeCompound} contains a {@link ParticleCylinder}, named "cup", a {@link ParticleSpiral}, named "bottom",
     * and a {@link ParticleLineCurved}, named "handle" <br><br>
     *
     * You can get the individual shapes using {@link ParticleShapeCompound#getShape(String)}
     *
     * @param particle particle to use
     * @param bottom where the bottom of the cup should be
     * @param radius what the radius should be
     * @param height what the height should be
     * @param particleFrequency particle amount
     * @return a ParticleShapeCompound containing all the shapes used to display this cup
     */
    public static ParticleShapeCompound cup(Particle particle, LocationSafe bottom, double radius, double height, int particleFrequency) {
        return cup(particle, bottom, radius, radius, height, particleFrequency);
    }

    /**
     * The given {@link ParticleShapeCompound} contains a {@link ParticleCylinder}, named "cup", a {@link ParticleSpiral}, named "bottom",
     * and a {@link ParticleLineCurved}, named "handle" <br><br>
     *
     * You can get the individual shapes using {@link ParticleShapeCompound#getShape(String)}
     *
     * @param particle particle to use
     * @param bottom where the bottom of the cup should be
     * @param size the x and z radius are the given size, and the height is the size * 2
     * @param particleFrequency particle amount
     * @return a ParticleShapeCompound containing all the shapes used to display this cup
     */
    public static ParticleShapeCompound cup(Particle particle, LocationSafe bottom, double size, int particleFrequency) {
        return cup(particle, bottom, size, size, size * 2, particleFrequency);
    }

    /**
     * The given {@link ParticleShapeCompound} contains a {@link ParticleLineCurved}, named "stem", and another ParticleShapeCompound,
     * named "head", which contains 11 {@link ParticleCircleFilled}s, named "center" and "petal1" to "petal10" <br><br>
     *
     * You can get the individual shapes using {@link ParticleShapeCompound#getShape(String)}
     *
     * @param particle particle to use
     * @param bottom where the bottom of the stem should be
     * @param flowerSize the radius of the flower
     * @param stemHeight the height of the stem
     * @param particleFrequency particle amount
     * @return a ParticleShapeCompound containing all the shapes used to display this flower
     */
    public static ParticleShapeCompound flower(Particle particle, LocationSafe bottom, double flowerSize, double stemHeight, int particleFrequency) {
        ParticleShapeCompound flower = new ParticleShapeCompound();
        ParticleShapeCompound head = new ParticleShapeCompound();
        ParticleLineCurved stem = new ParticleLineCurved(particle, (int) (particleFrequency * 0.077), bottom, bottom.clone().add(stemHeight * 0.15, stemHeight, 0));
        ParticleCircleFilled center = new ParticleCircleFilled(particle, (LocationSafe) stem.getLocation(1), flowerSize / 2, flowerSize / 2, 0, 0, 0, (int) (particleFrequency * 0.154));

        for (int i = 0; i < 10; i++) {
            double radian = Math.PI * 2 / 10 * i;

            ParticleCircleFilled petal = new ParticleCircleFilled(particle,
                    new LocationSafe(center.getCenter()).add(
                            flowerSize * 0.9 * Math.cos(radian),
                            -(flowerSize * 0.13),
                            flowerSize * 0.9 * Math.sin(radian)
                    ), flowerSize / 4, flowerSize / 2, -15, 90 + (360D / 10 * i), 0, (int) (particleFrequency * 0.077)
            );

            head.addShape(petal, "petal" + (i + 1));
        }

        stem.addCurve(new CurveInfo(stemHeight * 0.1, stem.getTotalDistance(), stem.getTotalDistance() * 0.75, 90, 90, 0));
        head.addShape(center, "center");
        head.rotate(125, 90, 0);
        flower.addShape(stem, "stem");
        flower.addShape(head, "head");

        return flower;
    }

    /**
     * The given {@link ParticleShapeCompound} contains a {@link ParticleLineCurved}, named "stem", and another ParticleShapeCompound,
     * named "head", which contains 11 {@link ParticleCircleFilled}s, named "center" and "petal1" to "petal10" <br><br>
     *
     * You can get the individual shapes using {@link ParticleShapeCompound#getShape(String)}
     *
     * @param particle particle to use
     * @param bottom where the bottom of the stem should be
     * @param size the stem height is equal to the given size, and the flower size is the given size * 0.15
     * @param particleFrequency particle amount
     * @return a ParticleShapeCompound containing all the shapes used to display this flower
     */
    public static ParticleShapeCompound flower(Particle particle, LocationSafe bottom, double size, int particleFrequency) {
        return flower(particle, bottom, size * 0.15, size, particleFrequency);
    }

    /**
     * @param particle particle to use
     * @param bottom where the bottom of the shape should be
     * @param xRadius what the x radius should be
     * @param zRadius what the z radius should be
     * @param height what the height should be
     * @param particleFrequency particle amount
     * @return the shape displaying the mushroom
     */
    public static ParticleCylinder mushroom(Particle particle, LocationSafe bottom, double xRadius, double zRadius, double height, int particleFrequency) {
        ParticleCylinder shroom = new ParticleCylinder(particle, (int) ((((xRadius + zRadius) / 2) + height) * 2.5), particleFrequency,
                new CircleInfo(bottom.clone(), xRadius * 0.225, zRadius * 0.225),
                new CircleInfo(bottom.clone().add(0, height * 0.15, 0), xRadius * 0.15, xRadius * 0.15),
                new CircleInfo(bottom.clone().add(0, height * 0.85, 0), xRadius * 0.15, xRadius * 0.15),
                new CircleInfo(bottom.clone().add(0, height, 0), 0, 0)
        );

        for (int i = 0; i < 15; i++) {
            double radian = Math.PI * 0.5 / 15 * i;

            shroom.addCircleInfo(new CircleInfo(bottom.clone().add(0, (height * 0.71) + (height * 0.27 * Math.cos(radian)), 0), xRadius * Math.sin(radian), zRadius * Math.sin(radian)));
        }

        shroom.addCircleInfo(new CircleInfo(bottom.clone().add(0, height * 0.71, 0), xRadius * 0.9, zRadius * 0.9));
        shroom.addCircleInfo(new CircleInfo(bottom.clone().add(0, height * 0.85, 0), xRadius * 0.15, zRadius * 0.15));

        return shroom;
    }

    /**
     * @param particle particle to use
     * @param bottom where the bottom of the shape should be
     * @param radius what the radius should be
     * @param height what the height should be
     * @param particleFrequency particle amount
     * @return the shape displaying the mushroom
     */
    public static ParticleCylinder mushroom(Particle particle, LocationSafe bottom, double radius, double height, int particleFrequency) {
        return mushroom(particle, bottom, radius, radius, height, particleFrequency);
    }

    /**
     * @param particle particle to use
     * @param bottom where the bottom of the shape should be
     * @param size the X and Z radius will be the same as the given size, and the height will be the size * 2.6
     * @param particleFrequency particle amount
     * @return the shape displaying the mushroom
     */
    public static ParticleCylinder mushroom(Particle particle, LocationSafe bottom, double size, int particleFrequency) {
        return mushroom(particle, bottom, size, size, size * 2.6, particleFrequency);
    }

    /**
     * Creates a sphere with the particles travelling away from the center
     *
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param xRadius what the X radius should be
     * @param yRadius what the Y radius should be
     * @param zRadius what the Z radius should be
     * @param particleFrequency particle amount
     * @return the shape displaying the sun
     */
    public static ParticleSphere sun(TravellingParticle particle, LocationSafe center, double xRadius, double yRadius, double zRadius, int particleFrequency) {
        ParticleSphere sun = new ParticleSphere(particle, center, xRadius, yRadius, zRadius, particleFrequency);
        Vector v = new Vector();

        sun.addMechanic(ShapeDisplayMechanic.Phase.AFTER_ROTATION, ((p, current, addition, count) -> {
            if (p instanceof TravellingParticle) {
                ((TravellingParticle) p).setVelocity(v.zero().add(addition).multiply(3));
            }
        }));

        return sun;
    }

    /**
     * Creates a sphere with the particles travelling away from the center.
     *
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param radius what the radius should be
     * @param particleFrequency particle amount
     * @return the shape displaying the sun
     */
    public static ParticleSphere sun(TravellingParticle particle, LocationSafe center, double radius, int particleFrequency) {
        return sun(particle, center, radius, radius, radius, particleFrequency);
    }

    /**
     * The given {@link ParticleShapeCompound} contains a {@link ParticleSphere}, named "nucleus", and three {@link ParticleCircle}s,
     * named "electron1" to "electron3" <br><br>
     *
     * You can get the individual shapes using {@link ParticleShapeCompound#getShape(String)}
     *
     * @param nucleus particle to use for the nucleus
     * @param electron particle to use for the electrons
     * @param center the center of where the shape should be
     * @param radius what the radius should be
     * @param particleFrequency particle amount
     * @return a ParticleShapeCompound containing all the shapes used to display this atom
     */
    public static ParticleShapeCompound atom(Particle nucleus, Particle electron, LocationSafe center, double radius, int particleFrequency) {
        ParticleShapeCompound atom = new ParticleShapeCompound();

        atom.addShape(new ParticleSphere(nucleus, center, radius * 0.2, (int) (particleFrequency * 0.8)), "nucleus");

        for (int i = 0; i < 3; i++) {
            ParticleCircle circle = new ParticleCircle(electron, center, radius, radius, 90 + (120D / 2 * i), 0, 0, particleFrequency);

            circle.setParticlesPerDisplay((int) Math.ceil(circle.getParticleFrequency() * 0.066));
            circle.setDisplayPosition(circle.getParticleFrequency() / 2 * i);
            atom.addShape(circle, "electron" + (i + 1));
        }

        nucleus.setOffset(radius * 0.025, radius * 0.025, radius * 0.025);
        electron.setOffset(radius * 0.025, radius * 0.025, radius * 0.025);

        return atom;
    }

    /**
     * The given {@link ParticleShapeCompound} contains a {@link ParticleSphere}, named "nucleus", and three {@link ParticleCircle}s,
     * named "electron1" to "electron3" <br><br>
     *
     * You can get the individual shapes using {@link ParticleShapeCompound#getShape(String)}
     *
     * @param particle particle to use
     * @param center the center of where the shape should be
     * @param radius what the radius should be
     * @param particleFrequency particle amount
     * @return a ParticleShapeCompound containing all the shapes used to display this atom
     */
    public static ParticleShapeCompound atom(Particle particle, LocationSafe center, double radius, int particleFrequency) {
        return atom(particle, particle, center, radius, particleFrequency);
    }

    /**
     * @param rainParticle particle to use for rain
     * @param cloudParticle particle to use for the cloud
     * @param center the center of where the cloud should be and where the rain should spawn
     * @param xRadius what the x radius should be
     * @param yRadius what the y radius should be
     * @param zRadius what the z radius should be
     * @param rainAmount amount of rain particles
     * @param cloudAmount amount of cloud particles
     * @return the shape displaying the raincloud
     */
    public static ParticleFluid raincloud(Particle rainParticle, Particle cloudParticle, LocationSafe center, double xRadius, double yRadius, double zRadius, int rainAmount, int cloudAmount) {
        ParticleFluid rain = new ParticleFluid(rainParticle, center, 0.75, 0.5, 1);
        Vector v = new Vector();

        rain.addMechanic(ShapeDisplayMechanic.Phase.AFTER_DISPLAY_PARTICLE, ((particle, current, addition, count) -> {
            if (count == 1) {
                Location l = rain.getSpawnLocation();

                if (rain.getParticleFrequency() < rainAmount) {
                    for (int i = 0; i < (int) Math.ceil(rainAmount / 100D); i++) {
                        v.setX(rng.nextDouble(xRadius * 2) - xRadius);
                        v.setY(rng.nextDouble(yRadius * 2) - yRadius);
                        v.setZ(rng.nextDouble(zRadius * 2) - zRadius);

                        l.add(v);
                        rain.setParticleFrequency(rain.getParticleFrequency() + 1);
                        l.subtract(v);
                    }
                } else {
                    rain.setParticleFrequency(rain.getParticleFrequency() - (int) Math.ceil(rainAmount / 100D));
                }

                cloudParticle.setOffset(xRadius, yRadius, zRadius);

                for (int i = 0; i < cloudAmount; i++) {
                    if (!rain.getPlayers().isEmpty()) {
                        cloudParticle.displayForPlayers(l, rain.getPlayers());
                    } else {
                        cloudParticle.display(l);
                    }
                }
            }
        }));

        return rain;
    }

    /**
     * @param rainParticle particle to use for rain
     * @param cloudParticle particle to use for the cloud
     * @param center the center of where the cloud should be and where the rain should spawn
     * @param xRadius what the x radius should be
     * @param yRadius what the y radius should be
     * @param zRadius what the z radius should be
     * @param rainAmount amount of rain and cloud particles
     * @return the shape displaying the raincloud
     */
    public static ParticleFluid raincloud(Particle rainParticle, Particle cloudParticle, LocationSafe center, double xRadius, double yRadius, double zRadius, int rainAmount) {
        return raincloud(rainParticle, cloudParticle, center, xRadius, yRadius, zRadius, rainAmount, rainAmount);
    }

    /**
     * @param rainParticle particle to use for rain
     * @param cloudParticle particle to use for the cloud
     * @param center the center of where the cloud should be and where the rain should spawn
     * @param xRadius what the x radius should be
     * @param zRadius what the z radius should be
     * @param rainAmount amount of rain and cloud particles
     * @return the shape displaying the raincloud
     */
    public static ParticleFluid raincloud(Particle rainParticle, Particle cloudParticle, LocationSafe center, double xRadius, double zRadius, int rainAmount) {
        return raincloud(rainParticle, cloudParticle, center, xRadius, (xRadius + zRadius) / 2 * 0.4, zRadius, rainAmount, rainAmount);
    }

    /**
     * @param rainParticle particle to use for rain
     * @param cloudParticle particle to use for the cloud
     * @param center the center of where the cloud should be and where the rain should spawn
     * @param radius what the radius should be
     * @param rainAmount amount of rain and cloud particles
     * @return the shape displaying the raincloud
     */
    public static ParticleFluid raincloud(Particle rainParticle, Particle cloudParticle, LocationSafe center, double radius, int rainAmount) {
        return raincloud(rainParticle, cloudParticle, center, radius, radius / 2 * 0.4, radius, rainAmount, rainAmount);
    }

    /**
     * @param particle particle to use
     * @param bottom where the bottom of the shape should be
     * @param xRadius what the X radius should be
     * @param zRadius what the Z radius should be
     * @param height how high this shape should extend
     * @param particleFrequency particle amount
     * @return the shape displaying the tornado
     */
    public static ParticleSpiral tornado(Particle particle, LocationSafe bottom, double xRadius, double zRadius, double height, int particleFrequency) {
        ParticleSpiral spi = new ParticleSpiral(particle, 15, 1, particleFrequency / 2,
                new CircleInfo(bottom, 0, 0),
                new CircleInfo(bottom.clone().add(0, height * 0.75, 0), xRadius * 0.6, zRadius * 0.6),
                new CircleInfo(bottom.clone().add(0, height, 0), xRadius, zRadius)
        );

        spi.addMechanic(ShapeDisplayMechanic.Phase.AFTER_DISPLAY_PARTICLE, ((mechParticle, current, addition, count) -> {
            CircleInfo top = spi.getCircleInfo(spi.getCircleInfoAmount() - 1);

            if (count == 1) {
                for (int i = 0; i < spi.getCircleInfoAmount(); i++) {
                    //i cant figure out a way to do this that makes the spin look decent at every size so this is good enough
                    spi.getCircleInfo(i).setYaw(spi.getCircleInfo(i).getYaw() - Math.toDegrees(Math.PI * 0.04 * Math.sqrt((Math.pow(top.getXRadius(), 2) + Math.pow(top.getZRadius(), 2)) / 2)));
                }
            }

            mechParticle.setOffset(top.getXRadius(), top.getCenter().distance(spi.getCircleInfo(0).getCenter()) / 10, top.getZRadius());

            if (!spi.getPlayers().isEmpty()) {
                mechParticle.displayForPlayers(top.getCenter(), spi.getPlayers());
            } else {
                mechParticle.display(top.getCenter());
            }

            mechParticle.setOffset(0, 0, 0);
        }));

        return spi;
    }

    /**
     * @param particle particle to use
     * @param bottom where the bottom of the shape should be
     * @param radius what the radius should be
     * @param height how high this shape should extend
     * @param particleFrequency particle amount
     * @return the shape displaying the tornado
     */
    public static ParticleSpiral tornado(Particle particle, LocationSafe bottom, double radius, double height, int particleFrequency) {
        return tornado(particle, bottom, radius, radius, height, particleFrequency);
    }

    /**
     * Creates a circular arc, useful for slash effects.
     *
     * @param particle particle to use
     * @param center the center of the arc
     * @param radius what the radius should be
     * @param arcDegrees how many degrees of the circle should be shown (1 - 360)
     * @param pitch pitch rotation
     * @param yaw yaw rotation
     * @param roll roll rotation
     * @param particleFrequency particle amount
     * @return the shape displaying the arc
     */
    public static ParticleCircle arc(Particle particle, LocationSafe center, double radius, double arcDegrees, double pitch, double yaw, double roll, int particleFrequency) {
        Validate.isTrue(arcDegrees > 0 && arcDegrees <= 360, "Arc degrees must be between 1 and 360");
        ParticleCircle circle = new ParticleCircle(particle, center, radius, radius, pitch, yaw, roll, particleFrequency);
        double limit = 100 - ((arcDegrees / 360D) * 100);
        circle.setLimit(limit);
        return circle;
    }

    /**
     * Creates an arc that can be faced toward a target, useful for sword slash effects.
     *
     * @param particle particle to use
     * @param center the center of the arc
     * @param toFace where the arc should face
     * @param radius what the radius should be
     * @param arcDegrees how many degrees of the circle should be shown (1 - 360)
     * @param particleFrequency particle amount
     * @return the shape displaying the arc slash
     */
    public static ParticleCircle arcSlash(Particle particle, LocationSafe center, Location toFace, double radius, double arcDegrees, int particleFrequency) {
        ParticleCircle circle = arc(particle, center, radius, arcDegrees, 0, 0, 0, particleFrequency);
        circle.face(toFace);
        return circle;
    }

    /**
     * Creates a magic circle composed of outer/inner rings and a pentagram.
     *
     * @param ringParticle particle to use for the rings
     * @param glyphParticle particle to use for the glyph
     * @param center the center of the magic circle
     * @param outerRadius radius of the outer ring
     * @param innerRadius radius of the inner ring
     * @param particleFrequency particle amount
     * @return a compound containing the magic circle shapes
     */
    public static ParticleShapeCompound magicCircle(Particle ringParticle, Particle glyphParticle, LocationSafe center, double outerRadius, double innerRadius, int particleFrequency) {
        ParticleShapeCompound compound = new ParticleShapeCompound();
        ParticleCircle outer = new ParticleCircle(ringParticle, center, outerRadius, outerRadius, particleFrequency);
        ParticleCircle inner = new ParticleCircle(ringParticle, center, innerRadius, innerRadius, particleFrequency);
        ParticleLine glyph = pentagram(glyphParticle, center, innerRadius, particleFrequency);
        compound.addShape(outer, "outer");
        compound.addShape(inner, "inner");
        compound.addShape(glyph, "glyph");
        return compound;
    }

    /**
     * Creates a magic circle composed of outer/inner rings and a pentagram using the same particle.
     *
     * @param particle particle to use
     * @param center the center of the magic circle
     * @param outerRadius radius of the outer ring
     * @param innerRadius radius of the inner ring
     * @param particleFrequency particle amount
     * @return a compound containing the magic circle shapes
     */
    public static ParticleShapeCompound magicCircle(Particle particle, LocationSafe center, double outerRadius, double innerRadius, int particleFrequency) {
        return magicCircle(particle, particle, center, outerRadius, innerRadius, particleFrequency);
    }

    /**
     * Creates a wave line using a sine curve.
     *
     * @param particle particle to use
     * @param start where the wave begins
     * @param direction direction the wave travels
     * @param length total length of the wave
     * @param amplitude height of the wave
     * @param wavelength length of one full wave
     * @param points number of segments in the wave
     * @param particleFrequency particle amount
     * @return the shape displaying the wave
     */
    public static ParticleLine wave(Particle particle, LocationSafe start, Vector direction, double length, double amplitude, double wavelength, int points, int particleFrequency) {
        Validate.notNull(start, "Start location cannot be null!");
        Validate.notNull(start.getWorld(), "Start location world cannot be null!");
        Validate.notNull(direction, "Direction cannot be null!");
        Validate.isTrue(length > 0, "Length must be greater than 0");
        Validate.isTrue(wavelength > 0, "Wavelength must be greater than 0");
        Validate.isTrue(points >= 2, "Points must be 2 or greater");

        Vector dir = direction.clone();
        if (dir.lengthSquared() == 0) {
            dir = new Vector(1, 0, 0);
        }
        dir.normalize();
        LocationSafe[] locations = new LocationSafe[points + 1];
        for (int i = 0; i <= points; i++) {
            double distance = length * i / points;
            double phase = (Math.PI * 2 * distance) / wavelength;
            double vertical = Math.sin(phase) * amplitude;
            locations[i] = start.clone().add(dir.clone().multiply(distance)).add(0, vertical, 0);
        }
        return new ParticleLine(particle, particleFrequency, locations);
    }

    /**
     * Creates a wave line using the start location direction.
     *
     * @param particle particle to use
     * @param start where the wave begins
     * @param length total length of the wave
     * @param amplitude height of the wave
     * @param wavelength length of one full wave
     * @param points number of segments in the wave
     * @param particleFrequency particle amount
     * @return the shape displaying the wave
     */
    public static ParticleLine wave(Particle particle, LocationSafe start, double length, double amplitude, double wavelength, int points, int particleFrequency) {
        return wave(particle, start, start.getDirection(), length, amplitude, wavelength, points, particleFrequency);
    }

    /**
     * @param toFace the location to get the direction towards
     * @param location the starting location
     * @return a double array of the pitch and yaw of the direction where the pitch is [0] and the yaw is [1]
     */
    public static double[] getDirection(Location toFace, Location location) {
        Validate.isTrue(toFace.getWorld().equals(location.getWorld()), "Locations must be in the same world!");

        double x = toFace.getX() - location.getX();
        double y = toFace.getY() - location.getY();
        double z = toFace.getZ() - location.getZ();
        //i genuinely have no bloody clue why this works. if anyone sees this and understands what the heck this is please tell me im very curious
        //(code stolen from Location.setDirection(Vector) and condensed according to my code conventions)
        double pitch = Math.toDegrees(Math.atan(-y / Math.sqrt(NumberConversions.square(x) + NumberConversions.square(z)))) - 90;
        double yaw = Math.toDegrees((Math.atan2(-x, z) + (Math.PI * 2)) % (Math.PI * 2));

        return new double[] {pitch, yaw};
    }

    /**
     * @param plugin the plugin for all of this dependency's BukkitRunnables to be assigned to
     */
    public static void setPlugin(Plugin plugin) {
        ParticleSFX.plugin = plugin;
    }

    /**
     * @return the plugin that this dependency is using for BukkitRunnables
     */
    public static Plugin getPlugin() {
        return plugin;
    }
}
