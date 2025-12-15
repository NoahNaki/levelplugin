package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.utils.MythicEventUtil;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
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
        boolean skipChat = false;

        debugVanillaDamage(event);

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

        // 3) Mythic anchors/minions that carry an owner in PDC
        else {
            player = MythicEventUtil.resolveOwnerPlayer(rawDamager);
            if (player != null) {
                debugDamager("EntityDamageByEntityEvent", rawDamager, player);
                SpellContextManager.Context ctx = SpellContextManager.peek(player.getUniqueId());
                if (ctx != null) {
                    spellName = ctx.spellName;
                    isCrit = ctx.isCrit;
                }
                skipChat = MythicEventUtil.hasMythicOwner(rawDamager);
            }
        }

        // nothing to do if not a player spell/attack
        if (skipChat || player == null || spellName == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        SpellUtils.maybeSendDamageChat(player, target, event.getFinalDamage(), spellName, isCrit);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicDamage(MythicDamageEvent event) {
        Player player = MythicEventUtil.resolvePlayer(event);
        Entity damager = MythicEventUtil.resolveDamager(event);

        debugMythicEvent(event, player, damager);

        if (player == null) return;
        if (damager instanceof Player) return;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player) return;

        LivingEntity target = MythicEventUtil.resolveTarget(event);
        if (target == null) return;

        SpellContextManager.Context ctx = SpellContextManager.peek(player.getUniqueId());
        if (ctx == null) return;

        StatsEffectListener.recordCrit(player, ctx.isCrit);
        SpellUtils.maybeSendDamageChat(player, target, event.getDamage(), ctx.spellName, ctx.isCrit);
    }

    private void debugDamager(String source, Entity damager, Player shooter) {
        if (!debugDamageMetadata || damager == null) {
            return;
        }

        SpellContextManager.Context ctx = shooter != null
                ? SpellContextManager.peek(shooter.getUniqueId())
                : null;

        Main.getInstance().getLogger().info(
                "[DamageDebug] source=" + source +
                        " damager={" + describeEntity(damager) + "}" +
                        " shooter=" + (shooter != null ? shooter.getName() : "n/a") +
                        " spellCtx={" + describeSpellCtx(ctx) + "}");
    }

    private void debugMythicEvent(MythicDamageEvent event, Player player, Entity damager) {
        if (!debugDamageMetadata) {
            return;
        }

        LivingEntity target = MythicEventUtil.resolveTarget(event);
        Object trigger = safelyInvoke(event, "getTrigger");
        Object shooter = safelyInvoke(event, "getShooter");
        Object rawDamager = safelyInvoke(event, "getDamager");
        Object rawSource = safelyInvoke(event, "getSource");
        Object rawAttacker = safelyInvoke(event, "getAttacker");

        String skillName = String.valueOf(safelyInvoke(event, "getSkillName"));
        String casterInfo = describeParticipant(event.getCaster());
        String triggerInfo = describeParticipant(trigger);
        String shooterInfo = describeParticipant(shooter);
        String damagerInfo = describeParticipant(rawDamager);
        String sourceInfo = describeParticipant(rawSource);
        String attackerInfo = describeParticipant(rawAttacker);

        SpellContextManager.Context ctx = player != null
                ? SpellContextManager.peek(player.getUniqueId())
                : null;

        Main.getInstance().getLogger().info(
                "[DamageDebug] MythicDamageEvent skill=" + skillName +
                        " baseDamage=" + event.getDamage() +
                        " player=" + (player == null ? "n/a" : player.getName()) +
                        " caster=" + casterInfo +
                        " trigger=" + triggerInfo +
                        " shooter=" + shooterInfo +
                        " rawDamager=" + damagerInfo +
                        " rawSource=" + sourceInfo +
                        " rawAttacker=" + attackerInfo +
                        " resolvedDamager={" + describeEntity(damager) + "}" +
                        " target={" + describeEntity(target) + "}" +
                        " spellCtx={" + describeSpellCtx(ctx) + "}");
    }

    private void debugVanillaDamage(EntityDamageByEntityEvent event) {
        if (!debugDamageMetadata) {
            return;
        }

        Entity damager = event.getDamager();
        LivingEntity target = event.getEntity() instanceof LivingEntity le ? le : null;

        Entity shooter = null;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooterEntity) {
            shooter = shooterEntity;
        }

        Main.getInstance().getLogger().info(
                "[DamageDebug] EntityDamageByEntityEvent damager={" + describeEntity(damager) + "}" +
                        " shooter={" + describeEntity(shooter) + "}" +
                        " target={" + describeEntity(target) + "}" +
                        " damage=" + event.getFinalDamage() +
                        " cause=" + event.getCause());
    }

    private String describeParticipant(Object obj) {
        Entity entity = MythicMobModifier.toBukkitEntity(obj);
        if (entity != null) {
            return describeEntity(entity);
        }
        if (obj == null) {
            return "null";
        }
        return obj.getClass().getSimpleName();
    }

    private String describeEntity(Entity entity) {
        if (entity == null) return "null";

        StringBuilder metaDescription = new StringBuilder();
        for (String key : COMMON_METADATA_KEYS) {
            if (entity.hasMetadata(key)) {
                String value = describeMetadataValues(entity.getMetadata(key));
                metaDescription.append(key).append("=").append(value).append(", ");
            }
        }
        String metaInfo = metaDescription.length() > 0
                ? metaDescription.substring(0, metaDescription.length() - 2)
                : "none";

        String pdcKeys = entity.getPersistentDataContainer()
                .getKeys()
                .stream()
                .map(NamespacedKey::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        String scoreboardTags = entity.getScoreboardTags().isEmpty()
                ? "none"
                : String.join(", ", entity.getScoreboardTags());

        return entity.getType() + " class=" + entity.getClass().getSimpleName() +
                " metadata=" + metaInfo +
                " pdc=" + pdcKeys +
                " tags=" + scoreboardTags;
    }

    private String describeSpellCtx(SpellContextManager.Context ctx) {
        return ctx == null
                ? "none"
                : ctx.spellName + " (crit=" + ctx.isCrit + ", basic=" + ctx.basicAttack + ")";
    }

    private Object safelyInvoke(Object target, String method) {
        if (target == null) return null;
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (Exception ignore) {
            return null;
        }
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
