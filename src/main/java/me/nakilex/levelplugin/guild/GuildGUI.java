package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuildGUI {

    private final GuildManager manager;
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();
    private static final int SIZE = 54;
    private static final String TITLE = "Guilds";
    private static final int[] GUILD_SLOTS = GuiUtil.PAGED_SLOTS;

    public GuildGUI(GuildManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        boolean noGuild = manager.getGuild(player.getUniqueId()) == null;
        List<Guild> guilds = new ArrayList<>(manager.getGuilds());
        List<GuiWidget> widgets = buildWidgets(player, guilds, noGuild);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
        player.openInventory(inv);
    }

    private List<GuiWidget> buildWidgets(Player player, List<Guild> guilds, boolean noGuild) {
        List<GuiWidget> widgets = new ArrayList<>();
        int limit = Math.min(GUILD_SLOTS.length, guilds.size());
        for (int i = 0; i < limit; i++) {
            Guild guild = guilds.get(i);
            int slot = GUILD_SLOTS[i];
            widgets.add(new ActionWidget(slot,
                    context -> createGuildItem(context.player(), guild, noGuild),
                    (click, context) -> handleGuildClick(context.player(), guild, noGuild, click)));
        }
        return widgets;
    }

    private ItemStack createGuildItem(Player player, Guild guild, boolean noGuild) {
        OfflinePlayer leader = Bukkit.getOfflinePlayer(guild.getLeader());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Leader: " + guild.getLeaderName());
        lore.add(ChatColor.WHITE + "Members: " + guild.getMembers().size());
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + guild.getLevel());
        int need = guild.getExpNeeded();
        if (need > 0) {
            lore.add(ChatColor.GRAY + "XP: " + ChatColor.YELLOW + guild.getExp() + ChatColor.GRAY + "/" + ChatColor.YELLOW + need);
        }
        lore.add(ChatColor.GREEN + "Allies: " + String.join(", ", guild.getAllies()));
        lore.add(ChatColor.RED + "Hostile: " + String.join(", ", guild.getHostiles()));
        if (noGuild) {
            if (guild.getApplicants().containsKey(player.getUniqueId())) {
                lore.add(ChatColor.GRAY + "Status: " + ChatColor.YELLOW + "Pending");
            } else {
                lore.addAll(TooltipUtil.clickInstructions("to apply", null));
            }
        }
        return HeadUtil.createPlayerHead(leader, ChatColor.GOLD + guild.getName(), lore);
    }

    private void handleGuildClick(Player player, Guild guild, boolean noGuild, ClickType click) {
        if (!noGuild || click != ClickType.LEFT) {
            return;
        }
        if (manager.apply(player.getUniqueId(), guild.getName())) {
            player.sendMessage(ChatColor.GREEN + "Applied to guild " + guild.getName() + ".");
            OfflinePlayer leader = Bukkit.getOfflinePlayer(guild.getLeader());
            if (leader.isOnline()) {
                ((Player) leader.getPlayer()).sendMessage(ChatColor.YELLOW + player.getName() + " applied to join your guild.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Could not apply to guild.");
        }
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    public boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
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
}
