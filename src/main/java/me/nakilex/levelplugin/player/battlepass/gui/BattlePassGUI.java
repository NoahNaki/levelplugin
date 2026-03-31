package me.nakilex.levelplugin.player.battlepass.gui;

import me.nakilex.levelplugin.player.battlepass.BattlePassProvider;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassEntry;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassReward;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassView;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Redesigned battle pass menu with a three row layout:
 * free rewards, progress bar, and premium rewards. The class relies on the
 * {@link BattlePassProvider} to source tier data and perform claiming logic.
 */
public class BattlePassGUI implements Listener {

    private static final int GUI_SIZE = 45;
    private static final String TITLE = TextUtil.centerInventoryTitle("Battle Pass");

    private static final int[] FREE_ROW = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] PROGRESS_ROW = {19, 20, 21, 22, 23, 24, 25};
    private static final int[] PREMIUM_ROW = {28, 29, 30, 31, 32, 33, 34};

    private static final int PREVIOUS_PAGE_SLOT = 18;
    private static final int NEXT_PAGE_SLOT = 26;
    private static final int SEASON_SLOT = 4;
    private static final int CLAIM_ALL_SLOT = 40;
    private static final int INFO_SLOT = 8;

    private final BattlePassProvider provider;
    private final Map<UUID, PageState> openMenus = new HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public BattlePassGUI(BattlePassProvider provider) {
        this.provider = provider;
    }

    /**
     * Open the menu at the player's currently stored page.
     */
    public void open(Player player) {
        PageState state = openMenus.get(player.getUniqueId());
        int page = state != null ? state.page() : 0;
        open(player, page);
    }

    /**
     * Open the menu to a specific page.
     */
    public void open(Player player, int page) {
        BattlePassView view = provider.view(player.getUniqueId());
        int maxPage = Math.max(0, (int) Math.ceil((double) view.entries().size() / FREE_ROW.length) - 1);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        Inventory inv = buildInventory();
        List<GuiWidget> widgets = buildWidgets(view, page);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), new PageState(view, page, inv));
    }

    private Inventory buildInventory() {
        return GuiBuilder.create(GUI_SIZE, TITLE)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .border()
                .build();
    }

    private List<GuiWidget> buildWidgets(BattlePassView view, int page) {
        List<GuiWidget> widgets = new ArrayList<>();
        int startIndex = page * FREE_ROW.length;
        int endIndex = Math.min(view.entries().size(), startIndex + FREE_ROW.length);
        for (int idx = startIndex; idx < endIndex; idx++) {
            int column = idx - startIndex;
            BattlePassEntry entry = view.entries().get(idx);
            widgets.add(new ActionWidget(FREE_ROW[column],
                    context -> createRewardIcon(entry, entry.freeReward(), view, false),
                    (click, context) -> handleRewardClick(context.player(), entry, false, view, page)));
            widgets.add(new ActionWidget(PROGRESS_ROW[column],
                    context -> createProgressPane(entry, view),
                    null));
            widgets.add(new ActionWidget(PREMIUM_ROW[column],
                    context -> createRewardIcon(entry, entry.premiumReward(), view, true),
                    (click, context) -> handleRewardClick(context.player(), entry, true, view, page)));
        }

        if (page > 0) {
            widgets.add(new ActionWidget(PREVIOUS_PAGE_SLOT,
                    context -> GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous Page"),
                    (click, context) -> open(context.player(), page - 1)));
        }
        if (endIndex < view.entries().size()) {
            widgets.add(new ActionWidget(NEXT_PAGE_SLOT,
                    context -> GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next Page"),
                    (click, context) -> open(context.player(), page + 1)));
        }
        widgets.add(new ActionWidget(SEASON_SLOT,
                context -> createSeasonItem(view),
                null));
        widgets.add(new ActionWidget(CLAIM_ALL_SLOT,
                context -> createClaimAllItem(view),
                (click, context) -> handleClaimAllClick(context.player(), page)));
        widgets.add(new ActionWidget(INFO_SLOT,
                context -> createXpInfoItem(context.player().getUniqueId()),
                null));
        return widgets;
    }

    private ItemStack createClaimAllItem(BattlePassView view) {
        ItemStack icon = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Claim All Available");
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Claim every unlocked reward");
            lore.add(ChatColor.GRAY + "for this pass in one click.");
            if (view.premiumActive()) {
                lore.add(ChatColor.AQUA + "Includes premium rewards.");
            } else {
                lore.add(ChatColor.DARK_GRAY + "Premium rewards require premium pass.");
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to claim all available", null));
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack createSeasonItem(BattlePassView view) {
        ItemStack icon = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Battle Pass Overview");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Season: " + ChatColor.YELLOW + view.seasonLabel());
            lore.add(ChatColor.GRAY + "Season Ends: " + ChatColor.YELLOW + view.seasonEnds());
            if (!view.timeRemaining().isBlank()) {
                lore.add(ChatColor.GRAY + "Time Remaining: " + ChatColor.YELLOW + view.timeRemaining());
            }
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Battle Pass Tier: " + ChatColor.YELLOW + view.currentTier()
                    + ChatColor.GRAY + "/" + ChatColor.YELLOW + view.totalTiers());
            if (view.requiredProgress() > 0) {
                lore.add(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + view.currentProgress()
                        + ChatColor.GRAY + "/" + ChatColor.YELLOW + view.requiredProgress());
                lore.add(TooltipUtil.progressBar(view.currentProgress(), view.requiredProgress(), 20));
            } else {
                lore.add(ChatColor.GRAY + "All tiers complete!");
            }
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Free Rewards Claimed: " + ChatColor.YELLOW
                    + view.claimedFreeRewards() + ChatColor.GRAY + "/" + ChatColor.YELLOW + view.totalTiers());
            if (view.premiumActive()) {
                lore.add(ChatColor.GRAY + "Premium Rewards Claimed: " + ChatColor.AQUA
                        + view.claimedPremiumRewards() + ChatColor.GRAY + "/" + ChatColor.AQUA + view.totalTiers());
            } else {
                lore.add(ChatColor.GRAY + "Premium Status: " + ChatColor.RED + "Locked");
            }
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack createXpInfoItem(UUID playerId) {
        ItemStack icon = GuiUtil.getNexoItem("info", ChatColor.AQUA + "Earning Battle Pass XP");
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Complete activities to earn XP:");
            lore.addAll(TooltipUtil.bulletList(
                    ChatColor.YELLOW + "Defeat mobs",
                    ChatColor.YELLOW + "Open loot chests",
                    ChatColor.YELLOW + "Discover fast travel points"
            ));
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Horse Challenges:");
            java.util.List<String> challenges = provider.activeChallenges(playerId);
            if (challenges.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "No active challenges.");
            } else {
                lore.addAll(challenges);
            }
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Level up to unlock additional rewards.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack createProgressPane(BattlePassEntry entry, BattlePassView view) {
        boolean unlocked = entry.tier() <= view.currentTier();
        ItemStack pane = unlocked
                ? GuiUtil.getNexoItem("check", ChatColor.GREEN + "Tier " + entry.tier() + " Progress")
                : GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.DARK_GRAY) + "Tier " + entry.tier() + " Progress");
            List<String> lore = new ArrayList<>();
            if (unlocked) {
                lore.add(ChatColor.GREEN + "Unlocked");
            } else if (entry.tier() == view.currentTier() + 1 && view.requiredProgress() > 0) {
                lore.add(ChatColor.GRAY + "Progress towards unlock:");
                lore.add(TooltipUtil.progressBar(view.currentProgress(), view.requiredProgress(), 20));
            } else {
                lore.add(ChatColor.RED + "Locked");
            }
            meta.setLore(lore);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createRewardIcon(BattlePassEntry entry, BattlePassReward reward, BattlePassView view, boolean premium) {
        Material material = reward.claimed() ? Material.MINECART : Material.CHEST_MINECART;
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            ChatColor baseColor = premium ? ChatColor.AQUA : ChatColor.GOLD;
            meta.setDisplayName(baseColor + "Tier " + entry.tier() + (premium ? " Premium" : " Free"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + (premium ? "Premium Reward" : "Free Reward"));
            if (!reward.title().isEmpty()) {
                lore.add(ChatColor.YELLOW + reward.title());
            }
            if (!reward.description().isEmpty()) {
                lore.add(" ");
                for (String line : reward.description()) {
                    lore.add(ChatColor.GRAY + line);
                }
            }
            lore.add(" ");
            if (reward.claimed()) {
                lore.add(ChatColor.GREEN + "Claimed");
            } else if (reward.claimable() && (!premium || view.premiumActive())) {
                lore.add(ChatColor.YELLOW + "Ready to claim!");
                lore.add(" ");
                lore.addAll(TooltipUtil.clickInstructions("to claim this reward", null));
            } else if (entry.tier() <= view.currentTier()) {
                if (premium && !view.premiumActive()) {
                    lore.add(ChatColor.RED + "Requires Premium Pass");
                } else {
                    lore.add(ChatColor.GRAY + "Unlocked - claim via quest/objectives");
                }
            } else {
                lore.add(ChatColor.RED + "Locked");
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private void handleRewardClick(Player player, BattlePassEntry entry, boolean premium, BattlePassView view, int page) {
        BattlePassReward reward = premium ? entry.premiumReward() : entry.freeReward();
        if (reward.claimed()) {
            ChatMessageUtil.send(player, MessageType.INFO, "You have already claimed this reward.");
            return;
        }
        if (entry.tier() > view.currentTier()) {
            ChatMessageUtil.send(player, MessageType.ERROR, "You have not unlocked this tier yet.");
            return;
        }
        if (premium && !view.premiumActive()) {
            ChatMessageUtil.send(player, MessageType.ERROR, "You need the premium pass to claim this reward.");
            return;
        }
        if (!reward.claimable()) {
            ChatMessageUtil.send(player, MessageType.WARNING, "Complete the tier objectives before claiming this reward.");
            return;
        }

        provider.claimReward(player, entry.tier(), premium);
        BattlePassView refreshed = provider.view(player.getUniqueId());
        int desiredPage = page;
        int maxPage = Math.max(0, (int) Math.ceil((double) refreshed.entries().size() / FREE_ROW.length) - 1);
        if (desiredPage > maxPage) {
            desiredPage = maxPage;
        }
        open(player, desiredPage);
    }

    private void handleClaimAllClick(Player player, int page) {
        int claimed = provider.claimAllAvailable(player);
        if (claimed <= 0) {
            return;
        }
        BattlePassView refreshed = provider.view(player.getUniqueId());
        int maxPage = Math.max(0, (int) Math.ceil((double) refreshed.entries().size() / FREE_ROW.length) - 1);
        open(player, Math.min(page, maxPage));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) {
            return;
        }
        openMenus.remove(event.getPlayer().getUniqueId());
        widgetsByPlayer.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
        widgetsByPlayer.remove(event.getPlayer().getUniqueId());
    }

    public void refresh() {
        Iterator<Map.Entry<UUID, PageState>> iterator = openMenus.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PageState> entry = iterator.next();
            UUID viewerId = entry.getKey();
            PageState state = entry.getValue();
            boolean stillOpen = false;
            for (HumanEntity viewer : state.inventory().getViewers()) {
                if (viewer.getUniqueId().equals(viewerId)) {
                    stillOpen = true;
                    break;
                }
            }
            if (!stillOpen) {
                iterator.remove();
                continue;
            }
            Player player = (Player) state.inventory().getViewers().stream()
                    .filter(h -> h.getUniqueId().equals(viewerId))
                    .findFirst()
                    .orElse(null);
            if (player == null) {
                iterator.remove();
                continue;
            }
            open(player, state.page());
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

    private record PageState(BattlePassView view,
                             int page,
                             Inventory inventory) { }
}
