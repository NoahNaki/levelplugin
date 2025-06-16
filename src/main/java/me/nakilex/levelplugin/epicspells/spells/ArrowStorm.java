package me.nakilex.levelplugin.epicspells.spells;

import me.nakilex.levelplugin.epicspells.BaseSpell;
import me.nakilex.levelplugin.epicspells.SpellManager;
import me.nakilex.levelplugin.epicspells.utils.DirectionalParticle;
import me.nakilex.levelplugin.epicspells.utils.DirectionalParticleCollection;
import me.nakilex.levelplugin.epicspells.utils.LocationUtils;
import me.nakilex.levelplugin.epicspells.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class ArrowStorm extends BaseSpell {

    @Override
    public void init(SpellManager spellManager, Location location, Player player, int id, int parentID, String name){
        super.init(spellManager, location, player, id, parentID, name);
        maxLifeTime = 120;
    }

    @Override
    public void init(SpellManager spellManager, Player player, int id, int parentID, String name){
        Location loc = player.getTargetBlock(null, 80).getLocation().add(new Vector(0, 20, 0));
        init(spellManager, loc, player, id, parentID, name);
    }



    @Override
    public void tick(){
        super.tick();
        double radius = 6;
        int numPoints = 30;
        Vector vel = new Vector();
        if(lifetime % 10 == 0) {
            List<DirectionalParticleCollection> particles = new ArrayList<>();
            List<Location> circle = LocationUtils.generateCircle(position, radius, numPoints);
            for(Location location: circle){
                particles.add(new DirectionalParticleCollection(world, Particle.FLAME, location, vel,1, 0));
            }
            for (DirectionalParticleCollection temp : particles) {
                temp.spawn();
            }
        }
        if(lifetime > 20 && Utils.randomDouble(0, 1) < 0.5){
            Vector direction = new Vector(0, -1, 0);
            Location location = LocationUtils.randomPointInCircle(position, radius);
            Arrow arrow = world.spawnArrow(location, direction, 4, 12);
            arrow.setFireTicks(200);
            DirectionalParticle.spawn(Particle.CLOUD, location, vel, 0);
            world.playSound(location, Sound.ITEM_FIRECHARGE_USE, 5, 1);
        }
    }

    @Override
    public void on_lifetime_end() {
        alive = false;
    }

    @Override
    public void terminate(Location location){

    }
}
