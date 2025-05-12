package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.spells.MageSpell;
import me.nakilex.levelplugin.spells.RogueSpell;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;

import static me.nakilex.levelplugin.player.classes.data.PlayerClass.VILLAGER;

public class ClickComboListener implements Listener {

    private static final long MAX_COMBO_TIME = 2000L; // 2 seconds
    private static final Map<UUID, ClickSequence> playerCombos = new HashMap<>();
    private final Map<UUID, Map<String, Long>> spellCooldowns = new HashMap<>();
    private final Map<UUID, Long> activeLeftClicks = new HashMap<>();
    private final Map<UUID, Long> bowCooldowns = new HashMap<>();
    private static final long BOW_SHOT_COOLDOWN = 500L; // 0.5 seconds
    private final Map<UUID, Long> mageCooldowns = new HashMap<>();
    private static final long MAGE_ATTACK_COOLDOWN = 500L;
    private final MageSpell mageSpell = new MageSpell();

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player    = event.getPlayer();
        UUID   playerId  = player.getUniqueId();
        long   now       = System.currentTimeMillis();

        // — Debounce rapid swings —
        if (activeLeftClicks.containsKey(playerId) &&
            now - activeLeftClicks.get(playerId) < 100) {
            return;
        }
        activeLeftClicks.put(playerId, now);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(),
            () -> activeLeftClicks.remove(playerId), 5L);

        PlayerStats ps      = StatsManager.getInstance().getPlayerStats(playerId);
        String      cls     = ps.playerClass.name().toLowerCase();
        ItemStack   mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) return;

        // —— MAGE BASIC ATTACK (unchanged) ——
        if (cls.equals("mage")) {
            // … your existing mage‐branch here …
            return;
        }

        // —— ROGUE & WARRIOR: always do a cone‐AoE on click if no combo active ——
        if (cls.equals("rogue") || cls.equals("warrior")) {
            // if they’re in the middle of a combo, record it instead
            if (!getActiveCombo(player).isEmpty()) {
                recordComboClick(player, "L");
                return;
            }

            // level-gate (same as your other skills)
            int level = LevelManager.getInstance().getLevel(player);
            CustomItem ci = ItemManager.getInstance()
                .getCustomItemFromItemStack(mainHand);
            if (ci != null && level < ci.getLevelRequirement()) {
                player.sendMessage("§cYou must be level " + ci.getLevelRequirement()
                    + " to use that attack with your " + ci.getBaseName() + "!");
                return;
            }

            // perform the full‐cone sweep no matter if you “hit” or not
            doMeleeSweep(player, cls);
            return;
        }

        // —— everyone else just builds combos ——
        recordComboClick(player, "L");
    }


    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND ||
            (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        String className = ps.playerClass.name().toLowerCase();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) return;

        if ("archer".equals(className) && mainHand.getType() == Material.BOW) {
            String activeCombo = getActiveCombo(player);
            if (!activeCombo.isEmpty()) {
                event.setCancelled(true);
                recordComboClick(player, "R");
            } else {
                // Cancel the vanilla bow pull and do our custom shot
                event.setCancelled(true);
                cancelBowPullAndShootArrow(player);
            }
            return;
        }

        // Spell‐combo branch for all other classes (or archer combos)
        String activeCombo = getActiveCombo(player);
        if (!activeCombo.isEmpty() && activeCombo.length() < 3) {
            recordComboClick(player, "R");
            return;
        }

        recordComboClick(player, "R");
    }



    private void recordComboClick(Player player, String clickType) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        PlayerStats ps = StatsManager.getInstance().getPlayerStats(uuid);
        String className = ps.playerClass.name().toLowerCase();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) return;

        Map<String, Spell> classSpells = SpellManager.getInstance().getSpellsByClass(className);
        if (classSpells.isEmpty()) return;

        boolean validWeapon = classSpells.values().stream()
            .anyMatch(spell -> spell.getAllowedWeapons().contains(mainHand.getType()));
        if (!validWeapon) return;

        ClickSequence seq = playerCombos.getOrDefault(uuid, new ClickSequence());
        if (now - seq.getLastClickTime() > MAX_COMBO_TIME) seq.clear();
        seq.addClick(clickType, now);
        playerCombos.put(uuid, seq);

        if (seq.isComplete()) {
            String combo = seq.getComboString();
            seq.clear();
            handleSpellCast(player, combo);
        }
    }

    private void cancelBowPullAndShootArrow(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // —— LEVEL GATE ——
        int playerLevel = LevelManager.getInstance().getLevel(player);
        CustomItem inst = ItemManager.getInstance()
            .getCustomItemFromItemStack(mainHand);
        if (inst != null) {
            int req = inst.getLevelRequirement();
            if (playerLevel < req) {
                player.sendMessage("§cYou must be level "
                    + req + " to use that attack with your "
                    + inst.getBaseName() + "!");
                return;
            }
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (bowCooldowns.containsKey(playerId) &&
            currentTime - bowCooldowns.get(playerId) < BOW_SHOT_COOLDOWN) {
            return;
        }

        int strength = StatsManager.getInstance()
            .getStatValue(player, StatsManager.StatType.STR);
        double baseAtk = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double damage = baseAtk + (strength * 0.5);

        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setDamage(damage);
        arrow.setCustomName("BasicArcherArrow");
        arrow.setCustomNameVisible(false);
        arrow.setMetadata("BasicAttack",
            new FixedMetadataValue(Main.getInstance(), playerId));

        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.INSTANT_EFFECT,
            player.getLocation(), 20, 0.5, 1, 0.5);
        bowCooldowns.put(playerId, currentTime);
    }




    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        // Only care about players being hit by arrows
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        String name = arrow.getCustomName();

        // 1) Prevent self‑damage from your own spells (e.g. ArrowStorm, PowerShot, ExplosiveArrow)
        //    We’re keying off the custom name, but you could also use metadata.
        if ("ArrowStorm".equals(name) ||
            "PowerShot".equals(name) ||
            "ExplosiveArrow".equals(name) ||
            "GrappleHook".equals(name) /* if that ever has self‑damage */) {

            // if the shooter is the one being hit, cancel outright
            if (target.equals(shooter)) {
                event.setCancelled(true);
                return;
            }

            // still enforce duel‑only damage for other players
            if (!DuelManager.getInstance()
                .areInDuel(shooter.getUniqueId(), target.getUniqueId())) {
                event.setCancelled(true);
            }
            return;
        }

        // 2) Your existing BasicArcherArrow logic
        if ("BasicArcherArrow".equals(name)) {
            if (!DuelManager.getInstance()
                .areInDuel(shooter.getUniqueId(), target.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    private void doMeleeSweep(Player player, String cls) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) return;

        // —— 1) Requirement checks ——
        CustomItem inst = ItemManager.getInstance().getCustomItemFromItemStack(mainHand);
        if (inst != null) {
            int playerLevel = LevelManager.getInstance().getLevel(player);
            int reqLevel    = inst.getLevelRequirement();
            String clsReq   = inst.getClassRequirement();
            PlayerClass requiredClass;
            try {
                requiredClass = PlayerClass.valueOf(clsReq.toUpperCase());
            } catch (IllegalArgumentException ex) {
                requiredClass = VILLAGER;
            }
            PlayerClass playerClass = StatsManager
                .getInstance()
                .getPlayerStats(player.getUniqueId())
                .playerClass;

            if (playerLevel < reqLevel) {
                player.sendMessage("§cYou must be level " + reqLevel +
                    " to use that attack with your " +
                    inst.getBaseName() + "!");
                return;
            }
            if (requiredClass != VILLAGER && requiredClass != playerClass) {
                return;
            }
        }

        World   world      = player.getWorld();
        Location eye       = player.getEyeLocation();
        Vector  fwd        = eye.getDirection().setY(0).normalize();

        // —— 2) One singular sweep effect around your crosshair ——
        Location effectLoc = eye.clone().add(fwd.clone().multiply(2.0));
        world.spawnParticle(
            Particle.SWEEP_ATTACK,
            effectLoc,
            1, 0, 0, 0, 0
        );
        world.playSound(
            effectLoc,
            Sound.ENTITY_PLAYER_ATTACK_SWEEP,
            1f, 1f
        );

        // —— 3) Damage calculation (works for both rogue & warrior) ——
        double baseAtk = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        int    stat    = cls.equals("warrior")
            ? StatsManager.getInstance()
            .getStatValue(player, StatsManager.StatType.STR)
            : StatsManager.getInstance()
            .getStatValue(player, StatsManager.StatType.AGI);
        double damage  = baseAtk + (stat * 0.5);

        // —— 4) Cone parameters & hit detection ——
        double range     = 4.0;
        double halfAngle = Math.toRadians(60) / 2;
        for (Entity e : world.getNearbyEntities(
            player.getLocation(),
            range, range, range)) {
            if (!(e instanceof LivingEntity target) || target.equals(player)) continue;
            if (target instanceof Player p
                && !DuelManager.getInstance()
                .areInDuel(player.getUniqueId(), p.getUniqueId())) {
                continue;
            }

            Vector toTarget = target.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .setY(0)
                .normalize();
            if (fwd.angle(toTarget) <= halfAngle) {
                SpellUtils.dealWithChat(player, target, damage, "Sweep Attack");
            }
        }
    }


    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Bukkit.getLogger().info("Player " + player.getName() + " was damaged. Cause: " + event.getCause());
        }
    }

    private void handleSpellCast(Player player, String combo) {
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        String className = ps.playerClass.name().toLowerCase();

        Spell spell = SpellManager.getInstance().getSpell(className, combo);
        if (spell == null) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!spell.getAllowedWeapons().contains(mainHand.getType())) {
            player.sendMessage("§cYou must hold a valid " + className + " weapon to cast spells!");
            return;
        }

        int playerLevel = StatsManager.getInstance().getLevel(player);
        if (playerLevel < spell.getLevelReq()) {
            player.sendMessage("§cYou are not high enough level to cast " + spell.getDisplayName());
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Long> cdMap = spellCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        if (cdMap.containsKey(spell.getId()) && now < cdMap.get(spell.getId())) {
            long secsLeft = (cdMap.get(spell.getId()) - now) / 1000;
            player.sendMessage("§cSpell on cooldown for " + secsLeft + "s");
            return;
        }

        // Cast with dynamic mana cost handling
        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);

        long nextUse = now + (spell.getCooldown() * 1000L);
        cdMap.put(spell.getId(), nextUse);
        spellCooldowns.put(player.getUniqueId(), cdMap);
    }

    public static String getActiveCombo(Player player) {
        ClickSequence seq = playerCombos.get(player.getUniqueId());
        if (seq == null) {
            return "";
        }

        long now = System.currentTimeMillis();
        if (now - seq.getLastClickTime() > MAX_COMBO_TIME) {
            // combo expired → clear and remove so next action‐bar tick sees ""
            seq.clear();
            playerCombos.remove(player.getUniqueId());
            return "";
        }

        return seq.getComboString();
    }

    private static class ClickSequence {
        private StringBuilder clicks = new StringBuilder();
        private long lastClickTime;

        void addClick(String c, long time) {
            clicks.append(c);
            lastClickTime = time;
        }

        boolean isComplete() {
            return clicks.length() >= 3;
        }

        String getComboString() { return clicks.toString(); }
        long getLastClickTime() { return lastClickTime; }
        void clear() { clicks.setLength(0); lastClickTime = 0; }
    }

    public static boolean isLocTpSafe(Location location) {
        Block block = location.getBlock();
        return !block.isLiquid() && block.isPassable();
    }
}
