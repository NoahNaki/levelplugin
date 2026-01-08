package me.nakilex.levelplugin.mob.custom;

import me.nakilex.levelplugin.mob.listeners.MythicMobDamageTracker;
import me.nakilex.levelplugin.mob.utils.CombatPowerUtil;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.mob.utils.MobRewardService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Set;

public class CustomMobRewardListener implements Listener {
    private final CustomMobManager mobManager;
    private final MythicMobDamageTracker tracker;
    private final MobRewardService rewardService;

    public CustomMobRewardListener(CustomMobManager mobManager,
                                   MythicMobDamageTracker tracker,
                                   MobRewardService rewardService) {
        this.mobManager = mobManager;
        this.tracker = tracker;
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        var instanceOpt = mobManager.getInstance(entity);
        if (instanceOpt.isEmpty()) {
            return;
        }
        CustomMobInstance instance = instanceOpt.get();
        Set<Player> participants = tracker.getParticipantsAndClear(entity.getUniqueId());
        if (participants.isEmpty() && entity.getKiller() instanceof Player killer) {
            participants = Set.of(killer);
        }
        int combatPower = CombatPowerUtil.getCombatPower(entity, instance.level());
        boolean numericHpName = MobNameUtil.hasNumericHealth(entity);
        MobRewardService.DebugInfo debugInfo = new MobRewardService.DebugInfo(
                entity.getType().name(),
                entity.getType() + " (" + entity.getClass().getSimpleName() + ")",
                numericHpName
        );
        MobRewardService.MobRewardContext context = new MobRewardService.MobRewardContext(
                instance.id(),
                instance.definition().displayName(),
                instance.level(),
                combatPower,
                entity,
                participants,
                debugInfo
        );
        rewardService.awardRewards(context);
        mobManager.remove(entity.getUniqueId());
    }
}
