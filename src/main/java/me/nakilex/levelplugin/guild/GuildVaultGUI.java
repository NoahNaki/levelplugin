package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.storage.gui.StorageGUI;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.IntegerInputPrompt;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Extension of {@link StorageGUI} for guild vaults that adds a coin
 * placeholder item allowing deposit and withdrawal of abstract coins.
 */
public class GuildVaultGUI extends StorageGUI {
    private static final int COIN_SLOT = 4;
    private static final int BACK_SLOT = 0;
    private final String guildName;
    private final GuildMemberGUI memberGUI;
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public GuildVaultGUI(String guildName, StorageEvents events, GuildMemberGUI memberGUI) {
        super(guildName.toLowerCase(), "guildvault", "guild_", "Guild Storage", events, false, 1);
        this.guildName = guildName;
        this.memberGUI = memberGUI;
    }

    @Override
    public void open(Player player) {
        Guild g = GuildManager.getInstance().getGuildIgnoreCase(guildName);
        if (g != null) setMaxPages(g.getMaxPages());
        super.open(player);
        Inventory inv = player.getOpenInventory().getTopInventory();
        List<GuiWidget> widgets = buildWidgets();
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
    }

    private ItemStack createCoinItem() {
        Guild g = GuildManager.getInstance().getGuildIgnoreCase(guildName);
        int coins = g != null ? g.getCoins() : 0;
        int capacity = g != null ? g.getCoinCapacity() : 0;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Guild Coins: " + ChatColor.GOLD + coins + ChatColor.GRAY + "/" + ChatColor.GOLD + capacity + " <glyph:coins_icon>");
        lore.addAll(TooltipUtil.clickInstructions("to deposit", "to withdraw"));
        return GuiUtil.createGuiItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Guild Coins", lore);
    }

    @Override
    protected ItemStack createInfoItem() {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(
                    ChatColor.GRAY + "Shared guild storage.",
                    ChatColor.GRAY + "Use arrows to change pages."));
            info.setItemMeta(meta);
        }
        return info;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (handleWidgetClick(event)) {
            return;
        }
        super.handleClick(event);
    }

    @Override
    public void saveToDisk() {
        for (Inventory page : getPages()) {
            page.setItem(COIN_SLOT, null);
            page.setItem(BACK_SLOT, null);
        }
        super.saveToDisk();
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(BACK_SLOT,
                context -> GuiUtil.getNexoItem("arrow_left2", ChatColor.GRAY + "Back"),
                (click, context) -> handleBackClick(context.player())));
        widgets.add(new ActionWidget(COIN_SLOT,
                context -> createCoinItem(),
                (click, context) -> handleCoinClick(context.player(), click)));
        return widgets;
    }

    private void handleBackClick(Player player) {
        if (memberGUI != null) {
            memberGUI.open(player);
        }
    }

    private void handleCoinClick(Player player, org.bukkit.event.inventory.ClickType clickType) {
        Main.getInstance().getLogger().info("[GuildVault] coin slot clicked by " + player.getName());
        Guild g = GuildManager.getInstance().getGuildIgnoreCase(guildName);
        if (g == null) {
            Main.getInstance().getLogger().warning("[GuildVault] guild not found for name " + guildName);
            return;
        }
        EconomyManager econ = Main.getInstance().getEconomyManager();
        if (clickType.isLeftClick()) {
            player.closeInventory();
            Main.getInstance().getLogger().info("[GuildVault] opening deposit prompt for " + player.getName());
            ConversationFactory factory = new ConversationFactory(Main.getInstance())
                    .withFirstPrompt(IntegerInputPrompt.coinAmountWithinBalance(
                            Main.getInstance(),
                            player,
                            ChatColor.GOLD + "Enter amount to deposit:",
                            () -> econ.getBalance(player),
                            true,
                            amt -> {
                                Main.getInstance().getLogger().info("[GuildVault] depositing " + amt + " for " + player.getName());
                                econ.deductCoins(player, amt);
                                int before = g.getCoins();
                                g.addCoins(amt);
                                int added = g.getCoins() - before;
                                if (added < amt) {
                                    int refund = amt - added;
                                    econ.addCoins(player, refund, false);
                                    player.sendMessage(ChatColor.RED + "Storage full. Deposited " + ChatColor.GOLD + added + " <glyph:coins_icon>" + ChatColor.RED + " and refunded " + refund + ".");
                                    Main.getInstance().getLogger().info("[GuildVault] storage full, refunded " + refund);
                                } else {
                                    player.sendMessage(ChatColor.GRAY + "Deposited " + ChatColor.GOLD + amt + " <glyph:coins_icon>");
                                }
                                GuildManager.getInstance().save();
                            }
                    ))
                    .withLocalEcho(false)
                    .addConversationAbandonedListener(c -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player)));
            factory.buildConversation(player).begin();
        } else if (clickType.isRightClick()) {
            player.closeInventory();
            Main.getInstance().getLogger().info("[GuildVault] opening withdraw prompt for " + player.getName());
            ConversationFactory factory = new ConversationFactory(Main.getInstance())
                    .withFirstPrompt(IntegerInputPrompt.coinAmountWithinBalance(
                            Main.getInstance(),
                            player,
                            ChatColor.GOLD + "Enter amount to withdraw:",
                            g::getCoins,
                            true,
                            amt -> {
                                Main.getInstance().getLogger().info("[GuildVault] withdrawing " + amt + " for " + player.getName());
                                g.removeCoins(amt);
                                econ.addCoins(player, amt, false);
                                player.sendMessage(ChatColor.GRAY + "Withdrew " + ChatColor.GOLD + amt + " <glyph:coins_icon>");
                                GuildManager.getInstance().save();
                            }
                    ))
                    .withLocalEcho(false)
                    .addConversationAbandonedListener(c -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player)));
            factory.buildConversation(player).begin();
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return false;
        }
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
