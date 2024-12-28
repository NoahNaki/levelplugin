package me.nakilex.levelplugin.trade.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerRightClicksPlayerListener implements Listener {
    @EventHandler
    public void onPlayerInteracts(PlayerInteractEntityEvent e) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
        ConfigValues configValues = Main.getPlugin().getConfigValues();
        Player p = e.getPlayer();
        if(e.getRightClicked() instanceof Player) {
            if(configValues.ENABLE_TRADE_BY_RIGHTCLICK_PLAYER && (configValues.toggleUseWithoutPermission()
                || p.hasPermission("trade.tradebyclick")
                || p.hasPermission("trade.*"))) {

                if(configValues.REQUIRE_SHIFT_CLICK && !p.isSneaking()) return;

                Player target = (Player) e.getRightClicked();
                DealMaker dm = Main.getPlugin().getDealMaker();
                if(dm.addPlayerToCooldown(p)) {
                    if(dm.madePlayerARequest(target, p)) {
                        dm.acceptTrade(p, target);
                    } else {
                        boolean success = dm.makeTradeOffer(p, target);
                        if(success)
                            p.sendMessage(Main.PREFIX + String.format(
                                messageStrings.getTranslation(Translations.TRADE_REQUEST_SENT), target.getName()));
                    }
                }
            }
        }
    }
}