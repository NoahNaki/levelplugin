package me.nakilex.levelplugin.epicspells;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import me.nakilex.levelplugin.Main;

public class SpellRunner extends BukkitRunnable {
    private final BaseSpell spell;

    public SpellRunner(BaseSpell spell) {
        this.spell = spell;
    }

    @Override
    public void run() {
        if (!spell.isAlive()) {
            spell.terminate(spell.getPosition());
            cancel();
            return;
        }
        spell.tick();
        if (spell.getLifeTime() >= spell.getMaxLifeTime()) {
            spell.on_lifetime_end();
            spell.setAlive(false);
        }
        if (!spell.isAlive()) {
            Location loc = spell.getPosition();
            spell.terminate(loc);
            cancel();
        }
    }

    public void start() {
        runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
