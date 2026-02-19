package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.spells.impl.MageBasicFireballSpell;
import me.nakilex.levelplugin.spells.impl.MeteorSpell;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import org.bukkit.Bukkit;

public final class SpellCatalog {
    private SpellCatalog() {
    }

    public static void registerDefaults(Main plugin) {
        if (plugin == null) {
            return;
        }
        SpellRegistry registry = SpellRegistry.getInstance();
        ParticleService particleService = new ParticleService(plugin);

        SpellDefinition mageBasicFireball = new SpellDefinition("mage_basic_fireball", "Mage Fireball", 0, false);
        MageBasicFireballSpell mageBasicFireballSpell = new MageBasicFireballSpell(plugin);
        registry.registerSpell(mageBasicFireball, mageBasicFireballSpell);
        registry.registerBinding(SpellBinding.forInputType(mageBasicFireball.id(), ClassUtil::isMageFamily,
                SpellInputType.BASIC_ATTACK));
        Bukkit.getPluginManager().registerEvents(mageBasicFireballSpell, plugin);

        SpellDefinition meteor = new SpellDefinition("meteor", "Meteor", 18, false);
        registry.registerSpell(meteor, new MeteorSpell(plugin, particleService));
        registry.registerProgression(new SpellProgression(meteor.id(), null));
        registry.registerBinding(SpellBinding.forSequence(meteor.id(), ClassUtil::isMageFamily,
                SpellInputMode.MOUSE_COMBO, "RLL"));
        registry.registerBinding(SpellBinding.forSequence(meteor.id(), ClassUtil::isMageFamily,
                SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Left"));
    }
}
