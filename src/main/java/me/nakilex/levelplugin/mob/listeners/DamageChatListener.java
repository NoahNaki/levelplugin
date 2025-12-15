package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.utils.MythicEventUtil;
import me.nakilex.levelplugin.mob.utils.SweepAttack;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.MetadataValue;

public class DamageChatListener implements Listener {

    private static final String[] COMMON_METADATA_KEYS = new String[]{
            "Meteor", "BasicAttack", "ArcherSpell", "Shockwave", SweepAttack.SWEEP_META
    };

    private final boolean debugDamageMetadata = Main.getInstance()
            .getCustomConfig()
            .getBoolean("debug.damage-metadata", false);

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity rawDamager = event.getDamager();
        Player player = null;
        String spellName = null;
        boolean isCrit = false;

        // 1) Projectile-based spells & basic‐attack arrows
        if (rawDamager instanceof Projectile) {
            Projectile proj = (Projectile) rawDamager;
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();

                debugDamager("EntityDamageByEntityEvent", proj, player);

                SpellContextManager.Context ctx =
                        SpellContextManager.peek(player.getUniqueId());
                if (ctx != null) {
                    spellName = ctx.spellName;
                    isCrit = ctx.isCrit;
                    SpellContextManager.consume(player.getUniqueId());
                } else if (proj.hasMetadata("Meteor")) {
                    spellName = "Meteor";
                } else if (proj.hasMetadata("BasicAttack")) {
                    spellName = "Basic Attack";
                }
            }
        }
        // 2) Direct-damage via SpellContextManager or melee basic‐attack
        else if (rawDamager instanceof Player) {
            player = (Player) rawDamager;

            debugDamager("EntityDamageByEntityEvent", player, player);

            // a) consume any spell context
            SpellContextManager.Context ctx =
                SpellContextManager.peek(player.getUniqueId());

            if (ctx != null) {
                spellName = ctx.spellName;
                isCrit    = ctx.isCrit;
                SpellContextManager.consume(player.getUniqueId());
            } else {
                // b) no spell → check for Warrior/Rogue basic melee
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
                String className = ps.playerClass.name().toLowerCase();
                if ("warrior".equals(className) || "rogue".equals(className)) {
                    spellName = "Basic Attack";
                    isCrit = StatsEffectListener.consumeLastCrit(player);
                }
            }
        }

        // nothing to do if not a player spell/attack
        if (player == null || spellName == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        SpellUtils.maybeSendDamageChat(player, target, event.getFinalDamage(), spellName, isCrit);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicDamage(MythicDamageEvent event) {
        Player player = MythicEventUtil.resolvePlayer(event);
        if (player == null) return;

        Entity damager = MythicEventUtil.resolveDamager(event);
        if (damager instanceof Player) return;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player) return;

        debugDamager("MythicDamageEvent", damager, player);

        LivingEntity target = MythicEventUtil.resolveTarget(event);
        if (target == null) return;

        SpellContextManager.Context ctx = SpellContextManager.consume(player.getUniqueId());
        if (ctx == null) return;

        StatsEffectListener.recordCrit(player, ctx.isCrit);
        SpellUtils.maybeSendDamageChat(player, target, event.getDamage(), ctx.spellName, ctx.isCrit);
    }

    private void debugDamager(String source, Entity damager, Player shooter) {
        if (!debugDamageMetadata || damager == null) {
            return;
        }

        StringBuilder metaDescription = new StringBuilder();
        for (String key : COMMON_METADATA_KEYS) {
            if (damager.hasMetadata(key)) {
                String value = describeMetadataValues(damager.getMetadata(key));
                metaDescription.append(key).append("=").append(value).append(", ");
            }
        }

        String pdcKeys = damager.getPersistentDataContainer()
                .getKeys()
                .stream()
                .map(NamespacedKey::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        String scoreboardTags = damager.getScoreboardTags().isEmpty()
                ? "none"
                : String.join(", ", damager.getScoreboardTags());

        String shooterInfo = shooter != null ? shooter.getName() : "n/a";
        String metaInfo = metaDescription.length() > 0
                ? metaDescription.substring(0, metaDescription.length() - 2)
                : "none";

        SpellContextManager.Context ctx = shooter != null
                ? SpellContextManager.peek(shooter.getUniqueId())
                : null;
        String spellInfo = ctx != null
                ? ctx.spellName + " (crit=" + ctx.isCrit + ", basic=" + ctx.basicAttack + ")"
                : "none";

        Main.getInstance().getLogger().info(
                "[DamageDebug] source=" + source +
                        " damager=" + damager.getType() +
                        " class=" + damager.getClass().getSimpleName() +
                        " shooter=" + shooterInfo +
                        " metadata={" + metaInfo + "}" +
                        " spellCtx={" + spellInfo + "}" +
                        " pdcKeys={" + pdcKeys + "}" +
                        " scoreboardTags={" + scoreboardTags + "}");
    }

    private String describeMetadataValues(java.util.List<MetadataValue> values) {
        if (values == null || values.isEmpty()) {
            return "true";
        }
        StringBuilder sb = new StringBuilder();
        for (MetadataValue value : values) {
            if (sb.length() > 0) sb.append("|");
            try {
                sb.append(value.asString());
            } catch (Exception ex) {
                sb.append("?");
            }
        }
        return sb.toString();
    }

}
