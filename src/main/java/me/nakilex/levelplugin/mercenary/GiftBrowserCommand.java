package me.nakilex.levelplugin.mercenary;

import me.nakilex.levelplugin.mercenary.gui.MercenaryGiftBrowserGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Simple entry point for browsing mercenary gifts outside of /expedition. */
public class GiftBrowserCommand implements CommandExecutor {
    private final MercenaryGiftBrowserGUI giftBrowserGUI;

    public GiftBrowserCommand(MercenaryGiftBrowserGUI giftBrowserGUI) {
        this.giftBrowserGUI = giftBrowserGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        giftBrowserGUI.open(player);
        return true;
    }
}
