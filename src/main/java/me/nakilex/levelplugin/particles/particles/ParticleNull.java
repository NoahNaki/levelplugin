package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.shapers.parents.ParticleShaper;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Display does nothing in this class. it's meant to be used in {@link ParticleShaper#addParticle(Particle, int)}
 * to allow for certain spots to display nothing
 */
public class ParticleNull extends Particle {
    /**@see ParticleNull*/
    public ParticleNull() {
        super("", 0, 0, 0, 0);
    }

    @Override
    public void displayForPlayers(Location location, Player... players) {
    }

    @Override
    public void display(Location location) {
    }

    @Override
    protected void display(Location location, List<Player> players) {
    }

    @Override
    public ParticleNull inherit(Particle particle) {
        super.inherit(particle);

        return this;
    }

    @Override
    public ParticleNull clone() {
        return new ParticleNull().inherit(this);
    }
}
