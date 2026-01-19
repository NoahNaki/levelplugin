package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.MaterialParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.util.Validate;
import org.bukkit.Material;

public class ParticleBlockMarker extends Particle implements MaterialParticle {
    public ParticleBlockMarker(Material material, double offsetX, double offsetY, double offsetZ, int count) {
        super("", offsetX, offsetY, offsetZ, count);

        setMaterial(material);
    }

    public ParticleBlockMarker(double offsetX, double offsetY, double offsetZ, int count) {
        this(Material.BARRIER, offsetX, offsetY, offsetZ, count);
    }

    public ParticleBlockMarker(Material material, double offsetX, double offsetY, double offsetZ) {
        this(material,offsetX, offsetY, offsetZ, 1);
    }

    public ParticleBlockMarker(double offsetX, double offsetY, double offsetZ) {
        this(Material.BARRIER, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleBlockMarker(Material material, int count) {
        this(material, 0, 0, 0, count);
    }

    public ParticleBlockMarker(Material material) {
        this(material, 0, 0, 0, 1);
    }

    public ParticleBlockMarker(int count) {
        this(Material.BARRIER, 0, 0, 0, count);
    }

    public ParticleBlockMarker() {
        this(Material.BARRIER, 0, 0, 0, 1);
    }

    @Override
    public ParticleBlockMarker inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof MaterialParticle) {
            setMaterial(((MaterialParticle) particle).getMaterial());
        }

        return this;
    }

    @Override
    public ParticleBlockMarker clone() {
        return new ParticleBlockMarker().inherit(this);
    }

    public void setMaterial(Material material) {
        Validate.notNull(material, "Material cannot be null!");
        Validate.isTrue(material.isBlock(), "Material must be a block!");

        setParticleKey("block_marker", material.createBlockData());
    }

    public Material getMaterial() {
        if (data instanceof org.bukkit.block.data.BlockData blockData) {
            return blockData.getMaterial();
        }
        return Material.AIR;
    }
}
