package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryFriendship;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Displays friendship progress for a specific mercenary NPC. */
public class MercenaryFriendshipGUI implements Listener {
    private static final int SIZE = 27;

    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;
    private MercenaryExpeditionGUI expeditionGUI;
    private final Map<UUID, Consumer<Player>> backActions = new HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public MercenaryFriendshipGUI(Plugin plugin, MercenaryAffinityManager affinityManager) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setExpeditionGUI(MercenaryExpeditionGUI expeditionGUI) {
        this.expeditionGUI = expeditionGUI;
    }

    public void open(Player player, int npcId, String npcName) {
        Consumer<Player> defaultBack = p -> {
            if (expeditionGUI != null) {
                expeditionGUI.open(p);
            } else {
                p.closeInventory();
            }
        };
        openWithBack(player, npcId, npcName, defaultBack);
    }

    public void openWithBack(Player player, int npcId, String npcName, Consumer<Player> backAction) {
        if (backAction != null) {
            backActions.put(player.getUniqueId(), backAction);
        } else {
            backActions.remove(player.getUniqueId());
        }
        String title = npcName + " Affinity";
        GuiBuilder builder = GuiBuilder.create(SIZE, title).border();
        MercenaryFriendship friendship = affinityManager.getFriendship(player.getUniqueId(), npcId);

        Inventory inventory = builder.build();
        List<GuiWidget> widgets = buildWidgets(player, npcName, friendship);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inventory, player, widgets);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().contains("Affinity")) {
            return;
        }
        if (handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() != null) {
            player.sendMessage(ChatColor.GRAY + "Interact with gifts to raise friendship.");
        }
    }

    private List<GuiWidget> buildWidgets(Player player, String npcName, MercenaryFriendship friendship) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(11,
                context -> createPortraitItem(npcName, friendship),
                null));
        widgets.add(new ActionWidget(15,
                context -> createBenefitsItem(friendship),
                null));
        widgets.add(new ActionWidget(18,
                context -> GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Back"),
                (click, context) -> handleBackClick(context.player())));
        return widgets;
    }

    private ItemStack createPortraitItem(String npcName, MercenaryFriendship friendship) {
        ItemStack portrait = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = portrait.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + npcName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Friendship Level: " + ChatColor.GREEN + friendship.getLevel());
            int current = friendship.getPoints();
            int nextThreshold = affinityManager.thresholdForLevel(Math.min(5, friendship.getLevel() + 1));
            lore.add(ChatColor.GRAY + "Progress: " + TooltipUtil.progressBar(current, Math.max(nextThreshold, 1), 12));
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + current + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + nextThreshold);
            meta.setLore(lore);
            portrait.setItemMeta(meta);
        }
        return portrait;
    }

    private ItemStack createBenefitsItem(MercenaryFriendship friendship) {
        ItemStack benefits = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta bMeta = benefits.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ChatColor.AQUA + "Level Benefits");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Perks by level:");
            for (int lvl = 1; lvl <= 5; lvl++) {
                boolean unlocked = friendship.getLevel() >= lvl;
                String prefix = unlocked ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ";
                List<String> perks = affinityManager.getBenefits(lvl);
                if (perks.isEmpty()) {
                    lore.add(prefix + ChatColor.GRAY + "Level " + lvl + ChatColor.DARK_GRAY + ": " + ChatColor.GRAY + "No perks");
                    continue;
                }
                for (String perk : perks) {
                    lore.add(prefix + ChatColor.GRAY + "Level " + lvl + ChatColor.DARK_GRAY + ": " + ChatColor.RESET + perk);
                    prefix = ChatColor.DARK_GRAY + "• " + ChatColor.GRAY;
                }
            }
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Higher levels unlock new perks.");
            bMeta.setLore(lore);
            benefits.setItemMeta(bMeta);
        }
        return benefits;
    }

    private void handleBackClick(Player player) {
        Consumer<Player> back = backActions.get(player.getUniqueId());
        if (back != null) {
            back.accept(player);
        } else if (expeditionGUI != null) {
            expeditionGUI.open(player);
        } else {
            player.closeInventory();
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }
}
