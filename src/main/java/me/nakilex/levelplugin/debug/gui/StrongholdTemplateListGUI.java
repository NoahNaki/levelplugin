package me.nakilex.levelplugin.debug.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdGeneratorService;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Simple browser for stronghold templates with left-click teleport support. */
public class StrongholdTemplateListGUI implements Listener {
    private static final String TITLE = "Stronghold Templates";

    private final StrongholdGeneratorService strongholdGeneratorService;
    private final Map<UUID, Map<Integer, StrongholdGeneratorService.TemplateTeleportTarget>> openSlots = new HashMap<>();

    public StrongholdTemplateListGUI(Main plugin, StrongholdGeneratorService strongholdGeneratorService) {
        this.strongholdGeneratorService = strongholdGeneratorService;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        List<StrongholdGeneratorService.TemplateTeleportTarget> targets =
                strongholdGeneratorService.getTemplateTeleportTargets(player.getWorld());

        Inventory inventory = GuiBuilder.create(54, TITLE)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .border()
                .build();

        Map<Integer, StrongholdGeneratorService.TemplateTeleportTarget> slotTargets = new HashMap<>();
        int[] slots = GuiUtil.PAGED_SLOTS;
        for (int i = 0; i < Math.min(slots.length, targets.size()); i++) {
            StrongholdGeneratorService.TemplateTeleportTarget target = targets.get(i);
            slotTargets.put(slots[i], target);
            inventory.setItem(slots[i], buildTemplateItem(target));
        }
        openSlots.put(player.getUniqueId(), slotTargets);
        player.openInventory(inventory);
    }

    private ItemStack buildTemplateItem(StrongholdGeneratorService.TemplateTeleportTarget target) {
        Location loc = target.teleportLocation();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + (loc.getWorld() == null ? "unknown" : loc.getWorld().getName()));
        lore.add(ChatColor.GRAY + "X: " + ChatColor.WHITE + loc.getBlockX()
                + ChatColor.GRAY + " Y: " + ChatColor.WHITE + loc.getBlockY()
                + ChatColor.GRAY + " Z: " + ChatColor.WHITE + loc.getBlockZ());
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to teleport", null));
        return GuiUtil.createGuiItem(Material.STRUCTURE_BLOCK,
                ChatColor.GOLD + target.id(), lore);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        Map<Integer, StrongholdGeneratorService.TemplateTeleportTarget> slotTargets = openSlots.get(player.getUniqueId());
        if (slotTargets == null) {
            return;
        }
        StrongholdGeneratorService.TemplateTeleportTarget target = slotTargets.get(event.getRawSlot());
        if (target == null || event.getClick() != ClickType.LEFT) {
            return;
        }
        player.closeInventory();
        player.teleport(target.teleportLocation());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Teleported to template " + target.id() + ".");
    }
}
