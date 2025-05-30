package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import me.nakilex.levelplugin.runes.model.RuneEffect;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
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
    private RunesManager runeManager;


    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player   = event.getPlayer();
        UUID   playerId = player.getUniqueId();
        long   now      = System.currentTimeMillis();

        // Throttle rapid swings
        if (activeLeftClicks.containsKey(playerId) &&
            now - activeLeftClicks.get(playerId) < 100) {
            return;
        }
        activeLeftClicks.put(playerId, now);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(),
            () -> activeLeftClicks.remove(playerId), 5L);

        // Class, level, and held‐item check
        PlayerStats ps  = StatsManager.getInstance().getPlayerStats(playerId);
        String      cls = ps.playerClass.name().toLowerCase();
        ItemStack   main = player.getInventory().getItemInMainHand();
        if (main == null || main.getType() == Material.AIR) return;

        // —— MAGE BASIC RAY ——
        if ("mage".equals(cls) &&
            WeaponType.WAND.getMaterials().contains(main.getType())) {

            // Level requirement on the wand
            CustomItem wand = ItemManager.getInstance().getCustomItemFromItemStack(main);
            if (wand != null && LevelManager.getInstance().getLevel(player) < wand.getLevelRequirement()) {
                player.sendMessage("§cYou must be level " + wand.getLevelRequirement()
                    + " to use that attack with your " + wand.getBaseName() + "!");
                return;
            }

            // If in the middle of any combo, let combo logic handle it
            if (!getActiveCombo(player).isEmpty()) {
                recordComboClick(player, "L");
                return;
            }

            // **NEW**: Fire through your spell‐casting pipeline so runes are applied
            handleSpellCast(player, "L");
            return;
        }

        // —— ROGUE / WARRIOR melee sweep ——
        if ("rogue".equals(cls) || "warrior".equals(cls)) {
            if (!getActiveCombo(player).isEmpty()) {
                recordComboClick(player, "L");
                return;
            }

            int lvl = LevelManager.getInstance().getLevel(player);
            CustomItem ci2 = ItemManager.getInstance().getCustomItemFromItemStack(main);
            if (ci2 != null && lvl < ci2.getLevelRequirement()) {
                player.sendMessage("§cYou must be level " + ci2.getLevelRequirement()
                    + " to use that attack with your " + ci2.getBaseName() + "!");
                return;
            }

            doMeleeSweep(player, cls);
            return;
        }

        // —— All other classes build combos ——
        recordComboClick(player, "L");
    }


    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        // Debug: log click event details
        Player dbgPlayer = event.getPlayer();
        String dbgClassName = StatsManager.getInstance()
            .getPlayerStats(dbgPlayer.getUniqueId())
            .playerClass.name().toLowerCase();
        ItemStack dbgMain = dbgPlayer.getInventory().getItemInMainHand();
        String dbgItem = (dbgMain != null && dbgMain.getType() != Material.AIR)
            ? dbgMain.getType().name() : "none";
        String dbgCombo = getActiveCombo(dbgPlayer);
        Bukkit.getLogger().info(
            "[DBG] onRightClick -> class=" + dbgClassName +
                ", item=" + dbgItem +
                ", activeCombo=" + dbgCombo
        );

        // Debug: list all valid combos for this class
        Map<String, Spell> available = SpellManager.getInstance().getSpellsByClass(dbgClassName);
        Bukkit.getLogger().info("[DBG] Valid combos for class=" + dbgClassName + ": " + available.keySet());

        // Only handle main-hand right-clicks
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

        String activeCombo = getActiveCombo(player);

        // —— Archer bow logic ——
        if ("archer".equals(className) && mainHand.getType() == Material.BOW) {
            event.setCancelled(true);
            if (!activeCombo.isEmpty()) {
                Bukkit.getLogger().info(
                    "[DBG] Archer combo detected: " + activeCombo +
                        ", recording click"
                );
                recordComboClick(player, "R");
            } else {
                Bukkit.getLogger().info("[DBG] No combo: invoking BASIC_ATTACK effect");
                handleSpellCast(player, "BASIC_ATTACK");
            }
            return;
        }

        // Default: record combo
        Bukkit.getLogger().info("[DBG] Not an archer bow click, recording combo R");
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

    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        String name = arrow.getCustomName();


        if ("ArrowStorm".equals(name) ||
            "PowerShot".equals(name) ||
            "ExplosiveArrow".equals(name) ||
            "GrappleHook".equals(name) /* if that ever has self‑damage */) {

            if (target.equals(shooter)) {
                event.setCancelled(true);
                return;
            }

            if (!DuelManager.getInstance()
                .areInDuel(shooter.getUniqueId(), target.getUniqueId())) {
                event.setCancelled(true);
            }
            return;
        }

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

        if ("rogue".equals(cls)) {
            if (!WeaponType.isValidRogueWeapon(mainHand)) {
                return;
            }
        } else if ("warrior".equals(cls)) {
            if (!WeaponType.isValidWarriorWeapon(mainHand)) {
                return;
            }
        } else {
            return;
        }

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

        Location effectLoc = eye.clone().add(fwd.clone().multiply(2.0));
        world.spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 1, 0, 0, 0, 0);
        world.playSound(effectLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

        double baseAtk = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        int    stat    = cls.equals("warrior")
            ? StatsManager.getInstance().getStatValue(player, StatsManager.StatType.STR)
            : StatsManager.getInstance().getStatValue(player, StatsManager.StatType.AGI);
        double damage  = baseAtk + (stat * 0.5);

        boolean isCrit = Math.random() < 0.10; // 10% crit chance
        if (isCrit) damage *= 1.5;
        String attackName = isCrit ? "Critical Sweep Attack" : "Sweep Attack";

        double range     = 4.0;
        double halfAngle = Math.toRadians(60) / 2;
        for (Entity e : world.getNearbyEntities(player.getLocation(), range, range, range)) {
            if (!(e instanceof LivingEntity target) || target.equals(player)) continue;
            if (target instanceof Player p
                && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) {
                continue;
            }

            Vector toTarget = target.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .setY(0).normalize();
            if (fwd.angle(toTarget) <= halfAngle) {
                SpellUtils.dealWithChat(player, target, damage, attackName);
                Main.getInstance().getLogger().info(String.format(
                    "[CombatLog] %s → %s : %s for %.1f dmg",
                    player.getName(),
                    (target instanceof Player
                        ? ((Player) target).getName()
                        : target.getType().name()),
                    attackName,
                    damage
                ));
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
        // Debug: incoming combo and class
        String className = StatsManager.getInstance()
            .getPlayerStats(player.getUniqueId())
            .playerClass
            .name()
            .toLowerCase();
        Bukkit.getLogger().info(String.format(
            "[DBG] handleSpellCast -> class=%s, combo=%s",
            className, combo
        ));

        // 1) Lookup the Spell
        Spell spell = SpellManager.getInstance().getSpell(className, combo);
        if (spell == null) {
            Bukkit.getLogger().warning(String.format(
                "[DBG] No Spell found for class=%s, combo=%s",
                className, combo
            ));
            return;
        }
        Bukkit.getLogger().info(String.format(
            "[DBG] Found Spell id=%s, allowedWeapons=%s, levelReq=%d, cooldown=%ds",
            spell.getId(), spell.getAllowedWeapons(), spell.getLevelReq(), spell.getCooldownSeconds()
        ));

        // 2) Build the context
        SpellCastContext ctx = new SpellCastContext(spell, player);

        // 3) Apply rune modifiers/transforms
        List<Rune> runes = Main.getInstance()
            .getRunesManager()
            .getRunesForSpell(player, spell.getId());
        List<String> runeIds = runes.stream().map(Rune::getId).toList();
        Bukkit.getLogger().info(String.format(
            "[DBG] Equipped runes for %s: %s",
            spell.getId(), runeIds
        ));
        for (Rune rune : runes) {
            for (RuneEffect eff : rune.getEffects()) {
                if (eff.getType() == RuneEffect.Type.MODIFIER) {
                    ctx.addDamagePercent(eff.getBonusDamagePercent());
                    ctx.reduceCooldownPercent(eff.getCooldownReductionPercent());
                    eff.getExtraParams().forEach((k,v) -> ctx.putExtraParam(k, v, eff.getPriority()));
                } else if (eff.getType() == RuneEffect.Type.TRANSFORM) {
                    if (eff.getNewEffectKey() != null) ctx.addEffectKey(eff.getNewEffectKey());
                    eff.getExtraParams().forEach((k,v) -> ctx.putExtraParam(k, v, eff.getPriority()));
                }
            }
        }

        // 4) Debug: final effect keys
        List<String> effectKeys = ctx.getEffectKeys();
        Bukkit.getLogger().info(String.format(
            "[DBG] Effect keys to apply: %s",
            effectKeys
        ));

        // 5) Pre-cast checks: weapon, level, cooldown
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!spell.getAllowedWeapons().contains(mainHand.getType())) {
            player.sendMessage("§cYou must hold a valid " + className + " weapon!");
            return;
        }
        int playerLevel = StatsManager.getInstance().getLevel(player);
        if (playerLevel < spell.getLevelReq()) {
            player.sendMessage("§cYou are not high enough level for " + spell.getDisplayName());
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> cdMap = spellCooldowns
            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        if (cdMap.containsKey(spell.getId()) && now < cdMap.get(spell.getId())) {
            long secsLeft = (cdMap.get(spell.getId()) - now) / 1000;
            Bukkit.getLogger().info("[DBG] Spell on cooldown for " + secsLeft + "s");
            return;
        }

        // 6) Apply effects in order
        for (String key : effectKeys) {
            Bukkit.getLogger().info("[DBG] Applying effect -> " + key);
            SpellEffect effect = EffectRegistry.get(key);
            if (effect == null) {
                Bukkit.getLogger().warning("[DBG] No SpellEffect found for key -> " + key);
            } else {
                effect.apply(ctx);
            }
        }

        // 7) Recalculate stats & set cooldown
        StatsManager.getInstance().recalcDerivedStats(player);
        long nextUse = now + spell.getCooldownSeconds() * 1000L;
        cdMap.put(spell.getId(), nextUse);
    }





    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ClickSequence seq = playerCombos.get(uuid);
        if (seq != null) {
            seq.clear();
            playerCombos.remove(uuid);
        }
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // clear and remove any active combo
        ClickSequence seq = playerCombos.get(uuid);
        if (seq != null) {
            seq.clear();
            playerCombos.remove(uuid);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        // only care about clicks in the player's own inventory
        if (event.getClickedInventory() != player.getInventory()) return;

        int heldSlot = player.getInventory().getHeldItemSlot();
        if (event.getSlot() == heldSlot) {
            ClickSequence seq = playerCombos.remove(player.getUniqueId());
            if (seq != null) seq.clear();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int heldSlot = player.getInventory().getHeldItemSlot();
        for (int raw : event.getRawSlots()) {
            if (raw == heldSlot) {
                ClickSequence seq = playerCombos.remove(player.getUniqueId());
                if (seq != null) seq.clear();
                break;
            }
        }
    }

    public static String getActiveCombo(Player player) {
        ClickSequence seq = playerCombos.get(player.getUniqueId());
        if (seq == null) {
            return "";
        }

        long now = System.currentTimeMillis();
        if (now - seq.getLastClickTime() > MAX_COMBO_TIME) {
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
