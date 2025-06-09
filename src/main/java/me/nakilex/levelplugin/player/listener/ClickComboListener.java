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
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityShootBowEvent;
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
    private final Map<UUID, Long> activeLeftClicks = new HashMap<>();
    private final Map<UUID, Long> bowCooldowns = new HashMap<>();
    private static final long BOW_SHOT_COOLDOWN = 500L; // 0.5 seconds
    private final Map<UUID, Long> quickdrawCooldowns = new HashMap<>();
    private final Map<UUID, ItemStack> offhandBackups = new HashMap<>();
    private final ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    private static final int OFFHAND_SLOT = 45;


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
        if ("archer".equals(className) &&
            me.nakilex.levelplugin.items.data.WeaponType.BOW
                .getMaterials().contains(mainHand.getType())) {
            if (!activeCombo.isEmpty()) {
                event.setCancelled(true);
                recordComboClick(player, "R");
                return;
            }

            if (hasQuickdrawRune(player)) {
                if (quickdrawReady(player)) {
                    event.setCancelled(true);
                    handleSpellCast(player, "BASIC_ATTACK");
                    setQuickdrawUsed(player);
                } else {
                    event.setCancelled(true);
                }
                return;
            }

            if (!player.getInventory().contains(Material.ARROW)) {
                giveTempArrow(player);
            }

            // allow vanilla drawing; shot handled in EntityShootBowEvent
            return;
        }

        // Default: record combo
        recordComboClick(player, "R");
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null ||
            !me.nakilex.levelplugin.items.data.WeaponType.BOW
                .getMaterials().contains(bow.getType())) return;

        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        if (ps.playerClass != PlayerClass.ARCHER) return;

        String activeCombo = getActiveCombo(player);
        event.setConsumeItem(false);

        if (!activeCombo.isEmpty()) {
            event.setCancelled(true);
            removeTempArrow(player);
            return;
        }

        event.setCancelled(true);
        handleSpellCast(player, "BASIC_ATTACK");
        removeTempArrow(player);
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
        String className = StatsManager.getInstance()
            .getPlayerStats(player.getUniqueId())
            .playerClass.name().toLowerCase();

        Spell spell = SpellManager.getInstance().getSpell(className, combo);
        if (spell == null) {
            Bukkit.getLogger().warning(String.format(
                "No Spell found for class=%s, combo=%s", className, combo));
            return;
        }

        // Pre-cast checks for weapon and level
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

        // Delegate casting logic to Spell which handles mana, runes and cooldown
        spell.castEffect(player);

        // Recalculate derived stats to reflect mana changes
        StatsManager.getInstance().recalcDerivedStats(player);
    }

    private boolean hasQuickdrawRune(Player player) {
        List<Rune> runes = Main.getInstance()
            .getRunesManager()
            .getRunesForSpell(player, "basic_arrow");
        for (Rune r : runes) {
            if ("archer_basic_shot_quickdraw".equalsIgnoreCase(r.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean quickdrawReady(Player player) {
        Long last = quickdrawCooldowns.get(player.getUniqueId());
        return last == null || System.currentTimeMillis() - last >= 1L;
    }

    private void setQuickdrawUsed(Player player) {
        quickdrawCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void giveTempArrow(Player player) {
        UUID id = player.getUniqueId();
        if (offhandBackups.containsKey(id)) return;
        ItemStack prev = player.getInventory().getItemInOffHand();
        offhandBackups.put(id, prev);
        player.getInventory().setItemInOffHand(new ItemStack(Material.ARROW, 1));
        sendOffhandVisual(player, prev);
    }

    private void removeTempArrow(Player player) {
        UUID id = player.getUniqueId();
        ItemStack prev = offhandBackups.remove(id);
        if (prev == null) return;
        player.getInventory().setItemInOffHand(prev);
        sendOffhandVisual(player, prev);
    }

    private void sendOffhandVisual(Player player, ItemStack item) {
        PacketContainer pkt = protocol.createPacket(PacketType.Play.Server.SET_SLOT);
        pkt.getIntegers().write(0, 0); // window id
        pkt.getIntegers().write(1, OFFHAND_SLOT);
        pkt.getItemModifier().write(0, item);
        try {
            protocol.sendServerPacket(player, pkt);
        } catch (Exception ignored) {}
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
        removeTempArrow(player);
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
        removeTempArrow(player);
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
        removeTempArrow(player);
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
                removeTempArrow(player);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeTempArrow(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        removeTempArrow(player);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (offhandBackups.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            removeTempArrow(player);
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
