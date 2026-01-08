package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.utils.CombatPowerUtil;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.mob.utils.MobRewardService;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import java.util.Set;

/**
 * Awards XP, coins and loot to players that participated in killing MythicMobs.
 */
public class MythicMobRewardListener implements Listener {
    private final BukkitAPIHelper mythicHelper = MythicBukkit.inst().getAPIHelper();
    private final MythicMobDamageTracker tracker;
    private final MobRewardService rewardService;

    public MythicMobRewardListener(MythicMobDamageTracker tracker,
                                   MobRewardService rewardService) {
        this.tracker = tracker;
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mythicMob == null) return;

        String rawMobType = mythicMob.getMobType().replaceAll("§.", "");
        Set<Player> participants = tracker.getParticipantsAndClear(event.getEntity().getUniqueId());
        if (participants.isEmpty() && event.getEntity().getKiller() instanceof Player killer) {
            participants = Set.of(killer);
        }
        Entity baseEntity = mythicMob.getEntity().getBukkitEntity();
        if (!(baseEntity instanceof LivingEntity livingEntity)) {
            return;
        }
        boolean numericHpName = MobNameUtil.hasNumericHealth(livingEntity);

        PlaceholderString name = mythicMob.getType().getDisplayName();
        String display = name != null ? name.get() : rawMobType;
        int combatPower = CombatPowerUtil.getCombatPower(mythicMob);
        int mobLevel = (int) Math.round(mythicMob.getLevel());

        MobRewardService.DebugInfo debugInfo = new MobRewardService.DebugInfo(
                mythicMob.getType().getEntityType().name(),
                baseEntity.getType() + " (" + baseEntity.getClass().getSimpleName() + ")",
                numericHpName
        );
        MobRewardService.MobRewardContext context = new MobRewardService.MobRewardContext(
                rawMobType,
                display,
                mobLevel,
                combatPower,
                livingEntity,
                participants,
                debugInfo
        );
        rewardService.awardRewards(context);
    }
}
