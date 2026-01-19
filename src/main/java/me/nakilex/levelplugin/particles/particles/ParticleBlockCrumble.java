package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.MaterialParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.util.Validate;
import org.bukkit.Material;

public class ParticleBlockCrumble extends Particle implements MaterialParticle {
    public ParticleBlockCrumble(Material material, double offsetX, double offsetY, double offsetZ, int count) {
        super("", offsetX, offsetY, offsetZ, count);

        setMaterial(material);
    }

    public ParticleBlockCrumble(double offsetX, double offsetY, double offsetZ, int count) {
        this(Material.DRAGON_EGG, offsetX, offsetY, offsetZ, count);
    }

    public ParticleBlockCrumble(Material material, double offsetX, double offsetY, double offsetZ) {
        this(material, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleBlockCrumble(double offsetX, double offsetY, double offsetZ) {
        this(Material.DRAGON_EGG, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleBlockCrumble(Material material, int count) {
        this(material, 0, 0, 0, count);
    }

    public ParticleBlockCrumble(Material material) {
        this(material, 0, 0, 0, 1);
    }

    public ParticleBlockCrumble(int count) {
        this(Material.DRAGON_EGG, 0, 0, 0, count);
    }

    public ParticleBlockCrumble() {
        this(Material.DRAGON_EGG, 0, 0, 0, 1);
    }

    @Override
    public ParticleBlockCrumble inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof MaterialParticle) {
            setMaterial(((MaterialParticle) particle).getMaterial());
        }

        return this;
    }

    @Override
    public ParticleBlockCrumble clone() {
        return new ParticleBlockCrumble().inherit(this);
    }

    public void setMaterial(Material material) {
        Validate.notNull(material, "Material cannot be null!");
        Validate.isTrue(material.isBlock(), "Material must be a block!");

        setParticleKey("block_crumble", material.createBlockData());
    }

    public Material getMaterial() {
        if (data instanceof org.bukkit.block.data.BlockData blockData) {
            return blockData.getMaterial();
        }
        return Material.AIR;
    }
}
