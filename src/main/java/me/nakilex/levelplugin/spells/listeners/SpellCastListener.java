package me.nakilex.levelplugin.spells.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.SpellAccessUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.input.SpellInputDisplayManager;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SpellCastListener implements Listener {
    private final Main plugin;

    public SpellCastListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpellInput(SpellInputEvent event) {
        Player player = event.getPlayer();
        if (plugin.getStagedDungeonManager() != null && plugin.getStagedDungeonManager().isInRun(player.getUniqueId())) {
            return;
        }
        SpellRegistry.SpellEntry entry = SpellDeckManager.getInstance().getEquippedSpellEntry(player, event.getInputType());
        boolean deckSpell = entry != null;
        var playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        if (entry == null) {
            entry = SpellRegistry.getInstance().resolveSpell(playerClass,
                    player.getInventory().getItemInMainHand(),
                    event.getInputMode(), event.getInputSequence(), event.getInputType());
        }
        if (entry == null) {
            return;
        }
        if (!deckSpell) {
            String resolvedBaseSpellId = entry.definition().id();
            if (event.getInputType() != SpellInputType.BASIC_ATTACK
                    && (playerClass == null || !SpellRegistry.getInstance().isSpellBoundForClass(resolvedBaseSpellId, playerClass))) {
                return;
            }
            String effectiveSpellId = SpellProgressionManager.getInstance()
                    .getEffectiveSpellId(player.getUniqueId(), entry.definition().id());
            SpellRegistry.SpellEntry effectiveEntry = SpellRegistry.getInstance().getSpell(effectiveSpellId);
            if (effectiveEntry != null) {
                entry = effectiveEntry;
            }
        }
        if (deckSpell) {
            if (!SpellAccessUtil.isHoldingWeapon(player)) {
                if (!SpellAccessUtil.isHoldingLifeSkillTool(player)) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "You must hold a weapon to cast spell cards.");
                }
                return;
            }
        } else if (event.getInputType() == SpellInputType.BASIC_ATTACK) {
            if (!SpellAccessUtil.isHoldingBasicAttackWeapon(player)) {
                return;
            }
        } else if (!SpellAccessUtil.isHoldingValidClassWeapon(player)) {
            if (!SpellAccessUtil.isHoldingLifeSkillTool(player) && SpellAccessUtil.isHoldingWeapon(player)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "You must hold a valid weapon to cast skills.");
            }
            return;
        }
        String requirementFailure = SpellAccessUtil.getHeldWeaponRequirementFailure(player);
        if (requirementFailure != null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, requirementFailure);
            return;
        }
        SpellCastManager castManager = SpellCastManager.getInstance();
        long remainingCooldown = castManager.getRemainingCooldownMs(player, entry.definition());
        if (SpellCastManager.areCooldownsEnabled() && remainingCooldown > 0L) {
            int seconds = (int) Math.ceil(remainingCooldown / 1000.0);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    entry.definition().displayName() + " is on cooldown for " + seconds + "s.");
            return;
        }
        int manaCost = castManager.getManaCost(player, entry.definition());
        var stats = me.nakilex.levelplugin.player.attributes.managers.StatsManager.getInstance()
                .getPlayerStats(player.getUniqueId());
        if (SpellCastManager.areManaCostsEnabled() && manaCost > 0 && stats.getCurrentMana() < manaCost) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Not enough mana for " + entry.definition().displayName() + " (" + manaCost + ").");
            return;
        }
        if (!castManager.tryConsumeResources(player, entry.definition())) {
            return;
        }
        entry.handler().cast(new SpellContext(plugin, player, entry.definition(), event));
        castManager.recordCast(player, entry.definition());
        SpellInputDisplayManager.getInstance().markSpellCast(player);
    }
}
