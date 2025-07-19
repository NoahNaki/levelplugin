package me.nakilex.levelplugin.spells.listener;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.*;

/**
 * Generic spell listener handling all classes using predefined combos.
 * This replaces many duplicated *Spell listener classes.
 */
public class ClassSpellListener implements Listener {

    private enum Trigger { LEFT, LEFT_SNEAK, RIGHT, RIGHT_SNEAK, SNEAK_START, SNEAK_END }

    private static class Triggers {
        List<String> left = Collections.emptyList();
        List<String> leftSneak = Collections.emptyList();
        List<String> right = Collections.emptyList();
        List<String> rightSneak = Collections.emptyList();
        List<String> sneakStart = Collections.emptyList();
        List<String> sneakEnd = Collections.emptyList();
    }

    private static final Map<PlayerClass, Triggers> MAP = new EnumMap<>(PlayerClass.class);
    static {
        // Archer class
        Triggers t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LLR");
        MAP.put(PlayerClass.ARCHER, t);

        // Phoenix Hunter class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LRR");
        MAP.put(PlayerClass.PHOENIXHUNTER, t);

        // Mage class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LRR");
        MAP.put(PlayerClass.MAGE, t);

        // Warrior class
        t = new Triggers();
        t.leftSneak = List.of("RLL");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LRR");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LLR");
        MAP.put(PlayerClass.WARRIOR, t);

        // Barbarian class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LLR");
        MAP.put(PlayerClass.BARBARIAN, t);

        // Paladin class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LRR");
        MAP.put(PlayerClass.PALADIN, t);

        // Deadeye class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LRR");
        MAP.put(PlayerClass.DEADEYE, t);

        // Death Knight class (two combos on sneak start)
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LRR", "LLR");
        MAP.put(PlayerClass.DEATHKNIGHT, t);

        // Abyssion class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("RLL");
        MAP.put(PlayerClass.ABYSSION, t);

        // Dragonian class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LLR");
        MAP.put(PlayerClass.DRAGONIAN, t);

        // Dragon Warrior class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LLR");
        MAP.put(PlayerClass.DRAGONWARRIOR, t);

        // Windrune class (sneak start/stop different)
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LRR");
        t.sneakEnd = List.of("RLL");
        MAP.put(PlayerClass.WINDRUNE, t);

        // Arctic Knight class
        t = new Triggers();
        t.leftSneak = List.of("RRR");
        t.left = List.of("BASIC_ATTACK");
        t.rightSneak = List.of("LLL");
        t.right = List.of("LRL");
        t.sneakStart = List.of("LLR");
        MAP.put(PlayerClass.ARCTICKNIGHT, t);
    }

    private void cast(Player player, List<String> combos, PlayerClass pc) {
        if (combos == null) return;
        for (String combo : combos) {
            Spell spell = SpellManager.getInstance().getSpell(pc.name().toLowerCase(), combo);
            if (spell == null) {
                MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            } else {
                spell.castEffect(player);
                StatsManager.getInstance().recalcDerivedStats(player);
            }
        }
    }

    private PlayerClass getClass(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        if (p.isSneaking()) cast(p, tr.leftSneak, pc);
        else cast(p, tr.left, pc);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        event.setCancelled(true);
        if (p.isSneaking()) cast(p, tr.rightSneak, pc);
        else cast(p, tr.right, pc);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        if (event.isSneaking()) {
            cast(p, tr.sneakStart, pc);
        } else {
            cast(p, tr.sneakEnd, pc);
        }
    }
}
