package me.nakilex.levelplugin.mercenary;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionGUI;
import me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI;
import me.nakilex.levelplugin.mercenary.gui.MercenaryFriendshipGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Entry point for the /expedition command pipeline. */
public class ExpeditionCommand implements CommandExecutor {
    private final Main plugin;
    private final MercenaryAffinityManager affinityManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final MercenaryFriendshipGUI friendshipGUI;
    private final MercenaryExpeditionGUI expeditionGUI;
    private final MercenaryExpeditionRewardsGUI rewardsGUI;

    public ExpeditionCommand(Main plugin,
                             MercenaryAffinityManager affinityManager,
                             MercenaryExpeditionManager expeditionManager,
                             MercenaryFriendshipGUI friendshipGUI,
                             MercenaryExpeditionGUI expeditionGUI,
                             MercenaryExpeditionRewardsGUI rewardsGUI) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        this.expeditionManager = expeditionManager;
        this.friendshipGUI = friendshipGUI;
        this.expeditionGUI = expeditionGUI;
        this.rewardsGUI = rewardsGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            expeditionGUI.open(player);
            return true;
        }

        if ("rewards".equalsIgnoreCase(args[0])) {
            rewardsGUI.open(player, MercenaryExpeditionRewardsGUI.RewardView.EXPEDITIONS);
            return true;
        }

        try {
            int npcId = Integer.parseInt(args[0]);
            affinityManager.loadPlayer(player.getUniqueId());
            if (args.length >= 2 && "gift".equalsIgnoreCase(args[1])) {
                affinityManager.handGift(player, npcId, "Mercenary " + npcId);
                return true;
            }
            if (args.length >= 2 && "affinity".equalsIgnoreCase(args[1])) {
                String name = args.length >= 3 ? args[2] : "Mercenary " + npcId;
                friendshipGUI.open(player, npcId, name);
                return true;
            }
            expeditionGUI.open(player);
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Usage: /expedition [rewards|<npcId>]");
            return true;
        }
    }
}
