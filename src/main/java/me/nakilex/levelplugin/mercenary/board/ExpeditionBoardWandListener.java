package me.nakilex.levelplugin.mercenary.board;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Allows builders to register expedition board locations using an in-game wand.
 */
public class ExpeditionBoardWandListener implements Listener {

    private final ExpeditionBoardManager boardManager;

    public ExpeditionBoardWandListener(ExpeditionBoardManager boardManager) {
        this.boardManager = boardManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack stack = event.getItem();
        if (!boardManager.isWand(stack)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Location loc = event.getClickedBlock().getLocation();
            BlockFace facing = player.getFacing();
            ExpeditionBoardLocation board = boardManager.registerBoard(loc, facing);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Registered expedition board #" + board.id() + " facing " + facing.name()
                            + " at " + formattedCoords(loc));
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Location reference = event.getClickedBlock().getLocation();
            ExpeditionBoardLocation nearest = boardManager.findNearest(reference);
            if (nearest == null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "No expedition boards found to delete in this world.");
                return;
            }

            boardManager.deleteBoard(nearest.id());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Deleted expedition board #" + nearest.id() + " at " + formattedCoords(nearest.toLocation()));
        }
    }

    private String formattedCoords(Location location) {
        if (location == null) {
            return "unknown location";
        }
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }
}
