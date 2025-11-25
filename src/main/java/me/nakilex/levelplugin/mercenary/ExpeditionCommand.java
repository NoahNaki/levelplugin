package me.nakilex.levelplugin.mercenary;

import me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionGUI;
import me.nakilex.levelplugin.mercenary.gui.MercenaryFriendshipGUI;
import me.nakilex.levelplugin.mercenary.gui.MercenaryGiftBrowserGUI;
import me.nakilex.levelplugin.mercenary.MercenaryGift;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Entry point for the /expedition command pipeline. */
public class ExpeditionCommand implements CommandExecutor {
    private final MercenaryAffinityManager affinityManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final MercenaryGiftBrowserGUI giftBrowserGUI;
    private final MercenaryFriendshipGUI friendshipGUI;
    private final MercenaryExpeditionGUI expeditionGUI;

    public ExpeditionCommand(MercenaryAffinityManager affinityManager,
                             MercenaryExpeditionManager expeditionManager,
                             MercenaryGiftBrowserGUI giftBrowserGUI,
                             MercenaryFriendshipGUI friendshipGUI,
                             MercenaryExpeditionGUI expeditionGUI) {
        this.affinityManager = affinityManager;
        this.expeditionManager = expeditionManager;
        this.giftBrowserGUI = giftBrowserGUI;
        this.friendshipGUI = friendshipGUI;
        this.expeditionGUI = expeditionGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /expedition <npcId> | /expedition giftbrowser");
            return true;
        }

        if ("giftbrowser".equalsIgnoreCase(args[0])) {
            giftBrowserGUI.open(player);
            return true;
        }

        try {
            int npcId = Integer.parseInt(args[0]);
            affinityManager.loadPlayer(player.getUniqueId());
            if (args.length >= 2 && "gift".equalsIgnoreCase(args[1])) {
                MercenaryGift gift = affinityManager.matchGift(player.getInventory().getItemInMainHand());
                if (gift == null) {
                    player.sendMessage(ChatColor.RED + "Hold a mercenary gift in your main hand.");
                    return true;
                }
                affinityManager.addAffinity(player, npcId, gift.getAffinityValue());
                int remaining = player.getInventory().getItemInMainHand().getAmount() - 1;
                if (remaining <= 0) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    player.getInventory().getItemInMainHand().setAmount(remaining);
                }
                player.sendMessage(ChatColor.GREEN + "Gave a gift to mercenary " + npcId + ".");
                return true;
            }
            if (args.length >= 2 && "affinity".equalsIgnoreCase(args[1])) {
                String name = args.length >= 3 ? args[2] : "Mercenary " + npcId;
                friendshipGUI.open(player, npcId, name);
                return true;
            }
            if (affinityManager.getFriendship(player.getUniqueId(), npcId).getLevel() < 3) {
                player.sendMessage(ChatColor.RED + "Increase your friendship to level 3 to unlock expeditions with this mercenary.");
                return true;
            }
            expeditionGUI.open(player, npcId);
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "First argument must be an NPC id or 'giftbrowser'.");
            return true;
        }
    }
}
